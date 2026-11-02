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
package org.kegbot.app.alert;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.kegbot.app.R;
import org.kegbot.core.KegbotCore;

import java.util.List;

public class AlertActivity extends Activity {

  private static final String TAG = AlertActivity.class.getSimpleName();

  private final List<AlertCore.Alert> mPendingAlerts = Lists.newArrayList();

  @SuppressLint("ValidFragment")
  public static class AlertDialogFragment extends DialogFragment {

    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_ALERT_ID = "alert_id";

    void setAlertArgs(AlertCore.Alert alert) {
      final Bundle args = new Bundle();
      args.putString(KEY_TITLE, alert.getTitle());
      args.putString(KEY_DESCRIPTION, alert.getDescription());
      args.putString(KEY_ALERT_ID, alert.getId());
      setArguments(args);
    }

    private void notifyActionFinished(String alertId) {
      if (getActivity() instanceof AlertActivity) {
        ((AlertActivity) getActivity()).onAlertActionFinished(alertId);
      }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      final String title = getArguments().getString(KEY_TITLE);
      final String description = getArguments().getString(KEY_DESCRIPTION);
      final String alertId = getArguments().getString(KEY_ALERT_ID);

      KegbotCore core = KegbotCore.getInstance(getActivity());
      AlertCore alertCore = core.getAlertCore();
      final AlertCore.Alert alert = alertCore.getAlert(alertId);

      final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
          .setIcon(android.R.drawable.ic_dialog_alert)
          .setTitle(title)
          .setMessage(description);

      if (alert != null && alert.getAction() != null) {
        final String actionName = alert.getActionName();
        builder.setNegativeButton(getString(R.string.alert_button_dismiss), new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            notifyActionFinished(alertId);
          }
        });
        builder.setPositiveButton(Strings.isNullOrEmpty(actionName) ? getString(R.string.alert_button_details) : actionName,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                alert.getAction().run();
                notifyActionFinished(alertId);
              }
            });
      } else {
        builder.setPositiveButton(getString(R.string.alert_button_ok), new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            notifyActionFinished(alertId);
          }
        });
      }

      final AlertDialog dialog = builder.create();
      dialog.setCanceledOnTouchOutside(false);
      return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
      super.onCancel(dialog);
      final String alertId = getArguments().getString(KEY_ALERT_ID);
      notifyActionFinished(alertId);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onStart() {
    super.onStart();
    loadAndShowNextAlert();
  }

  private void loadAndShowNextAlert() {
    KegbotCore core = KegbotCore.getInstance(this);
    AlertCore alertCore = core.getAlertCore();

    if (mPendingAlerts.isEmpty()) {
      final List<AlertCore.Alert> alerts = alertCore.getAlerts();
      mPendingAlerts.addAll(alerts);
    }

    if (mPendingAlerts.isEmpty()) {
      finish();
      return;
    }

    final AlertCore.Alert alert = mPendingAlerts.remove(0);
    if (alertCore.getAlert(alert.getId()) == null) {
      loadAndShowNextAlert();
      return;
    }

    final AlertDialogFragment dialog = new AlertDialogFragment();
    dialog.setAlertArgs(alert);
    Log.d(TAG, "Showing dialog: " + alert.getId());
    dialog.show(getFragmentManager(), alert.getId());
  }

  void onAlertActionFinished(String alertId) {
    Log.d(TAG, "onAlertActionFinished: " + alertId);
    KegbotCore.getInstance(this).getAlertCore().cancelAlert(alertId);
    loadAndShowNextAlert();
  }

  @Override
  protected void onStop() {
    super.onStop();
    Log.d(TAG, "onStop");
  }

  public static void showDialogs(Context context) {
    final Intent intent = new Intent(context, AlertActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
    context.startActivity(intent);
  }
}
