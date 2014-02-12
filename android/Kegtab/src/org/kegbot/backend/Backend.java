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

package org.kegbot.backend;

import org.codehaus.jackson.JsonNode;
import org.kegbot.app.util.TimeSeries;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegSize;
import org.kegbot.proto.Models.KegTap;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.SoundEvent;
import org.kegbot.proto.Models.SystemEvent;
import org.kegbot.proto.Models.ThermoLog;
import org.kegbot.proto.Models.User;

import java.io.File;
import java.util.List;

import javax.annotation.Nullable;

/**
 * High-level Kegbot backend interface.
 */
public interface Backend {

  /** Activates a new keg on the specified tap. */
  public KegTap startKeg(String tapName, String beerName, String brewerName, String styleName,
      int kegSizeId) throws BackendException;

  /** Assigns an authentication token to a user. */
  public AuthenticationToken assignToken(String authDevice, String tokenValue, String username)
      throws BackendException;

  /** Attaches a picture to a drink record. */
  public Image attachPictureToDrink(int drinkId, File picture) throws BackendException;

  /** Creates a new user. */
  public User createUser(String username, String email, String password, String imagePath)
      throws BackendException;

  /** Ends the given keg. */
  public Keg endKeg(int kegId) throws BackendException;

  /**
   * Returns the authentication token record for the given token.
   *
   * @return the token record
   * @throws NotFoundException if there is no record for this token.
   */
  public AuthenticationToken getAuthToken(String authDevice, String tokenValue)
      throws BackendException;

  /**
   * Returns the currently-active drinking session.
   *
   * @return the active session
   * @throws NotFoundException if there is no active session.
   */
  public Session getCurrentSession() throws BackendException;

  /** Returns the most recent system events. The list may be empty. */
  public List<SystemEvent> getEvents() throws BackendException;

  /** Returns the most recent since the given event. The list may be empty. */
  public List<SystemEvent> getEventsSince(long sinceEventId) throws BackendException;

  /** Returns defined keg sizes. The list may be empty. */
  public List<KegSize> getKegSizes() throws BackendException;

  /** Returns statistics for the given session. */
  public JsonNode getSessionStats(int sessionId) throws BackendException;

  /** Returns any defined sound events. The list may be empty. */
  public List<SoundEvent> getSoundEvents() throws BackendException;

  /** Returns all defined taps. The list may be empty. */
  public List<KegTap> getTaps() throws BackendException;

  /**
   * Retrieves information about a single user.
   *
   * @param username the username to query
   * @return the {@link User}
   * @throws NotFoundException if the user does not exist
   */
  public User getUser(String username) throws BackendException;

  /** Retrieves a full user list for this system. The list may be empty. */
  public List<User> getUsers() throws BackendException;

  /**
   * Saves a new drink record from given pour data.
   *
   * <p>
   * Either or both of {@code volumeMl} and {@code ticks} should be specified.
   * </p>
   *
   * @param tapName the tap used for this pour (required).
   * @param volumeMl
   * @param ticks
   * @param shout
   * @param username
   * @param recordDate
   * @param durationMillis
   * @param timeSeries
   * @param picture
   * @return
   * @throws BackendException
   */
  public Drink recordDrink(String tapName, long volumeMl, long ticks, @Nullable String shout,
      @Nullable String username, @Nullable String recordDate, long durationMillis,
      @Nullable TimeSeries timeSeries, @Nullable File picture) throws BackendException;

  /** Saves a new temperature sensor record. */
  public ThermoLog recordTemperature(RecordTemperatureRequest request)
      throws BackendException;

  /** Sets the meter calibration factor. */
  public KegTap setTapMlPerTick(String tapName, double mlPerTick)
      throws BackendException;

}
