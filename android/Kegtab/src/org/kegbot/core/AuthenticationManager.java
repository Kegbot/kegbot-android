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
package org.kegbot.core;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiNotFoundError;
import org.kegbot.app.KegtabBroadcast;
import org.kegbot.app.util.PreferenceHelper;
import org.kegbot.proto.Models.User;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class AuthenticationManager extends Manager {

  private static final String TAG = AuthenticationManager.class.getSimpleName();

  private static final long CACHE_EXPIRE_HOURS = 3;

  private final KegbotApi mApi;

  private final PreferenceHelper mPrefsHelper;

  private final Context mContext;

  private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

  private User fetchUserForToken(AuthenticationToken token) throws KegbotApiException {
    Log.d(TAG, "Loading token");
    org.kegbot.proto.Models.AuthenticationToken tok = mApi.getAuthToken(token
        .getAuthDevice(), token.getTokenValue());
    Log.d(TAG, "Got auth token: " + tok);
    if (!tok.getEnabled()) {
      throw new KegbotApiNotFoundError("Token not enabled.");
    } else if (!tok.hasUser()) {
      throw new KegbotApiNotFoundError("Token not assigned.");
    }
    return tok.getUser();
  }

  private final LoadingCache<AuthenticationToken, User> mAuthTokenCache = CacheBuilder
      .newBuilder().expireAfterWrite(CACHE_EXPIRE_HOURS, TimeUnit.HOURS).build(
          new CacheLoader<AuthenticationToken, User>() {
            @Override
            public User load(AuthenticationToken token) throws Exception {
              return fetchUserForToken(token);
            }
          });

  private final LoadingCache<String, User> mUserDetailCache = CacheBuilder.newBuilder()
      .expireAfterWrite(CACHE_EXPIRE_HOURS, TimeUnit.HOURS).build(
          new CacheLoader<String, User>() {
            @Override
            public User load(String username) throws Exception {
              Log.d(TAG, "Loading user: " + username);
              return mApi.getUserDetail(username);
            }
          });

  AuthenticationManager(Context context, KegbotApi api) {
    mContext = context.getApplicationContext();
    mApi = api;
    mPrefsHelper = new PreferenceHelper(mContext);
  }

  public User authenticateToken(AuthenticationToken token) {
    if (!mPrefsHelper.getCacheCredentials()) {
      try {
        return fetchUserForToken(token);
      } catch (KegbotApiNotFoundError e) {
        Log.d(TAG, "Token is not assigned to anyone: " + token);
        return null;
      } catch (KegbotApiException e) {
        Log.w(TAG, "Error fetching token: " + e.getCause(), e);
        return null;
      }
    }

    try {
      return mAuthTokenCache.get(token);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause != null && cause instanceof KegbotApiNotFoundError) {
        Log.d(TAG, "Token is not assigned to anyone: " + token);
        return null;
      } else {
        Log.w(TAG, "Error fetching token: " + e.getCause(), e);
      }
      return null;
    }
  }

  public void authenticateUser(User user) {
    mUserDetailCache.put(user.getUsername(), user);
    final Intent intent = KegtabBroadcast.getUserAuthedBroadcastIntent(user.getUsername());
    mContext.sendBroadcast(intent);
  }

  public User authenticateUsername(String username) {
    // TODO(mikey): use pin
    try {
      final User user = mUserDetailCache.get(username);
      if (user != null) {
        authenticateUser(user);
      }
      return user;
    } catch (ExecutionException e) {
      Log.w(TAG, "Error fetching user: " + e, e);
      return null;
    }
  }

  public Future<User> authenticateUsernameAsync(final String username) {
    return mExecutorService.submit(new Callable<User>() {
      @Override
      public User call() {
        return authenticateUsername(username);
      }
    });
  }

  public User getUserDetail(String username) {
    return mUserDetailCache.getIfPresent(username);
  }

  public Set<User> getAllRecent() {
    return Sets.newLinkedHashSet(mUserDetailCache.asMap().values());
  }

  public void clearCache() {
    // TODO(mikey): clear me on config change broadcast
    mUserDetailCache.invalidateAll();
    mAuthTokenCache.invalidateAll();
  }

}
