/**
 *
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
