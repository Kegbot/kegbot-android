
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
import android.widget.Spinner;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;

import com.google.common.collect.Lists;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.kegbot.app.event.TapListUpdateEvent;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.SyncManager;
import org.kegbot.proto.Models.FlowMeter;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;

import java.util.List;

/**
 * A fragment representing a single Tap detail screen. This fragment is either
 * contained in a {@link TapListActivity} in two-pane mode (on tablets) or a
 * {@link TapDetailActivity} on handsets.
 */
public class TapDetailFragment extends Fragment {
  private static final String TAG = TapDetailFragment.class.getSimpleName();

  /**
   * The fragment argument representing the item ID that this fragment
   * represents.
   */
  public static final String ARG_ITEM_ID = "item_id";

  private View mView;
  private Bus mBus;

  private Spinner mMeterSelect;
  private final List<FlowMeter> mMeters = Lists.newArrayList();
  private FlowMeterAdapter mAdapter;

  private KegTap mTap;
  private int mTapId;

  Button mDeleteTapButton;

  public TapDetailFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final KegbotCore core = KegbotCore.getInstance(getActivity());
    mBus = core.getBus();

    mBus.register(this);

    mMeters.add(null); // "not connected"
    final SyncManager syncManager = KegbotCore.getInstance(getActivity()).getSyncManager();
    mMeters.addAll(syncManager.getCurrentFlowMeters());

    mTapId = getArguments().getInt(ARG_ITEM_ID, 0);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView");
    mView = inflater.inflate(R.layout.fragment_tap_detail, container, false);
    ButterKnife.inject(this, mView);

    mAdapter = new FlowMeterAdapter(getActivity());
    mMeterSelect = ButterKnife.findById(mView, R.id.meterSelect);
    mMeterSelect.setAdapter(mAdapter);
    mAdapter.addAll(mMeters);
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

            if (meter == null) {
              Log.d(TAG, "Disconnecting meter on tap.");
              backend.disconnectMeter(mTap);
            } else {
              Log.d(TAG, "Connecting meter on tap.");
              backend.connectMeter(mTap, meter);
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
  public void onTapUpdatedEvent(TapListUpdateEvent event) {
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
  }

  private void updateTapDetails(final KegTap tap) {
    Log.d(TAG, "updateTapDetails");
    mTap = tap;
    if (mView == null) {
      Log.w(TAG, "updateTapDetails: No rootview!");
      return;
    }

    Log.d(TAG, "Updating tap! + " + mTap);

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
    //getFragmentManager().popBackStackImmediate();
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
    public long getItemId(int position) {
      if (position == 0) {
        return 0;
      }
      return mMeters.get(position).getId();
    }

  }

}
