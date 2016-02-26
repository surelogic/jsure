package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.*;
import com.surelogic.aast.java.PrimaryExpressionNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.tree.Operator;

public class ItselfNode extends PrimaryExpressionNode 
implements IHasVariableBinding {
	// Fields

	public static final AbstractAASTNodeFactory factory =
		new AbstractAASTNodeFactory("Itself") {
		@Override
		public AASTNode create(String _token, int _start, int _stop,
				int _mods, String _id, int _dims, List<AASTNode> _kids) {
			return new ItselfNode (_start        );
		}
	};

	// Constructors
	/**
	 * Lists passed in as arguments must be @unique
	 */
	public ItselfNode(int offset) {
		super(offset);
	}

	@Override
	public String unparse(boolean debug, int indent) { 
		if (debug) { 
			StringBuilder sb = new StringBuilder();
			indent(sb, indent); 
			sb.append("Itself\n");
			return sb.toString();
		}
		return "itself";
	}

	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	@Override
  public boolean bindingExists() {
		return AASTBinder.getInstance().isResolvable(this);
	}

	@Override
  public IVariableBinding resolveBinding() {
		return AASTBinder.getInstance().resolve(this);
	}

	@Override
	public Operator getOp() {
		return FieldRef.prototype;
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		return new ItselfNode(getOffset());
	}
}

