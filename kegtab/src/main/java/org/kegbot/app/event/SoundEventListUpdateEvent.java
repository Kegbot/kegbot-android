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
package org.kegbot.app.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.kegbot.proto.Models.SoundEvent;

import java.util.List;

/**
 * Event posted when the {@link SoundEvent} list has been updated.
 */
public class SoundEventListUpdateEvent implements Event {

  private final List<SoundEvent> mEvents = Lists.newArrayList();

  public SoundEventListUpdateEvent(List<SoundEvent> events) {
    mEvents.addAll(events);
  }

  public List<SoundEvent> getEvents() {
    return ImmutableList.copyOf(mEvents);
  }

}
