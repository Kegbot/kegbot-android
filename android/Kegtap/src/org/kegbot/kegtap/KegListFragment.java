package org.kegbot.kegtap;

import org.kegbot.proto.Models;

import android.app.ListFragment;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class KegListFragment extends ListFragment {

  private ArrayAdapter<Models.Keg> mAdapter;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mAdapter = new ArrayAdapter<Models.Keg>(getActivity(), R.layout.keg_list_item);
    setListAdapter(mAdapter);
    setListShown(false);
  }
}
