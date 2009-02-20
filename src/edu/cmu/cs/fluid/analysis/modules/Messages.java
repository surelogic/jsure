package edu.cmu.cs.fluid.analysis.modules;

import edu.cmu.cs.fluid.util.AbstractMessages;

public class Messages extends AbstractMessages {
  private static final String BUNDLE_NAME = "edu.cmu.cs.fluid.analysis.modules.messages"; //$NON-NLS-1$

  public static final String ModuleEffectsAnalysis_computedEffectsDrop = "computed effects (on interesting regions) for method {0} are {1}";

  public static final String ModuleEffectsAnalysis_noWFMXInfoDrop1 = "Whole Module Effects not run.";

  public static final String ModuleEffectsAnalysis_noWFMXInfoDrop2 = "No interesting regions to consider";

  private Messages() {
    // Private empty constructor to prevent instantiation
  }

  static {
    // initialize resource bundle
    load(BUNDLE_NAME, Messages.class);
  }
}