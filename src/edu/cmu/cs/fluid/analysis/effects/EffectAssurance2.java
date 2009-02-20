package edu.cmu.cs.fluid.analysis.effects;

import com.surelogic.analysis.effects.EffectsAnalysis;

import edu.cmu.cs.fluid.analysis.util.*;
import edu.cmu.cs.fluid.dc.IAnalysis;

/**
 * Analysis routine to perform <I>Greenhouse</I> Java lock policy assurance.
 */
public final class EffectAssurance2 extends AbstractWholeAnalysisModule {
  private static EffectAssurance2 INSTANCE;

  /**
   * Provides a reference to the sole object of this class.
   *
   * @return a reference to the only object of this class
   */
  public static IAnalysis getInstance() {
    return INSTANCE;
  }

  /**
   * Public constructor that will be called by Eclipse when this analysis module
   * is created.
   */
  public EffectAssurance2() {
    super(new EffectsAnalysis());
    INSTANCE = this;
  }  
}