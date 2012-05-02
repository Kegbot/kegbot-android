/**
 *
 */
package org.kegbot.kegtap.service;

import org.kegbot.core.FlowManager;
import org.kegbot.core.TapManager;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public interface KegbotCoreServiceInterface {

  public FlowManager getFlowManager();

  public TapManager getTapManager();

}
