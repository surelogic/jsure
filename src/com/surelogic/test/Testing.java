/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/test/Testing.java,v 1.5 2008/06/24 19:13:12 thallora Exp $*/
package com.surelogic.test;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.util.QuickProperties;

public class Testing {
  public static final String TESTING_ATTR = "fluid.testing";
  
  public static final Logger LOG = SLLogger.getLogger(TESTING_ATTR);
  
  public static final QuickProperties.Flag testingFlag = 
    new QuickProperties.Flag(LOG, TESTING_ATTR, "Testing", false, true);
  
  public static final boolean testingIsOn = testingIsOn();
  
  public static boolean testingIsOn() {
    return QuickProperties.checkFlag(testingFlag);
  }
}
