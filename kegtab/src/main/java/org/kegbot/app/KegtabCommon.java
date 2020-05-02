/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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


package org.kegbot.app;

import android.content.Context;
import android.content.Intent;

public class KegtabCommon {

  /**
   * Intent action name for an activity which manually authenticates a drinker.
   */
  public static final String ACTIVITY_AUTH_DRINKER_ACTION = "org.kegbot.app.activity.AUTH_DRINKER";

  /**
   * The authenticated username.
   */
  public static final String ACTIVITY_AUTH_DRINKER_RESULT_EXTRA_USERNAME = "username";


  /**
   * Intent action name for an activity which manually creates a drinker.
   */
  public static final String ACTIVITY_CREATE_DRINKER_ACTION = "org.kegbot.app.activity.NEW_DRINKER";

  /**
   * The created username.
   */
  public static final String ACTIVITY_CREATE_DRINKER_RESULT_EXTRA_USERNAME = "username";

  public static Intent getAuthDrinkerActivityIntent(Context context) {
    final Intent intent = new Intent(ACTIVITY_AUTH_DRINKER_ACTION);
    return intent;
  }

  public static Intent getCreateDrinkerActivityIntent(Context context) {
    final Intent intent = new Intent(ACTIVITY_CREATE_DRINKER_ACTION);
    return intent;
  }

}
