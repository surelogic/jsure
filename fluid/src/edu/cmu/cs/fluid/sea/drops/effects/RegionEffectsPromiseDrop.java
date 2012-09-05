/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/effects/RegionEffectsPromiseDrop.java,v 1.4 2007/12/18 19:50:41 aarong Exp $*/
package edu.cmu.cs.fluid.sea.drops.effects;

import java.util.List;

import com.surelogic.aast.promise.EffectsSpecificationNode;
import com.surelogic.aast.promise.RegionEffectsNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * TODO Fill in purpose.
 * 
 * @author ethan
 */
public class RegionEffectsPromiseDrop extends PromiseDrop<RegionEffectsNode> {

	public RegionEffectsPromiseDrop(RegionEffectsNode s) {
		super(s);
		setCategory(JavaGlobals.EFFECTS_CAT);
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

	@Override
	protected void computeBasedOnAST() {
		final IRNode declNode = getNode();
		final String target = JavaNames.genMethodConstructorName(declNode);
		final List<EffectsSpecificationNode> effects = getAAST().getEffectsList();

		if (effects.size() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(effects.get(0).toString());
			for (int i = 1, len = effects.size(); i < len; i++) {
				sb.append("; ");
				sb.append(effects.get(i).toString());
			}
			sb.append(" on ");
			sb.append(target);
			setMessage(sb.toString());
		} else {
			setMessage("No effects on " + target);
		}
	}
}
