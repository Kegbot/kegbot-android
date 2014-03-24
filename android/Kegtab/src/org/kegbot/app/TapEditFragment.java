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
package org.kegbot.app;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;

/**
 * Fragment showing manager controls for the tap.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class TapEditFragment extends Fragment {

  private final String TAG = TapEditFragment.class.getSimpleName();

  private KegTap mTap = null;

  private Button mEndKegButton;
  private Button mNewKegButton;
  private Button mSpillButton;
  private Button mCalibrateButton;
  private Button mDoneButton;

  private View mView = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.tap_edit_controls_fragment, container, false);

    mEndKegButton = (Button) mView.findViewById(R.id.endKegButton);
    mEndKegButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onEndKeg();
      }
    });

    mNewKegButton = (Button) mView.findViewById(R.id.newKegButton);
    mNewKegButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onNewKeg();
      }
    });

    mSpillButton = (Button) mView.findViewById(R.id.spillButton);
    mSpillButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onSpill();
      }
    });

    mCalibrateButton = (Button) mView.findViewById(R.id.calibrateButton);
    mCalibrateButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onCalibrate();
      }
    });

    mDoneButton = (Button) mView.findViewById(R.id.doneButton);
    mDoneButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onDone();
      }
    });

    if (mTap != null) {
      // Tap was set prior to us having a view.
      setTapDetail(mTap);
    }

    return mView;
  }

  public void setTapDetail(KegTap tap) {
    mTap = tap;

    if (mView == null) {
      // No view yet.
      return;
    }

    if (!mTap.hasCurrentKeg()) {
      mNewKegButton.setVisibility(View.VISIBLE);
      mEndKegButton.setVisibility(View.GONE);
      mSpillButton.setVisibility(View.GONE);
    } else {
      mNewKegButton.setVisibility(View.GONE);
      mEndKegButton.setVisibility(View.VISIBLE);
      mSpillButton.setVisibility(View.VISIBLE);
    }
  }

  /** Called when the "end keg" button is pressed. */
  private void onEndKeg() {
    if (mTap == null || !mTap.hasCurrentKeg()) {
      Log.w(TAG, "No tap/keg, hmm.");
      return;
    }

    final Keg keg = mTap.getCurrentKeg();
    final Spanned message = Html.fromHtml(
        String.format(
            "Are you sure you want end <b>Keg %s</b> (<i>%s</i>) on tap <b>%s</b>?",
            Integer.valueOf(keg.getId()),
            keg.getType().getName(),
            mTap.getName()));

    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setMessage(message)
       .setCancelable(false)
       .setPositiveButton("End Keg", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
              doEndKeg();
            }
       })
       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
       });

    final AlertDialog alert = builder.create();
    alert.show();
  }

  private void doEndKeg() {
    final ProgressDialog dialog = new ProgressDialog(getActivity());
    dialog.setIndeterminate(true);
    dialog.setCancelable(false);
    dialog.setTitle("Ending Keg");
    dialog.setMessage("Please wait ...");
    dialog.show();

    final int kegId = mTap.getCurrentKeg().getId();

    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        Backend api = KegbotCore.getInstance(getActivity()).getBackend();
        try {
          api.endKeg(kegId);
        } catch (BackendException e) {
          Log.w(TAG, "Error ending keg: " + e, e);
        }
        return null;
      }

      @Override
      protected void onPostExecute(Void result) {
        dialog.dismiss();
        KegbotCore.getInstance(getActivity()).getSyncManager().requestSync();
      }

    }.execute();
  }

  private void onNewKeg() {
    if (mTap == null) {
      // TODO(mikey): Clean up.
      return;
    }
    startActivity(NewKegActivity.getStartIntent(getActivity(), mTap));
    getFragmentManager().popBackStackImmediate();
  }

  private void onSpill() {

  }

  private void onCalibrate() {
    final KegTap tap = mTap;
    if (tap == null) {
      // TODO(mikey): synchronize with button click listeners, etc
      return;
    }
    final Intent intent = CalibrationActivity.getStartIntent(getActivity(), tap);
    startActivity(intent);
    getFragmentManager().popBackStackImmediate();
  }

  private void onDone() {
    getFragmentManager().popBackStackImmediate();
  }

}
