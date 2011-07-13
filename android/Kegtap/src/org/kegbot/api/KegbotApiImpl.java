package org.kegbot.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.kegbot.proto.Api.DrinkDetail;
import org.kegbot.proto.Api.DrinkDetailHtmlSet;
import org.kegbot.proto.Api.DrinkSet;
import org.kegbot.proto.Api.KegDetail;
import org.kegbot.proto.Api.KegSet;
import org.kegbot.proto.Api.SessionDetail;
import org.kegbot.proto.Api.SessionSet;
import org.kegbot.proto.Api.SoundEventSet;
import org.kegbot.proto.Api.SystemEventDetailSet;
import org.kegbot.proto.Api.SystemEventHtmlSet;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Api.TapDetailSet;
import org.kegbot.proto.Api.ThermoLogSet;
import org.kegbot.proto.Api.ThermoSensorSet;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.ThermoLog;
import org.kegbot.proto.Models.User;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

public class KegbotApiImpl implements KegbotApi {

  private final HttpClient mHttpClient;
  private String mBaseUrl;
  private String mUsername = "";
  private String mPassword = "";

  private String apiKey = null;

  public static interface Listener {
    public void debug(String message);
  }

  private Listener mListener = null;

  public KegbotApiImpl(HttpClient httpClient, String baseUrl) {
    mHttpClient = httpClient;
    mBaseUrl = baseUrl;
  }

  public synchronized void setListener(Listener listener) {
    mListener = listener;
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

  private String getRequestUrl(String path) {
    return mBaseUrl + path;
  }

  private JsonNode doGet(String path) throws KegbotApiException {
    HttpGet request = new HttpGet(getRequestUrl(path));
    debug("GET: uri=" + request.getURI());
    debug("GET: request=" + request.getRequestLine());
    return toJson(execute(request));
  }

  private JsonNode doPost(String path, Map<String, String> params) throws KegbotApiException {
    if (apiKey != null) {
      params.put("api_key", apiKey);
    }
    return toJson(doRawPost(path, params));
  }

  private HttpResponse doRawPost(String path, Map<String, String> params)
  throws KegbotApiException {
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

  private HttpResponse execute(HttpUriRequest request)
  throws KegbotApiException {
    try {
      final HttpResponse response;
      synchronized (mHttpClient) {
        debug("Requesting: " + request.getURI());
        response = mHttpClient.execute(request);
        debug("DONE: " + request.getURI());
        debug(response.getStatusLine().toString());
      }
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        final String reason = response.getStatusLine().getReasonPhrase();
        final String message = "Error fetching " + request.getURI() + ": statusCode=" + statusCode
            + ", reason=" + reason;

        switch (statusCode) {
        case HttpStatus.SC_NOT_FOUND:
          throw new KegbotApiNotFoundError(message);
        default:
          throw new KegbotApiServerError(message);
        }
      }
      return response;
    } catch (ClientProtocolException e) {
      throw new KegbotApiException(e);
    } catch (IOException e) {
      throw new KegbotApiException(e);
    } finally {
    }
  }

  private JsonNode getJson(String path) throws KegbotApiException {
    JsonNode root = doGet(path);
    if (!root.has("result")) {
      throw new KegbotApiServerError("No result from server!");
    }
    return root.get("result");
  }

  private Message getProto(String path, Builder builder)
  throws KegbotApiException {
    return ProtoEncoder.toProto(builder, getJson(path)).build();
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
  public boolean setAccountCredentials(String username, String password) {
    this.mUsername = username;
    this.mPassword = password;
    return true;
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

  private void login() throws KegbotApiException {
    if (apiKey == null || apiKey.isEmpty()) {
      Map<String, String> params = Maps.newLinkedHashMap();
      params.put("username", mUsername);
      params.put("password", mPassword);
      // TODO: removeme!
      debug("Logging in as username=" + mUsername + " password=" + mPassword);
      doPost("/login/", params);

      // Made it this far -- login succeeded.
      JsonNode result = getJson("/get-api-key/");
      final JsonNode keyNode = result.get("api_key");
      if (keyNode != null) {
        setApiKey(keyNode.getValueAsText());
      }
    }
    /*
     * final int statusCode = response.getStatusLine().getStatusCode(); throw
     * new IllegalStateException("Got status code: " + statusCode);
     */
  }

  @Override
  public KegSet getAllKegs() throws KegbotApiException {
    return (KegSet) getProto("/kegs/", KegSet.newBuilder());
  }

  @Override
  public SoundEventSet getAllSoundEvents() throws KegbotApiException {
    return (SoundEventSet) getProto("/sound-events/",
        SoundEventSet.newBuilder());
  }

  @Override
  public TapDetailSet getAllTaps() throws KegbotApiException {
    return (TapDetailSet) getProto("/taps/", TapDetailSet.newBuilder());
  }

  @Override
  public AuthenticationToken getAuthToken(String authDevice, String tokenValue)
  throws KegbotApiException {
    return (AuthenticationToken) getProto("/auth-tokens/" + authDevice + "."
        + tokenValue + "/", AuthenticationToken.newBuilder());
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
    return (DrinkSet) getProto("/kegs/" + kegId + "/drinks/",
        DrinkSet.newBuilder());
  }

  @Override
  public SystemEventDetailSet getKegEvents(String kegId) throws KegbotApiException {
    return (SystemEventDetailSet) getProto("/keg/" + kegId + "/events/",
        SystemEventDetailSet.newBuilder());
  }

  @Override
  public SessionSet getKegSessions(String kegId) throws KegbotApiException {
    return (SessionSet) getProto("/kegs/" + kegId + "/sessions/",
        SessionSet.newBuilder());
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
  public DrinkDetailHtmlSet getRecentDrinksHtml() throws KegbotApiException {
    return (DrinkDetailHtmlSet) getProto("/last-drinks-html/",
        DrinkDetailHtmlSet.newBuilder());
  }

  @Override
  public SystemEventDetailSet getRecentEvents() throws KegbotApiException {
    return (SystemEventDetailSet) getProto("/events/", SystemEventDetailSet.newBuilder());
  }

  @Override
  public SystemEventHtmlSet getRecentEventsHtml() throws KegbotApiException {
    return (SystemEventHtmlSet) getProto("/events/html/",
        SystemEventHtmlSet.newBuilder());
  }

  @Override
  public SessionDetail getSessionDetail(String id) throws KegbotApiException {
    return (SessionDetail) getProto("/sessions/" + id,
        SessionDetail.newBuilder());
  }

  @Override
  public TapDetail getTapDetail(String tapName) throws KegbotApiException {
    return (TapDetail) getProto("/taps/" + tapName, TapDetail.newBuilder());
  }

  @Override
  public ThermoLogSet getThermoSensorLogs(String sensorId)
  throws KegbotApiException {
    return (ThermoLogSet) getProto("/thermo-sensors/" + sensorId + "/logs/",
        ThermoLogSet.newBuilder());
  }

  @Override
  public ThermoSensorSet getThermoSensors() throws KegbotApiException {
    return (ThermoSensorSet) getProto("/thermo-sensors/",
        ThermoSensorSet.newBuilder());
  }

  @Override
  public User getUser(String username) throws KegbotApiException {
    return (User) getProto("/users/" + username, TapDetail.newBuilder());
  }

  @Override
  public DrinkSet getUserDrinks(String username) throws KegbotApiException {
    return (DrinkSet) getProto("/users/" + username + "/drinks/",
        DrinkSet.newBuilder());
  }

  @Override
  public SystemEventDetailSet getUserEvents(String username)
  throws KegbotApiException {
    return (SystemEventDetailSet) getProto("/users/" + username + "/events/",
        SystemEventDetailSet.newBuilder());
  }

  @Override
  public Drink recordDrink(String tapName, int ticks) throws KegbotApiException {
    login();
    Map<String, String> params = Maps.newLinkedHashMap();
    params.put("ticks", String.valueOf(ticks));
    return (Drink) postProto("/taps/" + tapName, Drink.newBuilder(), params);
  }

  @Override
  public ThermoLog recordTemperature(String sensorName, double sensorValue)
      throws KegbotApiException {
    login();
    final Map<String, String> params = Maps.newLinkedHashMap();
    params.put("temp_c", String.valueOf(sensorValue));
    return (ThermoLog) postProto("/thermo-sensors/" + sensorName, ThermoLog.newBuilder(), params);
  }

  private synchronized void debug(String message) {
    if (mListener != null) {
      mListener.debug(message);
    }
  }

}
