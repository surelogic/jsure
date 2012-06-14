package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import java.util.Set;

import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.FieldTriple;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.State;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.Store;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.util.ImmutableSet;

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
  // == Compromising unique fields
  // ==================================================================
  
  public void recordCompromisingOfUnique(
      final IRNode srcOp, final Integer topOfStack, final State localStatus,
      final ImmutableSet<FieldTriple> fieldStore) {
    // Do Nothing
  }
  
  public void recordUndefiningOfUnique(
      final IRNode srcOp, final Integer topOfStack, final State localStatus,
      final Store s) {
    // Do nothing
  }
  
  public void recordIndirectLoadOfCompromisedField(
      final IRNode srcOp, final State fieldState, final IRNode fieldDecl) {
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
