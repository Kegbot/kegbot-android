package org.kegbot.kegtap;

import java.util.List;

import android.app.ActionBar;
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
import android.view.MenuItem;

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
      handleEnableManagerPinChanged();
      final ActionBar actionBar = getActivity().getActionBar();
      if (actionBar != null) {
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO);
      }

      final CheckBoxPreference enablePref = (CheckBoxPreference) findPreference("use_manager_pin");
      enablePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          handleEnableManagerPinChanged();
          return true;
        }
      });

    }

    private void handleEnableManagerPinChanged() {
      final CheckBoxPreference enablePref = (CheckBoxPreference) findPreference("use_manager_pin");
      final boolean enabled = enablePref.isChecked();
      findPreference("manager_pin").setEnabled(enabled);
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

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // app icon in Action Bar clicked; go home
        Intent intent = new Intent(this, KegtapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // TODO Auto-generated method stub
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onCreate(savedInstanceState);
  }

  public static void startSettingsActivity(Context context) {
    Intent intent = new Intent(context, SettingsActivity.class);
    context.startActivity(intent);
  }

}
