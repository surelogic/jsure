package com.surelogic.dropsea.ir.drops.method.constraints;

import java.util.List;

import com.surelogic.aast.promise.EffectsSpecificationNode;
import com.surelogic.aast.promise.RegionEffectsNode;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;

public class RegionEffectsPromiseDrop extends PromiseDrop<RegionEffectsNode> {

  public RegionEffectsPromiseDrop(RegionEffectsNode s) {
    super(s);
    setCategorizingString(JavaGlobals.EFFECTS_CAT);

    final IRNode declNode = getNode();
    final List<EffectsSpecificationNode> effects = getAAST().getEffectsList();

    if (XUtil.useExperimental()) {
        final String target = JavaNames.genRelativeFunctionName(declNode);
    	setMessage(138, getAAST().unparseForPromise(), target);
    } else {
        final String target = JavaNames.genMethodConstructorName(declNode);
    	if (effects.size() > 0) {
    		StringBuilder sb = new StringBuilder();
    		sb.append(effects.get(0).toString());
    		for (int i = 1, len = effects.size(); i < len; i++) {
    			sb.append("; ");
    			sb.append(effects.get(i).toString());
    		}
    		sb.append(" on ");
    		sb.append(target);
    		setMessage(12, sb.toString());
    	} else {
    		setMessage(171, target);
    	}
    }
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
