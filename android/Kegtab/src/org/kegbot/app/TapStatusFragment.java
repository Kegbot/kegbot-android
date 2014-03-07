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
import android.app.ListFragment;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.common.base.Strings;

import org.kegbot.app.util.ImageDownloader;
import org.kegbot.app.util.Units;
import org.kegbot.app.view.BadgeView;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;

public class TapStatusFragment extends ListFragment {

  private final String TAG = TapStatusFragment.class.getSimpleName();

  private KegbotCore mCore;

  private KegTap mTapDetail;

  private ImageDownloader mImageDownloader;

  private View mView;

  private static final int CHILD_INACTIVE = 1;
  private static final int CHILD_ACTIVE = 2;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mCore = KegbotCore.getInstance(getActivity());
    mImageDownloader = mCore.getImageDownloader();
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.tap_detail, container, false);
    if (mTapDetail != null) {
      setTapDetail(mTapDetail);
    }
    return mView;
  }

  public void setTapDetail(KegTap tap) {
    mTapDetail = tap;
    if (mView == null) {
      return;
    }
    final Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    final TextView title = (TextView) mView.findViewById(R.id.tapTitle);
    final TextView subtitle = (TextView) mView.findViewById(R.id.tapSubtitle);
    final TextView tapNotes = (TextView) mView.findViewById(R.id.tapNotes);
    final ViewFlipper flipper = (ViewFlipper) mView.findViewById(R.id.tapStatusFlipper);

    tapNotes.setText("Last synced: " + DateUtils.formatDateTime(activity, System.currentTimeMillis(),
        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));

    flipper.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        TapListActivity.startActivity(getActivity());
        return true;
      }
    });

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
    title.setText(keg.getType().getName());

    final ImageView tapImage = (ImageView) mView.findViewById(R.id.tapImage);
    tapImage.setImageResource(R.drawable.kegbot_unknown_square_2);
    if (keg.getType().hasImage()) {
      final Image image = keg.getType().getImage();
      final String imageUrl = image.getUrl();
      mImageDownloader.download(imageUrl, tapImage);
    }

    // TODO(mikey): proper units support
    // Badge 1: Pints Poured
    final BadgeView badge1 = (BadgeView) mView.findViewById(R.id.tapStatsBadge1);
    double mlPoured = keg.getSizeVolumeMl() * (100.0 - keg.getPercentFull()) / 100.0;
    Pair<String, String> qtyPoured = Units.localize(mCore.getConfiguration(), mlPoured);

    badge1.setBadgeValue(qtyPoured.first);
    badge1.setBadgeCaption(Units.capitalizeUnits(qtyPoured.second) + " Poured");

    // Badge 2: Pints Remain
    final BadgeView badge2 = (BadgeView) mView.findViewById(R.id.tapStatsBadge2);
    Pair<String, String> qtyRemain = Units.localize(mCore.getConfiguration(),
        keg.getVolumeMlRemain());

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

    return;
  }

  public KegTap getTapDetail() {
    return mTapDetail;
  }

}
