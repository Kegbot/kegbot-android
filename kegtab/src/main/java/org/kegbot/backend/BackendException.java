/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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

package org.kegbot.backend;

/**
 * Top-level exception class for backend errors.
 */
public class BackendException extends Exception {

  public BackendException() {
  }

  public BackendException(String detailMessage) {
    super(detailMessage);
  }

  public BackendException(Throwable throwable) {
    super(throwable);
  }

  public BackendException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

}
