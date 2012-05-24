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
package org.kegbot.app.setup;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class SetupAlertDialogFragment extends DialogFragment {

  public static SetupAlertDialogFragment newInstance(String message) {
    final Bundle args = new Bundle();
    args.putString("message", message);
    final SetupAlertDialogFragment frag = new SetupAlertDialogFragment();
    frag.setArguments(args);
    return frag;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final String message = getArguments().getString("message", "Error.");

    return new AlertDialog.Builder(getActivity())
        .setIcon(R.drawable.ic_dialog_alert)
        .setMessage(message)
        .setPositiveButton("Ok", null)
        .create();
  }

}
