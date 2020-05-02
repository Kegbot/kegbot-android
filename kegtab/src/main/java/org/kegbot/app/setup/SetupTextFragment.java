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
package org.kegbot.app.setup;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.kegbot.app.R;

public class SetupTextFragment extends Fragment {

  private static final String KEY_TITLE = "title";
  private static final String KEY_DESCRIPTION = "description";

  public static SetupTextFragment withText(int titleResource, int descriptionResource) {
    final SetupTextFragment frag = new SetupTextFragment();
    frag.setText(titleResource, descriptionResource);
    return frag;
  }

  void setText(int titleResource, int descriptionResource) {
    Bundle args = getArguments();
    if (args == null) {
      args = new Bundle();
    }

    args.putInt(KEY_TITLE, titleResource);
    args.putInt(KEY_DESCRIPTION, descriptionResource);

    setArguments(args);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.setup_text_fragment, null);

    final TextView titleView = (TextView) view.findViewById(R.id.setupTitleText);
    titleView.setText(getArguments().getInt(KEY_TITLE, 0));
    final TextView descriptionView = (TextView) view.findViewById(R.id.setupDescriptionText);
    descriptionView.setText(getArguments().getInt(KEY_DESCRIPTION, 0));

    return view;
  }

}
