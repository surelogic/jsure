
package com.surelogic.aast.java;


import java.util.List;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ArrayType;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class ArrayTypeNode extends ReferenceTypeNode { 
  // Fields
  private final TypeNode base;
  private final int dims;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ArrayType") {
      @Override   
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        TypeNode base =  (TypeNode) _kids.get(0);
        int dims = _dims;
        return new ArrayTypeNode (_start,
          base,
          dims        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ArrayTypeNode(int offset,
                       TypeNode base,
                       int dims) {
    super(offset);
    if (base == null) { throw new IllegalArgumentException("base is null"); }
    ((AASTNode) base).setParent(this);
    this.base = base;
    this.dims = dims;
  }

  @Override
  public String unparse(boolean debug, int indent) {	  
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
    	sb.append("ArrayType\n");
    	sb.append(getBase().unparse(debug, indent+2));
    	indent(sb, indent+2);
    	sb.append("dims=").append(getDims());
    	sb.append("\n");
    } else {
    	sb.append(getBase().unparse(debug, indent));
    	for(int i=0; i<dims; i++) {
    		sb.append("[]");
    	}
    }
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public TypeNode getBase() {
    return base;
  }
  /**
   * @return A non-null int
   */
  public int getDims() {
    return dims;
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
		Operator op = JJNode.tree.getOperator(type);
		if(op instanceof ArrayType){
			IRNode dBase = ArrayType.getBase(type);
			int dDims = ArrayType.getDims(type);
			if(base.matches(dBase) && dims == dDims){
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new ArrayTypeNode(getOffset(), (TypeNode)getBase().cloneTree(), getDims());
	}
}

