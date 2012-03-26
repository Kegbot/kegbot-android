/**
 *
 */
package org.kegbot.kegtap.setup;

import org.kegbot.kegtap.R;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class SetupProgressDialogFragment extends DialogFragment {

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

}
