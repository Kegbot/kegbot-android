package org.kegbot.kegtap;

import org.kegbot.core.ConfigurationManager;
import org.kegbot.core.Flow;
import org.kegbot.core.Flow.State;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Models.BeerType;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Strings;

public class PourStatusFragment extends Fragment {

  private static final String TAG = PourStatusFragment.class.getSimpleName();
  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.pour_status_fragment_layout, container);
    return mView;
  }


  public void updateForFlow(final Flow flow) {
    if (flow == null) {
      Log.w(TAG, "Null flow, wtf?");
      return;
    }

    // Set volume portion.
    final double ounces = Units.volumeMlToOunces(flow.getVolumeMl());

    final String volumeStr;
    if (ounces < 100) {
      volumeStr = String.format("%.1f", Double.valueOf(ounces));
    } else {
      volumeStr = String.format("%d", Integer.valueOf((int) ounces));
    }

    ((TextView) mView.findViewById(R.id.pourVolumeNumbers)).setText(volumeStr);
    ((TextView) mView.findViewById(R.id.pourVolumeWords)).setText("ounces");

    // Update tap / beer info.
    final ConfigurationManager config = ConfigurationManager.getSingletonInstance();
    final TapDetail tapDetail = config.getTapDetail(flow.getTap().getMeterName());
    if (tapDetail != null) {
      if (tapDetail.hasBeerType()) {
        final BeerType type = tapDetail.getBeerType();
        final String beerName = type.getName();
        if (!Strings.isNullOrEmpty(beerName)) {
          ((TextView) mView.findViewById(R.id.pourBeerName)).setText(beerName);
        }
      }
    }

    final TextView statusLine = (TextView) mView.findViewById(R.id.pourStatusLine);

    final Flow.State flowState = flow.getState();

    if (flowState == State.ACTIVE || flowState == State.IDLE) {
      final long msUntilIdle = flow.getMsUntilIdle();
      if (msUntilIdle <= 10000) {
        final long seconds = msUntilIdle / 1000;
        statusLine.setText("Pour automatically ends in " + seconds + " second"
            + ((seconds != 1) ? "s" : "") + ".");
        statusLine.setVisibility(View.VISIBLE);
      } else {
        statusLine.setVisibility(View.INVISIBLE);
      }
    } else if (flowState == State.COMPLETED) {
      statusLine.setText("Pour completed!");
      statusLine.setVisibility(View.VISIBLE);
    } else {
      statusLine.setVisibility(View.INVISIBLE);
    }
  }

}
