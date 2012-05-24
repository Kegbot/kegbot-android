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

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.Stats;
import org.kegbot.proto.Models.Stats.DrinkerVolume;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.collect.Lists;

public class SessionStatsFragment extends Fragment {

  private static final String TAG = SessionStatsFragment.class.getSimpleName();

  private final KegbotApi mApi = KegbotApiImpl.getSingletonInstance();

  private View mView;

  private final static Comparator<DrinkerVolume> VOLUMES_DESCENDING = new Comparator<DrinkerVolume>() {
    @Override
    public int compare(DrinkerVolume object1, DrinkerVolume object2) {
      return Float.valueOf(object2.getVolumeMl()).compareTo(Float.valueOf(object1.getVolumeMl()));
    }
  };

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.session_detail_fragment_layout, container);
    return mView;
  }

  private View updateSessionView(View view, Session session) {
    if (session == null) {
      view.setVisibility(View.GONE);
      return view;
    }
    view.setVisibility(View.VISIBLE);

    final Stats stats = session.getStats();
    final List<DrinkerVolume> volumeByDrinker = Lists.newArrayList(stats.getVolumeByDrinkerList());
    Collections.sort(volumeByDrinker, VOLUMES_DESCENDING);

    // Session name.
    final String sessionName = session.getName();
    ((TextView) view.findViewById(R.id.sessionTitle)).setText(sessionName);

    // Number of drinkers.
    final int numDrinkers = volumeByDrinker.size();
    ((TextView) view.findViewById(R.id.sessionDetailNumDrinkers)).setText(String
        .valueOf(numDrinkers));
    final TextView sessionDetailNumDrinkersHeader = (TextView) view
        .findViewById(R.id.sessionDetailNumDrinkersHeader);
    if (numDrinkers == 1) {
      sessionDetailNumDrinkersHeader.setText("drinker");
    } else {
      sessionDetailNumDrinkersHeader.setText("drinkers");
    }

    // Total volume.
    final Double volumePints = Double.valueOf(Units.volumeMlToPints(session.getVolumeMl()));
    ((TextView) view.findViewById(R.id.sessionDetailVolServed)).setText(String.format("%.1f",
        volumePints));

    final int[] boxes = {R.id.sessionDetailDrinker1, R.id.sessionDetailDrinker2,
        R.id.sessionDetailDrinker3,};
    for (int i = 0; i < boxes.length; i++) {
      final View box = view.findViewById(boxes[i]);
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

    view.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));

    return view;
  }

  public void loadCurrentSessionDetail() {
    new CurrentSessionDetailLoaderTask().execute();
  }

  private class CurrentSessionDetailLoaderTask extends AsyncTask<Void, Void, Session> {

    @Override
    protected Session doInBackground(Void... params) {
      try {
        final Session session = mApi.getCurrentSession();
        if (session == null) {
          return null;
        }

        if (session.hasStats()) {
          return session;
        }
        final Stats stats = mApi.getSessionStats(session.getId());
        Log.d(TAG, "Got stats: " + stats);
        return Session.newBuilder(session).setStats(stats).build();
      } catch (KegbotApiException e) {
        return null;
      } catch (RuntimeException e) {
        return null;
      }
    }

    @Override
    protected void onPostExecute(Session result) {
      try {
        updateSessionView(mView, result);
      } catch (Throwable e) {
        Log.wtf("CurrentSessionDetailLoaderTask", "UNCAUGHT EXCEPTION", e);
      }
    }

  }

}
