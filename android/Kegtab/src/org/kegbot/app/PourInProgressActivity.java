/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For more
 * information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.kegbot.app.camera.CameraFragment;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.core.Flow;
import org.kegbot.core.FlowManager;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.KegTap;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Activity shown while a pour is in progress.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class PourInProgressActivity extends CoreActivity {

  private static final String TAG = PourInProgressActivity.class.getSimpleName();

  /** Interval between polls for flow status. */
  private static final long FLOW_UPDATE_MILLIS = 500;

  /** Delay after a flow has ended, during which a dialog is show. */
  private static final long FLOW_FINISH_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1);

  /** Minimum idle seconds before a flow is considered idle for display purposes. */
  private static final long IDLE_SCROLL_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(3);

  /** Constant identifying {@link #mIdleDetectedDialog}. */
  private static final int DIALOG_IDLE_WARNING = 1;

  private KegbotCore mCore;
  private FlowManager mFlowManager;

  private CameraFragment mCameraFragment;

  private AlertDialog mIdleDetectedDialog;

  private final Handler mHandler = new Handler();

  private AppConfiguration mConfig;

  private PouringTapAdapter mPouringTapAdapter;

  private DialogFragment mProgressDialog;

  private ImageView mDrinkerImage;
  private TextView mShoutText;
  private Button mDoneButton;
  private ViewPager mTapPager;

  private KegTap mCurrentTap;

  private List<KegTap> mTaps;

  private final List<KegTap> mLastActiveTaps = Lists.newArrayList();

  private static final boolean DEBUG = false;

  private static final IntentFilter POUR_INTENT_FILTER = new IntentFilter(
      KegtabBroadcast.ACTION_POUR_START);
  static {
    POUR_INTENT_FILTER.addAction(KegtabBroadcast.ACTION_POUR_UPDATE);
    POUR_INTENT_FILTER.addAction(KegtabBroadcast.ACTION_PICTURE_TAKEN);
    POUR_INTENT_FILTER.addAction(KegtabBroadcast.ACTION_PICTURE_DISCARDED);
    POUR_INTENT_FILTER.setPriority(100);
  }

  private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (KegtabBroadcast.ACTION_POUR_UPDATE.equals(action)
          || KegtabBroadcast.ACTION_POUR_START.equals(action)) {
        handleIntent(intent);
        abortBroadcast();
      } else if (KegtabBroadcast.ACTION_PICTURE_TAKEN.equals(action)) {
        final String filename = intent.getStringExtra(KegtabBroadcast.PICTURE_TAKEN_EXTRA_FILENAME);
        Log.d(TAG, "Got photo: " + filename);
        final Flow flow = getCurrentlyFocusedFlow();
        if (flow != null) {
          Log.d(TAG, "  - attached to flow: " + flow);
          flow.addImage(filename);
        }
      } else if (KegtabBroadcast.ACTION_PICTURE_DISCARDED.equals(action)) {
        final String filename = intent
            .getStringExtra(KegtabBroadcast.PICTURE_DISCARDED_EXTRA_FILENAME);
        Log.d(TAG, "Discarded photo: " + filename);
        final Flow flow = getCurrentlyFocusedFlow();
        if (flow != null) {
          Log.d(TAG, "  - remove from flow: " + flow);
          flow.removeImage(filename);
        }
      }
    }
  };

  private final Runnable FLOW_UPDATE_RUNNABLE = new Runnable() {
    @Override
    public void run() {
      refreshFlows();
    }
  };

  private final Runnable FINISH_ACTIVITY_RUNNABLE = new Runnable() {
    @Override
    public void run() {
      if (mProgressDialog != null) {
        mProgressDialog.dismiss();
        mProgressDialog = null;
      }
      cancelIdleWarning();
      finish();
    }
  };

  private final ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
    @Override
    public void onPageSelected(int position) {
      PourStatusFragment frag = (PourStatusFragment) mPouringTapAdapter.getItem(position);
      final KegTap tap = frag.getTap();
      if (tap != mCurrentTap) {
        updateForNewlyFocusedTap(tap);
      }
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }
  };

  public class PouringTapAdapter extends FragmentPagerAdapter {

    private final Map<String, PourStatusFragment> mFrags = Maps.newLinkedHashMap();

    public PouringTapAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      final KegTap tap = mTaps.get(position);
      if (!mFrags.containsKey(tap.getMeterName())) {
        mFrags.put(tap.getMeterName(), new PourStatusFragment(tap));
      }
      return mFrags.get(tap.getMeterName());
    }

    @Override
    public int getItemPosition(Object object) {
      // TODO(mikey): maintain this.
      return POSITION_NONE;
    }

    public KegTap getTap(int position) {
      return ((PourStatusFragment) getItem(position)).getTap();
    }

    @Override
    public int getCount() {
      return mTaps.size();
    }
  }

  public static class PourFinishProgressDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      ProgressDialog dialog = new ProgressDialog(getActivity());
      dialog.setIndeterminate(true);
      dialog.setCancelable(false);
      dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      dialog.setMessage("Please wait..");
      dialog.setTitle("Saving drink");
      return dialog;
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");

    mCore = KegbotCore.getInstance(this);
    mFlowManager = mCore.getFlowManager();
    mConfig = mCore.getConfiguration();

    final ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }
    setContentView(R.layout.pour_in_progress_activity);

    mTaps = mFlowManager.getAllActiveTaps();

    if (mTaps.isEmpty()) {
      Log.e(TAG, "No taps!");
      finish();
      return;
    }

    mTapPager = (ViewPager) findViewById(R.id.tapPager);
    mPouringTapAdapter = new PouringTapAdapter(getFragmentManager());
    mTapPager.setAdapter(mPouringTapAdapter);
    mTapPager.setOnPageChangeListener(mPageChangeListener);

    mDoneButton = (Button) findViewById(R.id.pourEndButton);
    mDrinkerImage = (ImageView) findViewById(R.id.pourDrinkerImage);
    mShoutText = (TextView) findViewById(R.id.shoutText);

    mDrinkerImage.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final Intent intent = KegtabCommon
            .getAuthDrinkerActivityIntent(PourInProgressActivity.this);
        //startActivityForResult(intent, AUTH_DRINKER_REQUEST);
      }
    });

    /*
    if (!Strings.isNullOrEmpty(username) && !username.equals(mAppliedUsername)) {
      final AuthenticationManager authManager = mCore.getAuthenticationManager();
      final User user = authManager.getUserDetail(username);
      if (user != null && user.hasImage()) {
        // NOTE(mikey): Use the full-sized image rather than the thumbnail;
        // in many cases the former will already be in the cache from
        // DrinkerSelectActivity.
        final String thumbnailUrl = user.getImage().getThumbnailUrl();
        if (!Strings.isNullOrEmpty(thumbnailUrl)) {
          mImageDownloader.download(thumbnailUrl, mDrinkerImage);
        }
      }
      mDrinkerImage.setOnClickListener(null);
    }
*/

    mDoneButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        final FlowManager flowManager = mCore.getFlowManager();
        final Flow flow = flowManager.getFlowForTap(mCurrentTap);

        if (flow != null) {
          flowManager.endFlow(flow);
        }
      }
    });

    mShoutText = (EditText) findViewById(R.id.shoutText);
    mShoutText.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        final KegTap tap = mCurrentTap;
        if (tap == null) {
          Log.w(TAG, "Bad tap.");
          return;
        }
        final Flow flow = mCore.getFlowManager().getFlowForTap(tap);
        if (flow == null) {
          Log.w(TAG, "Flow went away, dropping shout.");
          return;
        }
        flow.setShout(s.toString());
        flow.pokeActivity();
      }
    });


    mCameraFragment = (CameraFragment) getFragmentManager().findFragmentById(R.id.camera);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage("Hey, are you still pouring?").setCancelable(false).setPositiveButton(
        "Continue Pouring", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            for (final Flow flow : mFlowManager.getAllActiveFlows()) {
              flow.pokeActivity();
            }
            dialog.cancel();
          }
        }).setNegativeButton("Done Pouring", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        for (final Flow flow : mFlowManager.getAllActiveFlows()) {
          mFlowManager.endFlow(flow);
        }
        dialog.cancel();
      }
    });

    mIdleDetectedDialog = builder.create();

    refreshFlows();
  }

  private Flow getCurrentlyFocusedFlow() {
    final KegTap tap = mCurrentTap;
    if (tap != null) {
      final Flow flow = mFlowManager.getFlowForTap(tap);
      if (flow != null) {
        return flow;
      }
    }
    return null;
  }

  private void updateForNewlyFocusedTap(final KegTap tap) {
    mCurrentTap = tap;

    final Flow flow = mFlowManager.getFlowForTap(tap);
    if (flow == null) {
      return;
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id == DIALOG_IDLE_WARNING) {
      return mIdleDetectedDialog;
    }
    return super.onCreateDialog(id);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");

    registerReceiver(mUpdateReceiver, POUR_INTENT_FILTER);
    handleIntent(getIntent());
    if (mCurrentTap != null) {
      updateForNewlyFocusedTap(mCurrentTap);
    }
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    final Flow flow = getCurrentlyFocusedFlow();
    if (flow != null && flow.getImages().isEmpty()) {
      if (mConfig.getEnableAutoTakePhoto()) {
        mCameraFragment.schedulePicture();
      }
    }
  }

  @Override
  protected void onPause() {
    Log.d(TAG, "onPause");
    mHandler.removeCallbacks(FLOW_UPDATE_RUNNABLE);
    unregisterReceiver(mUpdateReceiver);
    super.onPause();
  }

  @Override
  public void onBackPressed() {
    if (!mFlowManager.getAllActiveFlows().isEmpty()) {
      endAllFlows();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntent(intent);
  }

  private void handleIntent(final Intent intent) {
    final String action = intent.getAction();
    Log.d(TAG, "Handling intent: " + intent);

    if (KegtabBroadcast.ACTION_POUR_UPDATE.equals(action)
        || KegtabBroadcast.ACTION_POUR_START.equals(action)) {
      refreshFlows();
    }
  }

  private KegTap getMostActiveTap() {
    KegTap tap = mCurrentTap;
    long leastIdle = Long.MAX_VALUE;

    for (Flow flow : mFlowManager.getAllActiveFlows()) {
      final KegTap flowTap = flow.getTap();
      if (flow.getIdleTimeMs() < leastIdle) {
        tap = flowTap;
        leastIdle = flow.getIdleTimeMs();
      }
    }
    return tap;
  }

  private void scrollToMostActiveTap() {
    if (mCurrentTap != null) {
      final Flow currentFlow = mFlowManager.getFlowForTap(mCurrentTap);
      if (currentFlow != null && currentFlow.getIdleTimeMs() < IDLE_SCROLL_TIMEOUT_MILLIS) {
        // Current flow is active; don't scroll.
        return;
      }
    }

    final KegTap mostActive = getMostActiveTap();

    if (mostActive == null) {
      Log.d(TAG, "Could not find an active tap.");
      return;
    } else if (mostActive == mCurrentTap) {
      return;
    }

    final Flow candidateFlow = mFlowManager.getFlowForTap(mostActive);
    Log.d(TAG, "Scrolling to " + mostActive.getMeterName());
    scrollToPosition(mTaps.indexOf(candidateFlow.getTap()));
  }

  private void scrollToPosition(int position) {
    Log.d(TAG, "scrollToPosition: " + position);
    mTapPager.setCurrentItem(position, true);
    final KegTap tap = mPouringTapAdapter.getTap(position);
    if (tap != mCurrentTap) {
      updateForNewlyFocusedTap(tap);
    }
  }

  private void refreshFlows() {
    final Collection<Flow> allFlows = mFlowManager.getAllActiveFlows();

    mHandler.removeCallbacks(FLOW_UPDATE_RUNNABLE);
    mCameraFragment.setEnabled(!allFlows.isEmpty());

    final List<KegTap> activeTaps = mFlowManager.getAllActiveTaps();

    for (final KegTap tap : activeTaps) {
      if (!mTaps.contains(tap)) {
        mTaps.add(tap);
        Log.d(TAG, "+++ Added newly active tap "  + tap);
        mPouringTapAdapter.notifyDataSetChanged();
      }
    }

    long largestIdleTime = Long.MIN_VALUE;
    for (final Flow flow : allFlows) {
      if (DEBUG) {
        Log.d(TAG, "Refreshing with flow: " + flow);
      }

      final long idleTimeMs = flow.getIdleTimeMs();
      if (idleTimeMs > largestIdleTime) {
        largestIdleTime = idleTimeMs;
      }
    }

    if (!mLastActiveTaps.equals(activeTaps)) {
      scrollToMostActiveTap();
      mLastActiveTaps.clear();
      mLastActiveTaps.addAll(activeTaps);
    }

    for (int i=0; i < mPouringTapAdapter.getCount(); i++) {
      final PourStatusFragment frag = (PourStatusFragment) mPouringTapAdapter.getItem(i);
      final Flow flow = mFlowManager.getFlowForTap(frag.getTap());
      if (flow != null) {
        frag.updateWithFlow(flow);
      } else {
        frag.setEnded();
      }
    }

    if (largestIdleTime >= mConfig.getIdleWarningMs()) {
      sendIdleWarning();
    } else {
      cancelIdleWarning();
    }

    mHandler.removeCallbacks(FINISH_ACTIVITY_RUNNABLE);
    if (!allFlows.isEmpty()) {
      mHandler.postDelayed(FLOW_UPDATE_RUNNABLE, FLOW_UPDATE_MILLIS);
    } else {
      cancelIdleWarning();
      mProgressDialog = new PourFinishProgressDialog();
      mProgressDialog.show(getFragmentManager(), "finish");
      mHandler.postDelayed(FINISH_ACTIVITY_RUNNABLE, FLOW_FINISH_DELAY_MILLIS);
    }
  }

  private void endAllFlows() {
    for (final Flow flow : mFlowManager.getAllActiveFlows()) {
      mFlowManager.endFlow(flow);
    }
    cancelIdleWarning();
    mCameraFragment.cancelPendingPicture();
  }

  private void sendIdleWarning() {
    if (mIdleDetectedDialog.isShowing()) {
      return;
    }
    showDialog(DIALOG_IDLE_WARNING);
  }

  private void cancelIdleWarning() {
    if (!mIdleDetectedDialog.isShowing()) {
      return;
    }
    dismissDialog(DIALOG_IDLE_WARNING);
  }

  public static Intent getStartIntent(Context context, final String tapName) {
    final Intent intent = new Intent(context, PourInProgressActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.setAction(KegtabBroadcast.ACTION_POUR_START);
    intent.putExtra(KegtabBroadcast.POUR_UPDATE_EXTRA_TAP_NAME, tapName);
    return intent;
  }

}
