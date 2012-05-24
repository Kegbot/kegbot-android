/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
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

import org.kegbot.app.R;
import org.kegbot.proto.Models.Session;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class SessionDetailView extends LinearLayout {

  private Session mSessionDetail = null;

  private TextView mSessionSubtitle;
  private TextView mSessionTitle;

  public SessionDetailView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  public SessionDetailView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public SessionDetailView(Context context) {
    super(context);
    init(context);
  }

  private void init(Context context) {
    LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    layoutInflater.inflate(R.layout.session_detail_fragment_layout, this, true);

    mSessionTitle = (TextView) findViewById(R.id.sessionTitle);
    mSessionSubtitle = (TextView) findViewById(R.id.sessionSubtitle);
  }

  public void setSessionDetail(Session detail) {
    if (mSessionDetail == detail) {
      return;
    } else if (detail == null && mSessionDetail != null) {
      reset();
      return;
    }
    reset();
    mSessionDetail = detail;
    setVisibility(View.VISIBLE);
  }

  private void reset() {
    mSessionDetail = null;
    setVisibility(View.GONE);
  }

}
