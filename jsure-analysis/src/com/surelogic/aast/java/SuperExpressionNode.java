/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/java/SuperExpressionNode.java,v 1.2 2007/09/24 21:09:55 ethan Exp $*/
package com.surelogic.aast.java;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

/**
 * TODO Fill in purpose.
 * 
 * @author ethan
 */
public class SuperExpressionNode extends ConstructionObjectNode {
		
  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("SuperExpression") {
      @Override     
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        return new SuperExpressionNode (_start        );
      }
    };

		/**
		 * @param offset
		 */
		public SuperExpressionNode(int offset) {
				super(offset);
		}

  @Override
  public String unparse(boolean debug, int indent) { 
    if (debug) { 
      StringBuilder sb = new StringBuilder();
      indent(sb, indent); 
      sb.append("SuperExpression\n");
      return sb.toString();
    }
    return "super";
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

  public boolean bindingExists() {
    return AASTBinder.getInstance().isResolvable(this);
  }

  public IVariableBinding resolveBinding() {
    return AASTBinder.getInstance().resolve(this);
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		return new SuperExpressionNode(getOffset());
	}
}