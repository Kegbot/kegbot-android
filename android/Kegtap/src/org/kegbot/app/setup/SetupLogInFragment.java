package org.kegbot.app.setup;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.util.PreferenceHelper;
import org.kegbot.app.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.common.base.Strings;

public class SetupLogInFragment extends SetupFragment {

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

  @Override
  public String validate() {
    final String username = getUsername();
    final String password = getPassword();

    if (Strings.isNullOrEmpty(username)) {
      return "Please enter a username.";
    } else if (Strings.isNullOrEmpty(password)) {
      return "Please enter a password";
    }

    PreferenceHelper prefs = new PreferenceHelper(getActivity());
    KegbotApi api = new KegbotApiImpl();
    api.setApiUrl(prefs.getKegbotUrl().toString());

    try {
      api.login(username, password);
    } catch (KegbotApiException e) {
      return "Error logging in: " + toHumanError(e);
    }

    final String apiKey;
    try {
      apiKey = api.getApiKey();
    } catch (KegbotApiException e) {
      return "Error fetching API key: " + toHumanError(e);
    }

    prefs.setUsername(username);
    prefs.setPassword(password);
    prefs.setApiKey(apiKey);

    return "";
  }

}
