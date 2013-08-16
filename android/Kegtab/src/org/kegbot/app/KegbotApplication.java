/**
 *
 */
package org.kegbot.app;

import org.kegbot.app.service.CheckinService;
import org.kegbot.app.util.Utils;
import org.kegbot.core.KegbotCore;

import android.app.Application;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

/**
 * Kegbot customized application.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class KegbotApplication extends Application {

  private static final String TAG = KegbotApplication.class.getSimpleName();

  @Override
  public void onCreate() {
    super.onCreate();
    Log.i(TAG, "Kegbot starting.");

    if (!BuildConfig.DEBUG) {
      try {
        Crashlytics.start(this);
      } catch (Exception e) {
        Log.w(TAG, "Crashlytics not started: " + e, e);
      } catch (NoClassDefFoundError e) {
        Log.w(TAG, "Crashlytics not started: " + e, e);
      }
    }

    final String userAgent = Utils.getUserAgent(getApplicationContext());
    Log.d(TAG, "Kegtab User-agent: " + userAgent);
    System.setProperty("http.agent", userAgent);

    KegbotCore.getInstance(getApplicationContext());
    CheckinService.startCheckinService(this);
  }

}
