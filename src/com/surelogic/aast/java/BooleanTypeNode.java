
package com.surelogic.aast.java;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.BooleanType;

public class BooleanTypeNode extends PrimitiveTypeNode { 
  // Fields

  public static final AbstractSingleNodeFactory factory =
    new AbstractSingleNodeFactory("BooleanType") {
      @Override
      @SuppressWarnings("unchecked")      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        return new BooleanTypeNode (_start        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public BooleanTypeNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
    	sb.append("boolean\n");
    } else {
    	sb.append("boolean");
    }
    return sb.toString();
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    
    return visitor.visit(this);
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.java.TypeNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode type) {
		return BooleanType.prototype.includes(type);
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new BooleanTypeNode(getOffset());
	}
}

