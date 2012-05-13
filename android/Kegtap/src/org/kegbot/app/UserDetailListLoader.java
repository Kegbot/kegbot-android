/**
 *
 */
package org.kegbot.app;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.proto.Api.UserDetail;
import org.kegbot.proto.Api.UserDetailSet;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.collect.Lists;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class UserDetailListLoader extends AsyncTaskLoader<List<UserDetail>> {

  private static final String TAG = UserDetailListLoader.class.getSimpleName();

  private KegbotApi mApi;

  private final List<UserDetail> mUsers = Lists.newArrayList();

  private final long MAX_LOAD_AGE_MILLIS = TimeUnit.SECONDS.toMillis(15);

  private long mLastLoadMillis = 0;

  private static Comparator<UserDetail> USERS_ALPHABETIC = new Comparator<UserDetail>() {
    @Override
    public int compare(UserDetail object1, UserDetail object2) {
      return object1.getUser().getUsername().toLowerCase().compareTo(
          object2.getUser().getUsername().toLowerCase());
    }
  };

  /**
   * @param context
   */
  public UserDetailListLoader(Context context) {
    super(context);
    mApi = KegbotApiImpl.getSingletonInstance();
  }

  @Override
  public List<UserDetail> loadInBackground() {
    Log.d(TAG, "loadInBackground");
    try {
      final UserDetailSet apiResult = mApi.getUsers();
      final List<UserDetail> result = Lists.newArrayList(apiResult.getUsersList());
      mUsers.clear();
      mUsers.addAll(result);
      Collections.sort(mUsers, USERS_ALPHABETIC);
      return mUsers;
    } catch (KegbotApiException e) {
      // TODO(mikey): retry?
      Log.d(TAG, "Error loading. ", e);
      return Lists.newArrayList();
    }
  }

  @Override
  protected void onStartLoading() {
    Log.d(TAG, "onStartLoading");
    final long now = SystemClock.uptimeMillis();
    if ((now - mLastLoadMillis) > MAX_LOAD_AGE_MILLIS) {
      Log.d(TAG, "Forcing load, mLastLoadMillis=" + mLastLoadMillis);
      mLastLoadMillis = now;
      forceLoad();
    } else {
      Log.d(TAG, "delivering cached results");
      deliverResult(mUsers);
    }
  }

  @Override
  protected void onReset() {
    Log.d(TAG, "onReset");
    super.onReset();
    onStopLoading();
  }

  /**
   * Handles a request to stop the Loader.
   */
  @Override
  protected void onStopLoading() {
      // Attempt to cancel the current load task if possible.
      cancelLoad();
  }

}
