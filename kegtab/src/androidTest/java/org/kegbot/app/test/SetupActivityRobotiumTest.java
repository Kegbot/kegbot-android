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

package org.kegbot.app.test;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.RadioButton;

import com.robotium.solo.Solo;

import org.kegbot.app.R;
import org.kegbot.app.setup.SetupActivity;

public class SetupActivityRobotiumTest extends ActivityInstrumentationTestCase2<SetupActivity> {

  private Solo mSolo;

  public SetupActivityRobotiumTest() {
    super(SetupActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    mSolo = new Solo(getInstrumentation(), getActivity());
  }

  public void testSetupWithStandaloneMode() {
    String expectedText = getText(org.kegbot.app.R.string.setup_welcome_title);
    assertTrue(mSolo.searchText(expectedText));

    mSolo.clickOnButton("Next");
    assertTrue(mSolo.searchText(getText(org.kegbot.app.R.string.setup_license_title)));

    mSolo.clickOnCheckBox(0);
    mSolo.clickOnButton("Next");
    mSolo.waitForText(getText(org.kegbot.app.R.string.setup_license_error));
    mSolo.clickOnButton("Ok");

    mSolo.clickOnCheckBox(0);
    mSolo.clickOnCheckBox(1);
    mSolo.clickOnButton("Next");
    mSolo.waitForText(getText(org.kegbot.app.R.string.setup_license_error));
    mSolo.clickOnButton("Ok");

    mSolo.clickOnCheckBox(0);
    mSolo.clickOnButton("Next");
    mSolo.waitForText(getText(org.kegbot.app.R.string.setup_select_backend_title));

    mSolo.clickOnRadioButton(1);
    mSolo.waitForText(getText(R.string.setup_select_backend_local_warning_ok));
    mSolo.clickOnButton(getText(R.string.setup_select_backend_local_warning_ok));
    Button button = mSolo.getButton(getText(org.kegbot.app.R.string.setup_select_backend_local));
    assertNotNull(button);
    RadioButton radio = (RadioButton) button;
    assertTrue(radio.isChecked());
  }

  private String getText(int resId) {
    return getActivity().getText(resId).toString();
  }


}
