package org.kegbot.kegtap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.kegbot.core.ConfigurationManager;
import org.kegbot.core.Flow;
import org.kegbot.core.Flow.State;
import org.kegbot.core.FlowManager;
import org.kegbot.core.Tap;
import org.kegbot.kegtap.util.image.ImageDownloader;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Models.BeerType;
import org.kegbot.proto.Models.Image;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class PourStatusFragment extends ListFragment {

  private static final String TAG = PourStatusFragment.class.getSimpleName();
  private ArrayAdapter<Flow> mAdapter;

  private final Map<Tap, Flow> mDisplayedFlowsByTap = Maps.newLinkedHashMap();

  private final List<Flow> mCurrentFlows = Lists.newArrayList();

  private final FlowManager mFlowManager = FlowManager.getSingletonInstance();

  private ImageDownloader mImageDownloader;

  /**
   * After this much inactivity, the "pour automatically ends" dialog is shown.
   */
  private static final long IDLE_TOOLTIP_MILLIS = TimeUnit.SECONDS.toMillis(5);

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.pour_status_fragment_layout, container);
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mImageDownloader = ImageDownloader.getSingletonInstance(activity);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    mAdapter = new ArrayAdapter<Flow>(getActivity(), R.layout.pour_status_item_layout,
        R.id.tapTitle) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        final Flow flow = mCurrentFlows.get(position);
        final View view = super.getView(position, convertView, parent);
        applyFlowToView(flow, view);
        return view;
      }

      @Override
      public boolean areAllItemsEnabled() {
        return false;
      }

      @Override
      public boolean isEnabled(int position) {
        return false;
      }

    };

    setListAdapter(mAdapter);
  }

  /**
   * Sets the current list of active flows.
   *
   * Each flow in the list will be added to {@link #mCurrentFlows} if not
   * already known. Any flows in {@link #mCurrentFlows} but not in this list
   * will be removed and no longer displayed. If the result is an empty list,
   * this activity is finished. Otherwise, the UI is refreshed.
   *
   * @param currentFlows
   */
  public void updateWithFlows(final Collection<Flow> currentFlows) {
    for (final Flow newFlow : currentFlows) {
      final Tap tap = newFlow.getTap();
      final Flow oldFlow = mDisplayedFlowsByTap.get(tap);

      if (oldFlow == newFlow) {
        // No change.
      } else if (oldFlow != null) {
        Log.d(TAG, "Replacing flow: " + oldFlow);
        mCurrentFlows.remove(oldFlow);
        mAdapter.remove(oldFlow);
        mCurrentFlows.add(newFlow);
        mAdapter.add(newFlow);
      } else {
        Log.d(TAG, "Adding flow: " + newFlow);
        mCurrentFlows.add(newFlow);
        mAdapter.add(newFlow);
      }
      mDisplayedFlowsByTap.put(tap, newFlow);
    }
    mAdapter.notifyDataSetChanged();
  }

  /**
   * Updates a view with information about the given flow.
   *
   * @param flow
   * @param view
   */
  private void applyFlowToView(final Flow flow, final View view) {
    if (flow == null) {
      Log.wtf(TAG, "Null flow, wtf?");
      return;
    }
    if (view == null) {
      Log.wtf(TAG, "Null view, wtf?");
      return;
    }

    final Flow.State flowState = flow.getState();
    final ConfigurationManager config = ConfigurationManager.getSingletonInstance();

    final TextView pourVolumeNumbers = (TextView) view.findViewById(R.id.pourVolumeNumbers);
    final TextView pourVolumeUnits = (TextView) view.findViewById(R.id.pourVolumeWords);
    final TextView pourBeerName = (TextView) view.findViewById(R.id.pourBeerName);
    final TextView statusLine = (TextView) view.findViewById(R.id.pourStatusLine);

    // Set volume portion.
    final double ounces = Units.volumeMlToOunces(flow.getVolumeMl());

    final String volumeStr;
    final String units = "ounces";
    if (ounces < 100) {
      volumeStr = String.format("%.1f", Double.valueOf(ounces));
    } else {
      volumeStr = String.format("%d", Integer.valueOf((int) ounces));
    }

    // Set pour volume and units.
    if (pourVolumeNumbers != null) {
      pourVolumeNumbers.setText(volumeStr);
    }
    if (pourVolumeUnits != null) {
      pourVolumeUnits.setText(units);
    }

    // Update tap / beer info.
    final TapDetail tapDetail = config.getTapDetail(flow.getTap().getMeterName());
    final ImageView tapImage = (ImageView) view.findViewById(R.id.pourImage);

    if (tapDetail != null) {
      if (tapDetail.hasBeerType()) {
        final BeerType type = tapDetail.getBeerType();
        final String beerName = type.getName();

        // Set beer name.
        if (!Strings.isNullOrEmpty(beerName) && pourBeerName != null) {
          pourBeerName.setText(beerName);
        }

        // Set beer image.
        if (tapImage != null) {
          tapImage.setBackgroundResource(R.drawable.kegbot_unknown_square_2);
          if (tapDetail.getBeerType().hasImage()) {
            final Image image = tapDetail.getBeerType().getImage();
            final String imageUrl = image.getUrl();
            mImageDownloader.download(imageUrl, tapImage);
          }
        }
      }
    }

    // Set drinker image.
    final ImageView pourDrinkerImage = (ImageView) view.findViewById(R.id.pourDrinkerImager);
    final Button logInButton = (Button) view.findViewById(R.id.logInButton);
    final TextView pourDrinkerName = (TextView) view.findViewById(R.id.pourDrinkerName);
    if (flow.isAnonymous()) {
      logInButton.setVisibility(View.VISIBLE);
      pourDrinkerName.setVisibility(View.GONE);
    } else {
      logInButton.setVisibility(View.GONE);
      pourDrinkerName.setText(flow.getUsername());
      pourDrinkerName.setVisibility(View.VISIBLE);
    }

    // Update beer info.
    if (statusLine != null) {
      if ((flowState == State.ACTIVE || flowState == State.IDLE)
          && (flow.getIdleTimeMs() >= IDLE_TOOLTIP_MILLIS)) {
        final long seconds = flow.getMsUntilIdle() / 1000;
        statusLine.setText("Pour automatically ends in " + seconds + " second"
            + ((seconds != 1) ? "s" : "") + ".");
        statusLine.setVisibility(View.VISIBLE);
      } else if (flowState == State.COMPLETED) {
        statusLine.setText("Pour completed!");
        statusLine.setVisibility(View.VISIBLE);
      } else {
        statusLine.setVisibility(View.INVISIBLE);
      }
    }

    final OnClickListener donePouringListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
        mFlowManager.endFlow(flow);
      }
    };
    ((Button) view.findViewById(R.id.endPourButton)).setOnClickListener(donePouringListener);

    view.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));
  }

}
