package org.kegbot.kegtap;

import java.text.ParseException;

import javax.measure.units.NonSI;
import javax.measure.units.SI;

import org.jscience.physics.measures.Measure;
import org.kegbot.kegtap.util.image.ImageDownloader;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Models.Image;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;

public class TapListFragment extends ListFragment {

  private final String TAG = TapListFragment.class.getSimpleName();

  private TapDetail mTapDetail;
  private ImageDownloader mImageDownloader;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mImageDownloader = ImageDownloader.getSingletonInstance(activity);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.tap_status_fragment_layout, container, false);
    if (mTapDetail != null) {
      buildTapView(view, mTapDetail);
    }
    return view;
  }

  private View buildTapView(View view, TapDetail tap) {
    final TextView title = (TextView) view.findViewById(R.id.tapTitle);
    if (tap == null) {
      Log.w(TAG, "Called with empty tap detail.");
      return view;
    }
    title.setText(tap.getBeerType().getName());

    final String tapName = tap.getTap().getName();
    if (!Strings.isNullOrEmpty(tapName)) {
      TextView subtitle = (TextView) view.findViewById(R.id.tapSubtitle);
      subtitle.setText(tapName);
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

  public void setTapDetail(TapDetail tapDetail) {
    mTapDetail = tapDetail;
  }

  public TapDetail getTapDetail() {
    return mTapDetail;
  }

}
