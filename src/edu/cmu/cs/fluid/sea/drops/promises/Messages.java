/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/sea/drops/promises/Messages.java,v 1.1 2007/03/09 20:26:57 tanmay Exp $*/
package edu.cmu.cs.fluid.sea.drops.promises;

import edu.cmu.cs.fluid.util.AbstractMessages;

public class Messages extends AbstractMessages {
  private static final String BUNDLE_NAME = "edu.cmu.cs.fluid.sea.drops.promises.messages"; //$NON-NLS-1$

  public static String MethodControlFlow_otherControlDrop = "Other control flow for {0}";

  private Messages() {
  }

  static {
    // initialize resource bundle
    load(BUNDLE_NAME, Messages.class);
  }
}
