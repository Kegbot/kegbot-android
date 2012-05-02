/**
 *
 */
package org.kegbot.app.view;

import org.kegbot.app.R;
import org.kegbot.proto.Api.SessionDetail;

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

  private SessionDetail mSessionDetail = null;

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

  public void setSessionDetail(SessionDetail detail) {
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
