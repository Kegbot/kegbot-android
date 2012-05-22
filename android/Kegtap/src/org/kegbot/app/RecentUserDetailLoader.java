/**
 *
 */
package org.kegbot.app;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.kegbot.core.AuthenticationManager;
import org.kegbot.proto.Models.User;

import android.content.Context;
import android.content.Loader;
import android.util.Log;

import com.google.common.collect.Lists;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class RecentUserDetailLoader extends Loader<List<User>> {

  private static final String TAG = RecentUserDetailLoader.class.getSimpleName();

  private boolean mReported = false;

  /**
   * @param context
   */
  public RecentUserDetailLoader(Context context) {
    super(context);
  }

  private static Comparator<User> USERS_ALPHABETIC = new Comparator<User>() {
    @Override
    public int compare(User object1, User object2) {
      return object1.getUsername().toLowerCase().compareTo(
          object2.getUsername().toLowerCase());
    }
  };

  @Override
  protected void onStartLoading() {
    super.onStartLoading();
    Log.d(TAG, "onStartLoading isStarted=" + isStarted() + " isReset=" + isReset()
        + " isAbandoned=" + isAbandoned());
    if (!mReported) {
      final AuthenticationManager am = AuthenticationManager.getSingletonInstance(getContext());
      final List<User> result = Lists.newArrayList(am.getAllRecent());
      Log.d(TAG, "Load result size: " + result.size());
      Collections.sort(result, USERS_ALPHABETIC);
      deliverResult(result);
      mReported = true;
    }
  }

  @Override
  protected void onReset() {
    Log.d(TAG, "onReset");
    mReported = false;
    super.onReset();
  }

}
