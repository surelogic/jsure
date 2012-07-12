package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import java.util.Set;

import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.FieldTriple;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.State;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.Store;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
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
  
  public void recordCompromisingOfUnique(
      IRNode srcOp, Integer topOfStack, State localStatus,
      ImmutableSet<FieldTriple> fieldStore, int msg, Object... args);
  
  public void recordUndefiningOfUnique(
      IRNode srcOp, Integer topOfStack, State localStatus, Store s);
  
  public void recordIndirectLoadOfCompromisedField(
      IRNode srcOp, final State fieldState, IRNode fieldDecl);
  
  
  
  // ==================================================================
  // == Alias burying
  // ==================================================================
  
  public void recordBuriedRead(IRNode srcOp, Object local, BuriedMessage msg);
  
  public void recordBuryingFieldRead(IRNode fieldDecl,
      Set<Object> affectedVars, IRNode srcOp);
  
  public void recordBuryingMethodEffects(Set<IRNode> loadedFields,
      Set<Object> affectedVars, IRNode srcOp,
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
