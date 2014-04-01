package org.kegbot.core.hardware;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.ThermoSensor;

import java.util.Collection;
import java.util.Map;

public class FakeControllerManager implements ControllerManager {

  private static FakeControllerManager sSingleton = null;

  private final Bus mBus;
  private final ControllerManager.Listener mListener;

  public static class FakeController implements Controller {

    private final Map<String, FlowMeter> mFlowMeters = Maps.newLinkedHashMap();
    private final Map<String, ThermoSensor> mThermoSensors = Maps.newLinkedHashMap();
    private final String mName;
    private String mStatus;
    private String mSerialNumber;

    public FakeController(final String name, final String status, final String serialNumber) {
      mName = name;
      mStatus = status;
      mSerialNumber = serialNumber;
    }

    @Override
    public String getSerialNumber() {
      return Strings.nullToEmpty(mSerialNumber);
    }

    @Override
    public String getDeviceType() {
      if (getSerialNumber().startsWith("KB-01")) {
        return TYPE_KBPM;
      }
      return TYPE_UNKNOWN;
    }


    @Override
    public String getStatus() {
      return mStatus;
    }

    @Override
    public String getName() {
      return mName;
    }

    @Override
    public Collection<FlowMeter> getFlowMeters() {
      return mFlowMeters.values();
    }

    @Override
    public FlowMeter getFlowMeter(String meterName) {
      return mFlowMeters.get(meterName);
    }

    @Override
    public Collection<ThermoSensor> getThermoSensors() {
      return mThermoSensors.values();
    }

    @Override
    public ThermoSensor getThermoSensor(String sensorName) {
      return mThermoSensors.get(sensorName);
    }

  }

  public FakeControllerManager(Bus bus, Listener listener) {
    mBus = bus;
    mListener = listener;
  }

  @Override
  public void start() {
    mBus.register(this);
  }

  @Override
  public void stop() {
    mBus.unregister(this);
  }

  @Override
  public void refreshSoon() {
  }

  @Override
  public void dump(IndentingPrintWriter writer) {
  }

  @Subscribe
  public void onFakeControllerEvent(final FakeControllerEvent event) {
    if (event.isAdded()) {
      mListener.onControllerAttached(event.getController());
    } else {
      mListener.onControllerRemoved(event.getController());
    }
  }

}
