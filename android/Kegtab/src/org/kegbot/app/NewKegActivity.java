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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.collect.Lists;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.config.SharedPreferencesConfigurationStore;
import org.kegbot.app.service.KegbotCoreService;
import org.kegbot.proto.Models.KegSize;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class NewKegActivity extends Activity {
  private static final String TAG = NewKegActivity.class.getSimpleName();

  private static final String EXTRA_METER_NAME = "meter_name";

  private String mMeterName;

  private AutoCompleteTextView mName;
  private AutoCompleteTextView mBrewerName;
  private AutoCompleteTextView mStyle;
  private Spinner mSize;
  private ArrayAdapter<String> mSizeAdapter;
  private Button mActivateButton;

  private int mSizeId = -1;
  private KegbotApi mApi;

  private final List<KegSize> mKegSizes = Lists.newArrayList();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    KegbotCoreService.stopService(this);

    mMeterName = getIntent().getStringExtra(EXTRA_METER_NAME);

    setContentView(R.layout.new_keg_activity);

    AppConfiguration config = new AppConfiguration(new SharedPreferencesConfigurationStore(
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())));

    mApi = new KegbotApiImpl(config.getApiUrl(), config.getApiKey());

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
    mSizeAdapter = new ArrayAdapter<String>(this, R.layout.keg_size_spinner_item);
    mSizeAdapter.add("Loading Sizes..");
    mSize.setOnItemSelectedListener(new OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "Keg sizes: item " + position + " clicked");
        if (position < mKegSizes.size()) {
          mSizeId = mKegSizes.get(position).getId();
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> arg0) {
        Log.d(TAG, "Keg sizes: nothing selected");
        mSizeId = -1;
      }
    });

    mSize.setAdapter(mSizeAdapter);

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

    new AsyncTask<Void, Void, List<KegSize>>() {
      @Override
      protected List<KegSize> doInBackground(Void... params) {
        try {
          return mApi.getKegSizes();
        } catch (KegbotApiException e) {
          return Collections.emptyList();
        }
      }

      @Override
      protected void onPostExecute(List<KegSize> result) {
        Log.d(TAG, "Got keg sizes");
        mKegSizes.clear();
        mKegSizes.addAll(result);
        mSizeAdapter.clear();
        for (KegSize size : mKegSizes) {
          Log.d(TAG, "Adding: " + size.getName());
          mSizeAdapter.add(size.getName());
        }
      }

    }.execute();

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
        try {
          mApi.activateKeg(mMeterName, mName.getText().toString(),
              mBrewerName.getText().toString(), mStyle.getText().toString(), mSizeId);
          return "";
        } catch (KegbotApiException e) {
          Log.w(TAG, "Activation failed.", e);
          return e.toString();
        }
      }

      @Override
      protected void onPostExecute(String result) {
        dialog.dismiss();
        if (result.isEmpty()) {
          Log.d(TAG, "Calibrated successfully!");
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

  static Intent getStartIntent(Context context, String meterName) {
    final Intent intent = new Intent(context, NewKegActivity.class);
    intent.putExtra(EXTRA_METER_NAME, meterName);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    return intent;
  }

}
