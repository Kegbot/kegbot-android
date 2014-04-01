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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import org.kegbot.app.R;
import org.kegbot.app.service.CheckinService;

/**
 * {@link SetupFragment} which controls preference for running the Kegbot core.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SetupLicenseFragment extends SetupFragment {

  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.setup_license_fragment, null);
    return mView;
  }

  @Override
  public String validate() {
    CheckBox licenseBox = (CheckBox) mView.findViewById(R.id.agreeLicense);
    CheckBox privacyBox = (CheckBox) mView.findViewById(R.id.agreePrivacy);

    if (!licenseBox.isChecked() || !privacyBox.isChecked()) {
      return getActivity().getString(R.string.setup_license_error);
    }

    Context context = getActivity();
    if (context != null) {
      CheckinService.requestImmediateCheckin(context);
    }

    return "";
  }

}
