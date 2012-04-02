/**
 *
 */
package org.kegbot.kegtap.setup;

import org.apache.http.conn.HttpHostConnectException;
import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.api.KegbotApiServerError;
import org.kegbot.kegtap.R;
import org.kegbot.kegtap.util.PreferenceHelper;
import org.kegbot.proto.Api.SystemEventDetailSet;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.common.base.Strings;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public enum SetupTask {

  /**
   * First task, shows "getting started".
   */
  WELCOME {
    @Override
    public SetupTask next() {
      return API_URL;
    }

    @Override
    public int getTitle() {
      return R.string.setup_welcome_title;
    }

    @Override
    public int getDescription() {
      return R.string.setup_welcome_description;
    }
  },

  /**
   * Requests and validates the API URL.
   */
  API_URL {
    private final SetupApiUrlFragment mFragment = new SetupApiUrlFragment();

    @Override
    public String validate(Context context) {
      final String apiUrl = mFragment.getApiUrl();

      Log.d(API_URL.toString(), "Got api URL: " + apiUrl);
      final Uri uri = Uri.parse(apiUrl);
      final String scheme = uri.getScheme();

      if (!"http".equals(scheme) && !"https".equals(scheme)) {
        return "Please enter an HTTP or HTTPs URL.";
      }
      if (Strings.isNullOrEmpty(uri.getHost())) {
        return "Please provide a valid URL.";
      }

      KegbotApi api = new KegbotApiImpl();
      api.setApiUrl(apiUrl);

      try {
        SystemEventDetailSet events = api.getRecentEvents();
        Log.d(API_URL.toString(), "Success: " + events);
      } catch (KegbotApiException e) {
        Log.d(API_URL.toString(), "Error: " + e.toString(), e);
        StringBuilder builder = new StringBuilder();
        builder.append("Error contacting the Kegbot site: ");
        builder.append(toHumanError(e));
        builder.append("\n\nPlease check the URL and try again.");
        return builder.toString();
      }

      PreferenceHelper prefs = new PreferenceHelper(context);
      prefs.setKegbotUrl(apiUrl);

      return "";
    }

    @Override
    public SetupTask next() {
      return LOGIN;
    }

    @Override
    public Fragment getFragment() {
      return mFragment;
    }

    @Override
    public int getTitle() {
      return R.string.setup_api_url_title;
    }

    @Override
    public int getDescription() {
      return R.string.setup_api_url_description;
    }
  },

  LOGIN {
    private final SetupLogInFragment mFragment = new SetupLogInFragment();

    @Override
    public int getTitle() {
      return R.string.setup_log_in_title;
    }

    @Override
    public int getDescription() {
      return R.string.setup_log_in_description;
    }

    @Override
    public Fragment getFragment() {
      return mFragment;
    }

    @Override
    public String validate(Context context) {
      final String username = mFragment.getUsername();
      final String password = mFragment.getPassword();

      if (Strings.isNullOrEmpty(username)) {
        return "Please enter a username.";
      } else if (Strings.isNullOrEmpty(password)) {
        return "Please enter a password";
      }

      PreferenceHelper prefs = new PreferenceHelper(context);
      KegbotApi api = new KegbotApiImpl();
      api.setApiUrl(prefs.getKegbotUrl().toString());

      try {
        api.login(username, password);
      } catch (KegbotApiException e) {
        return "Error logging in: " + toHumanError(e);
      }

      final String apiKey;
      try {
        apiKey = api.getApiKey();
      } catch (KegbotApiException e) {
        return "Error fetching API key: " + toHumanError(e);
      }

      prefs.setUsername(username);
      prefs.setPassword(password);
      prefs.setApiKey(apiKey);

      return "";
    }

    @Override
    public SetupTask next() {
      return RUN_CORE;
    }
  },

  RUN_CORE {
    private final SetupRunCoreFragment mFragment = new SetupRunCoreFragment();

    @Override
    public int getTitle() {
      return R.string.setup_run_core_title;
    }

    @Override
    public int getDescription() {
      return R.string.setup_run_core_description;
    }

    @Override
    public Fragment getFragment() {
      return mFragment;
    }

    @Override
    public String validate(Context context) {
      PreferenceHelper prefs = new PreferenceHelper(context);
      prefs.setRunCore(mFragment.getRunCore());
      return "";
    }

    @Override
    public SetupTask next() {
      return SET_PIN;
    }
  },

  SET_PIN {
    private final SetupManagerPinFragment mFragment = new SetupManagerPinFragment();

    @Override
    public int getTitle() {
      return R.string.setup_manager_pin_title;
    }

    @Override
    public int getDescription() {
      return R.string.setup_manager_pin_description;
    }

    @Override
    public Fragment getFragment() {
      return mFragment;
    }

    @Override
    public String validate(Context context) {
      PreferenceHelper prefs = new PreferenceHelper(context);
      prefs.setPin(mFragment.getPin());
      return "";
    }

    @Override
    public SetupTask next() {
      return FINISHED;
    }
  },

  FINISHED {
    @Override
    public int getTitle() {
      return R.string.setup_finished_title;
    }

    @Override
    public int getDescription() {
      return R.string.setup_finished_description;
    }

    @Override
    public String validate(Context context) {
      PreferenceHelper prefs = new PreferenceHelper(context);
      prefs.setSetupVersion(SETUP_VERSION);
      return null;
    }

    @Override
    public SetupTask next() {
      return null;
    }
  };

  /**
   * Returns the fragment for this task.
   */
  public Fragment getFragment() {
    return new SetupEmptyFragment();
  }

  /**
   * Returns a short title for this task, used in the UI.
   */
  public abstract int getTitle();

  /**
   * Returns the descriptive text for this task.
   */
  public abstract int getDescription();

  /**
   * Run any applicable validation steps, returning a String error message if
   * the task is incomplete. A null or empty String indicates success.
   */
  public String validate(Context context) {
    return "";
  }

  public static final int SETUP_VERSION = 1;

  /**
   * Returns the next task to be performed.  If this is the final task, use {@code null};
   */
  public abstract SetupTask next();

  private static String toHumanError(KegbotApiException e) {
    StringBuilder builder = new StringBuilder();
    if (e.getCause() instanceof HttpHostConnectException) {
      builder.append("Could not connect to remote host.");
    } else if (e instanceof KegbotApiServerError) {
      builder.append("Bad response from server (");
      builder.append(e.toString());
      builder.append(")");
    } else {
      builder.append(e.getMessage());
    }
    return builder.toString();
  }

}
