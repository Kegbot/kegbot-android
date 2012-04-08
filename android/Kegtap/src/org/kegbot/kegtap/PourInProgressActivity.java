package org.kegbot.kegtap;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.kegbot.core.Flow;
import org.kegbot.core.FlowManager;
import org.kegbot.core.Tap;
import org.kegbot.core.TapManager;
import org.kegbot.kegtap.camera.CameraFragment;
import org.kegbot.kegtap.util.PreferenceHelper;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.Button;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class PourInProgressActivity extends CoreActivity {

  private static final String TAG = PourInProgressActivity.class.getSimpleName();

  private static final long FLOW_UPDATE_MILLIS = 500;

  private static final long FLOW_FINISH_DELAY_MILLIS = 5000;

  private static final long IDLE_SCROLL_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(3);

  private static final int DIALOG_IDLE_WARNING = 1;

  private final FlowManager mFlowManager = FlowManager.getSingletonInstance();
  private final TapManager mTapManager = TapManager.getSingletonInstance();

  private PowerManager.WakeLock mWakeLock;

  private CameraFragment mCameraFragment;

  private Button mEndPourButton;

  private AlertDialog mIdleDetectedDialog;

  private final Handler mHandler = new Handler();

  private PreferenceHelper mPrefs;

  private PouringTapAdapter mPouringTapAdapter;

  private ViewPager mTapPager;

  private Tap mCurrentTap;

  private List<Tap> mTaps;

  private static final boolean DEBUG = false;

  private static final IntentFilter POUR_INTENT_FILTER = new IntentFilter(
      KegtapBroadcast.ACTION_POUR_START);
  static {
    POUR_INTENT_FILTER.addAction(KegtapBroadcast.ACTION_POUR_UPDATE);
    POUR_INTENT_FILTER.addAction(KegtapBroadcast.ACTION_PICTURE_TAKEN);
    POUR_INTENT_FILTER.setPriority(100);
  }

  private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (KegtapBroadcast.ACTION_POUR_UPDATE.equals(action)
          || KegtapBroadcast.ACTION_POUR_START.equals(action)) {
        handleIntent(intent);
        abortBroadcast();
      } else if (KegtapBroadcast.ACTION_PICTURE_TAKEN.equals(action)) {
        final String filename = intent.getStringExtra(KegtapBroadcast.PICTURE_TAKEN_EXTRA_FILENAME);
        Log.d(TAG, "Got photo: " + filename);

        final Tap tap = mCurrentTap;
        if (tap != null) {
          final Flow flow = mFlowManager.getFlowForTap(tap);
          if (flow != null) {
            Log.d(TAG, "  - attached to flow: " + flow);
            flow.addImage(filename);
          }
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
      cancelIdleWarning();
      finish();
    }
  };

  private final ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
    @Override
    public void onPageSelected(int position) {
      mCurrentTap = ((PourStatusFragment) mPouringTapAdapter.getItem(position)).getTap();
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }
  };

  public class PouringTapAdapter extends FragmentPagerAdapter {

    private final List<PourStatusFragment> mFragments;

    public PouringTapAdapter(FragmentManager fm) {
      super(fm);
      mFragments = Lists.newArrayList();
      for (final Tap tap : mTapManager.getTaps()) {
        mFragments.add(new PourStatusFragment(tap));
      }
    }

    @Override
    public Fragment getItem(int position) {
      return mFragments.get(position);
    }

    public Tap getTap(int position) {
      return ((PourStatusFragment)getItem(position)).getTap();
    }

    @Override
    public int getCount() {
      return mFragments.size();
    }

    public List<PourStatusFragment> getFragments() {
      return ImmutableList.copyOf(mFragments);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }
    setContentView(R.layout.pour_in_progress_activity);
    bindToCoreService();

    mTaps = Lists.newArrayList(mTapManager.getTaps());
    mTapPager = (ViewPager) findViewById(R.id.tapPager);
    mPouringTapAdapter = new PouringTapAdapter(getFragmentManager());
    mTapPager.setAdapter(mPouringTapAdapter);
    mTapPager.setOnPageChangeListener(mPageChangeListener);
    scrollToMostActiveTap();

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
    mPrefs = new PreferenceHelper(this);
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
    final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE
        | PowerManager.ACQUIRE_CAUSES_WAKEUP, "kegbot-pour");
    mWakeLock.acquire();
    registerReceiver(mUpdateReceiver, POUR_INTENT_FILTER);

    handleIntent(getIntent());
  }

  @Override
  protected void onPause() {
    mWakeLock.release();
    mWakeLock = null;
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

    if (KegtapBroadcast.ACTION_POUR_UPDATE.equals(action)
        || KegtapBroadcast.ACTION_POUR_START.equals(action)) {
      refreshFlows();
    }
  }

  private Tap getMostActiveTap() {
    Tap tap = mCurrentTap;
    long leastIdle = Long.MAX_VALUE;
    for (Flow flow : mFlowManager.getAllActiveFlows()) {
      final Tap flowTap = flow.getTap();
      if (flow.getIdleTimeMs() < leastIdle) {
        tap = flowTap;
        leastIdle = flow.getIdleTimeMs();
      }
    }
    return tap;
  }

  private void scrollToMostActiveTap() {
    final Tap mostActive = getMostActiveTap();
    if (mostActive == mCurrentTap) {
      return;
    }

    if (mCurrentTap != null) {
      final Flow currentFlow = mFlowManager.getFlowForTap(mCurrentTap);
      if (currentFlow != null && currentFlow.getIdleTimeMs() < IDLE_SCROLL_TIMEOUT_MILLIS) {
        return;
      }
    }

    final Flow candidateFlow = mFlowManager.getFlowForTap(mostActive);
    if (candidateFlow.getIdleTimeMs() < IDLE_SCROLL_TIMEOUT_MILLIS) {
      scrollToPosition(mTaps.indexOf(candidateFlow.getTap()));
    }
  }

  private void scrollToPosition(int position) {
    Log.d(TAG, "scrollToPosition: " + position);
    mTapPager.setCurrentItem(position, true);
    mCurrentTap = mPouringTapAdapter.getTap(position);
  }

  private void refreshFlows() {
    mHandler.removeCallbacks(FLOW_UPDATE_RUNNABLE);

    final Collection<Flow> allFlows = mFlowManager.getAllActiveFlows();

    // Fetch all flows.
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
    scrollToMostActiveTap();

    for (final PourStatusFragment frag : mPouringTapAdapter.getFragments()) {
      final Flow flow = mFlowManager.getFlowForTap(frag.getTap());
      if (flow != null) {
        frag.updateWithFlow(flow);
      } else {
        frag.setIdle();
      }
    }

    if (largestIdleTime >= mPrefs.getIdleWarningMs()) {
      sendIdleWarning();
    } else {
      cancelIdleWarning();
    }

    if (!allFlows.isEmpty()) {
      mHandler.removeCallbacks(FINISH_ACTIVITY_RUNNABLE);
      mHandler.postDelayed(FLOW_UPDATE_RUNNABLE, FLOW_UPDATE_MILLIS);
    } else {
      cancelIdleWarning();
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
    intent.setAction(KegtapBroadcast.ACTION_POUR_START);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.putExtra(KegtapBroadcast.POUR_UPDATE_EXTRA_TAP_NAME, tapName);
    return intent;
  }

}
