package org.kegbot.kegtap;

import android.util.Log;
import retrofit.core.Callback;
import retrofit.http.RestAdapter;


public class KegbotApiImpl extends RestAdapter implements KegbotApi{
  
  private final String LOG_TAG = "KegbotApiImpl";

  public void taps(Callback<?> callback) {
    Log.w(LOG_TAG, "Called taps!");
  }

}
