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

import java.util.concurrent.TimeUnit;

import org.kegbot.app.util.ImageDownloader;
import org.kegbot.app.util.Units;
import org.kegbot.core.ConfigurationManager;
import org.kegbot.core.Flow;
import org.kegbot.core.Flow.State;
import org.kegbot.core.Tap;
import org.kegbot.proto.Models.BeerType;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;

public class PourStatusFragment extends ListFragment {

  private static final String TAG = PourStatusFragment.class.getSimpleName();

  private static final double VOLUME_COUNTER_INCREMENT = 0.1;
  private static final long VOLUME_COUNTER_INCREMENT_DELAY_MILLIS = 20;

  private double mTargetVolume = 0.0;
  private double mCurrentVolume = 0.0;

  private ImageDownloader mImageDownloader;

  private final Tap mTap;

  private View mView;
  private TextView mPourVolumeNumbers;
  private TextView mPourVolumeUnits;
  private TextView mPourBeerName;
  private TextView mStatusLine;
  private ImageView mBeerImage;

  private Handler mHandler;

  private final Runnable mCounterIncrementRunnable = new Runnable() {
    @Override
    public void run() {
      if (mCurrentVolume >= mTargetVolume) {
        return;
      }

      mCurrentVolume += VOLUME_COUNTER_INCREMENT;
      final String volumeStr;
      final String units = "ounces";
      volumeStr = String.format("%.1f", Double.valueOf(mCurrentVolume));
      if (mPourVolumeNumbers != null) {
        mPourVolumeNumbers.setText(volumeStr);
      }
      if (mPourVolumeUnits != null) {
        mPourVolumeUnits.setText(units);
      }

      if (mCurrentVolume < mTargetVolume) {
        mHandler.postDelayed(mCounterIncrementRunnable, VOLUME_COUNTER_INCREMENT_DELAY_MILLIS);
      }
    }
  };

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

    mPourVolumeNumbers = (TextView) mView.findViewById(R.id.pourVolumeNumbers);
    mPourVolumeUnits = (TextView) mView.findViewById(R.id.pourVolumeWords);
    mPourBeerName = (TextView) mView.findViewById(R.id.pourBeerName);
    mStatusLine = (TextView) mView.findViewById(R.id.pourStatusLine);
    mBeerImage = (ImageView) mView.findViewById(R.id.pourImage);

    return mView;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mImageDownloader = ImageDownloader.getSingletonInstance(activity);
    mHandler = new Handler();
  }

  @Override
  public void onResume() {
    super.onResume();
    applyTapDetail();
  }

  private void applyTapDetail() {
    final KegTap tapDetail = ConfigurationManager.getSingletonInstance().getTapDetail(
        getTap().getMeterName());
    if (tapDetail == null) {
      Log.wtf(TAG, "Tap detail is null.");
      return;
    }

    mBeerImage.setBackgroundDrawable(getResources().getDrawable(R.drawable.kegbot_unknown_square_2));

    if (tapDetail.hasCurrentKeg()) {
      final Keg keg = tapDetail.getCurrentKeg();
      final BeerType type = keg.getType();
      final String beerName = type.getName();

      // Set beer name.
      if (!Strings.isNullOrEmpty(beerName) && mPourBeerName != null) {
        mPourBeerName.setText(beerName);
      }

      // Set beer image.
      if (type.hasImage()) {
        final String imageUrl = type.getImage().getUrl();
        mImageDownloader.download(imageUrl, mBeerImage);
      }
    }
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

    // Set volume portion.
    final double ounces = Units.volumeMlToOunces(flow.getVolumeMl());
    mTargetVolume = ounces;
    if (mCurrentVolume < mTargetVolume) {
      mHandler.removeCallbacks(mCounterIncrementRunnable);
      mHandler.post(mCounterIncrementRunnable);
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
    if ((flowState == State.ACTIVE || flowState == State.IDLE)
        && (flow.getIdleTimeMs() >= IDLE_TOOLTIP_MILLIS)) {
      final long seconds = flow.getMsUntilIdle() / 1000;
      mStatusLine.setText("Pour automatically ends in " + seconds + " second"
          + ((seconds != 1) ? "s" : "") + ".");
      mStatusLine.setVisibility(View.VISIBLE);
    } else if (flowState == State.COMPLETED) {
      mStatusLine.setText("Pour completed!");
      mStatusLine.setVisibility(View.VISIBLE);
    } else {
      mStatusLine.setVisibility(View.INVISIBLE);
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
    if (mView != null) {
      mStatusLine.setText("Pour completed!");
      mStatusLine.setVisibility(View.VISIBLE);
    }
  }

}
