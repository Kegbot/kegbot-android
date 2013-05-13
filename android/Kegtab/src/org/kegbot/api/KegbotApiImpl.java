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
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.codehaus.jackson.JsonNode;
import org.kegbot.app.util.DateUtils;
import org.kegbot.proto.Api.RecordDrinkRequest;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegSize;
import org.kegbot.proto.Models.KegTap;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.SoundEvent;
import org.kegbot.proto.Models.SystemEvent;
import org.kegbot.proto.Models.ThermoLog;
import org.kegbot.proto.Models.ThermoSensor;
import org.kegbot.proto.Models.User;

import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message.Builder;
import com.squareup.okhttp.OkHttpClient;

public class KegbotApiImpl implements KegbotApi {

  private static final String TAG = KegbotApiImpl.class.getSimpleName();

  //private static final String CONTENT_TYPE_JSON = "application/json";

  private String mBaseUrl;
  private String mApiKey;

  private HttpParams mHttpParams;

  private final CookieManager mCookieManager;
  private final OkHttpClient mClient;
  private final Http mHttp;

  public KegbotApiImpl() {
    mBaseUrl = "http://localhost/";
    mCookieManager = new CookieManager();
    mClient = new OkHttpClient();
    mClient.setCookieHandler(mCookieManager);
    CookieHandler.setDefault(mCookieManager);
    mHttp = new Http(mClient);
  }

  private String getRequestUrl(String path) {
    return mBaseUrl + path;
  }

  private Request.Builder newRequest(String apiPath) {
    final Request.Builder builder = Request.newBuilder(getRequestUrl(apiPath));
    if (mApiKey != null) {
      builder.addHeader("X-Kegbot-Api-Key", mApiKey);
    }
    return builder;
  }

  private JsonNode requestJson(Request request) throws KegbotApiException {
    JsonNode root;
    try {
      root = mHttp.requestJson(request);
    } catch (IOException e) {
      throw new KegbotApiException(e);
    }
    if (!root.has("meta")) {
      throw new KegbotApiServerError("No result from server!");
    }
    return root;
  }

  /** Convenience wrapper around {@link #requestJson(Request)}, using GET. */
  private JsonNode getJson(String path, List<NameValuePair> params) throws KegbotApiException {
    final Request.Builder request = newRequest(path);
    if (params != null) {
      for (final NameValuePair param : params) {
        request.addParameter(param.getName(), param.getValue());
      }
    }
    return requestJson(request.build());
  }

  /** Convenience wrapper around {@link #requestJson(Request)}, using POST. */
  private JsonNode postJson(String path, Map<String, String> params) throws KegbotApiException {
    final Request.Builder request = newRequest(path).
        setMethod(Http.POST);

    if (params != null) {
      for (final Map.Entry<String, String> param : params.entrySet()) {
        request.addParameter(param.getKey(), param.getValue());
      }
    }
    return requestJson(request.build());
  }

  private <T extends GeneratedMessage> List<T> getProto(String path, Builder builder)
      throws KegbotApiException {
    return getProto(path, builder, null);
  }

  private <T extends GeneratedMessage> List<T> getProto(String path, Builder builder,
      List<NameValuePair> params) throws KegbotApiException {
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

  private <T extends GeneratedMessage> T getSingleProto(String path, Builder builder)
      throws KegbotApiException {
    JsonNode result = getJson(path, null);
    return getSingleProto(builder, result.get("object"));
  }

  private <T extends GeneratedMessage> T getSingleProto(Builder builder, JsonNode root) {
    builder.clear();
    @SuppressWarnings("unchecked")
    final T result = (T) ProtoEncoder.toProto(builder, root).build();
    return result;
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
    mApiKey = apiKey;
  }

  @Override
  public void setUserAgent(String userAgent) {
    HttpProtocolParams.setUserAgent(mHttpParams, userAgent);
  }

  @Override
  public void login(String username, String password) throws KegbotApiException {
    Map<String, String> params = Maps.newLinkedHashMap();
    params.put("username", username);
    params.put("password", password);
    debug("Logging in as username=" + username + " password=XXX");
    postJson("/login/", params);
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
  public List<KegSize> getKegSizes() throws KegbotApiException {
    return getProto("/keg-sizes/", KegSize.newBuilder());
  }

  @Override
  public Keg endKeg(String id) throws KegbotApiException {
    return (Keg) postProto("/kegs/" + id + "/end/", Keg.newBuilder(), null);
  }

  @Override
  public KegTap activateKeg(String tapName, String beerName, String brewerName, String styleName,
      int kegSizeId) throws KegbotApiException {
    final Request.Builder builder = newRequest("/taps/" + tapName + "/activate/")
        .setMethod(Http.POST)
        .addParameter("beer_name", beerName)
        .addParameter("brewer_name", brewerName)
        .addParameter("style_name", styleName)
        .addParameter("keg_size", Integer.valueOf(kegSizeId).toString());
    return getSingleProto(KegTap.newBuilder(), requestJson(builder.build()).get("object"));
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
  public JsonNode getSessionStats(int sessionId) throws KegbotApiException {
    return getJson("/sessions/" + sessionId + "/stats/", null).get("object");
  }

  @Override
  public KegTap getTapDetail(String tapName) throws KegbotApiException {
    return getSingleProto("/taps/" + tapName, KegTap.newBuilder());
  }

  @Override
  public KegTap setTapMlPerTick(String tapName, double mlPerTick) throws KegbotApiException {
    final Request.Builder builder = newRequest("/taps/" + tapName + "/calibrate/")
        .setMethod(Http.POST)
        .addParameter("ml_per_tick", Double.valueOf(mlPerTick).toString());
    return getSingleProto(KegTap.newBuilder(), requestJson(builder.build()).get("object"));
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
        final long pourTime = DateUtils.dateFromIso8601String(recordDate);
        params.put("pour_time", String.valueOf(pourTime)); // old API
      } catch (IllegalArgumentException e) {
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

    final String tickTimeSeries = request.getTickTimeSeries();
    if (!Strings.isNullOrEmpty(tickTimeSeries)) {
      params.put("tick_time_series", tickTimeSeries);
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
    final String url = String.format("/drinks/%s/add-photo/", Integer.valueOf(drinkId));

    final Request.Builder builder = newRequest(url)
        .setMethod(Http.POST)
        .addFile("photo", new File(imagePath));

    return getSingleProto(Image.newBuilder(), requestJson(builder.build()).get("object"));
  }

  @Override
  public User register(String username, String email, String password, String imagePath)
      throws KegbotApiException {
    final Request.Builder builder = newRequest("/new-user/")
        .setMethod(Http.POST)
        .addParameter("username", username)
        .addParameter("email", email)
        .addParameter("password", password);

    if (!Strings.isNullOrEmpty(imagePath)) {
      builder.addFile("photo", new File(imagePath));
    }

    return getSingleProto(User.newBuilder(), requestJson(builder.build()).get("object"));
  }

  @Override
  public AuthenticationToken assignToken(String authDevice, String tokenValue, String username)
      throws KegbotApiException {
    final String url = "/auth-tokens/" + authDevice + "/" + tokenValue + "/assign/";
    final Request.Builder builder = newRequest(url)
        .setMethod(Http.POST)
        .addParameter("username", username);

    return getSingleProto(AuthenticationToken.newBuilder(),
        requestJson(builder.build()).get("object"));
  }

  private synchronized void debug(String message) {
    Log.d(TAG, message);
  }

}
