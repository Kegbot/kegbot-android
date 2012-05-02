/**
 *
 */
package org.kegbot.kegtap.setup;

import org.apache.http.conn.HttpHostConnectException;
import org.kegbot.api.KegbotApiServerError;

import android.app.Fragment;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public abstract class SetupFragment extends Fragment {

  protected abstract String validate();

  protected void onValidationFailed() {

  }

  protected static String toHumanError(Exception e) {
    StringBuilder builder = new StringBuilder();
    if (e.getCause() instanceof HttpHostConnectException) {
      builder.append("Could not connect to remote host.");
    } else if (e instanceof KegbotApiServerError) {
      builder.append("Bad response from server (");
      builder.append(e.toString());
      builder.append(")");
    } else {
      builder.append(e.getMessage());
    }
    return builder.toString();
  }

}
