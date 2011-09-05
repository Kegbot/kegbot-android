package org.kegbot.kegtap;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class PourDrinkerFragment extends Fragment {

  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.pour_drinker_fragment_layout, container);
    return mView;
  }

}
