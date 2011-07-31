package org.kegbot.kegtap;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.proto.Api.SessionDetail;
import org.kegbot.proto.Api.SessionSet;
import org.kegbot.proto.Models.Session;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SessionStatsFragment extends Fragment {

  private final KegbotApi mApi = KegbotApiImpl.getSingletonInstance();

  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.session_detail_fragment_layout, container);
    return mView;
  }

  private View updateSessionView(View view, SessionDetail tap) {
    view.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));
    return view;
  }

  public void loadCurrentSessionDetail() {
    new CurrentSessionDetailLoaderTask().execute();
  }

  private class CurrentSessionDetailLoaderTask extends AsyncTask<Void, Void, SessionDetail> {

    @Override
    protected SessionDetail doInBackground(Void... params) {
      try {
        SessionSet sessions = mApi.getCurrentSessions();
        if (sessions.getSessionsCount() == 1) {
          Session session = sessions.getSessions(0);
          SessionDetail sessionDetail = mApi.getSessionDetail(session.getId());
          return sessionDetail;
        }
        return null;
      } catch (KegbotApiException e) {
        return null;
      }
    }

    @Override
    protected void onPostExecute(SessionDetail result) {
      try {
        updateSessionView(mView, result);
      } catch (Throwable e) {
        Log.wtf("CurrentSessionDetailLoaderTask", "UNCAUGHT EXCEPTION", e);
      }
    }

  }

}
