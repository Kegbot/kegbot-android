package org.kegbot.kegtap;

import org.kegbot.proto.Models;

import android.app.ListFragment;
import android.os.Bundle;
import android.widget.ArrayAdapter;

/**
 * Displays events in a System Event Set in a list.
 *
 * @author justinkoh
 */
public class EventListFragment extends ListFragment {

  private ArrayAdapter<Models.SystemEvent> mAdapter;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mAdapter = new ArrayAdapter<Models.SystemEvent>(getActivity(), R.layout.event_list_item);
    setListAdapter(mAdapter);
    setListShown(false);
  }
}
