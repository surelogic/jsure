/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorExprPromiseNode.java,v 1.1 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public abstract class ThreadRoleExprPromiseNode extends AASTRootNode {

  private final ThreadRoleExprNode theExprNode;
  private final String kind;
  
  
//  public static final AbstractAASTNodeFactory factory =
//    new AbstractAASTNodeFactory(
//      "ColorConstraint") {
//    @Override
//    @SuppressWarnings("unchecked")
//    public AASTNode create(String _token, int _start, int _stop, int _mods,
//        String _id, int _dims, List<AASTNode> _kids) {
//      return new ColorExprPromiseNode(_start, (ColorExprNode) _kids.get(0));
//    }
//  };
  
  // Constructors
  /**
   * Lists passed in as arguments must be
   * 
   * @unique
   */
  protected ThreadRoleExprPromiseNode(int offset, ThreadRoleExprNode n, String kind) {
    super(offset);
    theExprNode = n;
    this.kind = kind;
  }

  @Override
  public final String unparseForPromise() {
	  throw new UnsupportedOperationException();
  }
  
  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent);     
    	sb.append(kind);
    	sb.append('\n');
    } else {
    	sb.append(kind).append(' ');
    }
	sb.append(theExprNode.unparse(debug, indent+2));
	
    return sb.toString();
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  

  public ThreadRoleExprNode getTheExprNode() {
    return theExprNode;
  }
  @Override
  public IAASTNode cloneTree() {
    // TODO Auto-generated method stub
    return null;
  }

}
