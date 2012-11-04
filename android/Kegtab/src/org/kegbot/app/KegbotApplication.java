/**
 *
 */
package org.kegbot.app;

import org.kegbot.app.util.Utils;
import org.kegbot.core.KegbotCore;

import android.app.Application;
import android.util.Log;

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

    final String userAgent = Utils.getUserAgent();
    Log.d(TAG, "Kegtab User-agent: " + userAgent);
    System.setProperty("http.agent", userAgent);

    KegbotCore.getInstance(getApplicationContext());
  }

}
