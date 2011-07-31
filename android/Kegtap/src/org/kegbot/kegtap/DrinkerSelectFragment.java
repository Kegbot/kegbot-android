package org.kegbot.kegtap;

import java.util.Comparator;
import java.util.List;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.kegtap.util.image.ImageDownloader;
import org.kegbot.proto.Api.UserDetail;
import org.kegbot.proto.Api.UserDetailSet;

import android.app.ListFragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Strings;

public class DrinkerSelectFragment extends ListFragment {

  private static final String LOG_TAG = DrinkerSelectFragment.class.getSimpleName();

  private ArrayAdapter<UserDetail> mAdapter;

  private final KegbotApi mApi = KegbotApiImpl.getSingletonInstance();
  private final ImageDownloader mImageDownloader = ImageDownloader.getSingletonInstance();

  private static Comparator<UserDetail> USERS_ALPHABETIC = new Comparator<UserDetail>() {
    @Override
    public int compare(UserDetail object1, UserDetail object2) {
      return object1.getUser().getUsername().compareTo(object2.getUser().getUsername());
    }
  };

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

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d(LOG_TAG, "here");
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
        return view;
      }

      private void applyUser(UserDetail userDetail, View view) {
        Log.d(LOG_TAG, "Binding user " + userDetail.getUser().getUsername()
            + " to view " + view);
        final ImageView icon = (ImageView) view.findViewById(R.id.drinkerIcon);
        final String imageUrl = userDetail.getUser().getImage().getUrl();
        if (!Strings.isNullOrEmpty(imageUrl)) {
          icon.setBackgroundDrawable(null);
          icon.setBackgroundResource(0);
          mImageDownloader.download(imageUrl, icon);
        } else {
          icon.setBackgroundDrawable(null);
          icon.setBackgroundResource(R.drawable.unknown_drinker);
        }

        final TextView userName = (TextView) view.findViewById(R.id.drinkerName);
        final String userNameString = userDetail.getUser().getUsername();
        userName.setText(userNameString);
      }

    };

    AnimationSet set = new AnimationSet(true);

    Animation animation = new AlphaAnimation(0.0f, 1.0f);
    animation.setDuration(300);
    set.addAnimation(animation);
    animation = new TranslateAnimation(
        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
    );
    animation.setDuration(300);
    set.addAnimation(animation);

    LayoutAnimationController controller = new LayoutAnimationController(set, 0.3f);
    getListView().setLayoutAnimation(controller);
    setListAdapter(mAdapter);
  }

  /*
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    //return super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.select_drinker_fragment_inner, container);
  }
  */

  void loadEvents() {
    new UserLoaderTask().execute();
  }

  private class UserLoaderTask extends AsyncTask<Void, Void, UserDetailSet> {

    @Override
    protected UserDetailSet doInBackground(Void... params) {
      try {
        return mApi.getUsers();
      } catch (KegbotApiException e) {
        Log.w(LOG_TAG, "Could not load users.", e);
        return null;
      }
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
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
