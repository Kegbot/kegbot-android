package org.kegbot.app.setup;

import android.app.Fragment;

import com.google.common.base.Strings;

import org.kegbot.app.KegbotApplication;
import org.kegbot.app.R;

public class SetupSelectBackendStep extends SetupStep {

  private final SetupSelectBackendFragment mControlsFragment = new SetupSelectBackendFragment();

  public SetupSelectBackendStep(SetupActivity.SetupState state) {
    super(state);
  }

  @Override
  public Fragment getContentFragment() {
    return SetupTextFragment.withText(R.string.setup_select_backend_title,
        R.string.setup_select_backend_description);
  }

  @Override
  public Fragment getControlsFragment() {
    return mControlsFragment;
  }

  @Override
  public SetupStep advance() throws SetupValidationException {
    final String error = mControlsFragment.validate();
    if (!Strings.isNullOrEmpty(error)) {
      throw new SetupValidationException(error);
    }
    if (KegbotApplication.get(mControlsFragment.getActivity()).getConfig().isLocalBackend()) {
      return new SetupManagerPinStep(mState);
    } else {
      return new SetupKegbotUrlStep(mState);
    }
  }
}
