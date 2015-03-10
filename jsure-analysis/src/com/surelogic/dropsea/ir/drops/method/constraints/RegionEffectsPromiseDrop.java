package com.surelogic.dropsea.ir.drops.method.constraints;

import com.surelogic.aast.promise.EffectsSpecificationNode;
import com.surelogic.aast.promise.RegionEffectsNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

public class RegionEffectsPromiseDrop extends PromiseDrop<RegionEffectsNode> {

  public RegionEffectsPromiseDrop(RegionEffectsNode s) {
    super(s);
    setCategorizingMessage(JavaGlobals.EFFECTS_CAT);
  }

  /**
   * Returns a list of WritesNodes and ReadsNodes. If created properly, there
   * should only be 1 ReadsNode and 1 WritesNode at most. The list may be empty
   * if the user entered 'none' for the effect.
   * 
   * @return A list of 0 or more EffectSpecificationNodes (ReadsNodes,
   *         WritesNodes). The list should never be longer than the number of
   *         possible effects, current 2, Reads and Writes.
   */
  public Iterable<EffectsSpecificationNode> getEffects() {
    return getAAST().getEffectsList();
  }
}
