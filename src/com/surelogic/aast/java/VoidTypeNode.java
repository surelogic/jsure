
package com.surelogic.aast.java;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.IVoidType;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.VoidType;

public class VoidTypeNode extends ReturnTypeNode { 
  // Fields

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("VoidType") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        return new VoidTypeNode (_start        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public VoidTypeNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
    	sb.append("void\n");
    } else {
    	sb.append("void");
    }
    return sb.toString();
  }

  @Override
  public boolean typeExists() {
    return AASTBinder.getInstance().isResolvableToType(this);
  }

  /**
   * Gets the binding corresponding to the type of the VoidType
   */
  @Override
  public IVoidType resolveType() {
    return AASTBinder.getInstance().resolveType(this);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    
    return visitor.visit(this);
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.java.ReturnTypeNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode type) {
		return VoidType.prototype.includes(type);
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new VoidTypeNode(getOffset());
	}
}