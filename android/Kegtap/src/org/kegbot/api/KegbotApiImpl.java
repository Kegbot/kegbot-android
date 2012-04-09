package org.kegbot.api;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.kegbot.kegtap.Utils;
import org.kegbot.proto.Api.DrinkDetail;
import org.kegbot.proto.Api.DrinkSet;
import org.kegbot.proto.Api.KegDetail;
import org.kegbot.proto.Api.KegDetailSet;
import org.kegbot.proto.Api.RecordDrinkRequest;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Api.SessionDetail;
import org.kegbot.proto.Api.SessionSet;
import org.kegbot.proto.Api.SoundEventSet;
import org.kegbot.proto.Api.SystemEventDetailSet;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Api.TapDetailSet;
import org.kegbot.proto.Api.ThermoLogSet;
import org.kegbot.proto.Api.ThermoSensorSet;
import org.kegbot.proto.Api.UserDetail;
import org.kegbot.proto.Api.UserDetailSet;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.ThermoLog;

import android.os.SystemClock;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

public class KegbotApiImpl implements KegbotApi {

  private static KegbotApiImpl sSingleton = null;

  private String mBaseUrl;

  private String apiKey = null;

  private long mLastApiAttemptUptimeMillis = -1;
  private long mLastApiSuccessUptimeMillis = -1;
  private long mLastApiFailureUptimeMillis = -1;

  private final CookieStore mCookieStore = new BasicCookieStore();

  private ClientConnectionManager mConnManager;
  private HttpParams mHttpParams;
  private DefaultHttpClient mHttpClient;

  public static interface Listener {
    public void debug(String message);
  }

  private Listener mListener = null;

  public KegbotApiImpl() {
    mBaseUrl = "http://localhost/";

    mHttpParams = new BasicHttpParams();
    HttpProtocolParams.setVersion(mHttpParams, HttpVersion.HTTP_1_1);
    HttpProtocolParams.setContentCharset(mHttpParams, HTTP.DEFAULT_CONTENT_CHARSET);
    HttpProtocolParams.setUseExpectContinue(mHttpParams, true);

    SchemeRegistry registry = new SchemeRegistry();
    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    //registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

    mConnManager = new ThreadSafeClientConnManager(mHttpParams, registry);
    mHttpClient = new DefaultHttpClient(mConnManager, mHttpParams);

  }

  public synchronized void setListener(Listener listener) {
    mListener = listener;
  }

  private void noteApiAttempt() {
    mLastApiAttemptUptimeMillis = SystemClock.uptimeMillis();
  }

  private void noteApiSuccess() {
    mLastApiSuccessUptimeMillis = SystemClock.uptimeMillis();
  }

  private void noteApiFailure() {
    mLastApiFailureUptimeMillis = SystemClock.uptimeMillis();
  }

  private JsonNode toJson(HttpResponse response) throws KegbotApiException {
    final Header header = response.getFirstHeader("Content-type");
    if (header == null) {
      throw new KegbotApiServerError("No content-type header.");
    }
    final String contentType = header.getValue();
    if (!"application/json".equals(contentType)) {
      throw new KegbotApiServerError("Unknown content-type: " + contentType);
    }
    try {
      final ObjectMapper mapper = new ObjectMapper();
      final JsonNode rootNode = mapper.readValue(response.getEntity().getContent(), JsonNode.class);
      return rootNode;
    } catch (IOException e) {
      throw new KegbotApiException(e);
    }
  }

  private JsonNode readResponse(HttpURLConnection conn) throws KegbotApiException {
    try {
      int responseCode = conn.getResponseCode();

      String contentType = conn.getContentType();
      if (!"application/json".equals(contentType)) {
        throw new KegbotApiServerError("Unknown content-type: " + contentType);
      }

      InputStream is = conn.getInputStream();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] bytes = new byte[1024];
      int bytesRead;
      while ((bytesRead = is.read(bytes)) != -1) {
          baos.write(bytes, 0, bytesRead);
      }
      byte[] bytesReceived = baos.toByteArray();
      baos.close();
      is.close();
      String response = new String(bytesReceived);

      final ObjectMapper mapper = new ObjectMapper();
      final JsonNode rootNode = mapper.readValue(response, JsonNode.class);
      return rootNode;
    } catch (IOException e) {
      throw new KegbotApiServerError("IOException during response: " + e.toString(), e);
    } finally {
      conn.disconnect();
    }
  }

  private String getRequestUrl(String path) {
    return mBaseUrl + path;
  }

  private JsonNode doGet(String path) throws KegbotApiException {
    return doGet(path, null);
  }

  private JsonNode doGet(String path, List<NameValuePair> params) throws KegbotApiException {
    final List<NameValuePair> actualParams = Lists.newArrayList();
    if (params != null) {
      actualParams.addAll(params);
    }
    if (!Strings.isNullOrEmpty(apiKey)) {
      actualParams.add(new BasicNameValuePair("api_key", apiKey));
    }
    if (!actualParams.isEmpty()) {
      path += "?" + URLEncodedUtils.format(actualParams, "utf-8");
    }

    final HttpGet request = new HttpGet(getRequestUrl(path));
    debug("GET: uri=" + request.getURI());
    debug("GET: request=" + request.getRequestLine());
    return toJson(execute(request));
  }

  private JsonNode doPost(String path, Map<String, String> params) throws KegbotApiException {
    if (apiKey != null) {
      debug("POST: api_key=" + apiKey);
      params.put("api_key", apiKey);
    }
    return toJson(doRawPost(path, params));
  }

  private HttpResponse doRawPost(String path, Map<String, String> params) throws KegbotApiException {
    HttpPost request = new HttpPost(getRequestUrl(path));
    debug("POST: uri=" + request.getURI());
    debug("POST: request=" + request.getRequestLine());
    List<NameValuePair> pairs = Lists.newArrayList();
    for (Map.Entry<String, String> entry : params.entrySet()) {
      pairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
    }
    try {
      request.setEntity(new UrlEncodedFormEntity(pairs));
    } catch (UnsupportedEncodingException e) {
      throw new KegbotApiException(e);
    }
    return execute(request);
  }

  private HttpResponse execute(HttpUriRequest request) throws KegbotApiException {
    noteApiAttempt();
    boolean success = false;
    try {
      final HttpResponse response;
      debug("Requesting: " + request.getURI());

      //HttpContext localContext = new BasicHttpContext();
      //localContext.setAttribute(ClientContext.COOKIE_STORE, mCookieStore);

      response = mHttpClient.execute(request);
      debug("DONE: " + request.getURI());
      debug(response.getStatusLine().toString());
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        String reason = response.getStatusLine().getReasonPhrase();
        try {
          final JsonNode responseJson = toJson(response);
          final JsonNode errorNode = responseJson.get("error");
          if (errorNode != null) {
            final JsonNode messageNode = errorNode.get("message");
            if (messageNode != null) {
              reason = reason + " (" + messageNode.getTextValue() + ")";
            }
          }
        } catch (KegbotApiException e) {
          // Pass
        }
        final String message = "Error fetching " + request.getURI() + ": statusCode=" + statusCode
            + ", reason=" + reason;

        switch (statusCode) {
          case HttpStatus.SC_NOT_FOUND:
            throw new KegbotApiNotFoundError(message);
          default:
            throw new KegbotApiServerError(message);
        }
      }
      success = true;
      return response;
    } catch (ClientProtocolException e) {
      throw new KegbotApiException(e);
    } catch (IOException e) {
      throw new KegbotApiException(e);
    } finally {
      if (success) {
        noteApiSuccess();
      } else {
        debug("Method failed, aborting request.");
        noteApiFailure();
        request.abort();
      }
      //client.close();
    }
  }

  private JsonNode getJson(String path) throws KegbotApiException {
    JsonNode root = doGet(path);
    if (!root.has("result")) {
      throw new KegbotApiServerError("No result from server!");
    }
    return root.get("result");
  }

  private JsonNode getJson(String path, List<NameValuePair> params) throws KegbotApiException {
    JsonNode root = doGet(path, params);
    if (!root.has("result")) {
      throw new KegbotApiServerError("No result from server!");
    }
    return root.get("result");
  }

  private Message getProto(String path, Builder builder) throws KegbotApiException {
    return ProtoEncoder.toProto(builder, getJson(path)).build();
  }

  private Message getProto(String path, Builder builder, List<NameValuePair> params) throws KegbotApiException {
    return ProtoEncoder.toProto(builder, getJson(path, params)).build();
  }

  private JsonNode postJson(String path, Map<String, String> params) throws KegbotApiException {
    JsonNode root = doPost(path, params);
    if (!root.has("result")) {
      throw new KegbotApiServerError("No result from server!");
    }
    return root.get("result");
  }

  private Message postProto(String path, Builder builder, Map<String, String> params)
      throws KegbotApiException {
    return ProtoEncoder.toProto(builder, postJson(path, params)).build();
  }

  @Override
  public void setApiUrl(String apiUrl) {
    mBaseUrl = apiUrl;
  }

  @Override
  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
    debug("Set apiKey=" + apiKey);
  }

  @Override
  public void login(String username, String password) throws KegbotApiException {
    Map<String, String> params = Maps.newLinkedHashMap();
    params.put("username", username);
    params.put("password", password);
    debug("Logging in as username=" + username + " password=XXX");
    doPost("/login/", params);
  }

  @Override
  public String getApiKey() throws KegbotApiException {
    JsonNode result = getJson("/get-api-key/");
    final JsonNode keyNode = result.get("api_key");
    if (keyNode != null) {
      debug("Got api key:" + keyNode.getValueAsText());
      return keyNode.getValueAsText();
    }
    throw new KegbotApiServerError("Invalid response.");
  }

  @Override
  public KegDetailSet getAllKegs() throws KegbotApiException {
    return (KegDetailSet) getProto("/kegs/", KegDetailSet.newBuilder());
  }

  @Override
  public SoundEventSet getAllSoundEvents() throws KegbotApiException {
    return (SoundEventSet) getProto("/sound-events/", SoundEventSet.newBuilder());
  }

  @Override
  public TapDetailSet getAllTaps() throws KegbotApiException {
    return (TapDetailSet) getProto("/taps/", TapDetailSet.newBuilder());
  }

  @Override
  public AuthenticationToken getAuthToken(String authDevice, String tokenValue)
      throws KegbotApiException {
    return (AuthenticationToken) getProto("/auth-tokens/" + authDevice + "." + tokenValue + "/",
        AuthenticationToken.newBuilder());
  }

  @Override
  public DrinkDetail getDrinkDetail(String id) throws KegbotApiException {
    return (DrinkDetail) getProto("/drinks/" + id, DrinkDetail.newBuilder());
  }

  @Override
  public KegDetail getKegDetail(String id) throws KegbotApiException {
    return (KegDetail) getProto("/kegs/" + id, KegDetail.newBuilder());
  }

  @Override
  public DrinkSet getKegDrinks(String kegId) throws KegbotApiException {
    return (DrinkSet) getProto("/kegs/" + kegId + "/drinks/", DrinkSet.newBuilder());
  }

  @Override
  public SystemEventDetailSet getKegEvents(String kegId) throws KegbotApiException {
    return (SystemEventDetailSet) getProto("/keg/" + kegId + "/events/", SystemEventDetailSet
        .newBuilder());
  }

  @Override
  public SessionSet getKegSessions(String kegId) throws KegbotApiException {
    return (SessionSet) getProto("/kegs/" + kegId + "/sessions/", SessionSet.newBuilder());
  }

  @Override
  public String getLastDrinkId() throws KegbotApiException {
    JsonNode root = getJson("/last-drink-id/");
    return root.get("id").getTextValue();
  }

  @Override
  public DrinkSet getRecentDrinks() throws KegbotApiException {
    return (DrinkSet) getProto("/last-drinks/", DrinkSet.newBuilder());
  }

  @Override
  public SystemEventDetailSet getRecentEvents() throws KegbotApiException {
    return (SystemEventDetailSet) getProto("/events/", SystemEventDetailSet.newBuilder());
  }

  @Override
  public SystemEventDetailSet getRecentEvents(final long sinceEventId) throws KegbotApiException {
    final List<NameValuePair> params = Lists.newArrayList();
    params.add(new BasicNameValuePair("since", String.valueOf(sinceEventId)));
    return (SystemEventDetailSet) getProto("/events/", SystemEventDetailSet.newBuilder(), params);
  }

  @Override
  public SessionDetail getSessionDetail(String id) throws KegbotApiException {
    return (SessionDetail) getProto("/sessions/" + id, SessionDetail.newBuilder());
  }

  @Override
  public TapDetail getTapDetail(String tapName) throws KegbotApiException {
    return (TapDetail) getProto("/taps/" + tapName, TapDetail.newBuilder());
  }

  @Override
  public ThermoLogSet getThermoSensorLogs(String sensorId) throws KegbotApiException {
    return (ThermoLogSet) getProto("/thermo-sensors/" + sensorId + "/logs/", ThermoLogSet
        .newBuilder());
  }

  @Override
  public ThermoSensorSet getThermoSensors() throws KegbotApiException {
    return (ThermoSensorSet) getProto("/thermo-sensors/", ThermoSensorSet.newBuilder());
  }

  @Override
  public UserDetail getUserDetail(String username) throws KegbotApiException {
    return (UserDetail) getProto("/users/" + username, UserDetail.newBuilder());
  }

  @Override
  public DrinkSet getUserDrinks(String username) throws KegbotApiException {
    return (DrinkSet) getProto("/users/" + username + "/drinks/", DrinkSet.newBuilder());
  }

  @Override
  public SystemEventDetailSet getUserEvents(String username) throws KegbotApiException {
    return (SystemEventDetailSet) getProto("/users/" + username + "/events/", SystemEventDetailSet
        .newBuilder());
  }

  @Override
  public UserDetailSet getUsers() throws KegbotApiException {
    return (UserDetailSet) getProto("/users/", UserDetailSet.newBuilder());
  }

  @Override
  public SessionSet getCurrentSessions() throws KegbotApiException {
    return (SessionSet) getProto("/sessions/current/", SessionSet.newBuilder());
  }

  @Override
  public Drink recordDrink(final RecordDrinkRequest request) throws KegbotApiException {
    if (!request.isInitialized()) {
      throw new KegbotApiException("Request is missing required field(s)");
    }

    final Map<String, String> params = Maps.newLinkedHashMap();

    final String tapName = request.getTapName();
    final int ticks = request.getTicks();

    params.put("ticks", String.valueOf(ticks));

    final float volumeMl = request.getVolumeMl();
    if (volumeMl > 0) {
      params.put("volume_ml", String.valueOf(volumeMl));
    }

    final String username = request.getUsername();
    if (!Strings.isNullOrEmpty(username)) {
      params.put("username", username);
    }

    final String recordDate = request.getRecordDate();
    boolean haveDate = false;
    if (!Strings.isNullOrEmpty(recordDate)) {
      try {
        params.put("record_date", recordDate); // new API
        haveDate = true;
        final long pourTime = Utils.dateFromIso8601String(recordDate);
        params.put("pour_time", String.valueOf(pourTime)); // old API
      } catch (ParseException e) {
        // Ignore.
      }
    }

    final int secondsAgo = request.getSecondsAgo();
    if (!haveDate & secondsAgo > 0) {
      params.put("seconds_ago", String.valueOf(secondsAgo));
    }

    final int durationSeconds = request.getDurationSeconds();
    if (durationSeconds > 0) {
      params.put("duration_seconds", String.valueOf(durationSeconds));
    }

    final boolean spilled = request.getSpilled();
    if (spilled) {
      params.put("spilled", String.valueOf(spilled));
    }
    return (Drink) postProto("/taps/" + tapName, Drink.newBuilder(), params);
  }

  @Override
  public ThermoLog recordTemperature(final RecordTemperatureRequest request)
      throws KegbotApiException {
    if (!request.isInitialized()) {
      throw new KegbotApiException("Request is missing required field(s)");
    }

    final String sensorName = request.getSensorName();
    final String sensorValue = String.valueOf(request.getTempC());

    final Map<String, String> params = Maps.newLinkedHashMap();
    params.put("temp_c", sensorValue);
    return (ThermoLog) postProto("/thermo-sensors/" + sensorName, ThermoLog.newBuilder(), params);
  }

  private HttpURLConnection newConnection(String urlString) throws IOException {
    final URL url = new URL(urlString);
    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    return conn;
  }

  private Image uploadDrinkImageALT(String drinkId, String imagePath) throws KegbotApiException {
    HttpURLConnection conn;
    try {
      conn = newConnection(getRequestUrl("/drinks/" + drinkId + "/add-photo/"));
    } catch (IOException e) {
      throw new KegbotApiException("Error connecting to URL: " + e.toString(), e);
    }

    byte[] fileBytes;
    try {
      fileBytes = Utils.readFile(imagePath);
    } catch (IOException e1) {
      throw new KegbotApiException("Error reading image file: " + e1.toString(), e1);
    }

    final String BOUNDRY = "############apiboundary############";

    conn.setDoOutput(true);
    conn.setDoInput(true);
    conn.setUseCaches(false);
    try {
      conn.setRequestMethod("POST");
    } catch (ProtocolException e1) {
      throw new IllegalStateException("Request method unsupported", e1);
    }
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDRY);

    String fileName = new File(imagePath).getName();

    StringBuffer requestBody = new StringBuffer();

    requestBody.append("--").append(BOUNDRY).append("\n");
    requestBody.append("Content-Disposition: form-data; name=\"photo\"; filename=\"" + fileName + "\"\r\n");
    requestBody.append("Content-Type: application/octet-stream\r\n");
    requestBody.append("\r\n");
    requestBody.append(new String(fileBytes));
    requestBody.append("\r\n");

    requestBody.append("--").append(BOUNDRY).append("\r\n");
    requestBody.append("Content-Disposition: form-data; name=\"api_key\"\r\n");
    requestBody.append("\r\n");
    requestBody.append(apiKey);
    requestBody.append("\r\n");

    requestBody.append("--").append(BOUNDRY).append("--").append("\r\n");

    DataOutputStream outputStream;
    try {
      outputStream = new DataOutputStream(conn.getOutputStream());
      outputStream.writeBytes(requestBody.toString());
      outputStream.flush();
      outputStream.close();
    } catch (IOException e1) {
      throw new KegbotApiException(e1);
    }

    debug("api_key=" + apiKey);

    final JsonNode responseJson = readResponse(conn);
    debug("UPLOAD RESPONSE: " + responseJson);
    return (Image) ProtoEncoder.toProto(Image.newBuilder(), responseJson.get("result")).build();
  }

  @Override
  public Image uploadDrinkImage(String drinkId, String imagePath) throws KegbotApiException {

    final File imageFile = new File(imagePath);
    final HttpPost httpost = new HttpPost(getRequestUrl("/drinks/" + drinkId + "/add-photo/"));
    MultipartEntity entity = new MultipartEntity();
    entity.addPart("photo", new FileBody(imageFile));
    if (apiKey != null) {
      try {
        entity.addPart("api_key", new StringBody(apiKey));
        debug("image upload set api key");
      } catch (UnsupportedEncodingException e) {
        debug("BAD API KEY");
      }
    }
    httpost.setEntity(entity);
    final HttpResponse response = execute(httpost);
    final JsonNode responseJson = toJson(response);
    debug("UPLOAD RESPONSE: " + responseJson);
    return (Image) ProtoEncoder.toProto(Image.newBuilder(), responseJson.get("result")).build();
  }

  private synchronized void debug(String message) {
    if (mListener != null) {
      mListener.debug(message);
    }
  }

  public static synchronized KegbotApiImpl getSingletonInstance() {
    if (sSingleton == null) {
      sSingleton = new KegbotApiImpl();
    }
    return sSingleton;
  }
}
