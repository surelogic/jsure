/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.*;

public class MayReferToNode extends AbstractLayerMatchRootNode {
	public static final AbstractSingleNodeFactory factory =
		new AbstractSingleNodeFactory("MayReferTo") {
		@Override
		public AASTNode create(String _token, int _start, int _stop,
				int _mods, String _id, int _dims, List<AASTNode> _kids) {			
			return new MayReferToNode(_start, (AbstractLayerMatchTarget) _kids.get(0));
		}
	};
	
	MayReferToNode(int offset, AbstractLayerMatchTarget target) {
		super(offset, target);
	}
	
	@Override
	public IAASTNode cloneTree() {
		return new MayReferToNode(offset, getTarget());
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String unparse(boolean debug, int indent) {
		return unparse(debug, indent, "MayReferTo");
	}

}
