/**
 *
 */
package org.kegbot.app.view;

import org.kegbot.app.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A {@link View} which lists a numeric value and a caption.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class NumericBadgeView extends LinearLayout {

  private TextView mBadgeValue;
  private TextView mBadgeCaption;

  public NumericBadgeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  public NumericBadgeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public NumericBadgeView(Context context) {
    super(context);
    init(context);
  }

  private void init(Context context) {
    final LayoutInflater layoutInflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    layoutInflater.inflate(R.layout.numeric_badge_view_layout, this, true);

    mBadgeValue = (TextView) findViewById(R.id.badgeValue);
    mBadgeCaption = (TextView) findViewById(R.id.badgeCaption);
  }

  public void setBadgeValue(String value) {
    mBadgeValue.setText(value);
  }

  public void setBadgeCaption(String caption) {
    mBadgeCaption.setText(caption);
  }

}
