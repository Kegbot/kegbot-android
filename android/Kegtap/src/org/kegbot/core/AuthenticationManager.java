/**
 *
 */
package org.kegbot.core;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.api.KegbotApiNotFoundError;
import org.kegbot.kegtap.KegtapBroadcast;
import org.kegbot.proto.Api.UserDetail;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class AuthenticationManager {

  private static final String TAG = AuthenticationManager.class.getSimpleName();

  private static final long CACHE_EXPIRE_HOURS = 3;

  private static AuthenticationManager sSingleton = null;

  private final KegbotApi mApi = KegbotApiImpl.getSingletonInstance();

  private final Context mContext;

  private final LoadingCache<AuthenticationToken, UserDetail> mAuthTokenCache = CacheBuilder
      .newBuilder().expireAfterWrite(CACHE_EXPIRE_HOURS, TimeUnit.HOURS).build(
          new CacheLoader<AuthenticationToken, UserDetail>() {
            @Override
            public UserDetail load(AuthenticationToken token) throws Exception {
              Log.d(TAG, "Loading token");
              org.kegbot.proto.Models.AuthenticationToken tok = mApi.getAuthToken(token
                  .getAuthDevice(), token.getTokenValue());
              final String username = tok.getUsername();
              if (!Strings.isNullOrEmpty(username)) {
                return mApi.getUserDetail(username);
              }
              return null;
            }
          });

  private final LoadingCache<String, UserDetail> mUserDetailCache = CacheBuilder.newBuilder()
      .expireAfterWrite(CACHE_EXPIRE_HOURS, TimeUnit.HOURS).build(
          new CacheLoader<String, UserDetail>() {
            @Override
            public UserDetail load(String username) throws Exception {
              Log.d(TAG, "Loading user: " + username);
              return mApi.getUserDetail(username);
            }
          });

  private AuthenticationManager(Context context) {
    mContext = context.getApplicationContext();
  }

  public UserDetail authenticateToken(AuthenticationToken token) {
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

  public UserDetail authenticateUsername(String username, String pin) {
    // TODO(mikey): use pin
    try {
      return mUserDetailCache.get(username);
    } catch (ExecutionException e) {
      Log.w(TAG, "Error fetching user: " + e, e);
      return null;
    }
  }

  public UserDetail getUserDetail(String username) {
    return mUserDetailCache.getIfPresent(username);
  }

  public void noteUserAuthenticated(UserDetail userDetail) {
    mUserDetailCache.put(userDetail.getUser().getUsername(), userDetail);
    final Intent intent = KegtapBroadcast.getUserAuthedBroadcastIntent(userDetail.getUser()
        .getUsername());
    mContext.sendBroadcast(intent);
  }

  public void noteUserAuthenticated(UserDetail userDetail, String tapName) {
    mUserDetailCache.put(userDetail.getUser().getUsername(), userDetail);
    final Intent intent = KegtapBroadcast.getUserAuthedBroadcastIntent(userDetail.getUser()
        .getUsername());
    intent.putExtra(KegtapBroadcast.DRINKER_SELECT_EXTRA_TAP_NAME, tapName);
    mContext.sendBroadcast(intent);
  }

  public Set<UserDetail> getAllRecent() {
    return Sets.newLinkedHashSet(mUserDetailCache.asMap().values());
  }

  public void clearCache() {
    // TODO(mikey): clear me on config change broadcast
    mUserDetailCache.invalidateAll();
    mAuthTokenCache.invalidateAll();
  }

  public static synchronized AuthenticationManager getSingletonInstance(Context context) {
    if (sSingleton == null) {
      sSingleton = new AuthenticationManager(context);
    }
    return sSingleton;
  }

}
