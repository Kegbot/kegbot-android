/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import org.kegbot.app.R;

public class SetupProgressDialogFragment extends DialogFragment {

  public interface Listener {
    public void onCancel(DialogInterface dialog);
  }

  private final Listener mListener;

  public SetupProgressDialogFragment(Listener listener) {
    mListener = listener;
  }

  public SetupProgressDialogFragment() {
    this(null);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    ProgressDialog dialog = new ProgressDialog(getActivity());
    dialog.setIndeterminate(true);
    dialog.setCancelable(false);
    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    dialog.setMessage(getActivity().getResources().getString(
        R.string.setup_dialog_validate_description));
    dialog.setTitle(getActivity().getResources().getString(R.string.setup_dialog_validate_title));
    return dialog;
  }

  @Override
  public void onCancel(DialogInterface dialog) {
    if (mListener != null) {
      mListener.onCancel(dialog);
    }
    super.onCancel(dialog);
  }

}
