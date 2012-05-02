/**
 *
 */
package org.kegbot.app.setup;

import org.kegbot.app.R;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

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
