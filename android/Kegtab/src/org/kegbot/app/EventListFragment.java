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
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.squareup.otto.Subscribe;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.event.SystemEventListUpdateEvent;
import org.kegbot.app.util.ImageDownloader;
import org.kegbot.app.util.Units;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.SyncManager;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.SystemEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lists recent events.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class EventListFragment extends ListFragment {

  private static final String TAG = EventListFragment.class.getSimpleName();

  /** Maximum number of events to show/retain. */
  private static final int MAX_EVENTS = 20;
  private static final long REFRESH_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

  private View mView;
  private ArrayAdapter<SystemEvent> mAdapter;
  private KegbotCore mCore;
  private ImageDownloader mImageDownloader;
  private final Handler mHandler = new Handler();
  private final List<SystemEvent> mCachedEvents = Lists.newArrayList();

  /** Refreshes event timestamps by invalidating all ListView children periodically. */
  private final Runnable mTimeUpdateRunnable = new Runnable() {
    @Override
    public void run() {
      ListView v = getListView();
      if (v != null) {
        v.invalidateViews();
      }
      mHandler.postDelayed(this, REFRESH_INTERVAL_MILLIS);
    }
  };

  /**
   *
   */
  private class EventListArrayAdapter extends ArrayAdapter<SystemEvent> {

    private EventListArrayAdapter(Context context, int resource, int textViewResourceId) {
      super(context, resource, textViewResourceId);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final View view = super.getView(position, convertView, parent);
      final SystemEvent eventDetail = getItem(position);
      try {
        formatEvent(eventDetail, view);
      } catch (Throwable e) {
        Log.wtf(TAG, "UNCAUGHT EXCEPTION", e);
      }
      return view;
    }

    private void formatEvent(SystemEvent event, View view) {
      // Common: image.
      final ImageView icon = (ImageView) view.findViewById(R.id.eventIcon);
      icon.setVisibility(View.GONE);
      if (event.hasImage()) {
        final String imageUrl = event.getImage().getThumbnailUrl();
        if (!Strings.isNullOrEmpty(imageUrl)) {
          icon.setVisibility(View.VISIBLE);
          icon.setImageBitmap(null);

          // Default to unknown drinker, may be immediately replaced by downloader.
          icon.setBackgroundResource(R.drawable.unknown_drinker);
          mImageDownloader.download(imageUrl, icon);
        }
      }

      final String eventTextContent;
      final String userNameString = getUsernameForEvent(event);
      if (userNameString != null) {
        eventTextContent = "<b>" + userNameString + "</b> " + getTitle(event);
      } else {
        eventTextContent = getTitle(event);
      }

      final TextView eventText = (TextView) view.findViewById(R.id.eventText);
      eventText.setText(Html.fromHtml(eventTextContent));

      // Date and time.
      TextView dateView = (TextView) view.findViewById(R.id.eventDate);
      String isoTime = event.getTime();
      try {
        long time = org.kegbot.app.util.DateUtils.dateFromIso8601String(isoTime);

        CharSequence relTime = DateUtils.getRelativeDateTimeString(getContext(), time, 60 * 1000,
            7 * 24 * 60 * 60 * 100, 0);
        dateView.setText(relTime);
        dateView.setVisibility(View.VISIBLE);
      } catch (IllegalArgumentException e) {
        dateView.setVisibility(View.GONE);
      }

    }

    private String getTitle(SystemEvent eventDetail) {
      final String kind = eventDetail.getKind();
      final Drink drink = eventDetail.getDrink();
      String result;

      if ("keg_ended".equals(kind)) {
        result = "ended";
      } else if ("keg_tapped".equals(kind)) {
        result = "tapped";
      } else if ("drink_poured".equals(kind)) {
        final AppConfiguration appConfig = KegbotCore.getInstance(getContext()).getConfiguration();
        Pair<String, String> qty = Units.localize(appConfig, drink.getVolumeMl());
        result = String.format("poured %s %s", qty.first, qty.second);
      } else if ("session_joined".equals(kind)) {
        result = "started drinking";
      } else if ("session_started".equals(kind)) {
        result = "started a new session";
      } else {
        result = "Unknown event";
      }
      return result;
    }

    private String getUsernameForEvent(SystemEvent eventDetail) {
      final String userName;
      if (!eventDetail.hasUser()) {
        userName = "a guest";
      } else {
        userName = eventDetail.getUser().getUsername();
      }

      final String kind = eventDetail.getKind();
      if ("drink_poured".equals(kind)) {
        return userName;
      } else if ("session_joined".equals(kind)) {
        return userName;
      } else if ("session_started".equals(kind)) {
        return userName;
      } else if ("keg_ended".equals(kind) || "keg_tapped".equals(kind)) {
        if (eventDetail.hasKeg()) {
          return "Keg " + eventDetail.getKeg().getId();
        }
      }

      return null;
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mImageDownloader = KegbotCore.getInstance(activity).getImageDownloader();
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    mAdapter = new EventListArrayAdapter(getActivity(), R.layout.event_list_item, R.id.eventText);

    AnimationSet set = new AnimationSet(true);

    Animation animation = new AlphaAnimation(0.0f, 1.0f);
    animation.setDuration(300);
    set.addAnimation(animation);
    animation = new TranslateAnimation(
        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
    );
    animation.setDuration(300);
    set.addAnimation(animation);

    LayoutAnimationController controller = new LayoutAnimationController(set, 0.3f);
    getListView().setLayoutAnimation(controller);
    setListAdapter(mAdapter);
  }

  @Override
  public void onResume() {
    super.onResume();
    mHandler.postDelayed(mTimeUpdateRunnable, REFRESH_INTERVAL_MILLIS);
    mCore = KegbotCore.getInstance(getActivity());
    mCore.getBus().register(this);
  }

  @Override
  public void onPause() {
    mCore.getBus().unregister(this);
    mHandler.removeCallbacks(mTimeUpdateRunnable);
    super.onPause();
  }

  @Subscribe
  public void onEventListUpdate(SystemEventListUpdateEvent event) {
    List<SystemEvent> newEvents = event.getEvents();
    if (!newEvents.isEmpty()) {
      Log.d(TAG, "Events updated: " + newEvents.size());
      for (SystemEvent e : newEvents) {
        if (!mCachedEvents.contains(e)) {
          mCachedEvents.add(e);
        }
      }

      Collections.sort(mCachedEvents, SyncManager.EVENTS_DESCENDING);
      while (mCachedEvents.size() > MAX_EVENTS) {
        mCachedEvents.remove(mCachedEvents.size() - 1);
      }
      mAdapter.clear();
      mAdapter.addAll(mCachedEvents);
    }

    if (mView != null) {
      if (mCachedEvents.isEmpty()) {
        mView.setVisibility(View.GONE);
      } else {
        mView.setVisibility(View.VISIBLE);
      }
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.event_list_fragment_layout, container, false);
    return mView;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

}
