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
package org.kegbot.api;

import java.util.List;

import org.kegbot.proto.Api.RecordDrinkRequest;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.SoundEvent;
import org.kegbot.proto.Models.Stats;
import org.kegbot.proto.Models.SystemEvent;
import org.kegbot.proto.Models.ThermoLog;
import org.kegbot.proto.Models.ThermoSensor;
import org.kegbot.proto.Models.User;

/**
 * High-level Kegbot API interface.
 */
public interface KegbotApi {

  public void setApiUrl(String apiUrl);

  public void setApiKey(String apiKey);

  public void login(String username, String password) throws KegbotApiException;

  public String getApiKey() throws KegbotApiException;

  /**
   * Returns all kegs known to the system.
   *
   * @return all kegs
   * @throws KegbotApiException
   */
  public List<Keg> getAllKegs() throws KegbotApiException;

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

  /**
   * Returns details for a specific drink.
   *
   * @param id
   *          the drink id
   * @return the {@link Drink}
   * @throws KegbotApiException
   */
  public Drink getDrinkDetail(String id) throws KegbotApiException;

  /**
   * @param id
   * @return
   * @throws KegbotApiException
   */
  public Keg getKegDetail(String id) throws KegbotApiException;

  /**
   * @param kegId
   * @return
   * @throws KegbotApiException
   */
  public List<Drink> getKegDrinks(String kegId) throws KegbotApiException;

  /**
   * @param kegId
   * @return
   * @throws KegbotApiException
   */
  public List<SystemEvent> getKegEvents(String kegId) throws KegbotApiException;

  /**
   * @param kegId
   * @return
   * @throws KegbotApiException
   */
  public List<Session> getKegSessions(String kegId) throws KegbotApiException;

  /**
   * @return
   * @throws KegbotApiException
   */
  public String getLastDrinkId() throws KegbotApiException;

  /**
   * Returns the most recent drinks.
   *
   * @return the drinks
   * @throws KegbotApiException
   */
  public List<Drink> getRecentDrinks() throws KegbotApiException;

  /**
   * Returns recent system events.
   *
   * @return the events
   * @throws KegbotApiException
   */
  public List<SystemEvent> getRecentEvents() throws KegbotApiException;
  public List<SystemEvent> getRecentEvents(final long sinceEventId) throws KegbotApiException;

  /**
   * Returns detailed information about a session.
   *
   * @param id
   * @return the {@link SessionDetail}
   * @throws KegbotApiException
   */
  public Session getSessionDetail(String id) throws KegbotApiException;

  public Stats getSessionStats(int sessionId) throws KegbotApiException;


  /**
   * Returns information about a tap.
   *
   * @param tapName
   * @return the {@link TapDetail}
   * @throws KegbotApiException
   */
  public KegTap getTapDetail(String tapName) throws KegbotApiException;

  /**
   * @param sensorId
   * @return
   * @throws KegbotApiException
   */
  public List<ThermoLog> getThermoSensorLogs(String sensorId) throws KegbotApiException;

  /**
   * @return
   * @throws KegbotApiException
   */
  public List<ThermoSensor> getThermoSensors() throws KegbotApiException;

  /**
   * Returns details for a single user.
   *
   * @param username
   * @return the {@link User}
   * @throws KegbotApiException
   */
  public User getUserDetail(String username) throws KegbotApiException;

  public List<Drink> getUserDrinks(String username) throws KegbotApiException;

  public List<SystemEvent> getUserEvents(String username) throws KegbotApiException;

  public Drink recordDrink(final RecordDrinkRequest request) throws KegbotApiException;

  public ThermoLog recordTemperature(final RecordTemperatureRequest request)
      throws KegbotApiException;

  public List<User> getUsers() throws KegbotApiException;

  /**
   * Returns the currently-active drinking session, or {@code null} if none is
   * active.
   *
   * @return
   * @throws KegbotApiException
   */
  public Session getCurrentSession() throws KegbotApiException;

  public Image uploadDrinkImage(final int drinkId, final String imagePath) throws KegbotApiException;

  public User register(final String username, final String email, final String password, final String imagePath) throws KegbotApiException;

}
