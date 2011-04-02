/**
 * 
 */
package org.kegbot.kegtap.service;

import android.app.IntentService;
import android.content.Intent;

/**
 * 
 * @author mike
 */
public abstract class BackgroundService extends IntentService {

  /**
   * @param name
   */
  public BackgroundService(String name) {
    super(name);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    runInBackground();
  }

  protected abstract void runInBackground();

}
