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

package org.kegbot.api;

import org.codehaus.jackson.JsonNode;
import org.kegbot.proto.Api.RecordDrinkRequest;
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

import java.util.List;

/**
 * High-level Kegbot API interface.
 */
public interface KegbotApi {

  /** Activates a new keg on the specified tap. */
  public KegTap startKeg(String tapName, String beerName, String brewerName, String styleName,
      int kegSizeId) throws KegbotApiException;

  /** Assigns an authentication token to a user. */
  public AuthenticationToken assignToken(String authDevice, String tokenValue, String username)
      throws KegbotApiException;

  /** Attaches a picture to a drink record. */
  public Image attachPictureToDrink(int drinkId, String imagePath) throws KegbotApiException;

  /** Creates a new user. */
  public User createUser(String username, String email, String password, String imagePath)
      throws KegbotApiException;

  /** Ends the given keg. */
  public Keg endKeg(int kegId) throws KegbotApiException;

  /**
   * Returns the authentication token record for the given token.
   *
   * @return the token record
   * @throws KegbotApiNotFoundError if there is no record for this token.
   */
  public AuthenticationToken getAuthToken(String authDevice, String tokenValue)
      throws KegbotApiException;

  /**
   * Returns the currently-active drinking session.
   *
   * @return the active session
   * @throws KegbotApiNotFoundError if there is no active session.
   */
  public Session getCurrentSession() throws KegbotApiException;

  /** Returns the most recent system events. The list may be empty. */
  public List<SystemEvent> getEvents() throws KegbotApiException;

  /** Returns the most recent since the given event. The list may be empty. */
  public List<SystemEvent> getEventsSince(long sinceEventId) throws KegbotApiException;

  /** Returns defined keg sizes. The list may be empty. */
  public List<KegSize> getKegSizes() throws KegbotApiException;

  /** Returns statistics for the given session. */
  public JsonNode getSessionStats(int sessionId) throws KegbotApiException;

  /** Returns any defined sound events. The list may be empty. */
  public List<SoundEvent> getSoundEvents() throws KegbotApiException;

  /** Returns all defined taps. The list may be empty. */
  public List<KegTap> getTaps() throws KegbotApiException;

  /**
   * Retrieves information about a single user.
   *
   * @param username the username to query
   * @return the {@link User}
   * @throws KegbotApiNotFoundError if the user does not exist
   */
  public User getUser(String username) throws KegbotApiException;

  /** Retrieves a full user list for this system. The list may be empty. */
  public List<User> getUsers() throws KegbotApiException;

  /** Saves a new drink record. */
  public Drink recordDrink(RecordDrinkRequest request) throws KegbotApiException;

  /** Saves a new temperature sensor record. */
  public ThermoLog recordTemperature(RecordTemperatureRequest request)
      throws KegbotApiException;

  /** Sets the meter calibration factor. */
  public KegTap setTapMlPerTick(String tapName, double mlPerTick)
      throws KegbotApiException;

}
