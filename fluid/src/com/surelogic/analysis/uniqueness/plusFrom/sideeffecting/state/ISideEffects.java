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

public interface ISideEffects {
  // ==================================================================
  // === Control side effect production 
  // ==================================================================
  
  public void setSuppressDrops(boolean value);
  
  public void setAbruptResults(boolean value);

  

  // ==================================================================
  // == Compromising unique fields
  // ==================================================================
  
  public void recordReadOfBorrowedField(
      IRNode srcOp, PromiseDrop<? extends IAASTRootNode> promiseDrop);
  
  public void recordCompromisingOfUnique(
      IRNode srcOp, Integer topOfStack, State localStatus,
      ImmutableSet<FieldTriple> fieldStore, int msg, Object... args);
  
  public void recordUndefiningOfUnique(
      IRNode srcOp, Integer topOfStack, State localStatus, Store s);
  
  public void recordLoadOfCompromisedField(
      IRNode srcOp, State fieldState, IRNode fieldDecl);
  
  public void recordIndirectLoadOfCompromisedField(
      IRNode srcOp, State fieldState, IRNode fieldDecl);
  
  public void recordLossOfCompromisedField(
      IRNode srcOp, State fieldState, IRNode fieldDecl);
  

  
  // ==================================================================
  // == Alias burying
  // ==================================================================
  
  public void recordBuriedRead(IRNode srcOp, Object local, BuriedMessage msg);
  
  public void recordUndefinedFrom(
      IRNode srcOp, Set<Object> affectedVars, int msg);
  
  public void recordBuryingFieldRead(IRNode srcOp,
      IRNode fieldDecl, Set<Object> affectedVars);
  
  public void recordBuryingMethodEffects(IRNode srcOp,
      Set<IRNode> loadedFields, Set<Object> affectedVars,
      RegionEffectsPromiseDrop fxDrop);

  
  
  // ==================================================================
  // == Bad Values
  // ==================================================================

  public void recordBadSet(Object local, IRNode op);



  // ==================================================================
  // == Manage Result Drops
  // ==================================================================

  public void makeResultDrops();
}
