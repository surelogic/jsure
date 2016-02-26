
package com.surelogic.aast.java;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.tree.Operator;

public class VariableUseExpressionNode extends PrimaryExpressionNode 
implements IHasVariableBinding {
  // Fields
  private final String id;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("VariableUseExpression") {
      @Override    
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String id = _id;
        return new VariableUseExpressionNode (_start,
          id        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public VariableUseExpressionNode(int offset,
                                   String id) {
    super(offset);
    if (id == null) { throw new IllegalArgumentException("id is null"); }
    this.id = id;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    if (!debug) {
      return getId();
    }
    StringBuilder sb = new StringBuilder();
    indent(sb, indent); 
    sb.append("VariableUseExpression\n");
    indent(sb, indent+2);
    sb.append("id=").append(getId());
    sb.append("\n");
    return sb.toString();
  }

  @Override
  public boolean bindingExists() {
    return AASTBinder.getInstance().isResolvable(this);
  }

  @Override
  public IVariableBinding resolveBinding() {
    return AASTBinder.getInstance().resolve(this);
  }

  /**
   * @return A non-null String
   */
  public String getId() {
    return id;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    
    return visitor.visit(this);
  }
  
  @Override
  public Operator getOp() {
    return VariableUseExpression.prototype;
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		return new VariableUseExpressionNode(getOffset(), getId());
	}
}

