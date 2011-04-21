package org.kegbot.kegtap;

import java.text.ParseException;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.kegtap.util.image.ImageDownloader;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Models.Image;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class TapStatusFragment extends Fragment {

  private KegbotApi mApi;

  private View mView;
  private ImageDownloader mImageDownloader;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.tap_list_item, container);
    return mView;
  }

  void setImageDownloader(ImageDownloader imageDownloader) {
    mImageDownloader = imageDownloader;
  }

  private View buildTapView(View view, TapDetail tap) {
    TextView title = (TextView) view.findViewById(R.id.tapTitle);
    title.setText(tap.getBeerType().getName());
    TextView subtitle = (TextView) view.findViewById(R.id.tapSubtitle);
    subtitle.setText(tap.getTap().getName());

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

    ImageView tapImage = (ImageView) view.findViewById(R.id.tapImage);
    if (tapImage != null && tap.getBeerType().hasImage()) {
      final Image image = tap.getBeerType().getImage();
      final String imageUrl = image.getUrl();
      mImageDownloader.download(imageUrl, tapImage);
    }

    view.setBackgroundDrawable(getResources().getDrawable(
        R.drawable.shape_rounded_rect));

    return view;
  }

  void setKegbotApi(KegbotApi api) {
    mApi = api;
  }

  public void loadTap() {
    new TapLoaderTask().execute();
  }

  private class TapLoaderTask extends AsyncTask<Void, Void, TapDetail> {

    @Override
    protected TapDetail doInBackground(Void... params) {
      try {
        return mApi.getAllTaps().getTaps(0);
      } catch (KegbotApiException e) {
        return null;
      }
    }

    @Override
    protected void onPostExecute(TapDetail result) {
      buildTapView(mView, result);
    }

  }

}
