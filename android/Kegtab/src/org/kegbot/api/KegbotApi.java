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

  /**
   * @return
   * @throws KegbotApiException
   */
  public List<SoundEvent> getAllSoundEvents() throws KegbotApiException;

  /**
   * @return
   * @throws KegbotApiException
   */
  public List<KegTap> getAllTaps() throws KegbotApiException;

  /**
   * @param authDevice
   * @param tokenValue
   * @return
   * @throws KegbotApiException
   */
  public AuthenticationToken getAuthToken(String authDevice, String tokenValue)
      throws KegbotApiException;

  public List<KegSize> getKegSizes() throws KegbotApiException;

  /**
   * Ends the given keg.
   *
   * @param id
   * @return
   * @throws KegbotApiException
   */
  public Keg endKeg(String id) throws KegbotApiException;

  public KegTap activateKeg(String tapName, String beerName, String brewerName, String styleName,
      int kegSizeId) throws KegbotApiException;

  /**
   * Returns recent system events.
   *
   * @return the events
   * @throws KegbotApiException
   */
  public List<SystemEvent> getRecentEvents() throws KegbotApiException;

  public List<SystemEvent> getRecentEvents(final long sinceEventId) throws KegbotApiException;

  public JsonNode getSessionStats(int sessionId) throws KegbotApiException;

  public KegTap setTapMlPerTick(String tapName, double mlPerTick) throws KegbotApiException;

  /**
   * Returns details for a single user.
   *
   * @param username
   * @return the {@link User}
   * @throws KegbotApiException
   */
  public User getUserDetail(String username) throws KegbotApiException;

  public Drink recordDrink(final RecordDrinkRequest request) throws KegbotApiException;

  public ThermoLog recordTemperature(final RecordTemperatureRequest request)
      throws KegbotApiException;

  public List<User> getUsers() throws KegbotApiException;

  public AuthenticationToken assignToken(String authDevice, String tokenValue, String username) throws KegbotApiException;

  /**
   * Returns the currently-active drinking session, or {@code null} if none is
   * active.
   *
   * @return
   * @throws KegbotApiException
   */
  public Session getCurrentSession() throws KegbotApiException;

  public Image uploadDrinkImage(final int drinkId, final String imagePath)
      throws KegbotApiException;

  public User register(final String username, final String email, final String password,
      final String imagePath) throws KegbotApiException;

}
