package org.kegbot.kegtap;

import java.util.List;

import org.kegbot.core.FlowManager;
import org.kegbot.kegtap.setup.SetupActivity;
import org.kegbot.kegtap.util.PreferenceHelper;

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
import android.util.Log;
import android.view.MenuItem;

import com.google.common.base.Strings;

public class SettingsActivity extends PreferenceActivity {

  private static final String TAG = SettingsActivity.class.getSimpleName();

  private static final int REQUEST_PIN = 100;

  private PreferenceHelper mPrefs;
  private boolean mPinValid = false;
  private boolean mPinChecked = false;

  @Override
  protected void onStart() {
    super.onStart();
    mPrefs = new PreferenceHelper(getApplicationContext());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (Strings.isNullOrEmpty(mPrefs.getPin())) {
      mPinValid = true;
    }

    if (!isFinishing()) {
      if (!mPinValid) {
        if (!mPinChecked) {
          Log.d(TAG, "Checking pin...");
          final Intent intent = new Intent(this, EnterPinActivity.class);
          startActivityForResult(intent, REQUEST_PIN);
          mPinChecked = true;
        } else {
          Log.d(TAG, "Pin checked, exiting.");
          finish();
        }
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_PIN) {
      Log.d(TAG, "Pin result: " + resultCode);
      if (resultCode == RESULT_OK) {
        mPinValid = true;
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onBuildHeaders(List<Header> target) {
    loadHeadersFromResource(R.xml.settings_headers, target);
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

  public static class KegeratorFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings_kegerator);

      handleCoreEnabledChanged();

      findPreference(PreferenceHelper.KEY_RUN_CORE).setOnPreferenceChangeListener(
          new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
              handleCoreEnabledChanged();
              return true;
            }
          });

      findPreference("controller_type").setOnPreferenceChangeListener(
          new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
              handleControllerTypeChanged();
              return true;
            }
          });

      findPreference("idle_timeout_seconds").setOnPreferenceChangeListener(
          new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
              final PreferenceHelper helper = new PreferenceHelper(getActivity());
              FlowManager.getSingletonInstance()
                  .setDefaultIdleTimeMillis(helper.getIdleTimeoutMs());
              return true;
            }
          });
    }

    private void handleCoreEnabledChanged() {
      final CheckBoxPreference enablePref = (CheckBoxPreference) findPreference(PreferenceHelper.KEY_RUN_CORE);
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

  public static class AboutFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings_about);
      final ActionBar actionBar = getActivity().getActionBar();
      if (actionBar != null) {
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO);
      }

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

  public static void startSettingsActivity(Context context) {
    Intent intent = new Intent(context, SettingsActivity.class);
    context.startActivity(intent);
  }

}
