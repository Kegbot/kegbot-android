package org.kegbot.app.setup;

import android.app.Fragment;

import com.google.common.base.Strings;

import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.KegbotApplication;
import org.kegbot.app.R;
import org.kegbot.app.config.AppConfiguration;

public class SetupKegbotUrlStep extends SetupStep {

  private final SetupKegbotUrlFragment mControlsFragment = new SetupKegbotUrlFragment();

  public SetupKegbotUrlStep(SetupActivity.SetupState state) {
    super(state);
  }

  @Override
  public Fragment getContentFragment() {
    return SetupTextFragment.withText(R.string.setup_kegbot_url_title,
        R.string.setup_kegbot_url_description);
  }

  @Override
  public Fragment getControlsFragment() {
    return mControlsFragment;
  }

  @Override
  public SetupStep advance() throws SetupValidationException {
    final String error = mControlsFragment.validate();
    if (!Strings.isNullOrEmpty(error)) {
      mControlsFragment.onValidationFailed();
      throw new SetupValidationException(error);
    }

    final AppConfiguration config = KegbotApplication.get(mControlsFragment.getActivity()).getConfig();
    final KegbotApiImpl api = new KegbotApiImpl(config);

    try {
      if (api.supportsDeviceLink()) {
        return new SetupPairStep(mState);
      }
      return new SetupLoginStep(mState);
    } catch (KegbotApiException e) {
      throw new SetupValidationException("Error connecting to server: " + SetupFragment.toHumanError(e));
    }
  }
}
