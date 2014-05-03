package org.kegbot.app.setup;

import android.app.Fragment;

import com.google.common.base.Strings;

import org.kegbot.app.R;

public class SetupManagerPinStep extends SetupStep {

  private final Fragment mContentFragment = new SetupTextFragment() {
    @Override
    public String getTitle() {
      return getString(R.string.setup_manager_pin_title);
    }

    @Override
    public String getDescription() {
      return getString(R.string.setup_manager_pin_description);
    }
  };

  private final SetupManagerPinFragment mControlsFragment = new SetupManagerPinFragment();

  public SetupManagerPinStep(SetupActivity.SetupState state) {
    super(state);
  }

  @Override
  public Fragment getContentFragment() {
    return mContentFragment;
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
    return new SetupCompleteStep(mState);
  }
}
