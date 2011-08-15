package org.kegbot.kegtap;

import java.util.Comparator;
import java.util.List;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.kegtap.util.image.ImageDownloader;
import org.kegbot.proto.Api.UserDetail;
import org.kegbot.proto.Api.UserDetailSet;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
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

public class DrinkerSelectFragment extends Fragment {

  private static final String LOG_TAG = DrinkerSelectFragment.class.getSimpleName();

  private ArrayAdapter<UserDetail> mAdapter;

  private View mView;

  private GridView mGridView;

  private final KegbotApi mApi = KegbotApiImpl.getSingletonInstance();
  private final ImageDownloader mImageDownloader = ImageDownloader.getSingletonInstance();

  private static Comparator<UserDetail> USERS_ALPHABETIC = new Comparator<UserDetail>() {
    @Override
    public int compare(UserDetail object1, UserDetail object2) {
      return object1.getUser().getUsername().toLowerCase()
          .compareTo(object2.getUser().getUsername().toLowerCase());
    }
  };

/*
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    UserDetail user = (UserDetail) l.getItemAtPosition(position);
    Log.d(LOG_TAG, "Clicked on user: " + user);
    final String username;
    if (user == null) {
      username = "";
    } else {
      // Defaults to "" in User if necessary.
      username = user.getUser().getUsername();
    }
    final Intent intent = KegtapBroadcast.getUserAuthedBroadcastIntent(username);
    getActivity().sendBroadcast(intent);
    getActivity().finish();
  }
  */
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.select_drinker_fragment_inner, container);
    mGridView = (GridView) mView.findViewById(R.id.drinkerGridView);
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
        Log.d(LOG_TAG, "Binding user " + userDetail.getUser().getUsername()
            + " to view " + view);
        final ImageView icon = (ImageView) view.findViewById(R.id.drinkerIcon);
        final String imageUrl = userDetail.getUser().getImage().getUrl();
        icon.setImageBitmap(null);
        if (!Strings.isNullOrEmpty(imageUrl)) {
          icon.setBackgroundDrawable(null);
          mImageDownloader.download(imageUrl, icon);
        } else {
          icon.setBackgroundResource(R.drawable.unknown_drinker);
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
    animation = new TranslateAnimation(
        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
    );
    animation.setDuration(300);
    set.addAnimation(animation);

    LayoutAnimationController controller = new LayoutAnimationController(set, 0.1f);
    mGridView.setAdapter(mAdapter);
    mGridView.setLayoutAnimation(controller);

    mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        final UserDetail user = (UserDetail) mGridView.getItemAtPosition(position);
        Log.d(LOG_TAG, "Clicked on user: " + user);
        final String username;
        if (user == null) {
          username = "";
        } else {
          // Defaults to "" in User if necessary.
          username = user.getUser().getUsername();
        }
        final Intent intent = KegtapBroadcast.getUserAuthedBroadcastIntent(username);
        getActivity().sendBroadcast(intent);
        getActivity().finish();

      }
    });
  }

  /*
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    //return super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.select_drinker_fragment_inner, container);
  }
  */

  void loadEvents() {
    Log.d(LOG_TAG, "+++ Loading events");
    new UserLoaderTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private class UserLoaderTask extends AsyncTask<Void, Void, UserDetailSet> {

    @Override
    protected UserDetailSet doInBackground(Void... params) {
      try {
        Log.d(LOG_TAG, "+++ Fetching users");
        UserDetailSet result = mApi.getUsers();
        Log.d(LOG_TAG, "+++ Done! Result = " + result);
        return result;
      } catch (KegbotApiException e) {
        Log.w(LOG_TAG, "Could not load users.", e);
        return null;
      }
    }

    @Override
    protected void onPostExecute(UserDetailSet result) {
      Log.d(LOG_TAG, "Users reloaded");
      if (result != null) {
        final List<UserDetail> users = result.getUsersList();
        for (final UserDetail user : users) {
          mAdapter.add(user);
        }
        mAdapter.sort(USERS_ALPHABETIC);
      }
    }
  }
}
