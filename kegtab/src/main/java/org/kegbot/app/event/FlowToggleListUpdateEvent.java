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
package org.kegbot.app.event;

import org.kegbot.proto.Models;

import java.util.List;

/**
 * Event posted when the list of flow toggles has been updated.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class FlowToggleListUpdateEvent implements Event {

  private final List<Models.FlowToggle> mToggles;

  public FlowToggleListUpdateEvent(List<Models.FlowToggle> toggles) {
    mToggles = toggles;
  }

  public List<Models.FlowToggle> getEvents() {
    return mToggles;
  }

}
