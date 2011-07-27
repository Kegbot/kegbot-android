package org.kegbot.kegtap;

import javax.measure.quantities.Volume;
import javax.measure.units.NonSI;
import javax.measure.units.SI;

import org.jscience.physics.measures.Measure;
import org.kegbot.core.Flow;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PourStatusFragment extends Fragment {

  private View mView;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    mView = inflater.inflate(R.layout.pour_status_fragment_layout, container);
    return mView;
  }


  public void updateForFlow(final Flow flow) {
    // Set volume portion.
    final double volumeMl = flow.getVolumeMl();
    Measure<Volume> vol = Measure.valueOf(volumeMl, SI.MILLI(NonSI.LITER));
    double ounces = vol.doubleValue(NonSI.OUNCE_LIQUID_US);

    final String volumeStr;
    if (ounces < 100) {
      volumeStr = String.format("%.1f", Double.valueOf(ounces));
    } else {
      volumeStr = String.format("%d", Integer.valueOf((int) ounces));
    }

    ((TextView) mView.findViewById(R.id.pourVolumeNumbers)).setText(volumeStr);
    ((TextView) mView.findViewById(R.id.pourVolumeWords)).setText("ounces");

    // Update tap / beer info.
  }

}
