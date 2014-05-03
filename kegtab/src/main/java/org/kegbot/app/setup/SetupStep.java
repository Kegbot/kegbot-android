package org.kegbot.app.setup;

import android.app.Fragment;

import org.kegbot.app.R;

/**
 * Created by mikey on 5/2/14.
 */
public abstract class SetupStep {

  protected final SetupActivity.SetupState mState;

  public SetupStep(SetupActivity.SetupState state) {
    mState = state;
  }

  public abstract Fragment getContentFragment();

  public abstract Fragment getControlsFragment();

  public void onDisplay() {
    mState.setNextButtonEnabled(true);
    mState.setNextButtonText(R.string.setup_button_next);
  }

  public abstract SetupStep advance() throws SetupValidationException;

}
