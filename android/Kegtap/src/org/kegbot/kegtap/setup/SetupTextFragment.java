package org.kegbot.kegtap.setup;

import org.kegbot.kegtap.R;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SetupTextFragment extends Fragment {

  final String mTitle;
  final String mDescription;

  public SetupTextFragment() {
    this("","");
  }

  public SetupTextFragment(String title, String description) {
    mTitle = title;
    mDescription = description;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.setup_text_fragment, null);

    TextView titleView = (TextView) view.findViewById(R.id.setupTitleText);
    titleView.setText(mTitle);
    TextView descriptionView = (TextView) view.findViewById(R.id.setupDescriptionText);
    descriptionView.setText(mDescription);

    return view;
  }

}
