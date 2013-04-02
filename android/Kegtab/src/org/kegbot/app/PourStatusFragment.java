/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For more
 * information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app;

import java.util.concurrent.TimeUnit;

import org.kegbot.app.util.ImageDownloader;
import org.kegbot.app.util.Units;
import org.kegbot.app.view.BadgeView;
import org.kegbot.core.AuthenticationManager;
import org.kegbot.core.Flow;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class PourStatusFragment extends ListFragment {

  private static final String TAG = PourStatusFragment.class.getSimpleName();

  private static final double VOLUME_COUNTER_INCREMENT = 0.1;
  private static final long VOLUME_COUNTER_INCREMENT_DELAY_MILLIS = 20;

  private static final int AUTH_DRINKER_REQUEST = 1;

  private double mTargetVolume = 0.0;
  private double mCurrentVolume = 0.0;

  private KegbotCore mCore;

  private ImageDownloader mImageDownloader;

  private final KegTap mTap;

  private View mView;
  private BadgeView mPourVolumeBadge;
  private TextView mTapTitle;
  private TextView mTapSubtitle;
  private TextView mStatusLine;

  private ImageView mBeerImage;

  private String mAppliedUsername;

  private Handler mHandler;

  private final Runnable mCounterIncrementRunnable = new Runnable() {
    @Override
    public void run() {
      if (mCurrentVolume >= mTargetVolume) {
        return;
      }

      mCurrentVolume += VOLUME_COUNTER_INCREMENT;

      final String volumeStr = String.format("%.1f", Double.valueOf(mCurrentVolume));
      mPourVolumeBadge.setBadgeValue(volumeStr);

      final String units;
      if (volumeStr == "1.0") {
        units = "Ounce Poured";
      } else {
        units = "Ounces Poured";
      }
      mPourVolumeBadge.setBadgeCaption(units);

      if (mCurrentVolume < mTargetVolume) {
        mHandler.postDelayed(mCounterIncrementRunnable, VOLUME_COUNTER_INCREMENT_DELAY_MILLIS);
      }
    }
  };

  /**
   * After this much inactivity, the "pour automatically ends" dialog is shown.
   */
  private static final long IDLE_TOOLTIP_MILLIS = TimeUnit.SECONDS.toMillis(5);

  private Flow mFlow = null;

  @SuppressLint("ValidFragment")
  public PourStatusFragment(KegTap tap) {
    mTap = tap;
  }

  public KegTap getTap() {
    return mTap;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mCore = KegbotCore.getInstance(getActivity());
    mImageDownloader = mCore.getImageDownloader();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.pour_status_item_layout, container, false);

    mPourVolumeBadge = (BadgeView) mView.findViewById(R.id.tapStatsBadge1);
    mPourVolumeBadge.setBadgeValue("0.0");
    mPourVolumeBadge.setBadgeCaption("Ounces Poured");

    mTapTitle = (TextView) mView.findViewById(R.id.tapTitle);
    mTapSubtitle = (TextView) mView.findViewById(R.id.tapSubtitle);

    mStatusLine = (TextView) mView.findViewById(R.id.tapNotes);
    mBeerImage = (ImageView) mView.findViewById(R.id.tapImage);

    return mView;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case AUTH_DRINKER_REQUEST:
        if (resultCode == Activity.RESULT_OK) {
          final String username =
              data.getStringExtra(KegtabCommon.ACTIVITY_AUTH_DRINKER_RESULT_EXTRA_USERNAME);
          if (!Strings.isNullOrEmpty(username)) {
            Log.d(TAG, "Authenticating async.");
            AuthenticationManager am = mCore.getAuthenticationManager();
            am.authenticateUsernameAsync(username);
          }
        }
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mHandler = new Handler();
  }

  @Override
  public void onResume() {
    super.onResume();
    applyTapDetail();
    if (mFlow != null) {
      updateWithFlow(mFlow);
    }
  }

  private void applyTapDetail() {
    final KegTap tap = getTap();
    mBeerImage.setImageResource(R.drawable.kegbot_unknown_square_2);

    final Keg keg = tap.getCurrentKeg();

    if (keg != null) {
      final String beerName = keg.getType().getName();

      // Set beer name.
      if (!Strings.isNullOrEmpty(beerName) && mTapTitle != null) {
        mTapTitle.setText(beerName);
      }

      // Set beer image.
      if (keg.getType().hasImage()) {
        mImageDownloader.download(keg.getType().getImage().getUrl(), mBeerImage);
      }
      mTapSubtitle.setText(mTap.getName());
    } else {
      mTapTitle.setText(mTap.getName());
    }
  }

  public void updateWithFlow(final Flow flow) {
    Preconditions.checkNotNull(flow, "null flow given to updateWithFlow()");
    mFlow = flow;

    if (mFlow.isFinished()) {
      setEnded();
      return;
    }

    if (mView == null) {
      return;
    }

    // Set volume portion.
    final double ounces = Units.volumeMlToOunces(flow.getVolumeMl());
    mTargetVolume = ounces;
    if (mCurrentVolume < mTargetVolume) {
      mHandler.removeCallbacks(mCounterIncrementRunnable);
      mHandler.post(mCounterIncrementRunnable);
    }

    // Set tap title.
    final String username = flow.getUsername();

    // Update beer info.
    if (flow.getIdleTimeMs() >= IDLE_TOOLTIP_MILLIS) {
      final long seconds = flow.getMsUntilIdle() / 1000;
      mStatusLine.setText("Pour automatically ends in " + seconds + " second"
          + ((seconds != 1) ? "s" : "") + ".");
      mStatusLine.setVisibility(View.VISIBLE);
    } else {
      mStatusLine.setVisibility(View.INVISIBLE);
    }

    mAppliedUsername = username;
  }

  /** Marks the tap as ended (no current flow). */
  public void setEnded() {
    if (mView != null) {
      mStatusLine.setText("Pour completed!");
      mStatusLine.setVisibility(View.VISIBLE);
    }
  }

}
