/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
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

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import org.kegbot.app.service.KegbotCoreService;
import org.kegbot.app.settings.ThirdPartyLicensesActivity;
import org.kegbot.app.setup.SetupActivity;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    KegbotCoreService.stopService(this);
  }

  @Override
  public void onBuildHeaders(List<Header> target) {
    loadHeadersFromResource(R.xml.settings_headers, target);
  }

  @Override
  protected boolean isValidFragment(String fragmentName) {
    return true;
  }

  public static class GeneralFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings_general);

      findPreference("run_setup").setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          final Intent setupIntent = new Intent(getActivity(), SetupActivity.class);
          setupIntent.putExtra(SetupActivity.EXTRA_REASON, SetupActivity.EXTRA_REASON_USER);
          startActivity(setupIntent);
          return true;
        }
      });

      final ActionBar actionBar = getActivity().getActionBar();
      if (actionBar != null) {
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO);
      }
    }
  }

  public static class AccessFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings_access);
    }
  }

  public static class KegeratorFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings_kegerator);
    }
  }

  public static class AboutFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings_about);
      final ActionBar actionBar = getActivity().getActionBar();
      if (actionBar != null) {
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO);
      }

      findPreference("third_party").setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          final Intent thirdPartyIntent = new Intent(getActivity(), ThirdPartyLicensesActivity.class);
          startActivity(thirdPartyIntent);
          return true;
        }
      });

      final PackageManager pm = getActivity().getPackageManager();
      final String versionString;
      final int versionCode;
      try {
        final PackageInfo info = pm.getPackageInfo(getActivity().getPackageName(), 0);
        versionString = info.versionName;
        versionCode = info.versionCode;
      } catch (NameNotFoundException e) {
        // Impossible!
        throw new IllegalStateException(e);
      }

      findPreference("version").setSummary(String.format("%s (build #%d)", versionString,
          Integer.valueOf(versionCode)));
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // app icon in Action Bar clicked; go home
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public static void startSettingsActivity(Context context) {
    Intent intent = new Intent(context, SettingsActivity.class);
    PinActivity.startThroughPinActivity(context, intent);
  }

}
