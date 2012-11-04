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

import org.kegbot.core.AuthenticationManager;
import org.kegbot.core.KegbotCore;
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
  private KegbotCore mCore;
  private boolean mReported = false;

  public RecentUserDetailLoader(Context context, KegbotCore core) {
    super(context);
    mCore = core;
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
      final AuthenticationManager am = mCore.getAuthenticationManager();
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
