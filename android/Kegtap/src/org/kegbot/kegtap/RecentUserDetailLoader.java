/**
 *
 */
package org.kegbot.kegtap;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.kegbot.core.AuthenticationManager;
import org.kegbot.proto.Api.UserDetail;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.google.common.collect.Lists;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class RecentUserDetailLoader extends AsyncTaskLoader<List<UserDetail>> {

  private static final String TAG = RecentUserDetailLoader.class.getSimpleName();

  /**
   * @param context
   */
  public RecentUserDetailLoader(Context context) {
    super(context);
  }

  private static Comparator<UserDetail> USERS_ALPHABETIC = new Comparator<UserDetail>() {
    @Override
    public int compare(UserDetail object1, UserDetail object2) {
      return object1.getUser().getUsername().toLowerCase().compareTo(
          object2.getUser().getUsername().toLowerCase());
    }
  };

  @Override
  public List<UserDetail> loadInBackground() {
    Log.d(TAG, "loadInBackground");
    final AuthenticationManager am = AuthenticationManager.getSingletonInstance();
    final List<UserDetail> result = Lists.newArrayList(am.getAllRecent());
    Collections.sort(result, USERS_ALPHABETIC);
    return result;
  }

  @Override
  protected void onStartLoading() {
    Log.d(TAG, "onStartLoading");
    forceLoad();
    super.onStartLoading();
  }

  @Override
  protected void onReset() {
    Log.d(TAG, "onReset");
    super.onReset();
  }

}
