/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.proto.Models.User;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.collect.Lists;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class UserDetailListLoader extends AsyncTaskLoader<List<User>> {

  private static final String TAG = UserDetailListLoader.class.getSimpleName();

  private KegbotApi mApi;

  private final List<User> mUsers = Lists.newArrayList();

  private final long MAX_LOAD_AGE_MILLIS = TimeUnit.SECONDS.toMillis(15);

  private long mLastLoadMillis = 0;

  private static Comparator<User> USERS_ALPHABETIC = new Comparator<User>() {
    @Override
    public int compare(User object1, User object2) {
      return object1.getUsername().toLowerCase().compareTo(
          object2.getUsername().toLowerCase());
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
  public List<User> loadInBackground() {
    Log.d(TAG, "loadInBackground");
    try {
      final List<User> result = mApi.getUsers();
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
