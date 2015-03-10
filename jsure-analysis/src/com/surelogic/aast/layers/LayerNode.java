package com.surelogic.aast.layers;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.promise.PromiseTargetNode;

public class LayerNode extends AbstractLayerMatchDeclNode {
	public static final AbstractAASTNodeFactory factory =
		new AbstractAASTNodeFactory("Layer") {
		@Override
		public AASTNode create(String _token, int _start, int _stop,
				int _mods, String _id, int _dims, List<AASTNode> _kids) {			
			PromiseTargetNode target = null;
			if (_kids.size() > 0) {
				target = (PromiseTargetNode) _kids.get(0);
			}
			return new LayerNode(_start, _id, target);
		}
	};
	
	LayerNode(int offset, String name, PromiseTargetNode target) {
		super(offset, name, target);
	}
	
	@Override
	public IAASTNode cloneTree() {
		return new LayerNode(offset, getId(), getTarget());
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
	public String unparse(boolean debug, int indent) {
		return unparse(debug, indent, "Layer", " may refer to ");
	}

}
