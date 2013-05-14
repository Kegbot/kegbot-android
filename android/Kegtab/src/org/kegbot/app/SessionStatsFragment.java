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

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.kegbot.app.event.CurrentSessionChangedEvent;
import org.kegbot.app.util.Units;
import org.kegbot.app.view.BadgeView;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.Session;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.squareup.otto.Subscribe;

public class SessionStatsFragment extends Fragment {

  private static final String TAG = SessionStatsFragment.class.getSimpleName();

  private KegbotCore mCore;
  private View mView;
  private Session mSession;
  private JsonNode mStats;

  private BadgeView mSessionDrinkersBadge;
  private BadgeView mSessionVolumeBadge;
  private BadgeView mDrinker1Badge;
  private BadgeView mDrinker2Badge;
  private BadgeView mDrinker3Badge;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mCore = KegbotCore.getInstance(getActivity());
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.session_detail_fragment_layout, container, false);
    mSessionVolumeBadge = (BadgeView) mView.findViewById(R.id.sessionVolumeBadge);
    mSessionDrinkersBadge = (BadgeView) mView.findViewById(R.id.numDrinkersBadge);
    mDrinker1Badge = (BadgeView) mView.findViewById(R.id.sessionDrinker1Badge);
    mDrinker2Badge = (BadgeView) mView.findViewById(R.id.sessionDrinker2Badge);
    mDrinker3Badge = (BadgeView) mView.findViewById(R.id.sessionDrinker3Badge);
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

    JsonNode stats = mStats.get("volume_by_drinker");
    if (stats == null || !stats.isObject()) {
      Log.w(TAG, "Stats error: " + stats);
      return;
    }

    Map<String, Double> volumeMap;
    try {
      volumeMap = new ObjectMapper().readValue(stats,
          new TypeReference<Map<String, Double>>() {});
    } catch (JsonMappingException e) {
      Log.w(TAG, "Stats error", e);
      return;
    } catch (IOException e) {
      Log.w(TAG, "Stats error", e);
      return;
    }

    final Ordering<String> order = Ordering.natural().reverse()
        .onResultOf(Functions.forMap(volumeMap))
        .compound(Ordering.<String> natural());
    final Map<String, Double> volumeMapSorted = ImmutableSortedMap.copyOf(volumeMap, order);

    // Session name.
    final String sessionName = mSession.getName();
    ((TextView) mView.findViewById(R.id.sessionTitle)).setText(sessionName);

    // Number of drinkers.
    final int numDrinkers = volumeMapSorted.size();
    mSessionDrinkersBadge.setBadgeValue(Integer.valueOf(numDrinkers).toString());
    mSessionDrinkersBadge.setBadgeCaption(numDrinkers == 1 ? "Drinker" : "Drinkers");

    // Total volume.
    final Pair<String, String> qty = Units.localize(mCore.getConfiguration(),
        mSession.getVolumeMl());
    mSessionVolumeBadge.setBadgeValue(qty.first);
    mSessionVolumeBadge.setBadgeCaption(String.format("Total %s",
        Units.capitalizeUnits(qty.second)));

    final BadgeView[] badges = {
        mDrinker1Badge,
        mDrinker2Badge,
        mDrinker3Badge
    };

    int i = 0;
    for (Map.Entry<String, Double> entry : volumeMapSorted.entrySet()) {
      final BadgeView badge = badges[i++];

      badge.setVisibility(View.VISIBLE);

      String strname = entry.getKey();
      if (strname.isEmpty()) {
        strname = "anonymous";
      }

      badge.setBadgeValue(strname);
      final Pair<String, String> drinkerQty = Units.localize(mCore.getConfiguration(),
          entry.getValue().doubleValue());
      badge.setBadgeCaption(String.format("%s %s", drinkerQty.first, drinkerQty.second));
    }

    for (; i < badges.length; i++) {
      final BadgeView badge = badges[i];
      badge.setVisibility(View.GONE);
    }

  }

}
