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
