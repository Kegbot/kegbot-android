/**
 *
 */
package org.kegbot.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;
import android.util.Log;

//import com.crashlytics.android.Crashlytics;

import com.crashlytics.android.Crashlytics;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.config.SharedPreferencesConfigurationStore;
import org.kegbot.app.service.CheckinService;
import org.kegbot.app.util.Utils;

/**
 * Kegbot customized application.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class KegbotApplication extends Application {

  private static final String TAG = KegbotApplication.class.getSimpleName();

  private static final String RELEASE_SIGNATURE = "06D936CB1BB9FB1A6BD4FC80105BDD79A5AF137F";

  private boolean mReleaseBuild = !BuildConfig.DEBUG;

  private SharedPreferences mSharedPreferences;
  private AppConfiguration mConfig;

  @Override
  public void onCreate() {
    super.onCreate();
    Log.i(TAG, "Kegbot starting.");

    if (!BuildConfig.DEBUG) {
      final PackageInfo packageInfo = Utils.getOwnPackageInfo(this);
      mReleaseBuild &= Utils.packageMatchesFingerprint(packageInfo, RELEASE_SIGNATURE);
    }
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    mConfig = new AppConfiguration(new SharedPreferencesConfigurationStore(mSharedPreferences));

    if (mReleaseBuild) {
      Log.d(TAG, "Activating crashlytics ...");
      try {
        Crashlytics.start(this);
        Log.d(TAG, "Crashlytics activated.");
      } catch (Exception e) {
        Log.w(TAG, "Crashlytics not activated: " + e, e);
      } catch (NoClassDefFoundError e) {
        Log.w(TAG, "Crashlytics not activated: " + e, e);
      }
    }

    final String userAgent = Utils.getUserAgent(getApplicationContext());
    Log.d(TAG, "Kegtab User-agent: " + userAgent);
    System.setProperty("http.agent", userAgent);

    CheckinService.startCheckinService(this, false);
  }

  public AppConfiguration getConfig() {
    return mConfig;
  }

  public SharedPreferences getSharedPreferences() {
    return mSharedPreferences;
  }

  public boolean isReleaseBuild() {
    return mReleaseBuild;
  }

  public static KegbotApplication get(final Context context) {
    return (KegbotApplication) context.getApplicationContext();
  }

}
