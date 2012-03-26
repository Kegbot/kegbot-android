package org.kegbot.api;

import org.kegbot.proto.Api.DrinkDetail;
import org.kegbot.proto.Api.DrinkDetailHtmlSet;
import org.kegbot.proto.Api.DrinkSet;
import org.kegbot.proto.Api.KegDetail;
import org.kegbot.proto.Api.KegSet;
import org.kegbot.proto.Api.RecordDrinkRequest;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Api.SessionDetail;
import org.kegbot.proto.Api.SessionSet;
import org.kegbot.proto.Api.SoundEventSet;
import org.kegbot.proto.Api.SystemEventDetailSet;
import org.kegbot.proto.Api.SystemEventHtmlSet;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Api.TapDetailSet;
import org.kegbot.proto.Api.ThermoLogSet;
import org.kegbot.proto.Api.ThermoSensorSet;
import org.kegbot.proto.Api.UserDetailSet;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.ThermoLog;
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
  public KegSet getAllKegs() throws KegbotApiException;

  /**
   * @return
   * @throws KegbotApiException
   */
  public SoundEventSet getAllSoundEvents() throws KegbotApiException;

  /**
   * @return
   * @throws KegbotApiException
   */
  public TapDetailSet getAllTaps() throws KegbotApiException;

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
  public DrinkSet getKegDrinks(String kegId) throws KegbotApiException;

  /**
   * @param kegId
   * @return
   * @throws KegbotApiException
   */
  public SystemEventDetailSet getKegEvents(String kegId) throws KegbotApiException;

  /**
   * @param kegId
   * @return
   * @throws KegbotApiException
   */
  public SessionSet getKegSessions(String kegId) throws KegbotApiException;

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
  public DrinkSet getRecentDrinks() throws KegbotApiException;

  /**
   * @return
   * @throws KegbotApiException
   */
  public DrinkDetailHtmlSet getRecentDrinksHtml() throws KegbotApiException;

  /**
   * Returns recent system events.
   *
   * @return the events
   * @throws KegbotApiException
   */
  public SystemEventDetailSet getRecentEvents() throws KegbotApiException;
  public SystemEventDetailSet getRecentEvents(final long sinceEventId) throws KegbotApiException;

  /**
   * @return
   * @throws KegbotApiException
   */
  public SystemEventHtmlSet getRecentEventsHtml() throws KegbotApiException;

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
  public ThermoLogSet getThermoSensorLogs(String sensorId) throws KegbotApiException;

  /**
   * @return
   * @throws KegbotApiException
   */
  public ThermoSensorSet getThermoSensors() throws KegbotApiException;

  /**
   * Returns details for a single user.
   *
   * @param username
   * @return the {@link User}
   * @throws KegbotApiException
   */
  public User getUser(String username) throws KegbotApiException;

  public DrinkSet getUserDrinks(String username) throws KegbotApiException;

  public SystemEventDetailSet getUserEvents(String username) throws KegbotApiException;

  public Drink recordDrink(final RecordDrinkRequest request) throws KegbotApiException;

  public ThermoLog recordTemperature(final RecordTemperatureRequest request)
      throws KegbotApiException;

  public UserDetailSet getUsers() throws KegbotApiException;

  public SessionSet getCurrentSessions() throws KegbotApiException;

  public Image uploadDrinkImage(final String drinkId, final String imagePath) throws KegbotApiException;

}
