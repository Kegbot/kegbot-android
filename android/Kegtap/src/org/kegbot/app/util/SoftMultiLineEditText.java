/**
 *
 */
package org.kegbot.app.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/*
 * http://stackoverflow.com/questions/5014219/multiline-edittext-with-done-softinput-action-label-on-2-3
 */
public class SoftMultiLineEditText extends EditText {

  public SoftMultiLineEditText(Context context) {
    super(context);
  }

  public SoftMultiLineEditText(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public SoftMultiLineEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
      InputConnection connection = super.onCreateInputConnection(outAttrs);
      int imeActions = outAttrs.imeOptions&EditorInfo.IME_MASK_ACTION;
      if ((imeActions & EditorInfo.IME_ACTION_DONE) != 0) {
          // clear the existing action
          outAttrs.imeOptions ^= imeActions;
          // set the DONE action
          outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
      }
      if ((outAttrs.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
          outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
      }
      return connection;
  }

}
