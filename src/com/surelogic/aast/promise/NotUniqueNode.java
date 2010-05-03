/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/NotUniqueNode.java,v 1.1 2007/10/17 19:02:38 ethan Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

/**
 * Represents the @NotUnique annotation in the AAST
 * @author ethan
 */
public class NotUniqueNode extends AbstractBooleanNode {

  public static final AbstractAASTNodeFactory factory = new Factory("NotUnique") {   
    @Override
    public AASTNode create(int _start) {
      return new NotUniqueNode (_start);
    }
  };
  
  public NotUniqueNode(int offset){
  	super(offset);
  }
  
  
	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTRootNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new NotUniqueNode(getOffset());
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#accept(com.surelogic.aast.INodeVisitor)
	 */
	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#unparse(boolean, int)
	 */
	@Override
	public String unparse(boolean debug, int indent) {
		return unparse(debug, indent, "NotUnique");
	}

}
