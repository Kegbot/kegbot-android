package org.kegbot.kegtap;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

class KBEventView extends TableLayout {

  private TextView _lblName;
  private ImageView _lblIcon;
  private TextView _lblDescription;

  public KBEventView(Context context) {
    super(context);
    _createUI();
  }

  /** create the ui components */
  private void _createUI() {

    Context context = getContext();

    // make the 2nd col growable/wrappable
    setColumnShrinkable(1, true);
    setColumnStretchable(1, true);

    // set the padding
    setPadding(10, 10, 10, 10);

    // single row that holds icon/flag & name
    TableRow row = new TableRow(context);
    //LayoutUtils.Layout.WidthFill_HeightWrap.applyTableLayoutParams(row);

    // fill the first row with: icon/flag, name
    {
      _lblName = new TextView(context);
      //LayoutUtils.Layout.WidthWrap_HeightWrap.applyTableRowParams(_lblName);
      _lblName.setPadding(10, 10, 10, 10);

      //_lblIcon = AppUtils.createImageView(_context, -1, -1, -1);
      //LayoutUtils.Layout.WidthWrap_HeightWrap.applyTableRowParams(_lblIcon);
      //_lblIcon.setPadding(10, 10, 10, 10);

      //row.addView(_lblIcon);
      row.addView(_lblName);
    }

    // create the 2nd row with: description
    {
      _lblDescription = new TextView(context);
      //LayoutUtils.Layout.WidthFill_HeightWrap.applyTableLayoutParams(_lblDescription);
      _lblDescription.setPadding(10, 10, 10, 10);
    }

    // add the rows to the table
    {
      addView(row);
      addView(_lblDescription);
    }

    //Log.i(getClass().getSimpleName(), "CellRendererView created");

  }

  /** update the views with the data corresponding to selection index */
  public void display(int index, boolean selected) {

    /*
    //String zip = getItem(index).toString();
    //Hashtable<String, String> weatherForZip = _data.get(zip);

    Log.i(getClass().getSimpleName(), "row[" + index + "] = " + weatherForZip.toString());

    String temp = weatherForZip.get("temperature");

    String icon = weatherForZip.get("icon");
    //int iconId = ResourceUtils.getResourceIdForDrawable(_context, "com.developerlife", "w" + icon);

    String humidity = weatherForZip.get("humidity");

    _lblName.setText("Feels like: " + temp + " F, in: " + zip);
    //_lblIcon.setImageResource(iconId);
    _lblDescription.setText("Humidity: " + humidity + " %");

    Log.i(getClass().getSimpleName(), "rendering index:" + index);

    if (selected) {
      _lblDescription.setVisibility(View.VISIBLE);
      Log.i(getClass().getSimpleName(), "hiding descripton for index:" + index);
    }
    else {
      _lblDescription.setVisibility(View.GONE);
      Log.i(getClass().getSimpleName(), "showing description for index:" + index);
    }
    */

  }

}
