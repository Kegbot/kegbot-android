/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>.
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
import com.google.common.collect.Maps;

import org.kegbot.app.R;
import org.kegbot.core.KegbotCore;

import java.util.List;
import java.util.Map;

public class AlertActivity extends Activity {

  private static final String TAG = AlertActivity.class.getSimpleName();

  private final Map<String, AlertCore.Alert> mActiveDialogs = Maps.newLinkedHashMap();

  @SuppressLint("ValidFragment")
  class AlertDialogFragment extends DialogFragment {

    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_ALERT_ID = "alert_id";

    AlertDialogFragment(AlertCore.Alert alert) {
      final Bundle args = new Bundle();
      args.putString(KEY_TITLE, alert.getTitle());
      args.putString(KEY_DESCRIPTION, alert.getDescription());
      args.putString(KEY_ALERT_ID, alert.getId());
      setArguments(args);
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

      if (alert.getAction() != null) {
        final String actionName = alert.getActionName();
        builder.setNegativeButton(getString(R.string.alert_button_dismiss), null);
        builder.setPositiveButton(Strings.isNullOrEmpty(actionName) ? getString(R.string.alert_button_details) : actionName,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                alert.getAction().run();
              }
            });
      } else {
        builder.setPositiveButton(getString(R.string.alert_button_ok), null);
      }

      return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
      super.onCancel(dialog);
      Log.d(TAG, "onCancel");
      final String alertId = getArguments().getString(KEY_ALERT_ID);
      onDialogDismissed(alertId);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
      super.onDismiss(dialog);
      Log.d(TAG, "onDismiss");
      final String alertId = getArguments().getString(KEY_ALERT_ID);
      onDialogDismissed(alertId);
    }


  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onStart() {
    super.onStart();
    mActiveDialogs.clear();

    KegbotCore core = KegbotCore.getInstance(this);
    AlertCore alertCore = core.getAlertCore();

    final List<AlertCore.Alert> alerts = alertCore.getAlerts();
    Log.d(TAG, "Number of active alerts: " + alerts.size());
    if (alerts.isEmpty()) {
      finish();
      return;
    }

    for (final AlertCore.Alert alert : alerts) {
      final DialogFragment dialog = new AlertDialogFragment(alert);
      Log.d(TAG, "Showing dialog: " + alert.getId());
      dialog.show(getFragmentManager(), alert.getId());
      mActiveDialogs.put(alert.getId(), alert);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  private void onDialogDismissed(String alertId) {
    Log.d(TAG, "onDialogDismissed: " + alertId);
    final AlertCore.Alert alert = mActiveDialogs.remove(alertId);
    if (alert != null) {
      if (alert.getDismissOnView()) {
        // TODO(mikey): Might be better to move this to the dialog
        // and truly dismiss it on view.
        KegbotCore.getInstance(this).getAlertCore().cancelAlert(alertId);
      }
    }
    if (mActiveDialogs.isEmpty()) {
      Log.d(TAG, "No active dialogs, finish.");
      finish();
    }
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
