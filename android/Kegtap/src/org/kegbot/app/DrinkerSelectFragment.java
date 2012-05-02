package org.kegbot.app;

import java.util.List;

import org.kegbot.app.util.image.ImageDownloader;
import org.kegbot.core.AuthenticationManager;
import org.kegbot.app.R;
import org.kegbot.proto.Api.UserDetail;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
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

public class DrinkerSelectFragment extends Fragment implements LoaderCallbacks<List<UserDetail>> {

  private static final String LOG_TAG = DrinkerSelectFragment.class.getSimpleName();

  private ArrayAdapter<UserDetail> mAdapter;

  private View mView;

  private GridView mGridView;

  private AuthenticationManager mAuthManager;

  private ImageDownloader mImageDownloader;

  public static final String LOAD_SOURCE = "source";
  public static final String LOAD_SOURCE_RECENT = "recent";

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mImageDownloader = ImageDownloader.getSingletonInstance(activity);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.select_drinker_fragment_inner, null);
    mGridView = (GridView) mView.findViewById(R.id.drinkerGridView);
    mAuthManager = AuthenticationManager.getSingletonInstance(getActivity());
    return mView;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d(LOG_TAG, "onActivityCreated");
    mAdapter = new ArrayAdapter<UserDetail>(getActivity(), R.layout.selectable_drinker,
        R.id.drinkerName) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        final UserDetail userDetail = getItem(position);
        final View view = super.getView(position, convertView, parent);

        try {
          applyUser(userDetail, view);
        } catch (Throwable e) {
          Log.wtf(LOG_TAG, "UNCAUGHT EXCEPTION", e);
        }
        view.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));
        return view;
      }

      private void applyUser(UserDetail userDetail, View view) {
        final ImageView icon = (ImageView) view.findViewById(R.id.drinkerIcon);
        final String imageUrl = userDetail.getUser().getImage().getUrl();
        icon.setImageBitmap(null);
        icon.setBackgroundDrawable(null);
        if (!Strings.isNullOrEmpty(imageUrl)) {
          mImageDownloader.download(imageUrl, icon);
        } else {
          mImageDownloader.cancelDownloadForView(icon);
          icon.setBackgroundResource(R.drawable.unknown_drinker);
          icon.setAlpha(1.0f);
        }

        final TextView userName = (TextView) view.findViewById(R.id.drinkerName);
        final String userNameString = userDetail.getUser().getUsername();
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
        final UserDetail user = (UserDetail) mGridView.getItemAtPosition(position);
        if (user == null) {
          Log.wtf(LOG_TAG, "Null user selected.");
          return;
        }
        Log.d(LOG_TAG, "Clicked on user: " + user);
        final Intent srcIntent = getActivity().getIntent();
        final String tapName = srcIntent.getStringExtra(KegtapBroadcast.DRINKER_SELECT_EXTRA_TAP_NAME);
        if (!Strings.isNullOrEmpty(tapName)) {
          mAuthManager.noteUserAuthenticated(user, tapName);
        } else {
          mAuthManager.noteUserAuthenticated(user);
        }
        getActivity().finish();
      }
    });

    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public Loader<List<UserDetail>> onCreateLoader(int id, Bundle args) {
    Log.d(LOG_TAG, "+++ onCreateLoader");
    final Bundle fragArgs = getArguments();

    String method = "";
    if (fragArgs != null) {
      method = getArguments().getString(LOAD_SOURCE, method);
    }

    if (LOAD_SOURCE_RECENT.equalsIgnoreCase(method)) {
      Log.d(LOG_TAG, "load from recent!");
      return new RecentUserDetailLoader(getActivity());
    } else {
      return new UserDetailListLoader(getActivity());
    }
  }

  @Override
  public void onLoadFinished(Loader<List<UserDetail>> loader, List<UserDetail> userList) {
    Log.d(LOG_TAG, "+++ onLoadFinished");

    for (final UserDetail user : userList) {
      mAdapter.add(user);
    }
  }

  @Override
  public void onLoaderReset(Loader<List<UserDetail>> loader) {
    Log.d(LOG_TAG, "+++ onLoaderReset");
    mAdapter.clear();
  }

}
