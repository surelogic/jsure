package edu.cmu.cs.fluid.analysis.unique;

import com.surelogic.analysis.uniqueness.UniquenessAnalysisModule;

import edu.cmu.cs.fluid.analysis.util.*;

public final class UniquenessAssurance2 extends AbstractWholeAnalysisModule {
  private static UniquenessAssurance2 INSTANCE;

  /**
   * Provides a reference to the sole object of this class.
   *
   * @return a reference to the only object of this class
   */
  public static UniquenessAssurance2 getInstance() {
    return INSTANCE;
  }

  public UniquenessAssurance2() {
    super(new UniquenessAnalysisModule());
    INSTANCE = this;
  }
}
