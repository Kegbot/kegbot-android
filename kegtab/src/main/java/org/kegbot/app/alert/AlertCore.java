package org.kegbot.app.alert;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.kegbot.app.event.AlertEvent;
import org.kegbot.core.Manager;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

/**
 * In-app notification system.
 */
public class AlertCore extends Manager {

  private static final String TAG = AlertCore.class.getSimpleName();

  private final Context mContext;
  private final Bus mBus;

  private final Map<String, Alert> mAlerts = Maps.newLinkedHashMap();

  public static class Builder {
    private String mId;
    private final String mTitle;
    private String mDescription = "";
    private String mSeverity = Alert.SEVERITY_INFO;
    private Intent mAction;

    private boolean mDismissOnView = true;
    private long mAutoDismissTimeoutMillis = 0;

    public Builder(String title) {
      mTitle = title;
    }

    public Builder setId(String id) {
      mId = id;
      return this;
    }

    public Builder setDescription(String description) {
      mDescription = description;
      return this;
    }

    public Builder severityInfo() {
      mSeverity = Alert.SEVERITY_INFO;
      return this;
    }

    public Builder severityWarning() {
      mSeverity = Alert.SEVERITY_WARNING;
      return this;
    }

    public Builder severityError() {
      mSeverity = Alert.SEVERITY_ERROR;
      return this;
    }

    public Builder setAction(Intent action) {
      mAction = action;
      return this;
    }

    public Builder setAutoDismissTimeoutMillis(long autoDismissTimeoutMillis) {
      mAutoDismissTimeoutMillis = autoDismissTimeoutMillis;
      return this;
    }

    public Builder setDismissOnView(boolean dismissOnView) {
      mDismissOnView = dismissOnView;
      return this;
    }

    public Alert build() {
      if (mId == null) {
        mId = String.valueOf(new SecureRandom().nextInt());
      }
      return new Alert(mId, mTitle, mDescription, mSeverity, mAction, mDismissOnView,
          SystemClock.uptimeMillis(), mAutoDismissTimeoutMillis);
    }
  }

  public static class Alert {

    public static final String SEVERITY_INFO = "info";
    public static final String SEVERITY_WARNING = "warning";
    public static final String SEVERITY_ERROR = "error";

    private final String mId;
    private final String mTitle;
    private final String mDescription;
    private final String mSeverity;
    private final Intent mAction;
    private final boolean mDismissOnView;
    private final long mPostTimeMillis;
    private final long mAutoDismissTimeoutMillis;

    Alert(final String id, final String title, final String description, final String severity,
        final Intent action, final boolean dismissOnView,
        final long postTimeMillis, final long autoDismissTimeoutMillis) {
      mId = Preconditions.checkNotNull(id);
      mTitle = Preconditions.checkNotNull(title);
      mDescription = Strings.nullToEmpty(description);
      mAction = action;
      mSeverity = Preconditions.checkNotNull(severity);
      mDismissOnView = dismissOnView;
      mPostTimeMillis = postTimeMillis;
      mAutoDismissTimeoutMillis = autoDismissTimeoutMillis;
    }

    public String getId() {
      return mId;
    }

    public String getTitle() {
      return mTitle;
    }

    public String getDescription() {
      return mDescription;
    }

    public String getSeverity() {
      return mSeverity;
    }

    public Intent getAction() {
      return mAction;
    }

    public boolean getDismissOnView() {
      return mDismissOnView;
    }

    public long getPostTimeMillis() {
      return mPostTimeMillis;
    }

    public long getAutoDismissTimeoutMillis() {
      return mAutoDismissTimeoutMillis;
    }

  }

  public AlertCore(final Bus bus, final Context context) {
    super(bus);
    mContext = context;
    mBus = bus;
  }

  @Override
  protected void start() {
    super.start();
    mBus.register(this);
  }

  @Override
  protected void stop() {
    mBus.unregister(this);
    super.stop();
  }

  @Subscribe
  public void onAlertEvent(AlertEvent event) {
    Log.d(TAG, "Got alert!");
    postAlert(event.getAlert());
  }

  @Override
  public synchronized void postAlert(Alert alert) {
    Log.d(TAG, "Posting alert: " + alert.getId());
    final String alertId = alert.getId();
    if (mAlerts.containsKey(alertId)) {
      cancelAlert(alertId);
    }
    mAlerts.put(alertId, alert);
    if (alert.getSeverity() == Alert.SEVERITY_ERROR) {
      AlertActivity.showDialogs(mContext);
    }
    postOnMainThread(new AlertPostedEvent(alert));
  }

  public synchronized boolean cancelAlert(String alertId) {
    Log.d(TAG, "Canceling alert " + alertId);
    final Alert alert = mAlerts.remove(alertId);
    if (alert != null) {
      Log.d(TAG, "Posting cancel event.");
      postOnMainThread(new AlertCancelledEvent(alert));
    }
    return alert != null;
  }

  public boolean cancelAlert(Alert alert) {
    return cancelAlert(alert.getId());
  }

  public List<Alert> getAlerts() {
    return ImmutableList.<Alert>copyOf(mAlerts.values());
  }

  public static Builder newBuilder(String title) {
    return new Builder(title);
  }

}
