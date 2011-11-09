/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/Messages.java,v 1.11 2007/11/05 14:21:07 aarong Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.util.AbstractMessages;

public class Messages extends AbstractMessages {
  private static final String BUNDLE_NAME = "edu.cmu.cs.fluid.java.analysis.messages"; //$NON-NLS-1$

  public static String ColorSecondPass_inferredColor = "Inferred @colorConstraint {0} for {1}";

  public static String ColorSecondPass_inheritedColor = "Inherited @colorConstraint {0} for {1}";

  public static String ColorSecondPass_inheritedTransparent = "Inherited @transparent for {0}";

  public static String ColorSecondPass_colorContextDrop = "{0} is accessed from color context {1}";

  private Messages() {
    // private constructor to prevent instantiation
  }

  static {
    // initialize resource bundle
    load(BUNDLE_NAME, Messages.class);
  }
}