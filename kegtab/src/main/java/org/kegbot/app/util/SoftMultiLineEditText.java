/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
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
    int imeActions = outAttrs.imeOptions & EditorInfo.IME_MASK_ACTION;
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
