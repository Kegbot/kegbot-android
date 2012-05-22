package org.kegbot.app;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.camera.CameraFragment;
import org.kegbot.app.setup.SetupAlertDialogFragment;
import org.kegbot.app.setup.SetupProgressDialogFragment;
import org.kegbot.proto.Models.User;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class DrinkerRegisterFragment extends Fragment {

  private static final String TAG = DrinkerRegisterFragment.class.getSimpleName();

  private Button mSubmitButton;
  private EditText mUsername;
  private EditText mEmail;
  private EditText mPassword;
  private CameraFragment mCameraFragment;

  private DialogFragment mDialog;

  private class RegistrationTask extends AsyncTask<Void, Void, User> {

    @Override
    protected User doInBackground(Void... params) {
      KegbotApi api = KegbotApiImpl.getSingletonInstance();
      Log.d(TAG, "Registering...");
      final String imagePath = mCameraFragment.getLastFilename();
      try {
        return api.register(mUsername.getText().toString(), mEmail.getText().toString(),
            mPassword.getText().toString(), imagePath);
      } catch (KegbotApiException e) {
        Log.w(TAG, "Registration failed: " + e.toString() + " errors=" + e.getErrors());
      }
      return null;
    }

    @Override
    protected void onPostExecute(User result) {
      hideDialog();
      if (result != null) {
        ((DrinkerSelectActivity) getActivity()).handlerUserSelected(result);
      }
    }

  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.create_drinker_activity, null);

    mUsername = (EditText) view.findViewById(R.id.username);
    mEmail = (EditText) view.findViewById(R.id.email);
    mPassword = (EditText) view.findViewById(R.id.password);
    mSubmitButton = (Button) view.findViewById(R.id.submitButton);
    mCameraFragment = (CameraFragment) getActivity().getFragmentManager().findFragmentById(R.id.camera);

    mSubmitButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (mSubmitButton.isEnabled()) {
          mSubmitButton.setEnabled(false);
          doRegister();
        }
      }
    });

    return view;
  }

  private void doRegister() {
    showProgressDialog();
    new RegistrationTask().execute();
  }

  private void showAlertDialog(String message) {
    hideDialog();
    final DialogFragment dialog = SetupAlertDialogFragment.newInstance(message);
    dialog.show(getFragmentManager(), "dialog");
  }

  private void showProgressDialog() {
    hideDialog();
    mDialog = new SetupProgressDialogFragment(new SetupProgressDialogFragment.Listener() {
      @Override
      public void onCancel(DialogInterface dialog) {
        //mHandler.sendEmptyMessage(MESSAGE_VALIDATION_ABORTED);
      }
    });
    //mDialog.getDialog().setTitle("Registering");
    mDialog.show(getFragmentManager(), "dialog");
  }

  private void hideDialog() {
    if (mDialog != null) {
      mDialog.dismiss();
      mDialog = null;
    }
    mSubmitButton.setEnabled(true);
  }

}
