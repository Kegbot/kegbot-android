package org.kegbot.app.setup;

import android.app.Fragment;

import org.kegbot.app.R;

public class SetupWelcomeStep extends SetupStep {
  @Override
  public Fragment getContentFragment() {
    return new SetupTextFragment() {
      @Override
      public String getTitle() {
        return getString(R.string.setup_welcome_title);
      }

      @Override
      public String getDescription() {
        return getString(R.string.setup_welcome_description);
      }
    };
  }

  public SetupWelcomeStep(SetupActivity.SetupState state) {
    super(state);
  }

  @Override
  public Fragment getControlsFragment() {
    return null;
  }

  @Override
  public SetupStep advance() throws SetupValidationException {
    return new SetupLicenseStep(mState);
  }
}
