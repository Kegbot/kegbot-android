/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import org.kegbot.app.util.KegSizes;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.KegTap;

import java.util.Map;

/**
 * @author mike wakerly (opensource@hoho.com)
 */
public class NewKegActivity extends Activity {
  private static final String TAG = NewKegActivity.class.getSimpleName();

  private static final String EXTRA_TAP_ID = "tap_id";

  private KegTap mTap;

  private AutoCompleteTextView mName;
  private AutoCompleteTextView mBrewerName;
  private AutoCompleteTextView mStyle;
  private Spinner mSize;
  private ArrayAdapter<KegSizeItem> mSizeAdapter;
  private Button mActivateButton;

  private static class KegSizeItem {
    private String mName;
    private String mDescription;

    KegSizeItem(String name, String description) {
      this.mName = name;
      this.mDescription = description;
    }

    @Override
    public String toString() {
      return mDescription;
    }

    public String getName() {
      return mName;
    }

  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.new_keg_activity);

    mName = (AutoCompleteTextView) findViewById(R.id.newKegBeerName);
    mBrewerName = (AutoCompleteTextView) findViewById(R.id.newKegBrewer);
    mStyle = (AutoCompleteTextView) findViewById(R.id.newKegStyle);

    // Hack: TextView "next" doesn't advance to the Spinner without
    // this hack..
    mStyle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_NEXT) {
          hideKeyboard();
          mSize.requestFocus();
          mSize.performClick();
        }
        return true;
      }
    });

    mSize = (Spinner) findViewById(R.id.newKegSize);
    mSizeAdapter = new ArrayAdapter<KegSizeItem>(this, R.layout.keg_size_spinner_item);

    mSize.setAdapter(mSizeAdapter);
    for (final Map.Entry<String, String> entry : KegSizes.DESCRIPTIONS.entrySet()) {
      mSizeAdapter.add(new KegSizeItem(entry.getKey(), entry.getValue()));
    }
    mSize.setSelection(0);

    mActivateButton = (Button) findViewById(R.id.newKegButton);
    mActivateButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        handleDoneButton();
      }
    });
  }

  private void hideKeyboard() {
    InputMethodManager inputManager =
        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),
        InputMethodManager.HIDE_NOT_ALWAYS);
  }

  @Override
  protected void onResume() {
    super.onResume();

    final int tapId = getIntent().getIntExtra(EXTRA_TAP_ID, 0);
    mTap = KegbotCore.getInstance(this).getTapManager().getTap(tapId);
    if (mTap == null) {
      Log.e(TAG, "Could not find tap for id: " + tapId);
      finish();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  private void handleDoneButton() {
    final ProgressDialog dialog = new ProgressDialog(this);
    dialog.setIndeterminate(true);
    dialog.setCancelable(false);
    dialog.setTitle("Activating Keg");
    dialog.setMessage("Please wait ...");
    dialog.show();

    new AsyncTask<Void, Void, String>() {
      @Override
      protected String doInBackground(Void... params) {
        KegSizeItem selected = (KegSizeItem) mSize.getSelectedItem();
        if (selected == null) {
          Log.e(TAG, "No Selection!!");
          return "No Selection.";
        }
        try {
          final Backend backend = KegbotCore.getInstance(NewKegActivity.this).getBackend();
          backend.startKeg(mTap, mName.getText().toString(),
              mBrewerName.getText().toString(), mStyle.getText().toString(),
              selected.getName());
          return "";
        } catch (BackendException e) {
          Log.w(TAG, "Activation failed.", e);
          return e.toString();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        dialog.dismiss();
        if (result.isEmpty()) {
          Log.d(TAG, "Calibrated successfully!");
          KegbotCore.getInstance(NewKegActivity.this).getSyncManager().requestSync();
          finish();
          return;
        }
        new AlertDialog.Builder(NewKegActivity.this)
            .setCancelable(true)
            .setNegativeButton("Ok", null)
            .setTitle("Activation failed")
            .setMessage("Activation failed: " + result)
            .show();
      }

    }.execute();
  }

  static Intent getStartIntent(Context context, final KegTap tap) {
    // TODO(mikey): Handle tap meter null.
    final Intent intent = new Intent(context, NewKegActivity.class);
    intent.putExtra(EXTRA_TAP_ID, tap.getId());
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    return intent;
  }

}
