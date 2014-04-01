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
package org.kegbot.app.setup;

import org.kegbot.app.R;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A simple fragment which shows a title and a description.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
@SuppressLint("ValidFragment")  // TODO(mikey): Remove ctor arguments
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
