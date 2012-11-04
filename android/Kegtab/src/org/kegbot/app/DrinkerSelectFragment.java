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
package org.kegbot.app;

import java.util.List;

import org.kegbot.app.util.ImageDownloader;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.User;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;

public class DrinkerSelectFragment extends Fragment implements LoaderCallbacks<List<User>> {

  private static final String LOG_TAG = DrinkerSelectFragment.class.getSimpleName();

  private KegbotCore mCore;

  private ArrayAdapter<User> mAdapter;

  private View mView;

  private GridView mGridView;

  private ImageDownloader mImageDownloader;

  public static final String LOAD_SOURCE = "source";
  public static final String LOAD_SOURCE_RECENT = "recent";

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mImageDownloader = ImageDownloader.getSingletonInstance(activity);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mCore = KegbotCore.getInstance(getActivity());
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.select_drinker_fragment_inner, null);
    mGridView = (GridView) mView.findViewById(R.id.drinkerGridView);
    return mView;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d(LOG_TAG, "onActivityCreated");
    mAdapter = new ArrayAdapter<User>(getActivity(), R.layout.selectable_drinker,
        R.id.drinkerName) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        final User userDetail = getItem(position);
        final View view = super.getView(position, convertView, parent);

        try {
          applyUser(userDetail, view);
        } catch (Throwable e) {
          Log.wtf(LOG_TAG, "UNCAUGHT EXCEPTION", e);
        }
        view.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));
        return view;
      }

      private void applyUser(User userDetail, View view) {
        final ImageView icon = (ImageView) view.findViewById(R.id.drinkerIcon);
        icon.setImageBitmap(null);
        icon.setBackgroundDrawable(null);

        final String imageUrl;
        if (userDetail.hasImage()) {
          imageUrl = userDetail.getImage().getThumbnailUrl();
        } else {
          imageUrl = "";
        }

        if (!Strings.isNullOrEmpty(imageUrl)) {
          mImageDownloader.download(imageUrl, icon);
        } else {
          mImageDownloader.cancelDownloadForView(icon);
          icon.setBackgroundResource(R.drawable.unknown_drinker);
          icon.setAlpha(1.0f);
        }

        final TextView userName = (TextView) view.findViewById(R.id.drinkerName);
        final String userNameString = userDetail.getUsername();
        userName.setText(userNameString);
      }

    };

    AnimationSet set = new AnimationSet(true);

    Animation animation = new AlphaAnimation(0.0f, 1.0f);
    animation.setDuration(100);
    set.addAnimation(animation);
    animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
        Animation.RELATIVE_TO_SELF, 0.0f);
    animation.setDuration(300);
    set.addAnimation(animation);

    LayoutAnimationController controller = new LayoutAnimationController(set, 0.1f);
    mGridView.setAdapter(mAdapter);
    mGridView.setLayoutAnimation(controller);

    mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        final User user = (User) mGridView.getItemAtPosition(position);
        if (user == null) {
          Log.wtf(LOG_TAG, "Null user selected.");
          return;
        }
        Log.d(LOG_TAG, "Clicked on user: " + user);
        ((DrinkerSelectActivity) getActivity()).handlerUserSelected(user);
      }
    });

    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public Loader<List<User>> onCreateLoader(int id, Bundle args) {
    Log.d(LOG_TAG, "+++ onCreateLoader");
    final Bundle fragArgs = getArguments();

    String method = "";
    if (fragArgs != null) {
      method = getArguments().getString(LOAD_SOURCE, method);
    }

    if (LOAD_SOURCE_RECENT.equalsIgnoreCase(method)) {
      Log.d(LOG_TAG, "load from recent!");
      return new RecentUserDetailLoader(getActivity(), mCore);
    } else {
      return new UserDetailListLoader(getActivity());
    }
  }

  @Override
  public void onLoadFinished(Loader<List<User>> loader, List<User> userList) {
    Log.d(LOG_TAG, "+++ onLoadFinished");

    for (final User user : userList) {
      mAdapter.add(user);
    }
  }

  @Override
  public void onLoaderReset(Loader<List<User>> loader) {
    Log.d(LOG_TAG, "+++ onLoaderReset");
    mAdapter.clear();
  }

}
