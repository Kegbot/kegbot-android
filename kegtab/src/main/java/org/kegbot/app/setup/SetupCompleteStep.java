package org.kegbot.app.setup;

import android.app.Fragment;

import org.kegbot.app.R;

public class SetupCompleteStep extends SetupStep {

  private final Fragment mContentFragment = new SetupTextFragment() {
    @Override
    public String getTitle() {
      return getString(R.string.setup_finished_title);
    }

    @Override
    public String getDescription() {
      return getString(R.string.setup_finished_description);
    }
  };

  public SetupCompleteStep(SetupActivity.SetupState state) {
    super(state);
  }

  @Override
  public Fragment getContentFragment() {
    mState.setNextButtonText(R.string.setup_button_finish);
    return mContentFragment;
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
