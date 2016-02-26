
package com.surelogic.aast.java;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.AASTBinder;
import com.surelogic.aast.bind.ISourceRefType;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.TypeRef;

public class TypeRefNode extends ClassTypeNode { 
  // Fields
  private final ClassTypeNode base;
  private final String id;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("TypeRef") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ClassTypeNode base =  (ClassTypeNode) _kids.get(0);
        String id = _id;
        return new TypeRefNode (_start,
          base,
          id        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public TypeRefNode(int offset,
                     ClassTypeNode base,
                     String id) {
    super(offset);
    if (base == null) { throw new IllegalArgumentException("base is null"); }
    ((AASTNode) base).setParent(this);
    this.base = base;
    if (id == null) { throw new IllegalArgumentException("id is null"); }
    this.id = id;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("TypeRef\n");
    sb.append(getBase().unparse(debug, indent+2));
    indent(sb, indent+2);
    sb.append("id=").append(getId());
    sb.append("\n");
    return sb.toString();
  }

  @Override
  public boolean typeExists() {
    return AASTBinder.getInstance().isResolvableToType(this);
  }

  /**
   * Gets the binding corresponding to the type of the TypeRef
   */
  @Override
  public ISourceRefType resolveType() {
    return AASTBinder.getInstance().resolveType(this);
  }

  /**
   * @return A non-null node
   */
  public ClassTypeNode getBase() {
    return base;
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

	/* (non-Javadoc)
	 * @see com.surelogic.aast.java.TypeNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode type) {
		if(TypeRef.prototype.includes(type)){
			final IRNode dBase = TypeRef.getBase(type);
			final String dId = TypeRef.getId(type);
			
			return base.matches(dBase) && id.equals(dId);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		return new TypeRefNode(getOffset(), (ClassTypeNode)getBase().cloneOrModifyTree(mod), getId());
	}
}

