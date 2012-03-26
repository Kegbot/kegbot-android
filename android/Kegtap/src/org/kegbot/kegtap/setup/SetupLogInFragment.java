package org.kegbot.kegtap.setup;

import org.kegbot.kegtap.R;
import org.kegbot.kegtap.util.PreferenceHelper;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class SetupLogInFragment extends Fragment {

  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.setup_log_in_fragment, null);
    PreferenceHelper prefs = new PreferenceHelper(getActivity());

    EditText text = (EditText) mView.findViewById(R.id.apiUsername);
    text.setText(prefs.getUsername());

    text = (EditText) mView.findViewById(R.id.apiPassword);
    text.setText(prefs.getPassword());

    mView.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));
    return mView;
  }

  public String getUsername() {
    EditText text = (EditText) mView.findViewById(R.id.apiUsername);
    return text.getText().toString();
  }

  public String getPassword() {
    EditText text = (EditText) mView.findViewById(R.id.apiPassword);
    return text.getText().toString();
  }

}
