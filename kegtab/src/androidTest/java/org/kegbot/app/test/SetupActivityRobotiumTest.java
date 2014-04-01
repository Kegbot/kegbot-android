package org.kegbot.app.test;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.RadioButton;

import com.robotium.solo.Solo;

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
    Button button = mSolo.getButton(getText(org.kegbot.app.R.string.setup_select_backend_local));
    assertNotNull(button);
    RadioButton radio = (RadioButton) button;
    assertTrue(radio.isChecked());
  }
  
  private String getText(int resId) {
    return getActivity().getText(resId).toString();
  }
  

}
