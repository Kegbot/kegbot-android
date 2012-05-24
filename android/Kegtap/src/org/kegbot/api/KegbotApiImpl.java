/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.api;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.kegbot.app.Utils;
import org.kegbot.proto.Api.RecordDrinkRequest;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.SoundEvent;
import org.kegbot.proto.Models.Stats;
import org.kegbot.proto.Models.SystemEvent;
import org.kegbot.proto.Models.ThermoLog;
import org.kegbot.proto.Models.ThermoSensor;
import org.kegbot.proto.Models.User;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message.Builder;

public class KegbotApiImpl implements KegbotApi {

  private static KegbotApiImpl sSingleton = null;

  private static final String CONTENT_TYPE_JSON = "application/json";

  private String mBaseUrl;

  private String apiKey = null;

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
    HttpProtocolParams.setUserAgent(mHttpParams, Utils.getUserAgent());

    SchemeRegistry registry = new SchemeRegistry();
    registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    // registry.register(new Scheme("https",
    // SSLSocketFactory.getSocketFactory(), 443));

    mConnManager = new ThreadSafeClientConnManager(mHttpParams, registry);
    mHttpClient = new DefaultHttpClient(mConnManager, mHttpParams);
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
    if (Strings.isNullOrEmpty(contentType)
        || (!CONTENT_TYPE_JSON.equals(contentType) && !contentType.startsWith(CONTENT_TYPE_JSON))) {
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
    boolean success = false;
    try {
      final HttpResponse response;
      debug("Requesting: " + request.getURI());

      // HttpContext localContext = new BasicHttpContext();
      // localContext.setAttribute(ClientContext.COOKIE_STORE, mCookieStore);

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
      if (!success) {
        debug("Method failed, aborting request.");
        request.abort();
      }
      // client.close();
    }
  }

  private JsonNode getJson(String path, List<NameValuePair> params) throws KegbotApiException {
    JsonNode root = doGet(path, params);
    if (!root.has("meta")) {
      throw new KegbotApiServerError("Server result missing 'meta' object.");
    }
    return root;
  }

  private <T extends GeneratedMessage> List<T> getProto(String path, Builder builder) throws KegbotApiException {
    return getProto(path, builder, null);
  }

  private <T extends GeneratedMessage> List<T> getProto(String path, Builder builder, List<NameValuePair> params) throws KegbotApiException {
    JsonNode result = getJson(path, params);
    if (result.has("object")) {
      final T resultMessage = getSingleProto(builder, result.get("object"));
      return Collections.singletonList(resultMessage);
    } else {
      final List<T> results = Lists.newArrayList();
      final JsonNode objects = result.get("objects");
      final Iterator<JsonNode> iter = objects.getElements();
      while (iter.hasNext()) {
        @SuppressWarnings("unchecked")
        final T res = (T) getSingleProto(builder, iter.next());
        results.add(res);
      }
      return results;
    }
  }

  private <T extends GeneratedMessage> T getSingleProto(String path, Builder builder) throws KegbotApiException {
    return getSingleProto(path, builder, null);
  }

  private <T extends GeneratedMessage> T getSingleProto(String path, Builder builder, List<NameValuePair> params) throws KegbotApiException {
    JsonNode result = getJson(path, params);
    return getSingleProto(builder, result.get("object"));
  }

  private <T extends GeneratedMessage> T getSingleProto(Builder builder, JsonNode root) {
    builder.clear();
    @SuppressWarnings("unchecked")
    final T result = (T) ProtoEncoder.toProto(builder, root).build();
    return result;
  }

  private JsonNode postJson(String path, Map<String, String> params) throws KegbotApiException {
    JsonNode root = doPost(path, params);
    if (!root.has("meta")) {
      throw new KegbotApiServerError("No result from server!");
    }
    return root;
  }

  private <T extends GeneratedMessage> T postProto(String path, Builder builder, Map<String, String> params)
      throws KegbotApiException {
    JsonNode result = postJson(path, params);
    return getSingleProto(builder, result.get("object"));
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
    final JsonNode result = getJson("/get-api-key/", null);
    final JsonNode rootNode = result.get("object");
    if (rootNode == null) {
      throw new KegbotApiServerError("Invalid response.");
    }

    final JsonNode keyNode = rootNode.get("api_key");
    if (keyNode == null) {
      throw new KegbotApiServerError("Invalid response.");
    }

    debug("Got api key:" + keyNode.getValueAsText());
    return keyNode.getValueAsText();
  }

  @Override
  public List<Keg> getAllKegs() throws KegbotApiException {
    return getProto("/kegs/", Keg.newBuilder());
  }

  @Override
  public List<SoundEvent> getAllSoundEvents() throws KegbotApiException {
    return getProto("/sound-events/", SoundEvent.newBuilder());
  }

  @Override
  public List<KegTap> getAllTaps() throws KegbotApiException {
    return getProto("/taps/", KegTap.newBuilder());
  }

  @Override
  public AuthenticationToken getAuthToken(String authDevice, String tokenValue)
      throws KegbotApiException {
    return getSingleProto("/auth-tokens/" + authDevice + "/" + tokenValue + "/",
        AuthenticationToken.newBuilder());
  }

  @Override
  public Drink getDrinkDetail(String id) throws KegbotApiException {
    return getSingleProto("/drinks/" + id, Drink.newBuilder());
  }

  @Override
  public Keg getKegDetail(String id) throws KegbotApiException {
    return getSingleProto("/kegs/" + id, Keg.newBuilder());
  }

  @Override
  public List<Drink> getKegDrinks(String kegId) throws KegbotApiException {
    return getProto("/kegs/" + kegId + "/drinks/", Drink.newBuilder());
  }

  @Override
  public List<SystemEvent> getKegEvents(String kegId) throws KegbotApiException {
    return getProto("/keg/" + kegId + "/events/", SystemEvent.newBuilder());
  }

  @Override
  public List<Session> getKegSessions(String kegId) throws KegbotApiException {
    return getProto("/kegs/" + kegId + "/sessions/", Session.newBuilder());
  }

  @Override
  public String getLastDrinkId() throws KegbotApiException {
    JsonNode root = getJson("/last-drink-id/", null);
    return root.get("id").getTextValue();
  }

  @Override
  public List<Drink> getRecentDrinks() throws KegbotApiException {
    return getProto("/last-drinks/", Drink.newBuilder());
  }

  @Override
  public List<SystemEvent> getRecentEvents() throws KegbotApiException {
    return getProto("/events/", SystemEvent.newBuilder());
  }

  @Override
  public List<SystemEvent> getRecentEvents(final long sinceEventId) throws KegbotApiException {
    final List<NameValuePair> params = Lists.newArrayList();
    params.add(new BasicNameValuePair("since", String.valueOf(sinceEventId)));
    return getProto("/events/", SystemEvent.newBuilder(), params);
  }

  @Override
  public Session getSessionDetail(String id) throws KegbotApiException {
    return getSingleProto("/sessions/" + id, Session.newBuilder());
  }

  @Override
  public Stats getSessionStats(int sessionId) throws KegbotApiException {
    return getSingleProto("/sessions/" + sessionId + "/stats/", Stats.newBuilder());
  }

  @Override
  public KegTap getTapDetail(String tapName) throws KegbotApiException {
    return getSingleProto("/taps/" + tapName, KegTap.newBuilder());
  }

  @Override
  public List<ThermoLog> getThermoSensorLogs(String sensorId) throws KegbotApiException {
    return getProto("/thermo-sensors/" + sensorId + "/logs/", ThermoLog.newBuilder());
  }

  @Override
  public List<ThermoSensor> getThermoSensors() throws KegbotApiException {
    return getProto("/thermo-sensors/", ThermoSensor.newBuilder());
  }

  @Override
  public User getUserDetail(String username) throws KegbotApiException {
    return getSingleProto("/users/" + username, User.newBuilder());
  }

  @Override
  public List<Drink> getUserDrinks(String username) throws KegbotApiException {
    return getProto("/users/" + username + "/drinks/", Drink.newBuilder());
  }

  @Override
  public List<SystemEvent> getUserEvents(String username) throws KegbotApiException {
    return getProto("/users/" + username + "/events/", SystemEvent
        .newBuilder());
  }

  @Override
  public List<User> getUsers() throws KegbotApiException {
    return getProto("/users/", User.newBuilder());
  }

  @Override
  public Session getCurrentSession() throws KegbotApiException {
    try {
      final List<NameValuePair> params = Lists.newArrayList();
      params.add(new BasicNameValuePair("limit", "1"));
      final List<Session> sessions = getProto("/sessions/", Session.newBuilder(), params);
      if (sessions.isEmpty()) {
        return null;
      }
      final Session sess = sessions.get(0);
      if (sess.getIsActive()) {
        return sess;
      }
      return null;
    } catch (KegbotApiNotFoundError e) {
      return null;
    }
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
      params.put("duration", String.valueOf(durationSeconds));
    }

    final boolean spilled = request.getSpilled();
    if (spilled) {
      params.put("spilled", String.valueOf(spilled));
    }

    final String shout = request.getShout();
    if (!Strings.isNullOrEmpty(shout)) {
      params.put("shout", shout);
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

  @Override
  public Image uploadDrinkImage(int drinkId, String imagePath) throws KegbotApiException {

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
    return (Image) ProtoEncoder.toProto(Image.newBuilder(), responseJson.get("object")).build();
  }

  @Override
  public User register(String username, String email, String password, String imagePath)
      throws KegbotApiException {

    final File imageFile = new File(imagePath);
    final HttpPost httpost = new HttpPost(getRequestUrl("/new-user/"));
    MultipartEntity entity = new MultipartEntity();

    if (!Strings.isNullOrEmpty(imagePath)) {
      entity.addPart("photo", new FileBody(imageFile));
    }
    try {
      if (!Strings.isNullOrEmpty(apiKey)) {
        entity.addPart("api_key", new StringBody(apiKey));
      } else {
        throw new KegbotApiException("Need an API key");
      }
      entity.addPart("username", new StringBody(username));
      entity.addPart("email", new StringBody(email));
      entity.addPart("password", new StringBody(password));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    httpost.setEntity(entity);

    final HttpResponse response = execute(httpost);
    final JsonNode responseJson = toJson(response);
    debug("UPLOAD RESPONSE: " + responseJson);
    return getSingleProto(User.newBuilder(), responseJson.get("object"));
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
