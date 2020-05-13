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

package org.kegbot.app;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.common.collect.Lists;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.kegbot.app.event.TapsChangedEvent;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.SyncManager;
import org.kegbot.core.TapManager;
import org.kegbot.proto.Models.FlowMeter;
import org.kegbot.proto.Models.FlowToggle;
import org.kegbot.proto.Models.ThermoSensor;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;

import java.util.List;

import butterknife.ButterKnife;

/**
 * A fragment representing a single Tap detail screen. This fragment is either contained in a {@link
 * TapListActivity} in two-pane mode (on tablets) or a {@link TapDetailActivity} on handsets.
 */
public class TapDetailFragment extends Fragment {
  private static final String TAG = TapDetailFragment.class.getSimpleName();

  /**
   * The fragment argument representing the item ID that this fragment represents.
   */
  public static final String ARG_ITEM_ID = "item_id";

  private View mView;
  private Bus mBus;

  private TapManager mTapManager;

  private ViewFlipper mFlipper;

  private Spinner mMeterSelect;
  private Spinner mThermoSelect;
  private Spinner mToggleSelect;
  private final List<FlowMeter> mMeters = Lists.newArrayList();
  private final List<ThermoSensor> mThermos = Lists.newArrayList();
  private final List<FlowToggle> mToggles = Lists.newArrayList();
  private FlowMeterAdapter mAdapter;
  private ThermoAdapter mThermoAdapter;
  private FlowToggleAdapter mToggleAdapter;
  private Switch mTapEnabledSwitch;

  private KegTap mTap;
  private int mTapId;

  Button mDeleteTapButton;

  public TapDetailFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final KegbotCore core = KegbotCore.getInstance(getActivity());
    mTapManager = core.getTapManager();
    mBus = core.getBus();

    mBus.register(this);

    mMeters.add(null); // "not connected"
    final SyncManager syncManager = KegbotCore.getInstance(getActivity()).getSyncManager();
    mMeters.addAll(syncManager.getCurrentFlowMeters());

    mThermos.add(null);
    mThermos.addAll(syncManager.getCurrentThermoSensors());

    mToggles.add(null); // "none"
    mToggles.addAll(syncManager.getCurrentFlowToggles());

    mTapId = getArguments().getInt(ARG_ITEM_ID, 0);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView");
    mView = inflater.inflate(R.layout.fragment_tap_detail, container, false);
    ButterKnife.inject(this, mView);

    mFlipper = ButterKnife.findById(mView, R.id.tapControlsFlipper);

    mTapEnabledSwitch = ButterKnife.findById(mView, R.id.tapEnabledSwitch);
    mTapEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (mTap == null) {
          Log.wtf(TAG, "Tap is null!");
          return;
        }
        mTapManager.setTapVisibility(mTap, b);
        updateTapDetails(mTap);
      }
    });

    mAdapter = new FlowMeterAdapter(getActivity());
    mMeterSelect = ButterKnife.findById(mView, R.id.meterSelect);
    mMeterSelect.setAdapter(mAdapter);

    mThermoAdapter = new ThermoAdapter(getActivity());
    mThermoSelect = ButterKnife.findById(mView, R.id.thermoSelect);
    mThermoSelect.setAdapter(mThermoAdapter);

    mToggleAdapter = new FlowToggleAdapter(getActivity());
    mToggleSelect = ButterKnife.findById(mView, R.id.toggleSelect);
    mToggleSelect.setAdapter(mToggleAdapter);

    mAdapter.addAll(mMeters);
    mThermoAdapter.addAll(mThermos);
    mToggleAdapter.addAll(mToggles);
    mDeleteTapButton = ButterKnife.findById(mView, R.id.deleteTapButton);

    mMeterSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemSelected: position=" + position + " id=" + id);

        final FlowMeter meter = mMeters.get(position);
        if (meter == mTap.getMeter() ||
            (meter != null && mTap.hasMeter() && meter.getId() == mTap.getMeter().getId())) {
          Log.d(TAG, "Not changed.");
          return;
        }

        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            final KegbotCore core = KegbotCore.getInstance(getActivity());
            final Backend backend = core.getBackend();
            final SyncManager sync = core.getSyncManager();

            try {
              if (meter == null) {
                Log.d(TAG, "Disconnecting meter on tap.");
                backend.disconnectMeter(mTap);
              } else {
                Log.d(TAG, "Connecting meter on tap.");
                backend.connectMeter(mTap, meter);
              }
            } catch (BackendException e) {
              Log.w(TAG, "Error: " + e, e);
            }
            sync.requestSync();
            return null;
          }
        }.execute((Void) null);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        Log.d(TAG, "onNothingSelected");
      }
    });

    mThermoSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "thermo selected: position=" + position + " id=" + id);

        final ThermoSensor thermo = mThermos.get(position);
        if (thermo == mTap.getThermoSensor() ||
            (thermo != null && mTap.hasThermoSensor() && thermo.getId() == mTap.getThermoSensor().getId())) {
          Log.d(TAG, "Not changed.");
          return;
        }

        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            final KegbotCore core = KegbotCore.getInstance(getActivity());
            final Backend backend = core.getBackend();
            final SyncManager sync = core.getSyncManager();

            try {
              if (thermo == null) {
                Log.d(TAG, "Disconnecting thermo on tap.");
                backend.disconnectThermo(mTap);
              } else {
                Log.d(TAG, "Connecting thermo on tap.");
                backend.connectThermo(mTap, thermo);
              }
            } catch (BackendException e) {
              Log.w(TAG, "Error: " + e, e);
            }
            sync.requestSync();
            return null;
          }
        }.execute((Void) null);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        Log.d(TAG, "onNothingSelected");
      }
    });

    mToggleSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "toggle selected: position=" + position + " id=" + id);

        final FlowToggle toggle = mToggles.get(position);
        if (toggle == mTap.getToggle() ||
            (toggle != null && mTap.hasToggle() && toggle.getId() == mTap.getToggle().getId())) {
          Log.d(TAG, "Not changed.");
          return;
        }

        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            final KegbotCore core = KegbotCore.getInstance(getActivity());
            final Backend backend = core.getBackend();
            final SyncManager sync = core.getSyncManager();

            try {
              if (toggle == null) {
                Log.d(TAG, "Disconnecting toggle on tap.");
                backend.disconnectToggle(mTap);
              } else {
                Log.d(TAG, "Connecting toggle on tap.");
                backend.connectToggle(mTap, toggle);
              }
            } catch (BackendException e) {
              Log.w(TAG, "Error: " + e, e);
            }
            sync.requestSync();
            return null;
          }
        }.execute((Void) null);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
        Log.d(TAG, "onNothingSelected");
      }
    });

    mDeleteTapButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View arg0) {
        confirmDeleteTap();
      }
    });

    if (mTapId != 0) {
      mTap = KegbotCore.getInstance(getActivity()).getTapManager().getTap(mTapId);
      updateTapDetails(mTap);
    }

    return mView;
  }

  @Override
  public void onDestroy() {
    mBus.unregister(this);
    super.onDestroy();
  }

  @Subscribe
  public void onTapUpdatedEvent(TapsChangedEvent event) {
    Log.d(TAG, "onTapUpdatedEvent");
    if (mTap == null) {
      return;
    }
    for (final KegTap tap : event.getTaps()) {
      if (tap.getId() == mTap.getId()) {
        updateTapDetails(tap);
        return;
      }
    }

    // We must have been deleted.
    getFragmentManager().popBackStackImmediate();
  }

  private void updateTapDetails(final KegTap tap) {
    Log.d(TAG, "updateTapDetails");
    mTap = tap;
    if (mView == null) {
      Log.w(TAG, "updateTapDetails: No rootview!");
      return;
    }

    Log.d(TAG, "Updating tap! + " + mTap);

    final boolean isVisible = mTapManager.getTapVisibility(mTap);
    mTapEnabledSwitch.setChecked(isVisible);
    if (isVisible) {
      mFlipper.setDisplayedChild(0);
    } else {
      mFlipper.setDisplayedChild(1);
    }

    final TextView title = (TextView) mView.findViewById(R.id.tapDetailTitle);
    title.setText(mTap.getName());

    int position = 0;
    final FlowMeter currentMeter = mTap.getMeter();
    for (final FlowMeter meter : mMeters) {
      if ((meter == null && currentMeter == null) ||
          (meter != null && meter.getId() == currentMeter.getId())) {
        mMeterSelect.setSelection(position);
        break;
      }
      position += 1;
    }

    position = 0;
    final ThermoSensor currentThermo = mTap.getThermoSensor();
    for (final ThermoSensor thermo : mThermos) {
      if ((thermo == null && currentThermo == null) ||
          (thermo != null && thermo.getId() == currentThermo.getId())) {
        mThermoSelect.setSelection(position);
        break;
      }
      position += 1;
    }

    position = 0;
    final FlowToggle currentToggle = mTap.getToggle();
    for (final FlowToggle toggle : mToggles) {
      if ((toggle == null && currentToggle == null) ||
          (toggle != null && toggle.getId() == currentToggle.getId())) {
        mToggleSelect.setSelection(position);
        break;
      }
      position += 1;
    }


    final TextView onTapTitle = ButterKnife.findById(mView, R.id.onTapTitle);
    final Button onTapButton = ButterKnife.findById(mView, R.id.tapKegButton);

    if (mTap.hasCurrentKeg()) {
      final Keg currentKeg = mTap.getCurrentKeg();
      if (currentKeg.hasBeverage()) {
        onTapTitle.setText(currentKeg.getBeverage().getName());
      } else {
        onTapTitle.setText(String.format("Keg %s", Integer.valueOf(currentKeg.getId())));
      }
      onTapButton.setText(R.string.end_keg_button);
      onTapButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
          confirmEndKeg();
        }
      });

    } else {
      onTapTitle.setText(R.string.tap_detail_tap_empty);
      onTapButton.setText(R.string.tap_keg_button);
      onTapButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          onStartKeg();
        }
      });
    }

    final Button calibrateButton = ButterKnife.findById(mView, R.id.calibrateButton);
    if (tap.hasMeter()) {
      calibrateButton.setEnabled(true);
      calibrateButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
          final Intent intent = CalibrationActivity.getStartIntent(getActivity(), tap);
          startActivity(intent);
        }
      });
    } else {
      calibrateButton.setEnabled(false);
    }
  }

  private void onStartKeg() {
    if (!mTap.hasMeter()) {
      Log.w(TAG, "Can't start keg, no meter.");
    }
    startActivity(NewKegActivity.getStartIntent(getActivity(), mTap));
  }

  /** Called when the "end keg" button is pressed. */
  private void confirmEndKeg() {
    if (mTap == null || !mTap.hasCurrentKeg()) {
      Log.w(TAG, "No tap/keg, hmm.");
      return;
    }

    final Keg keg = mTap.getCurrentKeg();
    final Spanned message = Html.fromHtml(
        String.format(
            "Are you sure you want end <b>Keg %s</b> (<i>%s</i>) on tap <b>%s</b>?",
            Integer.valueOf(keg.getId()),
            keg.getBeverage().getName(),
            mTap.getName())
    );

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

    final Keg keg = mTap.getCurrentKeg();

    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        Backend api = KegbotCore.getInstance(getActivity()).getBackend();
        try {
          api.endKeg(keg);
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

  private void confirmDeleteTap() {
    if (mTap == null) {
      Log.wtf(TAG, "No tap, hmm.");
      return;
    }

    final Spanned message = Html.fromHtml(
        String.format("Are you sure you want delete tap <b>%s</b>?", mTap.getName()));

    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setMessage(message)
        .setCancelable(false)
        .setPositiveButton("Delete Tap", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface arg0, int arg1) {
            doDeleteTap();
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

  private void doDeleteTap() {
    final ProgressDialog dialog = new ProgressDialog(getActivity());
    dialog.setIndeterminate(true);
    dialog.setCancelable(false);
    dialog.setTitle("Deleting Tap");
    dialog.setMessage("Please wait ...");
    dialog.show();

    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        Backend api = KegbotCore.getInstance(getActivity()).getBackend();
        try {
          api.deleteTap(mTap);
        } catch (BackendException e) {
          Log.w(TAG, "Error ending tap: " + e, e);
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

  private class FlowMeterAdapter extends ArrayAdapter<FlowMeter> {
    public FlowMeterAdapter(Context context) {
      super(context, android.R.layout.simple_spinner_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final View view;
      if (convertView != null) {
        view = convertView;
      } else {
        final LayoutInflater inflater =
            (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(android.R.layout.simple_list_item_1, null);
      }

      final FlowMeter item = getItem(position);
      final TextView text = ButterKnife.findById(view, android.R.id.text1);
      if (item == null) {
        text.setText("Not connected.");
      } else {
        text.setText(item.getName());
      }
      return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
      return getView(position, convertView, parent);
    }

    @Override
    public long getItemId(int position) {
      if (position == 0) {
        return 0;
      }
      return mMeters.get(position).getId();
    }

  }

  private class ThermoAdapter extends ArrayAdapter<ThermoSensor> {
    public ThermoAdapter(Context context) {
      super(context, android.R.layout.simple_spinner_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final View view;
      if (convertView != null) {
        view = convertView;
      } else {
        final LayoutInflater inflater =
            (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(android.R.layout.simple_list_item_1, null);
      }

      final ThermoSensor item = getItem(position);
      final TextView text = ButterKnife.findById(view, android.R.id.text1);
      if (item == null) {
        text.setText("None.");
      } else {
        text.setText(item.getSensorName());
      }
      return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
      return getView(position, convertView, parent);
    }

    @Override
    public long getItemId(int position) {
      if (position == 0) {
        return 0;
      }
      return mThermos.get(position).getId();
    }

  }

  private class FlowToggleAdapter extends ArrayAdapter<FlowToggle> {
    public FlowToggleAdapter(Context context) {
      super(context, android.R.layout.simple_spinner_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final View view;
      if (convertView != null) {
        view = convertView;
      } else {
        final LayoutInflater inflater =
            (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(android.R.layout.simple_list_item_1, null);
      }

      final FlowToggle item = getItem(position);
      final TextView text = ButterKnife.findById(view, android.R.id.text1);
      if (item == null) {
        text.setText("None.");
      } else {
        text.setText(item.getName());
      }
      return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
      return getView(position, convertView, parent);
    }

    @Override
    public long getItemId(int position) {
      if (position == 0) {
        return 0;
      }
      return mToggles.get(position).getId();
    }

  }

}
