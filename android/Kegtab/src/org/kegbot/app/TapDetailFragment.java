
package org.kegbot.app;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import butterknife.ButterKnife;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.kegbot.app.event.TapListUpdateEvent;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.SyncManager;
import org.kegbot.core.TapManager;
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
  private TapManager mTapManager;

  private Spinner mMeterSelect;
  private List<FlowMeter> mMeters;
  private ArrayAdapter<FlowMeter> mMeterSelectAdapter;

  /**
   * The dummy content this fragment is presenting.
   */
  private KegTap mItem;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the
   * fragment (e.g. upon screen orientation changes).
   */
  public TapDetailFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final KegbotCore core = KegbotCore.getInstance(getActivity());
    mBus = core.getBus();
    mTapManager = core.getTapManager();

    mBus.register(this);

    final int tapId = getArguments().getInt(ARG_ITEM_ID, 0);
    if (tapId != 0) {
      updateTapDetails(mTapManager.getTap(tapId));
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView");
    mView = inflater.inflate(R.layout.fragment_tap_detail, container, false);

    if (mItem != null) {
      updateTapDetails(mItem);
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
    if (mItem == null) {
      return;
    }
    for (final KegTap tap : event.getTaps()) {
      if (tap.getId() == mItem.getId()) {
        updateTapDetails(tap);
        return;
      }
    }
  }

  private void updateTapDetails(final KegTap tap) {
    Log.d(TAG, "updateTapDetails");
    mItem = tap;
    if (mView == null) {
      Log.w(TAG, "updateTapDetails: No rootview!");
      return;
    }

    final TextView title = (TextView) mView.findViewById(R.id.tapDetailTitle);
    title.setText(mItem.getName());

    final TextView onTapTitle = ButterKnife.findById(mView, R.id.onTapTitle);
    final Button onTapButton = ButterKnife.findById(mView, R.id.tapKegButton);
    final Spinner meterSelect = ButterKnife.findById(mView, R.id.meterSelect);

    final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
        R.layout.keg_size_spinner_item);

    final SyncManager syncManager = KegbotCore.getInstance(getActivity()).getSyncManager();
    mMeters = syncManager.getCurrentFlowMeters();
    meterSelect.setAdapter(adapter);

    for (final FlowMeter meter : syncManager.getCurrentFlowMeters()) {
      adapter.add(meter.getName());
    }

    if (mItem.hasCurrentKeg()) {
      final Keg currentKeg = mItem.getCurrentKeg();
      if (currentKeg.hasBeverage()) {
        onTapTitle.setText(currentKeg.getBeverage().getName());
      } else {
        onTapTitle.setText(String.format("Keg %s", Integer.valueOf(currentKeg.getId())));
      }
      onTapButton.setText(R.string.end_keg_button);
      onTapButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
          onEndKeg();
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
  }

  private void onStartKeg() {
    if (!mItem.hasMeter()) {
      Log.w(TAG, "Can't start keg, no meter.");
    }
    startActivity(NewKegActivity.getStartIntent(getActivity(), mItem));
    //getFragmentManager().popBackStackImmediate();
  }

  private class FlowMeterAdapter extends ArrayAdapter<FlowMeter> {

    public FlowMeterAdapter(Context context) {
      super(context, android.R.layout.simple_spinner_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // TODO hack - inflate view here
      final View view = super.getView(position, convertView, parent);
      final FlowMeter item = getItem(position);
      final TextView text = ButterKnife.findById(view, android.R.id.text1);
      if (item == null) {
        text.setText("Not connected.");
      } else {
        text.setText(item.getName());
      }
      return view;
    }

  }

  /** Called when the "end keg" button is pressed. */
  private void onEndKeg() {
    if (mItem == null || !mItem.hasCurrentKeg()) {
      Log.w(TAG, "No tap/keg, hmm.");
      return;
    }

    final Keg keg = mItem.getCurrentKeg();
    final Spanned message = Html.fromHtml(
        String.format(
            "Are you sure you want end <b>Keg %s</b> (<i>%s</i>) on tap <b>%s</b>?",
            Integer.valueOf(keg.getId()),
            keg.getBeverage().getName(),
            mItem.getName()));

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

    final Keg keg = mItem.getCurrentKeg();

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

}
