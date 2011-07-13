/**
 *
 */
package org.kegbot.core;

import java.util.Collection;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class TapManager {

  private static TapManager sSingleton = null;

  private final Collection<Tap> mTaps = Sets.newLinkedHashSet();

  @VisibleForTesting
  protected TapManager() {
  }

  public boolean addTap(final Tap newTap) {
    return mTaps.add(newTap);
  }

  public boolean removeTap(final Tap tap) {
    return mTaps.remove(tap);
  }

  public Tap getTapForMeterName(final String meterName) {
    for (final Tap tap : mTaps) {
      if (meterName.equals(tap.getMeterName())) {
        return tap;
      }
    }
    return null;
  }

  public Collection<Tap> getTaps() {
    return ImmutableSet.copyOf(mTaps);
  }

  public static synchronized TapManager getSingletonInstance() {
    if (sSingleton == null) {
      sSingleton = new TapManager();
    }
    return sSingleton;
  }
}
