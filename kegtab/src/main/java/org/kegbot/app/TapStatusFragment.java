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

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.common.base.Strings;
import com.squareup.otto.Subscribe;

import org.kegbot.app.alert.AlertCore;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.event.VisibleTapsChangedEvent;
import org.kegbot.app.util.ImageDownloader;
import org.kegbot.app.util.Units;
import org.kegbot.app.view.BadgeView;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;

import butterknife.ButterKnife;

public class TapStatusFragment extends Fragment {

  private static final String TAG = TapStatusFragment.class.getSimpleName();
  private static final String ARG_TAP_ID = "tap_id";
  private static final int CHILD_INACTIVE = 1;
  private static final int CHILD_ACTIVE = 2;

  private static final int REQUEST_AUTHENTICATE = 1000;

  private KegbotCore mCore;
  private ImageDownloader mImageDownloader;
  private View mView;

  private GestureDetector mGestureDetector;
  private GestureDetector.OnGestureListener mOnGestureListener;
  private View.OnTouchListener mOnTouchListener;

  private final Handler mHandler = new Handler(Looper.getMainLooper());

  public static TapStatusFragment forTap(final KegTap tap) {
    final TapStatusFragment frag = new TapStatusFragment();
    final Bundle args = new Bundle();
    args.putInt(ARG_TAP_ID, tap.getId());
    frag.setArguments(args);
    return frag;
  }


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mCore = KegbotCore.getInstance(getActivity());
    mImageDownloader = mCore.getImageDownloader();
    updateTapDetails();
  }

  @Override
  public void onStart() {
    super.onStart();
    mCore.getBus().register(this);
  }

  @Override
  public void onStop() {
    mCore.getBus().unregister(this);
    super.onStop();
  }

  @Subscribe
  public void onTapListChange(VisibleTapsChangedEvent event) {
    updateTapDetails();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.tap_detail, container, false);

    mOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
      @Override
      public boolean onSingleTapUp(MotionEvent e) {
        handleTapClicked();
        return true;
      }

      @Override
      public boolean onDown(MotionEvent e) {
        return true;
      }

      @Override
      public void onLongPress(MotionEvent e) {
        TapListActivity.startActivity(getActivity());
      }
    };

    mGestureDetector = new GestureDetector(getActivity(), mOnGestureListener);
    mOnTouchListener = new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        return mGestureDetector.onTouchEvent(motionEvent);
      }
    };

    mView.setOnTouchListener(mOnTouchListener);

    updateTapDetails();
    return mView;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_AUTHENTICATE:
        Log.d(TAG, "Got authentication result.");
        if (resultCode == Activity.RESULT_OK && data != null) {
          final String username =
              data.getStringExtra(KegtabCommon.ACTIVITY_AUTH_DRINKER_RESULT_EXTRA_USERNAME);
          if (username != null) {
            AuthenticatingActivity.startAndAuthenticate(getActivity(), username, getTap());
          }
        }
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void handleTapClicked() {
    final AppConfiguration config = KegbotApplication.get(getActivity()).getConfig();
    if (!config.getAllowManualLogin()) {
      Log.d(TAG, "Manual login is disabled.");
      return;
    }
    if (!getTap().hasCurrentKeg()) {
      Log.d(TAG, "Tap is offline");
      return;
    }

    if (!getTap().hasMeter()) {
      mCore.getAlertCore().postAlert(
          AlertCore.newBuilder("Tap Disconnected")
              .setDescription("This tap is not connected to a meter right now.")
              .severityError()
              .build());
      return;
    }

    if (config.useAccounts()) {
      final Intent intent = KegtabCommon.getAuthDrinkerActivityIntent(getActivity());
      startActivityForResult(intent, REQUEST_AUTHENTICATE);
    } else {
      mCore.getFlowManager().activateUserAtTap(getTap(), "");
    }
  }

  private void updateTapDetails() {
    if (mView == null) {
      return;
    }

    final Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    final KegTap tap = getTap();

    final TextView title = ButterKnife.findById(mView, R.id.tapTitle);
    final TextView subtitle = ButterKnife.findById(mView, R.id.tapSubtitle);
    final TextView tapNotes = ButterKnife.findById(mView, R.id.tapNotes);
    final ViewFlipper flipper = ButterKnife.findById(mView, R.id.tapStatusFlipper);

    title.setOnTouchListener(mOnTouchListener);
    subtitle.setOnTouchListener(mOnTouchListener);
    tapNotes.setOnTouchListener(mOnTouchListener);
    flipper.setOnTouchListener(mOnTouchListener);

    final Button button = ButterKnife.findById(mView, R.id.tapKegButton);
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final Intent intent = NewKegActivity.getStartIntent(getActivity(), tap);
        startActivity(intent);
      }
    });

    tapNotes.setText("Last synced: " + DateUtils.formatDateTime(activity, System.currentTimeMillis(),
        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));

    if (tap == null) {
      Log.w(TAG, "Called with empty tap detail.");
      flipper.setDisplayedChild(CHILD_INACTIVE);
      return;
    } else if (!tap.hasCurrentKeg()) {
      Log.d(TAG, "Tap inactive");
      flipper.setDisplayedChild(CHILD_INACTIVE);
      title.setText(tap.getName());
      return;
    } else {
      flipper.setDisplayedChild(CHILD_ACTIVE);
    }

    final String tapName = tap.getName();
    if (!Strings.isNullOrEmpty(tapName)) {
      subtitle.setText(tapName);
    }

    if (!tap.hasCurrentKeg()) {
      return;
    }

    final Keg keg = tap.getCurrentKeg();
    title.setText(keg.getBeverage().getName());

    // Find a description to show.
    String description = keg.getDescription();
    if (Strings.isNullOrEmpty(description)) {
      description = keg.getBeverage().getDescription();
    }
    if (Strings.isNullOrEmpty(description)) {
      description = tap.getDescription();
    }

    final ImageView tapImage = (ImageView) mView.findViewById(R.id.tapImage);
    final ImageView tapIllustration = (ImageView) mView.findViewById(R.id.tapIllustration);

    // Show tap image, or notes if none available.
    tapImage.setVisibility(View.VISIBLE);
    tapNotes.setVisibility(View.GONE);

    double pct = keg.getPercentFull();

    final int res;
    if (pct >= 95.0) {
      res = R.drawable.keg_srm14_5;
    } else if (pct >= 80) {
      res = R.drawable.keg_srm14_4;
    } else if (pct >= 40) {
      res = R.drawable.keg_srm14_3;
    } else if (pct >= 20) {
      res = R.drawable.keg_srm14_2;
    } else if (pct >= 5) {
      res = R.drawable.keg_srm14_1;
    } else {
      res = R.drawable.keg_srm14_0;
    }
    tapImage.setImageResource(res);

    if (keg.hasIllustrationUrl()) {
      final String illustrationUrl = keg.getIllustrationUrl();
      Log.d(TAG, "Fetching illustration: " + illustrationUrl);
      mImageDownloader.download(keg.getIllustrationUrl(), tapIllustration);
    }

    if (keg.getBeverage().hasPicture()) {
      final Image image = keg.getBeverage().getPicture();
      final String imageUrl = image.getUrl();
      mImageDownloader.download(imageUrl, tapImage);
    } else if (!Strings.isNullOrEmpty(description)) {
      tapImage.setVisibility(View.GONE);
      tapNotes.setVisibility(View.VISIBLE);
      tapNotes.setText(description);
    }

    showIllustration(true);

    // TODO(mikey): proper units support
    // Badge 1: Pints Poured
    final BadgeView badge1 = (BadgeView) mView.findViewById(R.id.tapStatsBadge1);
    double mlPoured = keg.getServedVolumeMl();
    Pair<String, String> qtyPoured = Units.localize(mCore.getConfiguration(), mlPoured);

    badge1.setBadgeValue(qtyPoured.first);
    badge1.setBadgeCaption(Units.capitalizeUnits(qtyPoured.second) + " Poured");

    // Badge 2: Pints Remain
    final BadgeView badge2 = (BadgeView) mView.findViewById(R.id.tapStatsBadge2);
    Pair<String, String> qtyRemain = Units.localize(mCore.getConfiguration(),
        keg.getRemainingVolumeMl());

    badge2.setBadgeValue(qtyRemain.first);
    badge2.setBadgeCaption(Units.capitalizeUnits(qtyRemain.second) + " Left");

    // Badge 3: Temperature
    // TODO(mikey): Preference for C/F
    final BadgeView badge3 = (BadgeView) mView.findViewById(R.id.tapStatsBadge3);
    if (tap.hasLastTemperature()) {
      double lastTemperature = tap.getLastTemperature().getTemperatureC();
      String units = "C";
      if (!mCore.getConfiguration().getTemperaturesCelsius()) {
        lastTemperature = Units.temperatureCToF(lastTemperature);
        units = "F";
      }
      final String tempValue = String.format("%.1f\u00B0", Double.valueOf(lastTemperature));
      badge3.setBadgeValue(tempValue);
      badge3.setBadgeCaption(String.format("Temperature (%s)", units));
      badge3.setVisibility(View.VISIBLE);
    } else {
      badge3.setVisibility(View.GONE);
    }
  }

  void showIllustration(boolean show) {
    final ViewFlipper flipper = (ViewFlipper) mView.findViewById(R.id.illustrationFlipper);
    flipper.setDisplayedChild(show ? 1 : 0);
  }

  int getTapId() {
    return getArguments().getInt(ARG_TAP_ID, -1);
  }

  private KegTap getTap() {
    final int tapId = getTapId();
    return mCore.getTapManager().getTap(tapId);
  }

}
