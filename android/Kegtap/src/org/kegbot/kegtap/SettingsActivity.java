package org.kegbot.kegtap;

import static org.kegbot.kegtap.util.PreferenceUtils.SELECTED_KEGBOT_KEY;

import java.util.ArrayList;
import java.util.List;

import org.kegbot.kegtap.util.KegbotDescriptor;
import org.kegbot.kegtap.util.PreferenceUtils;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
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
      mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
      // setupKegbotSelectPreference();
    }

    private void setupKegbotSelectPreference() {
      ListPreference preference = (ListPreference) findPreference(SELECTED_KEGBOT_KEY);
      preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          ListPreference listPref = (ListPreference) preference;
          PreferenceUtils.setKegbotName(mPreferences,
              listPref.getEntries()[listPref.findIndexOfValue((String) newValue)].toString());
          return true;
        }
      });
      new GetSelectionsTask().execute();
    }

    private class GetSelectionsTask extends AsyncTask<Void, Void, List<KegbotDescriptor>> {

      @Override
      protected void onPreExecute() {
        ListPreference preference = (ListPreference) findPreference(SELECTED_KEGBOT_KEY);
        preference.setEnabled(false);
      }

      @Override
      protected List<KegbotDescriptor> doInBackground(Void... params) {
        // TODO: in the future, allow selection from API, or custom. For now, just return prod or
        // dev.
        List<KegbotDescriptor> returnList = new ArrayList<KegbotDescriptor>();
        returnList.add(new KegbotDescriptor("Gertie", Uri.parse("http://oldgertie.kegbot.net")));
        returnList.add(new KegbotDescriptor("Kegbot SFO", Uri.parse("http://kegbot.net/sfo")));
        return returnList;
      }

      @Override
      protected void onPostExecute(List<KegbotDescriptor> result) {
        ListPreference preference = (ListPreference) findPreference(SELECTED_KEGBOT_KEY);
        preference.setEntries(KegbotDescriptor.getNames(result));
        preference.setEntryValues(KegbotDescriptor.getEntryValues(result));
        preference.setEnabled(true);
      }
    }
  }

  public static class KegeratorFragment extends PreferenceFragment {

    private SharedPreferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.settings_kegerator);

      setEnabledStates();

      mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

      CheckBoxPreference enablePref = (CheckBoxPreference) findPreference("run_core");
      enablePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          setEnabledStates();
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

      // setupKegbotSelectPreference();
    }

    private void setEnabledStates() {
      CheckBoxPreference enablePref = (CheckBoxPreference) findPreference("run_core");

      boolean enabled = enablePref.isChecked();
      findPreference("controller_type").setEnabled(enabled);
    }

    private void handleControllerTypeChanged() {
      ListPreference controllerTypePref = (ListPreference) findPreference("controller_type");
    }

  }


}
