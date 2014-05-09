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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.kegbot.app.KegbotApplication;
import org.kegbot.app.R;
import org.kegbot.app.config.AppConfiguration;

import butterknife.ButterKnife;

/**
 * Shows a choice of
 */
public class SetupSelectBackendFragment extends SetupFragment {

  private View mView;

  private final DialogFragment mDialogFragment = new DialogFragment() {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setTitle(R.string.setup_select_backend_local_warning_title);
      builder.setMessage(R.string.setup_select_backend_local_warning_description);
      builder.setPositiveButton(R.string.setup_select_backend_local_warning_ok, null);
      builder.setCancelable(false);
      return builder.create();
    }
  };

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.setup_select_backend_fragment, null);

    final RadioGroup group = ButterKnife.findById(mView, R.id.backend_group);
    final AppConfiguration prefs = ((KegbotApplication) getActivity().getApplication()).getConfig();
    if (prefs.isLocalBackend()) {
      group.check(R.id.radio_backend_local);
    } else {
      group.check(R.id.radio_backend_server);
    }

    final RadioButton button = ButterKnife.findById(mView, R.id.radio_backend_local);
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mDialogFragment.show(getFragmentManager(), null);
      }
    });

    return mView;
  }

  @Override
  public String validate() {
    final AppConfiguration prefs = ((KegbotApplication) getActivity().getApplication()).getConfig();
    final RadioGroup group = ButterKnife.findById(mView, R.id.backend_group);
    final int checkedId = group.getCheckedRadioButtonId();

    switch (checkedId) {
      case R.id.radio_backend_local:
        prefs.setIsLocalBackend(true);
        break;
      case R.id.radio_backend_server:
        prefs.setIsLocalBackend(false);
        break;
      default:
        return "Please select one of the backend modes.";
    }

    return "";
  }

}
