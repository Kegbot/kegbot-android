package org.kegbot.app.setup;

import android.app.Fragment;

import com.google.common.base.Strings;

import org.kegbot.app.R;

public class SetupLicenseStep extends SetupStep {

  private final SetupLicenseFragment mControlsFragment = new SetupLicenseFragment();

  public SetupLicenseStep(SetupActivity.SetupState state) {
    super(state);
  }

  @Override
  public Fragment getContentFragment() {
    return SetupTextFragment.withText(R.string.setup_license_title,
        R.string.setup_license_description);
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
    return new SetupSelectBackendStep(mState);
  }
}
