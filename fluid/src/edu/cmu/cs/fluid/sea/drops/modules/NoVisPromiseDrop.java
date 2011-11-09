/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/modules/NoVisPromiseDrop.java,v 1.1 2007/10/27 17:11:10 dfsuther Exp $*/
package edu.cmu.cs.fluid.sea.drops.modules;

import com.surelogic.aast.promise.NoVisClauseNode;

import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public class NoVisPromiseDrop extends VisibilityDrop<NoVisClauseNode> {

	public NoVisPromiseDrop(NoVisClauseNode a) {
		super(a);
	}

	public static NoVisPromiseDrop buildNoVisPromiseDrop(NoVisClauseNode a) {
		final NoVisPromiseDrop nvd = new NoVisPromiseDrop(a);
		return VisibilityDrop.buildVisDrop(nvd);
	}

	@Override
	public String toString() {
		if (image == null) {
			image = "@NoVis";
		}
		return image;
	}
}
