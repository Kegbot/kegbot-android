package org.kegbot.kegtap;

import org.kegbot.core.Flow;
import org.kegbot.core.FlowManager;
import org.kegbot.kegtap.service.KegbotCoreServiceInterface;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class PourInProgressActivity extends CoreActivity {

  public final String LOG_TAG = PourInProgressActivity.class.getSimpleName();

  private static final String ACTION_POUR_UPDATE = "org.kegbot.action.POUR_UPDATE";

  private static final String EXTRA_FLOW_ID = "flow";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.pour_in_progress);
    bindToCoreService();
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntent();
  }

  private void handleIntent() {
    final Intent intent = getIntent();
    final String action = intent.getAction();
    Log.d(LOG_TAG, "Handling intent: " + intent);
    if (ACTION_POUR_UPDATE.equals(action)) {
      final long flowId = intent.getLongExtra(EXTRA_FLOW_ID, -1);
      if (flowId > 0) {
        Log.d(LOG_TAG, "Flow id: " + flowId);
        updateForFlow(flowId);
      }
    }
  }

  private void updateForFlow(long flowId) {
    final KegbotCoreServiceInterface coreService = getCoreService();
    final FlowManager flowManager = coreService.getFlowManager();
    final Flow flow = flowManager.getFlowForFlowId(flowId);
    Log.d(LOG_TAG, "Updating fow flow: " + flow);
  }

  public static Intent getStartIntent(Context context, long flowId) {
    final Intent intent = new Intent(context, PourInProgressActivity.class);
    intent.setAction(ACTION_POUR_UPDATE);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.putExtra(EXTRA_FLOW_ID, flowId);
    return intent;
  }

  @Override
  protected void onCoreServiceBound() {
    super.onCoreServiceBound();
    handleIntent();
  }


}
