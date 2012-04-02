package org.kegbot.kegtap.setup;

import org.kegbot.kegtap.R;
import org.kegbot.kegtap.util.PreferenceHelper;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.common.base.Strings;

public class SetupApiUrlFragment extends Fragment {

  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.setup_api_url_fragment, null);
    PreferenceHelper prefs = new PreferenceHelper(getActivity());
    EditText text = (EditText) mView.findViewById(R.id.apiUrl);
    final String existingUrl = prefs.getKegbotUrl().toString();
    if (!Strings.isNullOrEmpty(existingUrl)) {
      // Don't clobber the hint if empty.
      text.setText(existingUrl);
    }
    mView.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));
    return mView;
  }

  public String getApiUrl() {
    EditText text = (EditText) mView.findViewById(R.id.apiUrl);
    return text.getText().toString();
  }

}
