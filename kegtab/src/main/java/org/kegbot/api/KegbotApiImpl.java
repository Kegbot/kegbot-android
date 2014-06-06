/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
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
import android.os.SystemClock;
import android.util.Log;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message.Builder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.apache.http.NameValuePair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.kegbot.app.KegbotApplication;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.util.TimeSeries;
import org.kegbot.app.util.Utils;
import org.kegbot.app.util.Version;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Models;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Controller;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.FlowMeter;
import org.kegbot.proto.Models.FlowToggle;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.SoundEvent;
import org.kegbot.proto.Models.SystemEvent;
import org.kegbot.proto.Models.ThermoLog;
import org.kegbot.proto.Models.User;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class KegbotApiImpl implements Backend {

  private static final String TAG = KegbotApiImpl.class.getSimpleName();

  private static final byte[] HYPHENS = {'-', '-'};
  private static final byte[] CRLF = {'\r', '\n'};

  private final AppConfiguration mConfig;
  private final CookieManager mCookieManager;
  private final OkHttpClient mClient;
  private final String mUserAgent;

  public KegbotApiImpl(AppConfiguration config, String userAgent) {
    mConfig = config;
    mCookieManager = new CookieManager();
    mClient = new OkHttpClient();
    mClient.setCookieHandler(mCookieManager);
    CookieHandler.setDefault(mCookieManager);
    mUserAgent = userAgent;
  }

  public static KegbotApiImpl fromContext(Context context) {
    final AppConfiguration config = KegbotApplication.get(context).getConfig();
    final String userAgent = Utils.getUserAgent(context);
    return new KegbotApiImpl(config, userAgent);
  }

  private static String getUrlParamsString(Map<String, String> params) {
    if (params == null || params.isEmpty()) {
      return "";
    }

    final List<String> parts = Lists.newArrayList();
    for (Map.Entry<String, String> param : params.entrySet()) {
      try {
        parts.add(String.format("%s=%s",
            URLEncoder.encode(param.getKey(), "utf-8"),
            URLEncoder.encode(param.getValue(), "utf-8")));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    return Joiner.on('&').join(parts);
  }

  private static RequestBody formBody(Map<String, String> params) {
    return RequestBody.create(
        MediaType.parse("application/x-www-form-urlencoded"),
        getUrlParamsString(params).getBytes());
  }

  private static String getBoundary() {
    return String.format("-------MultiPart%s", new SecureRandom().nextLong());
  }

  /** Builds a multi-part form body. */
  private static RequestBody formBody(Map<String, String> params, Map<String, File> files)
      throws KegbotApiException {
    final String boundary = getBoundary();
    final byte[] boundaryBytes = boundary.getBytes();

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();

    final byte[] outputBytes;
    try {
      // Form data.
      for (final Map.Entry<String, String> param : params.entrySet()) {
        bos.write(HYPHENS);
        bos.write(boundaryBytes);
        bos.write(CRLF);
        bos.write(
            String.format("Content-Disposition: form-data; name=\"%s\"", param.getKey()).getBytes());
        bos.write(CRLF);
        bos.write(CRLF);
        bos.write(param.getValue().getBytes());
        bos.write(CRLF);
      }

      // Files
      for (final Map.Entry<String, File> entry : files.entrySet()) {
        final String entityName = entry.getKey();
        final File file = entry.getValue();

        bos.write(HYPHENS);
        bos.write(boundaryBytes);
        bos.write(CRLF);
        bos.write(
            String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"",
                entityName, file.getName()).getBytes()
        );
        bos.write(CRLF);
        bos.write(CRLF);

        final FileInputStream fis = new FileInputStream(file);
        try {
          ByteStreams.copy(fis, bos);
        } finally {
          fis.close();
        }
        bos.write(CRLF);
      }
      bos.write(HYPHENS);
      bos.write(boundaryBytes);
      bos.write(HYPHENS);
      bos.write(CRLF);

      bos.flush();
      outputBytes = bos.toByteArray();
    } catch (IOException e) {
      throw new KegbotApiException(e);
    }

    return RequestBody.create(
        MediaType.parse("multipart/form-data;boundary=" + boundary), outputBytes);
  }

  private String apiUrl() {
    return String.format("%s/v1", Strings.nullToEmpty(mConfig.getApiUrl()));
  }

  private String apiKey() {
    return Strings.nullToEmpty(mConfig.getApiKey());
  }

  @Override
  public void start(Context context) {
  }

  private Request.Builder newRequest(final String path) {
    Request.Builder builder = new Request.Builder()
        .url(apiUrl() + path)
        .addHeader("User-Agent", mUserAgent)
        .addHeader("X-Kegbot-Api-Key", apiKey());
    return builder;
  }

  private JsonNode requestJson(Request request) throws KegbotApiException {
    final Response response;
    final long startTime = SystemClock.elapsedRealtime();
    try {
      response = mClient.newCall(request).execute();
    } catch (IOException e) {
      Log.w(TAG, String.format("--> %s %s [ERR]", request.method(), request.urlString()));
      throw new KegbotApiException(e);
    }
    final long endTime = SystemClock.elapsedRealtime();

    final int responseCode = response.code();
    final String logMessage = String.format("--> %s %s [%s] %sms", request.method(), request.urlString(),
        responseCode, endTime - startTime);
    if (responseCode >= 200 && responseCode < 300) {
      Log.d(TAG, logMessage);
    } else {
      Log.w(TAG, logMessage);
    }
    final ResponseBody body = response.body();

    final JsonNode rootNode;
    try {
      try {
        final ObjectMapper mapper = new ObjectMapper();
        rootNode = mapper.readValue(body.byteStream(), JsonNode.class);
      } finally {
        body.close();
      }
    } catch (JsonParseException e) {
      throw new KegbotApiMalformedResponseException(e);
    } catch (JsonMappingException e) {
      throw new KegbotApiMalformedResponseException(e);
    } catch (IOException e) {
      throw new KegbotApiException(e);
    }

    boolean success = false;
    try {
      // Handle structural errors.
      if (!rootNode.has("meta")) {
        throw new KegbotApiMalformedResponseException("Response is missing 'meta' field.");
      }
      final JsonNode meta = rootNode.get("meta");
      if (!meta.isContainerNode()) {
        throw new KegbotApiMalformedResponseException("'meta' field is wrong type.");
      }

      final String message;
      if (rootNode.has("error") && rootNode.get("error").has("message")) {
        message = rootNode.get("error").get("message").getTextValue();
      } else {
        message = null;
      }

      // Handle HTTP errors.
      if (responseCode < 200 || responseCode >= 400) {
        switch (responseCode) {
          case 401:
            throw new NotAuthorizedException(message);
          case 404:
            throw new KegbotApi404(message);
          case 405:
            throw new MethodNotAllowedException(message);
          default:
            if (message != null) {
              throw new KegbotApiServerError(message);
            } else {
              throw new KegbotApiServerError("Server error, response code=" + responseCode);
            }
        }
      }

      success = true;
      return rootNode;
    } finally {
      if (!success) {
        Log.d(TAG, "Response JSON was: " + rootNode.toString());
      }
    }
  }

  private JsonNode getJson(String path) throws KegbotApiException {
    final Request.Builder request = newRequest(path);
    return requestJson(request.build());
  }

  private JsonNode postJson(String path, Map<String, String> params) throws KegbotApiException {
    final Request.Builder request = newRequest(path).
        post(formBody(params));
    return requestJson(request.build());
  }

  private JsonNode postJson(String path, Map<String, String> params,
      Map<String, File> files) throws KegbotApiException {
    final Request.Builder request = newRequest(path).post(formBody(params, files));
    return requestJson(request.build());
  }

  private <T extends GeneratedMessage> List<T> getProto(String path, Builder builder) throws KegbotApiException {
    JsonNode result = getJson(path);
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
    JsonNode result = getJson(path);
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

  private <T extends GeneratedMessage> T postProto(String path, Builder builder,
      Map<String, String> params, Map<String, File> files) throws KegbotApiException {
    JsonNode result = postJson(path, params, files);
    return getSingleProto(builder, result.get("object"));
  }

  public Version getVersion() throws KegbotApiException {
    final String version = getJson("/version").get("object").get("server_version").getTextValue();
    return Version.fromString(version);
  }

  public boolean supportsDeviceLink() throws KegbotApiException {
    try {
      return getVersion().compareTo(Version.fromString("0.9.27")) >= 0;
    } catch (KegbotApi404 e) {
      return false;
    }
  }

  public String startDeviceLink(final String deviceName) throws KegbotApiException {
    final Map<String, String> params = Maps.newLinkedHashMap();
    params.put("name", deviceName);
    debug("Starting device link ...");
    final JsonNode result = postJson("/devices/link/", params);
    final String code = result.get("object").get("code").getTextValue();
    if (Strings.isNullOrEmpty(code)) {
      throw new KegbotApiException("Pairing code was empty.");
    }
    return code;
  }

  /** Returns an apikey once linked, {@code null} otherwise. */
  public String pollDeviceLink(final String code) throws KegbotApiException {
    final JsonNode response = getJson("/devices/link/status/" + code).get("object");
    if (response.has("linked") && response.get("linked").getBooleanValue()) {
      return response.get("api_key").getTextValue();
    }
    return null;
  }

  @Deprecated
  public void login(String username, String password) throws KegbotApiException {
    Map<String, String> params = Maps.newLinkedHashMap();
    params.put("username", username);
    params.put("password", password);
    debug("Logging in as username=" + username + " password=XXX");
    postJson("/login/", params);
  }

  @Deprecated
  public String getApiKey() throws KegbotApiException {
    final JsonNode result = getJson("/get-api-key/");
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

  @Override
  public List<SoundEvent> getSoundEvents() throws KegbotApiException {
    return getProto("/sound-events/", SoundEvent.newBuilder());
  }

  @Override
  public List<KegTap> getTaps() throws KegbotApiException {
    return getProto("/taps/", KegTap.newBuilder());
  }

  @Override
  public KegTap createTap(String tapName) throws BackendException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void deleteTap(KegTap tap) {
    // TODO Auto-generated method stub

  }

  @Override
  public AuthenticationToken getAuthToken(String authDevice, String tokenValue)
      throws KegbotApiException {
    try {
      return getSingleProto("/auth-tokens/" + authDevice + "/" + tokenValue + "/",
          AuthenticationToken.newBuilder());
    } catch (KegbotApi404 e) {
      return null;
    }
  }

  @Override
  public Keg endKeg(Keg keg) throws KegbotApiException {
    return (Keg) postProto("/kegs/" + keg.getId() + "/end/", Keg.newBuilder(), null);
  }

  @Override
  public KegTap startKeg(KegTap tap, String beerName, String brewerName, String styleName,
      String kegType) throws KegbotApiException {
    final Map<String, String> params = ImmutableMap.<String, String>builder()
        .put("beer_name", beerName)
        .put("brewer_name", brewerName)
        .put("style_name", styleName)
        .put("keg_size", kegType)
        .build();

    return postProto("/taps/" + tap.getId() + "/activate/", KegTap.newBuilder(), params);
  }

  @Override
  public List<SystemEvent> getEvents() throws KegbotApiException {
    return getProto("/events/", SystemEvent.newBuilder());
  }

  @Override
  public List<SystemEvent> getEventsSince(final long sinceEventId) throws KegbotApiException {
    final List<NameValuePair> params = Lists.newArrayList();
    return getProto("/events/?since=" + sinceEventId, SystemEvent.newBuilder());
  }

  @Override
  public JsonNode getSessionStats(int sessionId) throws KegbotApiException {
    return getJson("/sessions/" + sessionId + "/stats/").get("object");
  }

  @Override
  public FlowMeter calibrateMeter(FlowMeter meter, double ticksPerMl) throws BackendException {
    final Map<String, String> params = ImmutableMap.<String, String>builder()
        .put("ticks_per_ml", Double.valueOf(ticksPerMl).toString())
        .put("ml_per_tick", Double.valueOf(1.0 / ticksPerMl).toString())
        .build();
    return postProto("/flow-meters/" + meter.getId(), FlowMeter.newBuilder(), params);
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
    final List<Session> sessions = getProto("/sessions/?limit=1", Session.newBuilder());
    if (sessions.isEmpty()) {
      return null;
    }
    final Session session = sessions.get(0);
    if (session.getIsActive()) {
      return session;
    }
    return null;
  }

  @Override
  public Drink recordDrink(String tapName, long volumeMl, long ticks,
      @Nullable String shout, @Nullable String username, @Nullable String recordDate, long durationMillis,
      @Nullable TimeSeries timeSeries, @Nullable File picture) throws BackendException {

    final ImmutableMap.Builder<String, String> paramBuilder = ImmutableMap.builder();
    paramBuilder.put("ticks", String.valueOf(ticks));

    if (volumeMl > 0) {
      paramBuilder.put("volume_ml", String.valueOf(volumeMl));
    }

    if (!Strings.isNullOrEmpty(username)) {
      paramBuilder.put("username", username);
    }

    if (!Strings.isNullOrEmpty(recordDate)) {
      try {
        paramBuilder.put("record_date", recordDate); // new API
      } catch (IllegalArgumentException e) {
        // Ignore.
      }
    }

    if (durationMillis > 0) {
      // TODO: Fix API to report this in millis.
      paramBuilder.put("duration", String.valueOf(durationMillis / 1000));
    }

    // TODO: Handle spilled.

    if (!Strings.isNullOrEmpty(shout)) {
      paramBuilder.put("shout", shout);
    }

    if (timeSeries != null) {
      paramBuilder.put("tick_time_series", timeSeries.asString());
    }

    if (picture != null) {
      final Map<String, File> files = ImmutableMap.of("photo", picture);
      return postProto("/taps/" + tapName, Drink.newBuilder(), paramBuilder.build(), files);
    } else {
      return postProto("/taps/" + tapName, Drink.newBuilder(), paramBuilder.build());
    }
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
    final Map<String, File> files = ImmutableMap.of("photo", picture);
    return postProto(url, Image.newBuilder(), null, files);
  }

  @Override
  public User createUser(String username, String email, String password, String imagePath)
      throws KegbotApiException {
    final ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
        .put("username", username)
        .put("email", email);

    if (!Strings.isNullOrEmpty(password)) {
      builder.put("password", password);
    }

    if (!Strings.isNullOrEmpty(imagePath)) {
      final Map<String, File> files = ImmutableMap.of("photo", new File(imagePath));
      return postProto("/new-user/", User.newBuilder(), builder.build(), files);
    } else {
      return postProto("/new-user/", User.newBuilder(), builder.build());
    }
  }

  @Override
  public AuthenticationToken assignToken(String authDevice, String tokenValue, String username)
      throws KegbotApiException {
    final String url = "/auth-tokens/" + authDevice + "/" + tokenValue + "/assign/";
    final Map<String, String> params = ImmutableMap.<String, String>builder()
        .put("username", username)
        .build();
    return postProto(url, AuthenticationToken.newBuilder(), params);
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

  @Override
  public KegTap connectMeter(KegTap tap, FlowMeter meter) throws BackendException {
    final Map<String, String> params = Maps.newLinkedHashMap();
    params.put("meter", String.valueOf(meter.getId()));
    return (KegTap) postProto("/taps/" + tap.getId() + "/connect-meter", KegTap.newBuilder(), params);
  }

  @Override
  public KegTap disconnectMeter(KegTap tap) throws BackendException {
    return (KegTap) postProto("/taps/" + tap.getId() + "/disconnect-meter", KegTap.newBuilder(), null);
  }

  @Override
  public List<FlowToggle> getFlowToggles() throws BackendException {
    return getProto("/flow-toggles/", FlowToggle.newBuilder());
  }

  @Override
  public Models.FlowToggle updateFlowToggle(Models.FlowToggle flowToggle) throws BackendException {
    final Map<String, String> params = Maps.newLinkedHashMap();
    params.put("port_name", flowToggle.getPortName());

    return (FlowToggle) postProto("/flow-toggles/" + flowToggle.getId(), FlowToggle.newBuilder(),
        params);
  }

  @Override
  public KegTap connectToggle(KegTap tap, Models.FlowToggle toggle) throws BackendException {
    final Map<String, String> params = Maps.newLinkedHashMap();
    params.put("toggle", String.valueOf(toggle.getId()));
    return (KegTap) postProto("/taps/" + tap.getId() + "/connect-toggle", KegTap.newBuilder(), params);
  }

  @Override
  public KegTap disconnectToggle(KegTap tap) throws BackendException {
    return (KegTap) postProto("/taps/" + tap.getId() + "/disconnect-toggle", KegTap.newBuilder(),
        null);
  }
}
