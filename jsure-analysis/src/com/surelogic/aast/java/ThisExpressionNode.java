
package com.surelogic.aast.java;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.tree.Operator;

public class ThisExpressionNode extends ConstructionObjectNode 
implements IHasVariableBinding {
  // Fields

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ThisExpression") {
      @Override 
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        return new ThisExpressionNode (_start        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThisExpressionNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) { 
    if (debug) { 
      StringBuilder sb = new StringBuilder();
      indent(sb, indent); 
      sb.append("ThisExpression\n");
      return sb.toString();
    }
    return "this";
  }

  /**
   * @return A node, or null
   */
  public ClassTypeNode getType() {
    return null;
  }
  
  /**
   * Gets the binding corresponding to the type of the Expression
   */
  @Override
  public ISourceRefType resolveType() {
    return AASTBinder.getInstance().resolveType(this);
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
    return ThisExpression.prototype;
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		return new ThisExpressionNode(getOffset());
	}
}

