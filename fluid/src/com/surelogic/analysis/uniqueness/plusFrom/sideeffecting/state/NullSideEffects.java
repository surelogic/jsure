package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.FieldTriple;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.State;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.Store;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
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
  
  public void recordReadOfBorrowedField(final IRNode srcOp,
      final PromiseDrop<? extends IAASTRootNode> promiseDrop) {
    // Do nothing
  }

  public void recordCompromisingOfUnique(
      final IRNode srcOp, final Integer topOfStack, final State localStatus,
      final ImmutableSet<FieldTriple> fieldStore,
      final int msg, final Object... args) {
    // Do Nothing
  }
  
  public void recordUndefinedFrom(
      final IRNode srcOp, final Set<Object> affectedVars, final int msg) {
    // Do nothing
  }
  
  public void recordUndefiningOfUnique(
      final IRNode srcOp, final Integer topOfStack, final State localStatus,
      final Store s) {
    // Do nothing
  }
  
  public void recordLoadOfCompromisedField(
      final IRNode srcOp, final State fieldState, final IRNode fieldDecl) {
    // Do nothing
  }
  
  public void recordIndirectLoadOfCompromisedField(
      final IRNode srcOp, final State fieldState, final IRNode fieldDecl) {
    // Do nothing
  }
  
  public void recordLossOfCompromisedField(
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
  
  public void recordBuryingFieldRead(final IRNode srcOp,
      final IRNode fieldDecl, final Set<Object> affectedVars) {
    // Do nothing
  }
  
  public void recordBuryingMethodEffects(final IRNode srcOp,
      final Set<IRNode> loadedFields, final Set<Object> affectedVars,
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
