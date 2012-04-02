package org.kegbot.kegtap.setup;

import org.kegbot.kegtap.R;
import org.kegbot.kegtap.util.PreferenceHelper;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class SetupManagerPinFragment extends Fragment {

  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.setup_manager_pin_fragment, null);
    PreferenceHelper prefs = new PreferenceHelper(getActivity());
    EditText text = (EditText) mView.findViewById(R.id.managerPin);
    text.setText(prefs.getPin());
    mView.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));
    return mView;
  }

  public String getPin() {
    EditText text = (EditText) mView.findViewById(R.id.managerPin);
    return text.getText().toString();
  }

}
