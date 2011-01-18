package org.kegbot.kegtap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashScreenActivity extends Activity {
  
  private boolean active = true;
  private int splashTime = 1000;

  public void onCreate(Bundle savedInstanceState) {
    Thread splashThread = new Thread() {
      @Override
      public void run() {
        try {
          int waited = 0;
          while (active && (waited < splashTime)) {
            sleep(100);
            if (active) {
              waited += 100;
            }
          }
        } catch(InterruptedException e) {
          // do nothing
        } finally {
          finish();
          startActivity(new Intent("org.kegbot.kegtap.KegtapActivity"));
          stop();
        }
      }
    };
    splashThread.start();
  }
}
