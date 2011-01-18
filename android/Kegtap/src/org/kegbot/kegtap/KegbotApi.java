package org.kegbot.kegtap;

import retrofit.core.Callback;
import retrofit.http.Path;

public interface KegbotApi {
  @Path("taps") public void taps(Callback<?> callback);
}
