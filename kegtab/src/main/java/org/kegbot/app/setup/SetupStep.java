/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */

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
