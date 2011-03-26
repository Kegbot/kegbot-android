package org.kegbot.kegtap;

import org.apache.http.impl.client.DefaultHttpClient;
import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.kegtap.core.KegboardHardware;
import org.kegbot.kegtap.core.KegbotCore;
import org.kegbot.kegtap.core.android.AndroidLogger;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class KegbotCoreService extends IntentService {

  private static String TAG = KegbotCoreService.class.getSimpleName();

  private KegbotApi mApi;
  private KegbotCore mCore;
  private KegboardHardware mHw;

  public class LocalBinder extends Binder {
    KegbotCoreService getService() {
      return KegbotCoreService.this;
    }
  }

  private final IBinder mBinder = new LocalBinder();

  public KegbotCoreService(String name) {
    super(name);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mApi = new KegbotApiImpl(new DefaultHttpClient(), "http://kegbot.net/sfo/api");
    mCore = new KegbotCore(new AndroidLogger(), mApi, mHw);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.i(TAG, "Handing new intent.");
    mCore.run();
    Log.i(TAG, "Core finished, bye bye!");
  }

  public KegbotCore getCore() {
    return mCore;
  }

}
