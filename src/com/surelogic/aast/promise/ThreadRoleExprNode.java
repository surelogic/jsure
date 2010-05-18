
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ThreadRoleExprNode extends AASTRootNode { 
  private final ThreadRoleExprElem theExpr;
  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory(
      "ThreadRoleExpr") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
        String _id, int _dims, List<AASTNode> _kids) {

      return new ThreadRoleExprNode(_start, (ThreadRoleExprElem) _kids.get(0));
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be
   * 
   * @unique
   */
  public ThreadRoleExprNode(int offset, ThreadRoleExprElem elem) {
    super(offset);
    theExpr = elem;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ThreadRoleExpr\n");
    sb.append(theExpr.unparse(debug, indent+2));
    return sb.toString();
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  

	public ThreadRoleExprElem getTheExpr() {
    return theExpr;
  }
	
  /* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new ThreadRoleExprNode(getOffset(), (ThreadRoleExprElem) theExpr.cloneTree());
	}
}

