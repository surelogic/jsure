package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.FieldTriple;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.State;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.Store;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.RegionEffectsPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
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
  
  @Override
  public void setSuppressDrops(final boolean value) {
    // Do nothing
  }
  
  @Override
  public void setAbruptResults(final boolean value) {
    // Do nothing
  }

  

  // ==================================================================
  // == Compromising unique fields
  // ==================================================================
  
  @Override
  public void recordReadOfBorrowedField(final IRNode srcOp,
      final PromiseDrop<? extends IAASTRootNode> promiseDrop) {
    // Do nothing
  }

  @Override
  public void recordCompromisingOfUnique(
      final IRNode srcOp, final Integer topOfStack, final State localStatus,
      final ImmutableSet<FieldTriple> fieldStore,
      final int msg, final Object... args) {
    // Do Nothing
  }
  
  @Override
  public void recordUndefinedFrom(
      final IRNode srcOp, final Set<Object> affectedVars, final int msg) {
    // Do nothing
  }
  
  @Override
  public void recordUndefiningOfUnique(
      final IRNode srcOp, final Integer topOfStack, final State localStatus,
      final Store s) {
    // Do nothing
  }
  
  @Override
  public void recordLoadOfCompromisedField(
      final IRNode srcOp, final State fieldState, final IRNode fieldDecl) {
    // Do nothing
  }
  
  @Override
  public void recordIndirectLoadOfCompromisedField(
      final IRNode srcOp, final State fieldState, final IRNode fieldDecl) {
    // Do nothing
  }
  
  @Override
  public void recordLossOfCompromisedField(
      final IRNode srcOp, final State fieldState, final IRNode fieldDecl) {
    // Do nothing
  }
  
  
  
  // ==================================================================
  // == Alias burying
  // ==================================================================
  
  @Override
  public void recordBuriedRead(final IRNode srcOp, final Object local,
      final BuriedMessage msg) {
    // Do nothing
  }
  
  @Override
  public void recordBuryingFieldRead(final IRNode srcOp,
      final IRNode fieldDecl, final Set<Object> affectedVars) {
    // Do nothing
  }
  
  @Override
  public void recordBuryingMethodEffects(final IRNode srcOp,
      final Set<IRNode> loadedFields, final Set<Object> affectedVars,
      final RegionEffectsPromiseDrop fxDrop) {
    // Do nothing
  }

  
  // ==================================================================
  // == Bad Values
  // ==================================================================

  @Override
  public void recordBadSet(final Object local, final IRNode op) {
    // Do nothing
  }



  // ==================================================================
  // == Manage Result Drops
  // ==================================================================

  @Override
  public void makeResultDrops() {
    // Do nothing
  }
}
