/**
 *
 */
package org.kegbot.kegtap.setup;

import org.apache.http.conn.HttpHostConnectException;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiServerError;
import org.kegbot.kegtap.R;
import org.kegbot.kegtap.util.PreferenceHelper;

import android.app.Fragment;
import android.content.Context;

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
   * Alternate task, shows "getting started".
   */
  UPGRADE {
    @Override
    public SetupTask next() {
      return API_URL;
    }

    @Override
    public int getTitle() {
      return R.string.setup_upgrade_title;
    }

    @Override
    public int getDescription() {
      return R.string.setup_upgrade_description;
    }
  },

  /**
   * Requests and validates the API URL.
   */
  API_URL {
    private final SetupApiUrlFragment mFragment = new SetupApiUrlFragment();

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
    public void onExitSuccess(Context context) {
      PreferenceHelper prefs = new PreferenceHelper(context);
      prefs.setSetupVersion(SETUP_VERSION);
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

  public static final SetupTask FIRST_SETUP_STEP = API_URL;

  public static final int SETUP_VERSION = 2;

  /**
   * Returns the next task to be performed.  If this is the final task, use {@code null};
   */
  public abstract SetupTask next();

  protected void onExitSuccess(Context context) {

  }

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
