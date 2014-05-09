/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app.setup;

import android.app.Fragment;

import org.apache.http.conn.HttpHostConnectException;
import org.kegbot.api.KegbotApiServerError;
import org.kegbot.api.NotAuthorizedException;

import java.net.SocketException;

/**
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
    } else if (e instanceof NotAuthorizedException) {
      builder.append("Username or password was incorrect.");
    } else if (e instanceof KegbotApiServerError) {
      builder.append("Bad response from server (");
      builder.append(e.toString());
      builder.append(")");
    } else if (e instanceof SocketException || e.getCause() instanceof SocketException) {
      builder.append("Server is down or unreachable.");
    } else {
      builder.append(e.getMessage());
    }
    return builder.toString();
  }

}
