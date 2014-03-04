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

import android.content.Context;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message.Builder;
import com.squareup.okhttp.OkHttpClient;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.util.TimeSeries;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Controller;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.FlowMeter;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.SoundEvent;
import org.kegbot.proto.Models.SystemEvent;
import org.kegbot.proto.Models.ThermoLog;
import org.kegbot.proto.Models.User;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;

public class KegbotApiImpl implements Backend {

  private static final String TAG = KegbotApiImpl.class.getSimpleName();

  private final AppConfiguration mConfig;
  private final CookieManager mCookieManager;
  private final OkHttpClient mClient;
  private final Http mHttp;

  public KegbotApiImpl(AppConfiguration config) {
    mConfig = config;
    mCookieManager = new CookieManager();
    mClient = new OkHttpClient();

    //https://github.com/square/okhttp/issues/184
    SSLContext sslContext;
    try {
      sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, null, null);
    } catch (GeneralSecurityException e) {
      throw new AssertionError(); // The system has no TLS. Just give up.
    }
    mClient.setSslSocketFactory(sslContext.getSocketFactory());

    mClient.setCookieHandler(mCookieManager);
    CookieHandler.setDefault(mCookieManager);
    mHttp = new Http(mClient);
  }

  private String apiUrl() {
    return Strings.nullToEmpty(mConfig.getApiUrl());
  }

  private String apiKey() {
    return Strings.nullToEmpty(mConfig.getApiKey());
  }

  @Override
  public void start(Context context) {
    // TODO: implement me
  }

  private String getRequestUrl(String path) {
    return apiUrl() + path;
  }

  private Request.Builder newRequest(String apiPath) {
    final Request.Builder builder = Request.newBuilder(getRequestUrl(apiPath));
    builder.addHeader("X-Kegbot-Api-Key", apiKey());
    return builder;
  }

  private JsonNode requestJson(Request request) throws KegbotApiException {
    JsonNode root;
    try {
      root = mHttp.requestJson(request);
    } catch (IOException e) {
      throw new KegbotApiException(e);
    } catch (NullPointerException e) {
      // TODO(mikey): Figure out why OkHttp NPE's.
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

  public void login(String username, String password) throws KegbotApiException {
    Map<String, String> params = Maps.newLinkedHashMap();
    params.put("username", username);
    params.put("password", password);
    debug("Logging in as username=" + username + " password=XXX");
    postJson("/login/", params);
  }

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

    final String apiKey = keyNode.getValueAsText();
    debug("Got api key:" + apiKey);
    mConfig.setApiKey(apiKey);
    return apiKey();
  }

  public List<Keg> getAllKegs() throws KegbotApiException {
    return getProto("/kegs/", Keg.newBuilder());
  }

  @Override
  public List<SoundEvent> getSoundEvents() throws KegbotApiException {
    return getProto("/sound-events/", SoundEvent.newBuilder());
  }

  @Override
  public List<KegTap> getTaps() throws KegbotApiException {
    return getProto("/taps/", KegTap.newBuilder());
  }

  @Override
  public KegTap createTap(String meterName, double mlPerTick, String relayName, String description)
      throws BackendException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void removeTap(String meterName) {
    // TODO Auto-generated method stub

  }

  @Override
  public AuthenticationToken getAuthToken(String authDevice, String tokenValue)
      throws KegbotApiException {
    return getSingleProto("/auth-tokens/" + authDevice + "/" + tokenValue + "/",
        AuthenticationToken.newBuilder());
  }

  @Override
  public Keg endKeg(int kegId) throws KegbotApiException {
    return (Keg) postProto("/kegs/" + kegId + "/end/", Keg.newBuilder(), null);
  }

  @Override
  public KegTap startKeg(String tapName, String beerName, String brewerName, String styleName,
      String kegType) throws KegbotApiException {
    final Request.Builder builder = newRequest("/taps/" + tapName + "/activate/")
        .setMethod(Http.POST)
        .addParameter("beer_name", beerName)
        .addParameter("brewer_name", brewerName)
        .addParameter("style_name", styleName)
        .addParameter("keg_size", kegType);
    return getSingleProto(KegTap.newBuilder(), requestJson(builder.build()).get("object"));
  }

  @Override
  public List<SystemEvent> getEvents() throws KegbotApiException {
    return getProto("/events/", SystemEvent.newBuilder());
  }

  @Override
  public List<SystemEvent> getEventsSince(final long sinceEventId) throws KegbotApiException {
    final List<NameValuePair> params = Lists.newArrayList();
    params.add(new BasicNameValuePair("since", String.valueOf(sinceEventId)));
    return getProto("/events/", SystemEvent.newBuilder(), params);
  }

  @Override
  public JsonNode getSessionStats(int sessionId) throws KegbotApiException {
    return getJson("/sessions/" + sessionId + "/stats/", null).get("object");
  }

  @Override
  public KegTap setTapMlPerTick(String tapName, double mlPerTick) throws KegbotApiException {
    final Request.Builder builder = newRequest("/taps/" + tapName + "/calibrate/")
        .setMethod(Http.POST)
        .addParameter("ml_per_tick", Double.valueOf(mlPerTick).toString());
    return getSingleProto(KegTap.newBuilder(), requestJson(builder.build()).get("object"));
  }

  @Override
  public User getUser(String username) throws KegbotApiException {
    return getSingleProto("/users/" + username, User.newBuilder());
  }

  @Override
  public List<User> getUsers() throws KegbotApiException {
    return getProto("/users/", User.newBuilder());
  }

  @Override
  public Session getCurrentSession() throws KegbotApiException {
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
  }

  @Override
  public Drink recordDrink(String tapName, long volumeMl, long ticks,
      @Nullable String shout, @Nullable String username, @Nullable String recordDate, long durationMillis,
      @Nullable TimeSeries timeSeries, @Nullable File picture) throws BackendException {

    final Request.Builder builder = newRequest("/taps/" + tapName)
        .setMethod(Http.POST)
        .addParameter("ticks", String.valueOf(ticks));

    if (volumeMl > 0) {
      builder.addParameter("volume_ml", String.valueOf(volumeMl));
    }

    if (!Strings.isNullOrEmpty(username)) {
      builder.addParameter("username", username);
    }

    if (!Strings.isNullOrEmpty(recordDate)) {
      try {
        builder.addParameter("record_date", recordDate); // new API
      } catch (IllegalArgumentException e) {
        // Ignore.
      }
    }

    if (durationMillis > 0) {
      // TODO: Fix API to report this in millis.
      builder.addParameter("duration", String.valueOf(durationMillis / 1000));
    }

    // TODO: Handle spilled.

    if (!Strings.isNullOrEmpty(shout)) {
      builder.addParameter("shout", shout);
    }

    if (timeSeries != null) {
      builder.addParameter("tick_time_series", timeSeries.asString());
    }

    if (picture != null) {
      builder.addFile("photo", picture);
    }

    return getSingleProto(Drink.newBuilder(), requestJson(builder.build()).get("object"));
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
  public Image attachPictureToDrink(int drinkId, File picture) throws KegbotApiException {
    final String url = String.format("/drinks/%s/add-photo/", Integer.valueOf(drinkId));

    final Request.Builder builder = newRequest(url)
        .setMethod(Http.POST)
        .addFile("photo", picture);

    return getSingleProto(Image.newBuilder(), requestJson(builder.build()).get("object"));
  }

  @Override
  public User createUser(String username, String email, String password, String imagePath)
      throws KegbotApiException {
    final Request.Builder builder = newRequest("/new-user/")
        .setMethod(Http.POST)
        .addParameter("username", username)
        .addParameter("email", email);

    if (!Strings.isNullOrEmpty(password)) {
        builder.addParameter("password", password);
    }

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

  @Override
  public List<Controller> getControllers() throws BackendException {
    return getProto("/controllers/", Controller.newBuilder());
  }

  @Override
  public Controller updateController(Controller controller) throws BackendException {
    final Map<String, String> params = Maps.newLinkedHashMap();
    params.put("name", controller.getName());
    if (controller.hasSerialNumber()) {
      params.put("serial_number", controller.getSerialNumber());
    }
    if (controller.hasModelName()) {
      params.put("model_name", controller.getModelName());
    }

    return (Controller) postProto("/controllers/" + controller.getId(), Controller.newBuilder(),
        params);
  }

  @Override
  public List<FlowMeter> getFlowMeters() throws BackendException {
    return getProto("/flow-meters/", FlowMeter.newBuilder());
  }

  @Override
  public FlowMeter updateFlowMeter(FlowMeter flowMeter) throws BackendException {
    final Map<String, String> params = Maps.newLinkedHashMap();
    params.put("port_name", flowMeter.getPortName());

    return (FlowMeter) postProto("/flow-meters/" + flowMeter.getId(), FlowMeter.newBuilder(),
        params);
  }

  private synchronized void debug(String message) {
    Log.d(TAG, message);
  }

  @Override
  public Controller createController(String name, String serialNumber, String deviceType)
      throws BackendException {
    final Map<String, String> params = Maps.newLinkedHashMap();
    params.put("name", name);
    if (!Strings.isNullOrEmpty(serialNumber)) {
      params.put("serial_number", serialNumber);
    }
    if (!Strings.isNullOrEmpty(deviceType)) {
      params.put("model_name", deviceType);
    }

    return (Controller) postProto("/controllers/", Controller.newBuilder(), params);
  }

  @Override
  public FlowMeter createFlowMeter(Controller controller, String portName, double ticksPerMl) throws BackendException {
    final Map<String, String> params = Maps.newLinkedHashMap();
    params.put("controller", String.valueOf(controller.getId()));
    params.put("port_name", portName);
    params.put("ticks_per_ml", String.valueOf(ticksPerMl));
    return (FlowMeter) postProto("/flow-meters/", FlowMeter.newBuilder(), params);
  }

}
