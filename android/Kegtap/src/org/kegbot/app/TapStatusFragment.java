package org.kegbot.app;

import java.text.ParseException;

import javax.measure.units.NonSI;
import javax.measure.units.SI;

import org.jscience.physics.measures.Measure;
import org.kegbot.app.util.image.ImageDownloader;
import org.kegbot.core.FlowManager;
import org.kegbot.core.Tap;
import org.kegbot.core.TapManager;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;

public class TapStatusFragment extends ListFragment {

  private final String TAG = TapStatusFragment.class.getSimpleName();

  private KegTap mTapDetail;

  private ImageDownloader mImageDownloader;

  private static final int SELECT_DRINKER = 100;

  private final OnClickListener mOnBeerMeClickedListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      final String tapName = mTapDetail.getMeterName();
      final Intent intent = DrinkerSelectActivity.getStartIntentForTap(getActivity(), tapName);
      startActivity(intent);
      //startActivityForResult(intent, SELECT_DRINKER);
    }
  };

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case SELECT_DRINKER:
        if (resultCode == Activity.RESULT_OK) {
          String username = data.getStringExtra("username");
          final String tapName = mTapDetail.getMeterName();
          final Tap tap = TapManager.getSingletonInstance().getTapForMeterName(tapName);
          FlowManager.getSingletonInstance().activateUserAtTap(tap, username);
        }
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.tap_detail, container, false);
    if (mTapDetail != null) {
      buildTapView(view, mTapDetail);
    }
    ((Button) view.findViewById(R.id.beerMeButton)).setOnClickListener(mOnBeerMeClickedListener);
    return view;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mImageDownloader = ImageDownloader.getSingletonInstance(activity);
  }

  public View buildTapView(View view, KegTap tap) {
    if (tap == null) {
      Log.w(TAG, "Called with empty tap detail.");
      return view;
    }

    final String tapName = tap.getName();
    if (!Strings.isNullOrEmpty(tapName)) {
      TextView subtitle = (TextView) view.findViewById(R.id.tapSubtitle);
      subtitle.setText(tapName);
    }

    if (!tap.hasCurrentKeg()) {
      return view;
    }

    final Keg keg = tap.getCurrentKeg();
    final TextView title = (TextView) view.findViewById(R.id.tapTitle);
    title.setText(keg.getType().getName());

    CharSequence relTime;
    try {
      long tapDate = Utils.dateFromIso8601String(keg.getStartTime());
      relTime = DateUtils.getRelativeTimeSpanString(tapDate);
    } catch (ParseException e) {
      Log.w(TAG, "Error parsing time:", e);
      relTime = null;
    }

    TextView date = (TextView) view.findViewById(R.id.tapDateTapped);
    if (relTime != null) {
      date.setText("Tapped " + relTime);
    } else {
      date.setVisibility(View.GONE);
    }

    final ImageView tapImage = (ImageView) view.findViewById(R.id.tapImage);
    if (tapImage != null) {
      tapImage.setBackgroundResource(R.drawable.kegbot_unknown_square_2);
      if (keg.getType().hasImage()) {
        final Image image = keg.getType().getImage();
        final String imageUrl = image.getUrl();
        mImageDownloader.download(imageUrl, tapImage);
      }
    }

    float percentFull = tap.getCurrentKeg().getPercentFull();
    TextView kegStatusText = (TextView) view.findViewById(R.id.tapKeg);
    final String statusString;
    if (percentFull > 0) {
      statusString = String.format("%.1f%% full", Float.valueOf(percentFull));
    } else {
      statusString = "Empty!";
    }
    kegStatusText.setText(statusString);

    if (tap.hasLastTemperature()) {
      float lastTemperature = tap.getLastTemperature().getTemperatureC();
      TextView tapTemperature = (TextView) view.findViewById(R.id.tapTemperature);
      double lastTempF = Measure.valueOf(lastTemperature, SI.CELSIUS).doubleValue(NonSI.FAHRENHEIT);
      tapTemperature.setText(String.format("%.2f¡C / %.2f¡F", Float.valueOf(lastTemperature),
          Double.valueOf(lastTempF)));
    }

    view.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));

    final String description = tap.getCurrentKeg().getDescription();
    final TextView descView = (TextView) view.findViewById(R.id.tapDescription);
    if (!Strings.isNullOrEmpty(description)) {
      descView.setVisibility(View.VISIBLE);
      descView.setText(description);
    } else {
      descView.setVisibility(View.GONE);
    }

    return view;
  }

  public void setTapDetail(KegTap tapDetail) {
    mTapDetail = tapDetail;
  }

  public KegTap getTapDetail() {
    return mTapDetail;
  }

}
