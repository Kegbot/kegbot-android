/**
 *
 */
package org.kegbot.app;

import android.content.Context;
import android.content.Intent;

/**
 * @author mike wakerly (opensource@hoho.com)
 */
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
