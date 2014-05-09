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
package org.kegbot.kegboard;

/**
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegboardMessageException extends Exception {

  public KegboardMessageException() {
  }

  public KegboardMessageException(String detailMessage) {
    super(detailMessage);
  }

  public KegboardMessageException(Throwable throwable) {
    super(throwable);
  }

  public KegboardMessageException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

}
