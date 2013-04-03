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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.kegbot.app.event.CurrentSessionChangedEvent;
import org.kegbot.app.util.Units;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.Stats;
import org.kegbot.proto.Models.Stats.DrinkerVolume;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.squareup.otto.Subscribe;

public class SessionStatsFragment extends Fragment {

  private static final String TAG = SessionStatsFragment.class.getSimpleName();

  private KegbotCore mCore;
  private View mView;
  private Session mSession;
  private Stats mStats;

  private final static Comparator<DrinkerVolume> VOLUMES_DESCENDING = new Comparator<DrinkerVolume>() {
    @Override
    public int compare(DrinkerVolume object1, DrinkerVolume object2) {
      return Float.valueOf(object2.getVolumeMl()).compareTo(Float.valueOf(object1.getVolumeMl()));
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mCore = KegbotCore.getInstance(getActivity());
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.session_detail_fragment_layout, container, false);
    if (mSession != null) {
      updateSessionView();
    }
    return mView;
  }

  @Override
  public void onResume() {
    mCore.getBus().register(this);
    super.onResume();
    updateSessionView();
  }

  @Override
  public void onPause() {
    mCore.getBus().unregister(this);
    super.onPause();
  }

  @Subscribe
  public void handleSessionChange(CurrentSessionChangedEvent event) {
    mSession = event.getSession();
    mStats = event.getSessionStats();
    Log.d(TAG, "Session change: session=" + mSession);
    updateSessionView();
  }

  private void updateSessionView() {
    if (mView == null) {
      return;
    }

    if (mSession == null) {
      mView.setVisibility(View.GONE);
      return;
    }
    mView.setVisibility(View.VISIBLE);

    final List<DrinkerVolume> volumeByDrinker = Lists.newArrayList(mStats.getVolumeByDrinkerList());
    Collections.sort(volumeByDrinker, VOLUMES_DESCENDING);

    // Session name.
    final String sessionName = mSession.getName();
    ((TextView) mView.findViewById(R.id.sessionTitle)).setText(sessionName);

    // Number of drinkers.
    final int numDrinkers = volumeByDrinker.size();
    ((TextView) mView.findViewById(R.id.sessionDetailNumDrinkers)).setText(String
        .valueOf(numDrinkers));
    final TextView sessionDetailNumDrinkersHeader = (TextView) mView
        .findViewById(R.id.sessionDetailNumDrinkersHeader);
    if (numDrinkers == 1) {
      sessionDetailNumDrinkersHeader.setText("drinker");
    } else {
      sessionDetailNumDrinkersHeader.setText("drinkers");
    }

    // Total volume.
    final Double volumePints = Double.valueOf(Units.volumeMlToPints(mSession.getVolumeMl()));
    ((TextView) mView.findViewById(R.id.sessionDetailVolServed)).setText(String.format("%.1f",
        volumePints));

    final int[] boxes = {R.id.sessionDetailDrinker1, R.id.sessionDetailDrinker2,
        R.id.sessionDetailDrinker3,};
    for (int i = 0; i < boxes.length; i++) {
      final View box = mView.findViewById(boxes[i]);
      if (i >= numDrinkers) {
        box.setVisibility(View.GONE);
        continue;
      }
      box.setVisibility(View.VISIBLE);

      final TextView drinkerName = (TextView) box.findViewById(R.id.drinkerName);
      final TextView drinkerHeader = (TextView) box.findViewById(R.id.drinkerHeader);
      String strname = volumeByDrinker.get(i).getUsername();
      if (strname.isEmpty()) {
        strname = "anonymous";
      }
      drinkerName.setText(strname);
      final double pints = Units.volumeMlToPints(volumeByDrinker.get(i).getVolumeMl());
      drinkerHeader.setText(String.format("%.1f pints", Double.valueOf(pints)));
    }
  }

}
