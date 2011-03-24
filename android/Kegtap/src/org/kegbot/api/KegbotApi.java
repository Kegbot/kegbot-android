package org.kegbot.api;

import java.util.Collection;
import java.util.List;

import org.kegbot.proto.Api.DrinkDetail;
import org.kegbot.proto.Api.DrinkDetailHtml;
import org.kegbot.proto.Api.KegDetail;
import org.kegbot.proto.Api.SessionDetail;
import org.kegbot.proto.Api.SystemEventHtml;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.SoundEvent;
import org.kegbot.proto.Models.SystemEvent;
import org.kegbot.proto.Models.ThermoLog;
import org.kegbot.proto.Models.ThermoSensor;
import org.kegbot.proto.Models.User;

public interface KegbotApi {

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
  public List<TapDetail> getAllTaps() throws KegbotApiException;

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
   * @return the {@link DrinkDetail}
   * @throws KegbotApiException
   */
  public DrinkDetail getDrinkDetail(String id) throws KegbotApiException;

  /**
   * @param id
   * @return
   * @throws KegbotApiException
   */
  public KegDetail getKegDetail(String id) throws KegbotApiException;

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
  public Collection<Drink> getRecentDrinks() throws KegbotApiException;

  /**
   * @return
   * @throws KegbotApiException
   */
  public Collection<DrinkDetailHtml> getRecentDrinksHtml()
      throws KegbotApiException;

  /**
   * Returns recent system events.
   *
   * @return the events
   * @throws KegbotApiException
   */
  public Collection<SystemEvent> getRecentEvents() throws KegbotApiException;

  /**
   * @return
   * @throws KegbotApiException
   */
  public List<SystemEventHtml> getRecentEventsHtml() throws KegbotApiException;

  /**
   * Returns details for a single session.
   *
   * @param id
   * @return the {@link Session}
   * @throws KegbotApiException
   */
  public Session getSession(String id) throws KegbotApiException;

  /**
   * Returns detailed information about a session.
   *
   * @param id
   * @return the {@link SessionDetail}
   * @throws KegbotApiException
   */
  public SessionDetail getSessionDetail(String id) throws KegbotApiException;

  /**
   * Returns information about a tap.
   *
   * @param tapName
   * @return the {@link TapDetail}
   * @throws KegbotApiException
   */
  public TapDetail getTapDetail(String tapName) throws KegbotApiException;

  /**
   * @param sensorId
   * @return
   * @throws KegbotApiException
   */
  public Collection<ThermoLog> getThermoSensorLogs(String sensorId)
      throws KegbotApiException;

  /**
   * @return
   * @throws KegbotApiException
   */
  public Collection<ThermoSensor> getThermoSensors() throws KegbotApiException;

  /**
   * Returns details for a single user.
   *
   * @param username
   * @return the {@link User}
   * @throws KegbotApiException
   */
  public User getUser(String username) throws KegbotApiException;

}
