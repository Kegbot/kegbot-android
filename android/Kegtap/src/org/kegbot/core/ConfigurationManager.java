/**
 *
 */
package org.kegbot.core;

import java.util.Map;

import org.kegbot.proto.Api.TapDetail;

import com.google.common.collect.Maps;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class ConfigurationManager {


  private static ConfigurationManager sSingleton = null;

  /**
   * Maps tap names to TapDetail.
   */
  private final Map<String, TapDetail> mTapConfig = Maps.newLinkedHashMap();

  public TapDetail getTapDetail(final String tapName) {
    return mTapConfig.get(tapName);
  }

  public void setTapDetail(final String tapName, final TapDetail tapDetail) {
    mTapConfig.put(tapName, tapDetail);
  }

  public static ConfigurationManager getSingletonInstance() {
    if (sSingleton == null) {
      sSingleton = new ConfigurationManager();
    }
    return sSingleton;
  }

}
