package org.kegbot.app.setup;

import android.app.Fragment;

import org.kegbot.app.R;

public class SetupCompleteStep extends SetupStep {

  public SetupCompleteStep(SetupActivity.SetupState state) {
    super(state);
  }

  @Override
  public Fragment getContentFragment() {
    mState.setNextButtonText(R.string.setup_button_finish);
    return SetupTextFragment.withText(R.string.setup_finished_title,
        R.string.setup_finished_description);
  }

  @Override
  public Fragment getControlsFragment() {
    return null;
  }

  @Override
  public SetupStep advance() throws SetupValidationException {
    return null;
  }
}
