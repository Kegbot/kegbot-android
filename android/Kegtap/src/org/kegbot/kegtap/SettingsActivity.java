package org.kegbot.kegtap;

import java.util.List;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {

  @Override
  public void onBuildHeaders(List<Header> target) {
    loadHeadersFromResource(R.xml.settings_headers, target);
  }

  public static class GeneralFragment extends PreferenceFragment {

    private SharedPreferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings_general);
      mPreferences = PreferenceManager
      .getDefaultSharedPreferences(getActivity());
    }

  }

  public static class KegeratorFragment extends PreferenceFragment {

    private SharedPreferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings_kegerator);

      handleCoreEnabledChanged();

      mPreferences = PreferenceManager
      .getDefaultSharedPreferences(getActivity());

      CheckBoxPreference enablePref = (CheckBoxPreference) findPreference("run_core");
      enablePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          handleCoreEnabledChanged();
          return true;
        }
      });

      ListPreference controllerTypePref = (ListPreference) findPreference("controller_type");
      controllerTypePref
      .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference,
            Object newValue) {
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

}
