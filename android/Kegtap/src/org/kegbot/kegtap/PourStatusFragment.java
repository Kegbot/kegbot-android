package org.kegbot.kegtap;

import java.util.concurrent.TimeUnit;

import org.kegbot.core.ConfigurationManager;
import org.kegbot.core.Flow;
import org.kegbot.core.Flow.State;
import org.kegbot.core.Tap;
import org.kegbot.kegtap.util.image.ImageDownloader;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Models.BeerType;
import org.kegbot.proto.Models.Image;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;

public class PourStatusFragment extends ListFragment {

  private static final String TAG = PourStatusFragment.class.getSimpleName();

  private ImageDownloader mImageDownloader;

  private final Tap mTap;

  private View mView;

  /**
   * After this much inactivity, the "pour automatically ends" dialog is shown.
   */
  private static final long IDLE_TOOLTIP_MILLIS = TimeUnit.SECONDS.toMillis(5);

  public PourStatusFragment(Tap tap) {
    mTap = tap;
  }

  public Tap getTap() {
    return mTap;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.pour_status_item_layout, container, false);
    //mView.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));
    return mView;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mImageDownloader = ImageDownloader.getSingletonInstance(activity);
  }

  public void updateWithFlow(final Flow flow) {
    if (flow == null) {
      Log.wtf(TAG, "Null flow, wtf?");
      return;
    }
    if (mView == null) {
      return;
    }

    final Flow.State flowState = flow.getState();
    final ConfigurationManager config = ConfigurationManager.getSingletonInstance();

    final TextView pourVolumeNumbers = (TextView) mView.findViewById(R.id.pourVolumeNumbers);
    final TextView pourVolumeUnits = (TextView) mView.findViewById(R.id.pourVolumeWords);
    final TextView pourBeerName = (TextView) mView.findViewById(R.id.pourBeerName);
    final TextView statusLine = (TextView) mView.findViewById(R.id.pourStatusLine);

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
    final ImageView tapImage = (ImageView) mView.findViewById(R.id.pourImage);

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
          tapImage.setBackgroundDrawable(null);
          tapImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.kegbot_unknown_square_2));
          //tapImage.setBackgroundResource(R.drawable.kegbot_unknown_square_2);
          if (tapDetail.getBeerType().hasImage()) {
            final Image image = tapDetail.getBeerType().getImage();
            final String imageUrl = image.getUrl();
            mImageDownloader.download(imageUrl, tapImage);
          }
        }
      }
    }

    // Set drinker image.
    //final ImageView pourDrinkerImage = (ImageView) view.findViewById(R.id.pourDrinkerImager);
    /*
    //final Button logInButton = (Button) mView.findViewById(R.id.logInButton);
    final TextView pourDrinkerName = (TextView) mView.findViewById(R.id.pourDrinkerName);
    if (flow.isAnonymous()) {
      logInButton.setVisibility(View.VISIBLE);
      pourDrinkerName.setVisibility(View.GONE);
    } else {
      logInButton.setVisibility(View.GONE);
      pourDrinkerName.setText(flow.getUsername());
      pourDrinkerName.setVisibility(View.VISIBLE);
    }
    */

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

    /*
    final OnClickListener donePouringListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
        mFlowManager.endFlow(flow);
      }
    };
    ((Button) view.findViewById(R.id.endPourButton)).setOnClickListener(donePouringListener);

     */
  }

  public void setIdle() {

  }

}
