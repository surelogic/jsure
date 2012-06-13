package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import java.util.Set;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;

public final class NullSideEffects implements ISideEffects {
  public static final NullSideEffects prototype = new NullSideEffects();

  
  
  // ==================================================================
  // === Constructor 
  // ==================================================================

  private NullSideEffects() {
  }
  
  
  
  // ==================================================================
  // === Control side effect production 
  // ==================================================================
  
  public void setSuppressDrops(final boolean value) {
    // Do nothing
  }
  
  public void setAbruptResults(final boolean value) {
    // Do nothing
  }

  

  // ==================================================================
  // == Alias burying
  // ==================================================================
  
  public void recordBuriedRead(final IRNode srcOp, final Object local,
      final BuriedMessage msg) {
    // Do nothing
  }
  
  public void recordBuryingFieldRead(final IRNode fieldDecl,
      final Set<Object> affectedVars, final IRNode srcOp) {
    // Do nothing
  }
  
  public void recordBuryingMethodEffects(final Set<IRNode> loadedFields,
      final Set<Object> affectedVars, final IRNode srcOp,
      final RegionEffectsPromiseDrop fxDrop) {
    // Do nothing
  }

  
  // ==================================================================
  // == Bad Values
  // ==================================================================

  public void recordBadSet(final Object local, final IRNode op) {
    // Do nothing
  }



  // ==================================================================
  // == Manage Result Drops
  // ==================================================================

  public void makeResultDrops() {
    // Do nothing
  }
}
