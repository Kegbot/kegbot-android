package org.kegbot.kegtap;

import org.kegbot.core.Flow;
import org.kegbot.core.FlowManager;
import org.kegbot.kegtap.core.KegtapBroadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class PourInProgressActivity extends CoreActivity {

  public final String LOG_TAG = PourInProgressActivity.class.getSimpleName();

  private static final int MESSAGE_FLOW_UPDATE = 1;

  private static final int MESSAGE_FLOW_FINISH = 2;

  private static final long FLOW_UPDATE_MILLIS = 1000;

  private static final long FLOW_FINISH_DELAY_MILLIS = 5000;

  private PourStatusFragment mPourStatus;

  private static final IntentFilter POUR_INTENT_FILTER = new IntentFilter(KegtapBroadcast.ACTION_POUR_UPDATE);
  static {
    POUR_INTENT_FILTER.setPriority(100);
  }

  private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (KegtapBroadcast.ACTION_POUR_UPDATE.equals(action)) {
        handleIntent(intent);
        abortBroadcast();
      }
    }
  };

  private final Handler mHandler = new Handler() {

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MESSAGE_FLOW_UPDATE:
          Flow flow = (Flow) msg.obj;
          doFlowUpdate(flow);

          if (flow.getState() != Flow.State.COMPLETED) {
            final Message message = mHandler.obtainMessage(MESSAGE_FLOW_UPDATE, flow);
            mHandler.sendMessageDelayed(message, FLOW_UPDATE_MILLIS);
          } else {
            mHandler.sendEmptyMessageDelayed(MESSAGE_FLOW_FINISH, FLOW_FINISH_DELAY_MILLIS);
          }
          return;
        case MESSAGE_FLOW_FINISH:
          finish();
          return;
      }
      super.handleMessage(msg);
    }

  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getActionBar().hide();
    setContentView(R.layout.pour_in_progress_activity);
    bindToCoreService();

    findViewById(R.id.pourInProgressRightCol)
        .setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_rounded_rect));

    mPourStatus = (PourStatusFragment) getFragmentManager().findFragmentById(R.id.tap_status);
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    registerReceiver(mUpdateReceiver, POUR_INTENT_FILTER);
    handleIntent(getIntent());
  }

  @Override
  protected void onPause() {
    unregisterReceiver(mUpdateReceiver);
    super.onPause();
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntent(intent);
  }

  private void handleIntent(final Intent intent) {
    final String action = intent.getAction();
    Log.d(LOG_TAG, "Handling intent: " + intent);
    if (KegtapBroadcast.ACTION_POUR_UPDATE.equals(action)) {
      final long flowId = intent.getLongExtra(KegtapBroadcast.POUR_UPDATE_EXTRA_FLOW_ID, -1);
      if (flowId > 0) {
        Log.d(LOG_TAG, "Flow id: " + flowId);
        updateForFlow(flowId);
      }
    }
  }

  private void updateForFlow(long flowId) {
    final FlowManager flowManager = FlowManager.getSingletonInstance();
    final Flow flow = flowManager.getFlowForFlowId(flowId);
    doFlowUpdate(flow);
    final Message updateMessage = mHandler.obtainMessage(MESSAGE_FLOW_UPDATE, flow);
    mHandler.removeMessages(MESSAGE_FLOW_UPDATE);
    mHandler.sendMessageDelayed(updateMessage, FLOW_UPDATE_MILLIS);
  }

  private void doFlowUpdate(Flow flow) {
    Log.d(LOG_TAG, "Updating from flow: " + flow);
    mPourStatus.updateForFlow(flow);
  }

  public static Intent getStartIntent(Context context, long flowId) {
    final Intent intent = new Intent(context, PourInProgressActivity.class);
    intent.setAction(KegtapBroadcast.ACTION_POUR_UPDATE);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.putExtra(KegtapBroadcast.POUR_UPDATE_EXTRA_FLOW_ID, flowId);
    return intent;
  }

}
