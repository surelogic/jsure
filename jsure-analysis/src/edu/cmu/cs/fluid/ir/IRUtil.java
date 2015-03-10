/*
 * Created on Sep 13, 2004
 *
 */
package edu.cmu.cs.fluid.ir;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;


/**
 * @author Edwin
 *
 */
public class IRUtil {
  /**
   * Logger for this class
   */
  private static final Logger LOG = SLLogger.getLogger("FLUID.ir.util");
  
  public static IREnumeratedType makeIterator(String name, String[] values) {
    try {
      return new IREnumeratedType(name, values);
    } catch (EnumerationAlreadyDefinedException e) {
      LOG.log(Level.SEVERE, "Got exception while making enumerated type "+name, e);
      return null;
    }
  }
}
