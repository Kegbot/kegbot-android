package org.kegbot.kegtap;

import java.text.ParseException;

import javax.measure.units.NonSI;
import javax.measure.units.SI;

import org.jscience.physics.measures.Measure;
import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.kegtap.util.image.ImageDownloader;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Models.Image;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;

public class TapStatusFragment extends Fragment {

  private KegbotApi mApi;

  private View mView;
  private final ImageDownloader mImageDownloader = ImageDownloader.getSingletonInstance();

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.tap_status_fragment_layout, container);
    return mView;
  }

  private View buildTapView(View view, TapDetail tap) {
    final TextView title = (TextView) view.findViewById(R.id.tapTitle);
    title.setText(tap.getBeerType().getName());

    final String tapName = tap.getTap().getName();
    if (!Strings.isNullOrEmpty(tapName)) {
      TextView subtitle = (TextView) view.findViewById(R.id.tapSubtitle);
      subtitle.setText(tapName + ": ");
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

    ImageView tapImage = (ImageView) view.findViewById(R.id.tapImage);
    if (tapImage != null && tap.getBeerType().hasImage()) {
      final Image image = tap.getBeerType().getImage();
      final String imageUrl = image.getUrl();
      tapImage.setBackgroundResource(0);
      mImageDownloader.download(imageUrl, tapImage);
    }

    float percentFull = tap.getKeg().getPercentFull();
    TextView kegStatusText = (TextView) view.findViewById(R.id.tapKeg);
    kegStatusText.setText(String.format("%.1f%% full", Float.valueOf(percentFull)));

    if (tap.getTap().hasLastTemperature()) {
      float lastTemperature = tap.getTap().getLastTemperature().getTemperatureC();
      TextView tapTemperature = (TextView) view.findViewById(R.id.tapTemperature);
      double lastTempF = Measure.valueOf(lastTemperature, SI.CELSIUS).doubleValue(NonSI.FAHRENHEIT);
      tapTemperature.setText(String.format("%.2f¡C / %.2f¡F", Float.valueOf(lastTemperature),
          Double.valueOf(lastTempF)));
    }

    view.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));

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
      try {
        buildTapView(mView, result);
      } catch (Throwable e) {
        Log.wtf("TapStatusFragment", "UNCAUGHT EXCEPTION", e);
      }
    }

  }

}
