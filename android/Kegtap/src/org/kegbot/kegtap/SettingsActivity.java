package org.kegbot.kegtap;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class SettingsActivity extends PreferenceActivity {

  @Override
  public void onBuildHeaders(List<Header> target) {
    loadHeadersFromResource(R.xml.settings_headers, target);
  }

  public static class GeneralFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings_general);
    }

  }

  public static class KegeratorFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings_kegerator);

      handleCoreEnabledChanged();

      CheckBoxPreference enablePref = (CheckBoxPreference) findPreference("run_core");
      enablePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          handleCoreEnabledChanged();
          return true;
        }
      });

      ListPreference controllerTypePref = (ListPreference) findPreference("controller_type");
      controllerTypePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          handleControllerTypeChanged();
          return true;
        }
      });
    }

    private void handleCoreEnabledChanged() {
      final CheckBoxPreference enablePref = (CheckBoxPreference) findPreference("run_core");
      boolean enabled = enablePref.isChecked();
      findPreference("controller_type").setEnabled(enabled);

      if (!enabled) {
        // getActivity().sendBroadcast(intent)
      }
    }

    private void handleControllerTypeChanged() {
      ListPreference controllerTypePref = (ListPreference) findPreference("controller_type");
    }

  }

  public static void startSettingsActivity(Context context) {
    Intent intent = new Intent(context, SettingsActivity.class);
    context.startActivity(intent);
  }

}
