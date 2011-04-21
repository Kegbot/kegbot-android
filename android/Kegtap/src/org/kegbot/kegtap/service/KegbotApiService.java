package org.kegbot.kegtap.service;

import org.apache.http.impl.client.DefaultHttpClient;
import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
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
import org.kegbot.proto.Models.User;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * This service manages a connection to a Kegbot backend, using the Kegbot API.
 * It implements the {@link KegbotApi} interface, potentially employing caching.
 */
public class KegbotApiService extends BackgroundService implements KegbotApi {

  private static String TAG = KegbotApiService.class.getSimpleName();

  /**
   * Current state of this service with respect to its backend.
   */
  public enum ConnectionState {
    /**
     * The service is currently connecting to the backend.
     */
    CONNECTING,

    /**
     * The service is currently connected to the backend.
     */
    CONNECTED,

    /**
     * The service has become disconnected from the backend. API calls will
     * fail. The service will attempt to reconnect.
     */
    DISCONNECTED,

    /**
     * The service has entered a permanent failure state.
     */
    FAILED
  }

  /**
   * Receives notifications for API events.
   * 
   * @author mike
   */
  public interface Listener {

    /**
     * Notifies listener that the connection state has changed.
     * 
     * @param newState
     */
    public void onConnectionStateChange(ConnectionState newState);

    /**
     * Notifies the listener that the system's configuration has changed (for
     * instance, a tap was added or a keg was changed).
     */
    public void onConfigurationUpdate();

  }

  public class LocalBinder extends Binder {
    KegbotApiService getService() {
      return KegbotApiService.this;
    }
  }

  private final IBinder mBinder = new LocalBinder();

  private KegbotApiImpl mApi;

  public KegbotApiService() {
    super(TAG);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mApi = new KegbotApiImpl(new DefaultHttpClient(), "http://oldgertie.kegbot.net/api");
    mApi.setApiKey("ddbb9c0b65d0a9c7");
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  protected void runInBackground() {
    monitorConnectionStatus();
  }

  private void monitorConnectionStatus() {
    // TODO
  }

  public void attachListener(Listener listener) {
    // TODO
  }

  //
  // KegbotApi methods
  //


  @Override
  public boolean setAccountCredentials(String username, String password) {
    return mApi.setAccountCredentials(username, password);
  }

  @Override
  public void setApiKey(String apiKey) {
    mApi.setApiKey(apiKey);
  }

  @Override
  public KegSet getAllKegs() throws KegbotApiException {
    return mApi.getAllKegs();
  }

  @Override
  public SoundEventSet getAllSoundEvents() throws KegbotApiException {
    return mApi.getAllSoundEvents();
  }

  @Override
  public TapDetailSet getAllTaps() throws KegbotApiException {
    return mApi.getAllTaps();
  }

  @Override
  public AuthenticationToken getAuthToken(String authDevice, String tokenValue)
  throws KegbotApiException {
    return mApi.getAuthToken(authDevice, tokenValue);
  }

  @Override
  public DrinkDetail getDrinkDetail(String id) throws KegbotApiException {
    return mApi.getDrinkDetail(id);
  }

  @Override
  public KegDetail getKegDetail(String id) throws KegbotApiException {
    return mApi.getKegDetail(id);
  }

  @Override
  public DrinkSet getKegDrinks(String kegId) throws KegbotApiException {
    return mApi.getKegDrinks(kegId);
  }

  @Override
  public SystemEventDetailSet getKegEvents(String kegId) throws KegbotApiException {
    return mApi.getKegEvents(kegId);
  }

  @Override
  public SessionSet getKegSessions(String kegId) throws KegbotApiException {
    return mApi.getKegSessions(kegId);
  }

  @Override
  public String getLastDrinkId() throws KegbotApiException {
    return mApi.getLastDrinkId();
  }

  @Override
  public DrinkSet getRecentDrinks() throws KegbotApiException {
    return mApi.getRecentDrinks();
  }

  @Override
  public DrinkDetailHtmlSet getRecentDrinksHtml() throws KegbotApiException {
    return mApi.getRecentDrinksHtml();
  }

  @Override
  public SystemEventDetailSet getRecentEvents() throws KegbotApiException {
    return mApi.getRecentEvents();
  }

  @Override
  public SystemEventHtmlSet getRecentEventsHtml() throws KegbotApiException {
    return mApi.getRecentEventsHtml();
  }

  @Override
  public SessionDetail getSessionDetail(String id) throws KegbotApiException {
    return mApi.getSessionDetail(id);
  }

  @Override
  public TapDetail getTapDetail(String tapName) throws KegbotApiException {
    return mApi.getTapDetail(tapName);
  }

  @Override
  public ThermoLogSet getThermoSensorLogs(String sensorId) throws KegbotApiException {
    return mApi.getThermoSensorLogs(sensorId);
  }

  @Override
  public ThermoSensorSet getThermoSensors() throws KegbotApiException {
    return mApi.getThermoSensors();
  }

  @Override
  public User getUser(String username) throws KegbotApiException {
    return mApi.getUser(username);
  }

  @Override
  public DrinkSet getUserDrinks(String username) throws KegbotApiException {
    return mApi.getUserDrinks(username);
  }

  @Override
  public SystemEventDetailSet getUserEvents(String username) throws KegbotApiException {
    return mApi.getUserEvents(username);
  }

  @Override
  public Drink recordDrink(String tapName, int ticks) throws KegbotApiException {
    return mApi.recordDrink(tapName, ticks);
  }

}
