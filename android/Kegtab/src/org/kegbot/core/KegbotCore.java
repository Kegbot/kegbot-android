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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.app.util.PreferenceHelper;
import org.kegbot.core.FlowManager.Clock;

import android.content.Context;

import com.google.common.collect.Lists;


/**
 * Top-level class implementing the Kegbot core.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class KegbotCore {

  private static KegbotCore sInstance;

  private final PreferenceHelper mPreferences;
  private final TapManager mTapManager;
  private final FlowManager mFlowManager;
  private final AuthenticationManager mAuthenticationManager;
  private final KegbotApi mApi;
  private final ConfigurationManager mConfigurationManager;

  private final List<Manager> mManagers = Lists.newArrayList();

  private final FlowManager.Clock mClock = new Clock() {
    @Override
    public long currentTimeMillis() {
      return System.currentTimeMillis();
    }
  };

  public KegbotCore(Context context) {
    mPreferences = new PreferenceHelper(context);
    mTapManager = new TapManager();
    mFlowManager = new FlowManager(mTapManager, mClock);

    mApi = new KegbotApiImpl();
    mApi.setApiUrl(mPreferences.getApiUrl());
    mApi.setApiKey(mPreferences.getApiKey());

    mAuthenticationManager = new AuthenticationManager(context, mApi);
    mConfigurationManager = new ConfigurationManager();

    mManagers.add(mTapManager);
    mManagers.add(mFlowManager);
    mManagers.add(mAuthenticationManager);
    mManagers.add(mFlowManager);
    mManagers.add(mConfigurationManager);
  }

  /**
   * @return the preferences
   */
  public PreferenceHelper getPreferences() {
    return mPreferences;
  }

  /**
   * @return the tapManager
   */
  public TapManager getTapManager() {
    return mTapManager;
  }

  /**
   * @return the authenticationManager
   */
  public AuthenticationManager getAuthenticationManager() {
    return mAuthenticationManager;
  }

  /**
   * @return the api
   */
  public KegbotApi getApi() {
    return mApi;
  }

  /**
   * @return the flowManager
   */
  public FlowManager getFlowManager() {
    return mFlowManager;
  }

  /**
   * @return the configurationManager
   */
  public ConfigurationManager getConfigurationManager() {
    return mConfigurationManager;
  }

  public void dump(PrintWriter printWriter) {
    StringWriter writer = new StringWriter();
    IndentingPrintWriter newWriter = new IndentingPrintWriter(writer, "  ");
    for (final Manager manager : mManagers) {
      newWriter.printf("## %s\n", manager.getName());
      newWriter.increaseIndent();
      manager.dump(newWriter);
      newWriter.decreaseIndent();
      newWriter.println();
    }
    printWriter.write(writer.toString());
  }

  public static KegbotCore getInstance(Context context) {
    synchronized (KegbotCore.class) {
      if (sInstance == null) {
        sInstance = new KegbotCore(context.getApplicationContext());
      }
    }
    return sInstance;
  }

}
