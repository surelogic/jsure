
package com.surelogic.aast.java;


import java.util.List;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.IntType;

public class IntTypeNode extends IntegralTypeNode { 
  // Fields

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("IntType") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        return new IntTypeNode (_start        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public IntTypeNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
    	sb.append("int\n");
    } else {
    	sb.append("int");
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
		return IntType.prototype.includes(type);
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		return new IntTypeNode(getOffset());
	}
}

