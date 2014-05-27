/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.kegbot.app.R;

/**
 * A {@link View} which lists a numeric value and a caption.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class BadgeView extends LinearLayout {

  private TextView mBadgeValue;
  private TextView mBadgeCaption;

  public BadgeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  public BadgeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public BadgeView(Context context) {
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
