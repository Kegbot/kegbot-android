/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
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

package org.kegbot.backend;

import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.provider.MediaStore;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import org.codehaus.jackson.JsonNode;
import org.kegbot.app.util.KegSizes;
import org.kegbot.app.util.TimeSeries;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Models;
import org.kegbot.proto.Models.AuthenticationToken;
import org.kegbot.proto.Models.Beverage;
import org.kegbot.proto.Models.BeverageProducer;
import org.kegbot.proto.Models.Controller;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.FlowMeter;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;
import org.kegbot.proto.Models.Session;
import org.kegbot.proto.Models.SoundEvent;
import org.kegbot.proto.Models.SystemEvent;
import org.kegbot.proto.Models.ThermoLog;
import org.kegbot.proto.Models.User;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/** A {@link Backend} which uses the local filesystem for storage. */
public class LocalBackend implements Backend {

  private static final String TAG = LocalBackend.class.getSimpleName();

  private LocalBackendDbHelper mDb;
  private ContentResolver mContentResolver;

  @Override
  public void start(Context context) {
    mDb = new LocalBackendDbHelper(context);
    mContentResolver = context.getContentResolver();
  }

  @Override
  public KegTap startKeg(KegTap tap, String beerName, String brewerName, String styleName,
      String kegType) throws BackendException {
    if (tap.hasCurrentKeg()) {
      endKeg(tap.getCurrentKeg());
    }

    final double volume = KegSizes.getVolumeMl(kegType);
    final Keg keg = mDb.createOrUpdateKeg(Keg.newBuilder()
        .setId(0)
        .setKegType(kegType)
        .setFullVolumeMl(volume)
        .setRemainingVolumeMl(volume)
        .setServedVolumeMl(0)
        .setSpilledVolumeMl(0)
        .setPercentFull(100.0)
        .setOnline(true)
        .setBeverage(Beverage.newBuilder()
            .setId(0)
            .setBeverageType("beer")
            .setName(beerName)
            .setStyle(styleName)
            .setProducer(BeverageProducer.newBuilder()
                .setId(0)
                .setName(brewerName)
                .build())
            .build())
        .build());

    Log.d(TAG, "Created keg: " + keg);

    final KegTap updatedTap = mDb.createOrUpdateTap(KegTap.newBuilder(tap)
        .setCurrentKegId(keg.getId())
        .build());

    Log.d(TAG, "Updated tap: " + updatedTap);

    return updatedTap;
  }

  @Override
  public AuthenticationToken assignToken(String authDevice, String tokenValue, String username)
      throws BackendException {
    throw new OperationNotSupportedException("Local backend does not support users.");
  }

  @Override
  public Image attachPictureToDrink(int drinkId, File picture) throws BackendException {
    throw new OperationNotSupportedException("Local backend does not support photos.");
  }

  @Override
  public User createUser(String username, String email, String password, String imagePath)
      throws BackendException {
    throw new OperationNotSupportedException("Local backend does not support users.");
  }

  @Override
  public Keg endKeg(Keg keg) throws BackendException {
    final int kegId = keg.getId();

    Log.i(TAG, "Taking keg " + kegId + " offline");
    final Keg newKeg = Keg.newBuilder(keg)
        .setOnline(false)
        .build();

    final Keg result;
    try {
      result = mDb.createOrUpdateKeg(newKeg);
    } catch (SQLiteException e) {
      Log.w(TAG, "SQLiteException reading keg " + kegId);
      throw new BackendException(e);
    }

    for (final KegTap tap : getTaps()) {
      if (tap.getCurrentKegId() == keg.getId()) {
        mDb.createOrUpdateTap(KegTap.newBuilder(tap)
            .setCurrentKegId(0)
            .build());
        break;
      }
    }

    return result;
  }

  @Override
  public AuthenticationToken getAuthToken(String authDevice, String tokenValue)
      throws BackendException {
    throw new NotFoundException("No token.");
  }

  @Override
  public Session getCurrentSession() throws BackendException {
    return null;
  }

  @Override
  public List<SystemEvent> getEvents() throws BackendException {
    return Collections.emptyList();
  }

  @Override
  public List<SystemEvent> getEventsSince(long sinceExventId) throws BackendException {
    return Collections.emptyList();
  }

  @Override
  public JsonNode getSessionStats(int sessionId) throws BackendException {
    return null;
  }

  @Override
  public List<SoundEvent> getSoundEvents() throws BackendException {
    return Collections.emptyList();
  }

  @Override
  public List<KegTap> getTaps() throws BackendException {
    return mDb.getAllTaps();
  }

  @Override
  public KegTap createTap(String tapName) throws BackendException {
    final KegTap tap = KegTap.newBuilder()
        .setId(0)
        .setName(tapName)
        .build();
    try {
      return mDb.createOrUpdateTap(tap);
    } catch (SQLiteException e) {
      throw new BackendException("Error updating tap", e);
    }
  }

  @Override
  public void deleteTap(KegTap tap) throws BackendException {
    boolean result = mDb.deleteTap(tap);
    Log.d(TAG, "Deleted row result: " + result);
  }

  @Override
  public User getUser(String username) throws BackendException {
    throw new NotFoundException("Local backend does not support users");
  }

  @Override
  public List<User> getUsers() throws BackendException {
    return Collections.emptyList();
  }

  @Override
  public Drink recordDrink(String tapName, long volumeMl, long ticks, @Nullable String shout,
      @Nullable String username, @Nullable String recordDate, long durationMillis,
      @Nullable TimeSeries timeSeries, @Nullable File picture) throws BackendException {
    final Drink drink;

    String pictureUrl = "";
    if (picture != null) {
      final String imagePath = picture.getAbsolutePath();
      Log.d(TAG, "Storing image, path=" + imagePath + " exists=" + picture.exists());

      try {
        pictureUrl = MediaStore.Images.Media.insertImage(mContentResolver, imagePath,
            picture.getName(), "Kegbot drink snapshot");
      } catch (FileNotFoundException e) {
        Log.w(TAG, "Storing image '" + imagePath + "' failed: " + e);
      }
    }
    try {
      drink = mDb.recordDrink(tapName, volumeMl, ticks, shout, username, recordDate, durationMillis,
          timeSeries, Strings.nullToEmpty(pictureUrl));
    } catch (SQLiteException e) {
      throw new BackendException("Error recording drink", e);
    }

    return drink;
  }

  @Override
  public ThermoLog recordTemperature(RecordTemperatureRequest request) throws BackendException {
    // Ignored.
    return null;
  }

  @Override
  public FlowMeter calibrateMeter(FlowMeter meter, double ticksPerMl) throws BackendException {
    final FlowMeter newMeter = FlowMeter.newBuilder(meter)
        .setTicksPerMl(ticksPerMl)
        .build();
    try {
      return mDb.createOrUpdateFlowMeter(newMeter);
    } catch (SQLiteException e) {
      throw new BackendException("Error updating meter", e);
    }
  }

  @Override
  public Controller createController(String name, String serialNumber, String deviceType) {
    Preconditions.checkNotNull(name);
    serialNumber = Strings.nullToEmpty(serialNumber);
    deviceType = Strings.nullToEmpty(deviceType);
    final Controller controller = Controller.newBuilder()
        .setId(0)
        .setName(name)
        .setSerialNumber(serialNumber)
        .setModelName(deviceType)
        .build();
    return mDb.createOrUpdateController(controller);
  }

  @Override
  public List<Controller> getControllers() throws BackendException {
    return mDb.getAllControllers();
  }

  @Override
  public Controller updateController(Controller controller) throws BackendException {
    return mDb.createOrUpdateController(controller);
  }

  @Override
  public FlowMeter createFlowMeter(Controller controller, String portName, double ticksPerMl)
      throws BackendException {
    final FlowMeter meter = FlowMeter.newBuilder()
        .setId(0)
        .setController(controller)
        .setPortName(portName)
        .setName(String.format("%s.%s", controller.getName(), portName))
        .setTicksPerMl((float) ticksPerMl)
        .build();
    return mDb.createOrUpdateFlowMeter(meter);
  }

  @Override
  public List<FlowMeter> getFlowMeters() throws BackendException {
    return mDb.getAllFlowMeters();
  }

  @Override
  public FlowMeter updateFlowMeter(FlowMeter flowMeter) throws BackendException {
    return mDb.createOrUpdateFlowMeter(flowMeter);
  }

  @Override
  public KegTap connectMeter(KegTap tap, FlowMeter meter) {
    return mDb.connectTapToMeter(tap, meter);
  }

  @Override
  public KegTap disconnectMeter(KegTap tap) {
    return mDb.connectTapToMeter(tap, null);
  }

  @Override
  public List<Models.FlowToggle> getFlowToggles() throws BackendException {
    return mDb.getAllFlowToggles();
  }

  @Override
  public Models.FlowToggle updateFlowToggle(Models.FlowToggle flowToggle) throws BackendException {
    return mDb.createOrUpdateFlowToggle(flowToggle);
  }

  @Override
  public KegTap connectToggle(KegTap tap, Models.FlowToggle flowToggle) throws BackendException {
    return mDb.connectTapToToggle(tap, flowToggle);
  }

  @Override
  public KegTap disconnectToggle(KegTap tap) throws BackendException {
    return mDb.connectTapToToggle(tap, null);
  }
}
