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
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.kegbot.app.event.TapsChangedEvent;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.TapManager;
import org.kegbot.proto.Models.KegTap;

import java.util.List;

import butterknife.ButterKnife;

/**
 * A list fragment representing a list of Taps. This fragment also supports tablet devices by
 * allowing list items to be given an 'activated' state upon selection. This helps indicate which
 * item is currently being viewed in a {@link TapDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks} interface.
 */
public class TapListFragment extends ListFragment {

  private static final String TAG = TapListFragment.class.getSimpleName();

  /**
   * The serialization (saved instance state) Bundle key representing the activated item position.
   * Only used on tablets.
   */
  private static final String STATE_ACTIVATED_POSITION = "activated_position";

  /**
   * The fragment's current callback object, which is notified of list item clicks.
   */
  private Callbacks mCallbacks = sDummyCallbacks;

  private ArrayAdapter<KegTap> mAdapter;
  private final List<KegTap> mTaps = Lists.newArrayList();
  private Bus mBus;

  /**
   * The current activated item position. Only used on tablets.
   */
  private int mActivatedPosition = ListView.INVALID_POSITION;

  /**
   * A callback interface that all activities containing this fragment must implement. This
   * mechanism allows activities to be notified of item selections.
   */
  public interface Callbacks {
    /**
     * Callback for when an item has been selected.
     */
    public void onItemSelected(int tapId);
  }

  /**
   * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when
   * this fragment is not attached to an activity.
   */
  private static Callbacks sDummyCallbacks = new Callbacks() {
    @Override
    public void onItemSelected(int tapId) {
    }
  };

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
   * screen orientation changes).
   */
  public TapListFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final TapManager tapManager = KegbotCore.getInstance(getActivity()).getTapManager();
    mTaps.addAll(tapManager.getTaps());

    mAdapter = new KegTapAdapter(
        getActivity(), android.R.layout.simple_list_item_activated_1, android.R.id.text1, mTaps);

    mBus = KegbotCore.getInstance(getActivity()).getBus();

    mBus.register(this);
  }

  @Override
  public void onDestroy() {
    mBus.unregister(this);
    super.onDestroy();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.tap_list_fragment_layout, container);

    final Button addButton = ButterKnife.findById(view, R.id.addTapButton);
    addButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View arg0) {
        startActivity(NewTapActivity.getStartIntent(getActivity()));
      }
    });

    return view;
  }

  @Subscribe
  public void onTapsListUpdated(final TapsChangedEvent event) {
    Log.d(TAG, "onTapsListUpdated");
    if (getListAdapter() == null) {
      setListAdapter(mAdapter);
    }

    final List<KegTap> taps = event.getTaps();
    if (!mTaps.equals(taps)) {
      mTaps.clear();
      mTaps.addAll(taps);
      mAdapter.notifyDataSetChanged();
    }
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Restore the previously serialized activated item position.
    if (savedInstanceState != null
        && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
      setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);

    // Activities containing this fragment must implement its callbacks.
    if (!(activity instanceof Callbacks)) {
      throw new IllegalStateException("Activity must implement fragment's callbacks.");
    }

    mCallbacks = (Callbacks) activity;
  }

  @Override
  public void onDetach() {
    super.onDetach();

    // Reset the active callbacks interface to the dummy implementation.
    mCallbacks = sDummyCallbacks;
  }

  @Override
  public void onListItemClick(ListView listView, View view, int position, long id) {
    super.onListItemClick(listView, view, position, id);

    // Notify the active callbacks interface (the activity, if the
    // fragment is attached to one) that an item has been selected.
    mCallbacks.onItemSelected(mTaps.get(position).getId());
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (mActivatedPosition != ListView.INVALID_POSITION) {
      // Serialize and persist the activated item position.
      outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
    }
  }

  /**
   * Turns on activate-on-click mode. When this mode is on, list items will be given the 'activated'
   * state when touched.
   */
  public void setActivateOnItemClick(boolean activateOnItemClick) {
    // When setting CHOICE_MODE_SINGLE, ListView will automatically
    // give items the 'activated' state when touched.
    getListView().setChoiceMode(activateOnItemClick
        ? ListView.CHOICE_MODE_SINGLE
        : ListView.CHOICE_MODE_NONE);
  }

  private void setActivatedPosition(int position) {
    if (position == ListView.INVALID_POSITION) {
      getListView().setItemChecked(mActivatedPosition, false);
    } else {
      getListView().setItemChecked(position, true);
    }

    mActivatedPosition = position;
  }

  private static class KegTapAdapter extends ArrayAdapter<KegTap> {

    public KegTapAdapter(Context context, int resource, int textViewResourceId,
        List<KegTap> objects) {
      super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final View view = super.getView(position, convertView, parent);
      final TextView title = (TextView) view.findViewById(android.R.id.text1);
      final KegTap tap = getItem(position);
      title.setText(tap.getName());
      return view;
    }

  }

}
