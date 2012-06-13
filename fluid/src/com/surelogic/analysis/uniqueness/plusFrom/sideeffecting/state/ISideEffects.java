package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import java.util.Set;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;

public interface ISideEffects {
  // ==================================================================
  // === Control side effect production 
  // ==================================================================
  
  public void setSuppressDrops(boolean value);
  
  public void setAbruptResults(boolean value);

  

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
