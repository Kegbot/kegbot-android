package org.kegbot.kegtap;

import java.text.ParseException;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.kegtap.util.image.ImageDownloader;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Api.TapDetailSet;

import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TapListFragment extends ListFragment {

  private ArrayAdapter<TapDetail> mAdapter;

  private KegbotApi mApi;

  private ImageDownloader mImageDownloader;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mAdapter = new ArrayAdapter<TapDetail>(getActivity(), R.layout.tap_list_item, R.id.tapTitle) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View view =  super.getView(position, convertView, parent);
        TapDetail tap = getItem(position);
        TextView title = (TextView) view.findViewById(R.id.tapTitle);
        title.setText(tap.getTap().getName());
        TextView subtitle = (TextView) view.findViewById(R.id.tapSubtitle);
        if (tap.hasBeverage()) {
          subtitle.setText(tap.getBeverage().getName());
        }

        CharSequence relTime;
        try {
          long tapDate = Utils.dateFromIso8601String(tap.getKeg().getStartedTime());
          relTime = DateUtils.getRelativeTimeSpanString(tapDate);
        } catch (ParseException e) {
          relTime = null;
        }

        TextView date = (TextView) view.findViewById(R.id.tapDateTapped);
        if (relTime != null) {
          date.setText("Tapped " + relTime);
        } else {
          date.setVisibility(View.GONE);
        }

        return view;
      }

    };
    setListAdapter(mAdapter);
  }

  void setKegbotApi(KegbotApi api) {
    mApi = api;
  }

  void setImageDownloader(ImageDownloader imageDownloader) {
    mImageDownloader = imageDownloader;
  }

  void loadTaps() {
    new TapLoaderTask().execute();
  }

  private Spanned getTitle(TapDetail tap) {
    String tapName = tap.getTap().getName();
    String beverageName = tap.hasBeverage() ? tap.getBeverage().getName() : getResources().getString(R.string.empty_tap);
    return Html.fromHtml(getResources().getString(R.string.tap_name, tapName, beverageName));
  }

  private class TapLoaderTask extends AsyncTask<Void, Void, TapDetailSet> {

    @Override
    protected TapDetailSet doInBackground(Void... params) {
      try {
        return mApi.getAllTaps();
      } catch (KegbotApiException e) {
        e.printStackTrace();
        return null;
      }
    }

    @Override
    protected void onPreExecute() {
      setListShown(false);
    }

    @Override
    protected void onPostExecute(TapDetailSet result) {
      mAdapter.clear();
      if (result != null) {
        mAdapter.addAll(result.getTapsList());
        setListShown(true);
      }
    }
  }
}
