package org.kegbot.kegtap.setup;

import org.kegbot.kegtap.R;
import org.kegbot.kegtap.util.PreferenceHelper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

public class SetupRunCoreFragment extends SetupFragment {

  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.setup_run_core_fragment, null);
    PreferenceHelper prefs = new PreferenceHelper(getActivity());
    CheckBox box = (CheckBox) mView.findViewById(R.id.runCore);
    box.setChecked(prefs.getRunCore());
    mView.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));
    return mView;
  }

  public boolean getRunCore() {
    CheckBox box = (CheckBox) mView.findViewById(R.id.runCore);
    return box.isChecked();
  }

  @Override
  public String validate() {
    PreferenceHelper prefs = new PreferenceHelper(getActivity());
    prefs.setRunCore(getRunCore());
    return "";
  }

}
