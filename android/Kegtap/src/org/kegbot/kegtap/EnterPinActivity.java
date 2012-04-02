/**
 *
 */
package org.kegbot.kegtap;

import org.kegbot.kegtap.util.PreferenceHelper;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class EnterPinActivity extends Activity {

  private PreferenceHelper mPrefs;
  private EditText mPinText;
  private Button mPinButton;

  @Override
  protected void onStart() {
    super.onStart();
    setContentView(R.layout.enter_pin_activity);

    mPrefs = new PreferenceHelper(getApplicationContext());
    mPinText = (EditText) findViewById(R.id.managerPin);
    mPinButton = (Button) findViewById(R.id.submitButton);

    mPinButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        final String pinText = mPinText.getText().toString();
        if (mPrefs.getPin().equalsIgnoreCase(pinText)) {
          setResult(RESULT_OK);
          finish();
        }
      }
    });
  }

}
