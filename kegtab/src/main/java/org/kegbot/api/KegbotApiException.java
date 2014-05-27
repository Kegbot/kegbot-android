/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
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
package org.kegbot.api;

import org.codehaus.jackson.JsonNode;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;

/**
 * Base exception for {@link Backend} methods.
 */
public class KegbotApiException extends BackendException {

  private final JsonNode mErrors;

  public KegbotApiException() {
    mErrors = null;
  }

  public KegbotApiException(JsonNode errors) {
    mErrors = errors;
  }

  /**
   * @param detailMessage
   */
  public KegbotApiException(String detailMessage) {
    super(detailMessage);
    mErrors = null;
  }

  /**
   * @param throwable
   */
  public KegbotApiException(Throwable throwable) {
    super(throwable);
    mErrors = null;
  }

  /**
   * @param detailMessage
   * @param throwable
   */
  public KegbotApiException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
    mErrors = null;
  }

  public JsonNode getErrors() {
    return mErrors;
  }

}
