/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For more
 * information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app.setup;

import android.app.Fragment;

import org.kegbot.app.R;
import org.kegbot.app.config.AppConfiguration;

/**
 * All steps involved in application setup. Each steps represents a screen in
 * the setup wizard.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public enum SetupTask {

  /**
   * First task, shows "getting started".
   */
  WELCOME {
    @Override
    public SetupTask next(AppConfiguration config) {
      return LICENSE;
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
   * Alternate task, shows "we need to upgrade".
   */
  UPGRADE {
    @Override
    public SetupTask next(AppConfiguration config) {
      return LICENSE;
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
   * Shows license agreements
   */
  LICENSE {
    private final SetupLicenseFragment mFragment = new SetupLicenseFragment();

    @Override
    public SetupTask next(AppConfiguration config) {
      return SELECT_BACKEND;
    }

    @Override
    public Fragment getFragment() {
      return mFragment;
    }

    @Override
    public int getTitle() {
      return R.string.setup_license_title;
    }

    @Override
    public int getDescription() {
      return R.string.setup_license_description;
    }
  },

  SELECT_BACKEND {
    private final SetupSelectBackendFragment mFragment = new SetupSelectBackendFragment();

    @Override
    public Fragment getFragment() {
      return mFragment;
    }

    @Override
    public int getTitle() {
     return R.string.setup_select_backend_title;
    }

    @Override
    public int getDescription() {
      return R.string.setup_select_backend_description;
    }

    @Override
    public SetupTask next(AppConfiguration config) {
      if (config.isLocalBackend()) {
        return SET_PIN;
      } else {
        return KEGBOT_URL;
      }
    }

  },

  /**
   * Requests and validates the Kegbot URL.
   */
  KEGBOT_URL {
    private final SetupKegbotUrlFragment mFragment = new SetupKegbotUrlFragment();

    @Override
    public SetupTask next(AppConfiguration config) {
      return LOGIN;
    }

    @Override
    public Fragment getFragment() {
      return mFragment;
    }

    @Override
    public int getTitle() {
      return R.string.setup_kegbot_url_title;
    }

    @Override
    public int getDescription() {
      return R.string.setup_kegbot_url_description;
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
    public SetupTask next(AppConfiguration config) {
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
    public SetupTask next(AppConfiguration config) {
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
    public SetupTask next(AppConfiguration config) {
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
    public void onExitSuccess(AppConfiguration config) {
      config.setSetupVersion(SETUP_VERSION);
    }

    @Override
    public SetupTask next(AppConfiguration config) {
      return null;
    }
  };

  public static final SetupTask FIRST_SETUP_STEP = SELECT_BACKEND;

  public static final int SETUP_VERSION = 6;

  /**
   * Returns the fragment for this task.
   *
   * @return the {@link Fragment}
   */
  public Fragment getFragment() {
    return new SetupEmptyFragment();
  }

  /**
   * Returns a short title for this task, used in the UI.
   *
   * @return the resource id of the title
   */
  public abstract int getTitle();

  /**
   * Returns the descriptive text for this task.
   *
   * @return the resource id of the description
   */
  public abstract int getDescription();

  /**
   * Returns the next task to be performed. If this is the final task, use
   * {@code null};
   *
   * @return the next {@link SetupTask}
   */
  public abstract SetupTask next(AppConfiguration config);

  protected void onExitSuccess(AppConfiguration config) {
  }

}
