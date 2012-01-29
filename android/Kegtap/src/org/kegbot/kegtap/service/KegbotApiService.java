package org.kegbot.kegtap.service;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.api.KegbotApiNotFoundError;
import org.kegbot.core.Flow;
import org.kegbot.kegtap.core.backend.LocalDbHelper;
import org.kegbot.proto.Api.DrinkDetail;
import org.kegbot.proto.Api.DrinkDetailHtmlSet;
import org.kegbot.proto.Api.DrinkSet;
import org.kegbot.proto.Api.KegDetail;
import org.kegbot.proto.Api.KegSet;
import org.kegbot.proto.Api.RecordDrinkRequest;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Api.SessionDetail;
import org.kegbot.proto.Api.SessionSet;
import org.kegbot.proto.Api.SoundEventSet;
import org.kegbot.proto.Api.SystemEventDetailSet;
import org.kegbot.proto.Api.SystemEventHtmlSet;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Api.TapDetailSet;
import org.kegbot.proto.Api.ThermoLogSet;
import org.kegbot.proto.Api.ThermoSensorSet;
import org.kegbot.proto.Api.UserDetailSet;
import org.kegbot.proto.Internal.PendingPour;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.ThermoLog;
import org.kegbot.proto.Models.User;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * This service manages a connection to a Kegbot backend, using the Kegbot API.
 * It implements the {@link KegbotApi} interface, potentially employing caching.
 */
public class KegbotApiService extends BackgroundService implements KegbotApi {

  private static String TAG = KegbotApiService.class.getSimpleName();

  /**
   * All pending asynchronous requests. They will be serviced in order in the
   * background thread.
   *
   * TODO(mikey): fix capacity appropriately.
   */
  private final BlockingQueue<AbstractMessage> mPendingRequests =
    new LinkedBlockingQueue<AbstractMessage>(10);

  private KegbotApiImpl mApi;

  private SQLiteOpenHelper mLocalDbHelper;

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
    public KegbotApiService getService() {
      return KegbotApiService.this;
    }
  }

  private final IBinder mBinder = new LocalBinder();

  private final KegbotApiImpl.Listener mApiListener = new KegbotApiImpl.Listener() {
    @Override
    public void debug(String message) {
      Log.d("KegbotApiImpl", message);
    }
  };

  @Override
  public void onCreate() {
    Log.d(TAG, "Opening local database");
    mLocalDbHelper = new LocalDbHelper(this);

    super.onCreate();

    mApi = KegbotApiImpl.getSingletonInstance();
    mApi.setListener(mApiListener);
    //mApi.setApiKey("");
  }

  @Override
  public IBinder onBind(final Intent intent) {
    return mBinder;
  }

  @Override
  protected void runInBackground() {
    Log.i(TAG, "Running in background.");

    int numPending = numPendingEntries();
    try {
      while (true) {
        writeNewRequestsToDb();
        postPendingRequestsToServer();
        numPending = numPendingEntries();
        SystemClock.sleep(1000);
      }
    } catch (Throwable e) {
      Log.wtf(TAG, "Uncaught exception in background.", e);
    }
  }

  private void postPendingRequestsToServer() {
    final int numPending = numPendingEntries();
    //Log.i(TAG, "Posting pending requests: " + numPending);
    processRequestFromDb();
  }

  private void processRequestFromDb() {
    final SQLiteDatabase db = mLocalDbHelper.getWritableDatabase();

    // Fetch most recent entry.
    final Cursor cursor =
      db.query(LocalDbHelper.TABLE_NAME,
          null, null, null, null, null, LocalDbHelper.COLUMN_NAME_ADDED_DATE + " ASC", "1");
    try {
      if (cursor.getCount() == 0) {
        //Log.i(TAG, "processRequestFromDb: empty result set, exiting");
        return;
      }
      cursor.moveToFirst();

      boolean processed = false;
      try {
        final AbstractMessage record = LocalDbHelper.getCurrentRow(db, cursor);
        if (record instanceof PendingPour) {
          Log.d(TAG, "Posting pour");
          final PendingPour pour = (PendingPour) record;
          final Drink drink = recordDrink(pour.getDrinkRequest());
          Log.d(TAG, "Drink posted: " + drink);

          if (pour.getImagesCount() > 0) {
            Log.d(TAG, "Drink had images, trying to post them..");
            for (final String imagePath : pour.getImagesList()) {
              Log.d(TAG, "Uploading image: " + imagePath);
              try {
                uploadDrinkImage(drink.getId(), imagePath);
              } finally {
                new File(imagePath).delete();
              }
            }
          }
          processed = true;

        } else if (record instanceof RecordTemperatureRequest) {
          processed = true; // XXX drop even if fail
          Log.d(TAG, "Posting thermo");
          final ThermoLog log = recordTemperature((RecordTemperatureRequest) record);
          Log.d(TAG, "ThermoLog posted: " + log);
        } else {
          Log.w(TAG, "Unknown row type.");
        }

      } catch (InvalidProtocolBufferException e) {
        Log.w(TAG, "Error processing column: " + e);
        processed = true;
      } catch (KegbotApiNotFoundError e) {
        Log.w(TAG, "Tap not found, dropping record");
        processed = true;
      } catch (KegbotApiException e) {
        Log.w(TAG, "Error processing column: " + e);
        processed = true;
      }

      if (processed) {
        final int deleteResult = LocalDbHelper.deleteCurrentRow(db, cursor);
        Log.d(TAG, "Deleted row, result = " + deleteResult);
      }
    } finally {
      cursor.close();
      db.close();
    }
  }

  private int writeNewRequestsToDb() {
    AbstractMessage message;
    int result = 0;
    while ((message = mPendingRequests.poll()) != null) {
      if (addSingleRequestToDb(message)) {
        result++;
      }
    }
    return result;
  }

  private boolean addSingleRequestToDb(AbstractMessage message) {
    Log.d(TAG, "Adding request to db!");
    final String type;
    if (message instanceof PendingPour) {
      type = "pour";
    } else if (message instanceof RecordTemperatureRequest) {
      type = "thermo";
    } else {
      Log.w(TAG, "Unknown record type; dropping.");
      return false;
    }
    Log.d(TAG, "Request is a " + type);

    final ContentValues values = new ContentValues();
    values.put(LocalDbHelper.COLUMN_NAME_TYPE, type);
    values.put(LocalDbHelper.COLUMN_NAME_RECORD, message.toByteArray());

    boolean inserted = false;
    final SQLiteDatabase db = mLocalDbHelper.getWritableDatabase();
    try {
      db.insert(LocalDbHelper.TABLE_NAME, null, values);
      inserted = true;
    } finally {
      db.close();
    }
    return inserted;
  }

  private int numPendingEntries() {
    final String[] columns = {LocalDbHelper.COLUMN_NAME_ID};
    final SQLiteDatabase db = mLocalDbHelper.getReadableDatabase();
    final Cursor cursor =
      db.query(LocalDbHelper.TABLE_NAME, columns, null, null, null, null, null);
    try {
      return cursor.getCount();
    } finally {
      cursor.close();
      db.close();
    }
  }

  public void attachListener(final Listener listener) {
    // TODO
  }

  //
  // KegbotApi methods
  //

  @Override
  public boolean setAccountCredentials(final String username, final String password) {
    return mApi.setAccountCredentials(username, password);
  }

  @Override
  public void setApiUrl(final String apiUrl) {
    mApi.setApiUrl(apiUrl);
  }

  @Override
  public void setApiKey(final String apiKey) {
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
  public AuthenticationToken getAuthToken(final String authDevice, final String tokenValue)
      throws KegbotApiException {
    return mApi.getAuthToken(authDevice, tokenValue);
  }

  @Override
  public DrinkDetail getDrinkDetail(final String id) throws KegbotApiException {
    return mApi.getDrinkDetail(id);
  }

  @Override
  public KegDetail getKegDetail(final String id) throws KegbotApiException {
    return mApi.getKegDetail(id);
  }

  @Override
  public DrinkSet getKegDrinks(final String kegId) throws KegbotApiException {
    return mApi.getKegDrinks(kegId);
  }

  @Override
  public SystemEventDetailSet getKegEvents(final String kegId) throws KegbotApiException {
    return mApi.getKegEvents(kegId);
  }

  @Override
  public SessionSet getKegSessions(final String kegId) throws KegbotApiException {
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
  public SystemEventDetailSet getRecentEvents(final long sinceEventId) throws KegbotApiException {
    return mApi.getRecentEvents(sinceEventId);
  }

  @Override
  public SystemEventHtmlSet getRecentEventsHtml() throws KegbotApiException {
    return mApi.getRecentEventsHtml();
  }

  @Override
  public SessionDetail getSessionDetail(final String id) throws KegbotApiException {
    return mApi.getSessionDetail(id);
  }

  @Override
  public TapDetail getTapDetail(final String tapName) throws KegbotApiException {
    return mApi.getTapDetail(tapName);
  }

  @Override
  public ThermoLogSet getThermoSensorLogs(final String sensorId) throws KegbotApiException {
    return mApi.getThermoSensorLogs(sensorId);
  }

  @Override
  public ThermoSensorSet getThermoSensors() throws KegbotApiException {
    return mApi.getThermoSensors();
  }

  @Override
  public User getUser(final String username) throws KegbotApiException {
    return mApi.getUser(username);
  }

  @Override
  public DrinkSet getUserDrinks(final String username) throws KegbotApiException {
    return mApi.getUserDrinks(username);
  }

  @Override
  public SystemEventDetailSet getUserEvents(final String username) throws KegbotApiException {
    return mApi.getUserEvents(username);
  }

  @Override
  public UserDetailSet getUsers() throws KegbotApiException {
    return mApi.getUsers();
  }

  @Override
  public SessionSet getCurrentSessions() throws KegbotApiException {
    return mApi.getCurrentSessions();
  }

  @Override
  public Drink recordDrink(final RecordDrinkRequest request) throws KegbotApiException {
    return mApi.recordDrink(request);
  }

  @Override
  public ThermoLog recordTemperature(final RecordTemperatureRequest request)
      throws KegbotApiException {
    return mApi.recordTemperature(request);
  }

  /**
   * Schedules a drink to be recorded asynchronously.
   * @param flow
   */
  public void recordDrinkAsync(final Flow flow) {
    final RecordDrinkRequest request = getRequestForFlow(flow);
    final PendingPour pour = PendingPour.newBuilder()
        .setDrinkRequest(request)
        .addAllImages(flow.getImages())
        .build();

    Log.d(TAG, ">>> Enqueuing pour: " + pour);
    if (mPendingRequests.remainingCapacity() == 0) {
      // Drop head when full.
      mPendingRequests.poll();
    }
    mPendingRequests.add(pour);
    Log.d(TAG, "<<< Pour enqueued.");
  }

  private static RecordDrinkRequest getRequestForFlow(final Flow ended) {
    return RecordDrinkRequest.newBuilder()
        .setTapName(ended.getTap().getMeterName())
        .setTicks(ended.getTicks())
        .setVolumeMl((float) ended.getVolumeMl()).setUsername(ended.getUsername())
        .setSecondsAgo(0).setDurationSeconds((int) (ended.getUpdateTime() - ended.getStartTime()))
        .setSpilled(false).buildPartial();
  }

  /**
   * Schedules a temperature reading to be recorded asynchronously.
   *
   * @param request
   */
  public void recordTemperatureAsync(final RecordTemperatureRequest request) {
    Log.d(TAG, "Recording temperature: " + request);
    if (mPendingRequests.remainingCapacity() == 0) {
      // Drop head when full.
      mPendingRequests.poll();
    }
    mPendingRequests.add(request);
  }

  @Override
  public Image uploadDrinkImage(String drinkId, String imagePath) throws KegbotApiException {
    return mApi.uploadDrinkImage(drinkId, imagePath);
  }

}
