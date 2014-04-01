/*
 * Copyright 2013 Mike Wakerly <opensource@hoho.com>.
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
package org.kegbot.app.event;

import org.kegbot.app.alert.AlertCore;
import org.kegbot.app.alert.AlertCore.Alert;

/**
 * Event posted to add a new alert. This should only be consumed
 * by {@link AlertCore}.
 */
public class AlertEvent implements Event {

  private final Alert mAlert;

  public AlertEvent(Alert alert) {
    mAlert = alert;
  }

  public Alert getAlert() {
    return mAlert;
  }

}
