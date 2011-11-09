/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.promise.PromiseTargetNode;

public class AllowsReferencesFromNode extends AbstractLayerMatchRootNode {
	public static final AbstractAASTNodeFactory factory =
		new AbstractAASTNodeFactory("AllowsReferencesFrom") {
		@Override
		public AASTNode create(String _token, int _start, int _stop,
				int _mods, String _id, int _dims, List<AASTNode> _kids) {			
			return new AllowsReferencesFromNode(_start, (PromiseTargetNode) _kids.get(0));
		}
	};
	
	AllowsReferencesFromNode(int offset, PromiseTargetNode target) {
		super(offset, target);
	}
	
	@Override
	public IAASTNode cloneTree() {
		return new AllowsReferencesFromNode(offset, getTarget());
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String unparse(boolean debug, int indent) {
		return unparse(debug, indent, "AllowsReferencesFrom");
	}
}
