package org.kegbot.backend;

import android.test.AndroidTestCase;

import org.kegbot.app.util.KegSizes;
import org.kegbot.backend.LocalBackend;
import org.kegbot.backend.LocalBackendDbHelper;
import org.kegbot.proto.Models.Beverage;
import org.kegbot.proto.Models.BeverageProducer;
import org.kegbot.proto.Models.Controller;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.FlowMeter;
import org.kegbot.proto.Models.Keg;
import org.kegbot.proto.Models.KegTap;

import java.util.Collections;
import java.util.List;

public class LocalBackendTest extends AndroidTestCase {

  LocalBackend mBackend;
  
  @Override
  protected void setUp() throws Exception {
    mBackend = new LocalBackend();
    mBackend.start(getContext());
  }

  @Override
  protected void tearDown() throws Exception {
    getContext().deleteDatabase(LocalBackendDbHelper.DATABASE_NAME);
  }

  /** Tests default values in fresh database. */
  public void testDefaults() throws BackendException {
    List<KegTap> taps = mBackend.getTaps();
    assertFalse(taps.isEmpty());
    assertEquals(1, taps.size());
    
    KegTap tap = taps.get(0);
    assertEquals("Main Tap", tap.getName());
    assertTrue(tap.hasMeter());
    assertFalse(tap.hasCurrentKeg());
    assertFalse(tap.hasCurrentKegId());
    assertFalse(tap.hasToggle());
    assertFalse(tap.hasThermoSensorId());
    
    FlowMeter meter = tap.getMeter();
    assertEquals("kegboard.flow0", meter.getName());
    assertEquals("flow0", meter.getPortName());
    assertEquals(2.2, meter.getTicksPerMl(), 0.001);
    assertTrue(meter.hasController());
    
    Controller controller = meter.getController();
    assertEquals("kegboard", controller.getName());
    assertEquals("", controller.getSerialNumber());
    assertEquals("", controller.getModelName());
    
    // Check getFlowMeters() and getControllers()
    assertEquals(Collections.singletonList(meter), mBackend.getFlowMeters());
    assertEquals(Collections.singletonList(controller), mBackend.getControllers());
    
    // Other endpoints.
    assertTrue(mBackend.getEvents().isEmpty());
    assertTrue(mBackend.getEventsSince(0).isEmpty());
    assertTrue(mBackend.getUsers().isEmpty());
    assertNull(mBackend.getCurrentSession());
    assertNull(mBackend.getSessionStats(0));
  }
  
  /** Starts several kegs, one for each {@link KegSizes KegSize}. */
  public void testStartKeg() throws BackendException {
    int expectedKegId = 1;
    for (final String kegSize : KegSizes.allLabelsAscendingVolume()) {
      KegTap tap = mBackend.startKeg("kegboard.flow0", "Test Beer", "Test Brewer", "Test Style",
          kegSize);
      assertNotNull(tap);
      
      assertEquals("kegboard.flow0", tap.getMeter().getName());
      assertTrue(tap.hasCurrentKegId());
      assertEquals(expectedKegId, tap.getCurrentKegId());
      assertTrue(tap.hasCurrentKeg());
      
      Keg keg = tap.getCurrentKeg();
      assertEquals(expectedKegId, keg.getId());
      assertTrue(keg.getOnline());
      assertEquals(kegSize, keg.getKegType());
      
      Beverage beverage = keg.getBeverage();
      assertEquals(0, beverage.getId());
      assertEquals("Test Beer", beverage.getName());
      assertEquals("Test Style", beverage.getStyle());
      
      BeverageProducer producer = beverage.getProducer();
      assertEquals(0, producer.getId());
      assertEquals("Test Brewer", producer.getName());
      
      final double fullVolume = KegSizes.getVolumeMl(kegSize);
      assertEquals(fullVolume, keg.getFullVolumeMl());
      assertEquals(fullVolume, keg.getRemainingVolumeMl());
      assertEquals(0.0, keg.getServedVolumeMl());
      assertEquals(0.0, keg.getSpilledVolumeMl());
      
      // End the keg.
      keg = mBackend.endKeg(keg);
      assertFalse(keg.getOnline());
      
      KegTap endedTap = mBackend.getTaps().get(0);
      assertEquals(tap.getId(), endedTap.getId());
      assertFalse(endedTap.hasCurrentKegId());
      assertFalse(endedTap.hasCurrentKeg());
      
      expectedKegId++;
    }
  }
  
  public void testPourDrink() throws BackendException {
    KegTap tap = mBackend.startKeg("kegboard.flow0", "Test Beer", "Test Brewer", "Test Style",
        KegSizes.HALF_BARREL);
    assertNotNull(tap);
    
    final double fullVolume = KegSizes.getVolumeMl(KegSizes.HALF_BARREL);
    Keg keg = mBackend.getTaps().get(0).getCurrentKeg();
    assertNotNull(keg);
    assertEquals(fullVolume, keg.getRemainingVolumeMl());
    
    Drink drink = mBackend.recordDrink("kegboard.flow0", 1000, 123, "", "", "", 0,
        null, null);
    assertNotNull(drink);
    assertEquals(1, drink.getId());
    assertEquals(1000.0, drink.getVolumeMl(), 0.001);
    assertEquals(123, drink.getTicks());
    assertFalse(drink.hasUser());
    
    keg = mBackend.getTaps().get(0).getCurrentKeg();
    assertEquals(1000.0, keg.getServedVolumeMl());
    assertEquals(fullVolume - 1000.0, keg.getRemainingVolumeMl());
    
    // Recording with a username is ignored.
    drink = mBackend.recordDrink("kegboard.flow0", 500, 123, "", "user", "", 0,
        null, null);
    assertNotNull(drink);
    assertEquals(2, drink.getId());
    assertEquals(500.0, drink.getVolumeMl(), 0.001);
    assertEquals(123, drink.getTicks());
    assertFalse(drink.hasUser());
    
    keg = mBackend.getTaps().get(0).getCurrentKeg();
    assertEquals(1500.0, keg.getServedVolumeMl());
    assertEquals(fullVolume - 1500.0, keg.getRemainingVolumeMl());
    
    // Recording against a bogus tap is ignored.
    try {
      drink = mBackend.recordDrink("kegboard.bogus", 1000, 123, "", "", "", 0,
          null, null);
      fail("Expected error.");
    } catch (NotFoundException e) {
      // Expected.
    }
  }
}
