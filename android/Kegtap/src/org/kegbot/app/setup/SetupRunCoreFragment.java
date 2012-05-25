/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For more
 * information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app.setup;

import org.kegbot.app.R;
import org.kegbot.app.util.PreferenceHelper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

/**
 * {@link SetupFragment} which controls preference for running the Kegbot core.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SetupRunCoreFragment extends SetupFragment {

  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.setup_run_core_fragment, null);
    PreferenceHelper prefs = new PreferenceHelper(getActivity());
    CheckBox box = (CheckBox) mView.findViewById(R.id.runCore);
    box.setChecked(prefs.getRunCore());
    mView.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));
    return mView;
  }

  public boolean getRunCore() {
    CheckBox box = (CheckBox) mView.findViewById(R.id.runCore);
    return box.isChecked();
  }

  @Override
  public String validate() {
    PreferenceHelper prefs = new PreferenceHelper(getActivity());
    prefs.setRunCore(getRunCore());
    return "";
  }

}
