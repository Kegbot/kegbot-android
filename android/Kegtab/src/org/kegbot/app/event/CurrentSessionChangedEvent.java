/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For more
 * information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app.event;

import javax.annotation.Nullable;

import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.Stats;

/**
 * Event posted when a new session is in progress. The session may be
 * {@code null}.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class CurrentSessionChangedEvent implements Event {

  @Nullable
  private final Session mSession;

  @Nullable
  private final Stats mSessionStats;

  public CurrentSessionChangedEvent(@Nullable Session session, @Nullable Stats sessionStats) {
    mSession = session;
    mSessionStats = sessionStats;
  }

  @Nullable
  public Session getSession() {
    return mSession;
  }

  @Nullable
  public Stats getSessionStats() {
    return mSessionStats;
  }

}
