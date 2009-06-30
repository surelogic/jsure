/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorExprPromiseNode.java,v 1.1 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.parse.AbstractSingleNodeFactory;

public abstract class ColorExprPromiseNode extends AASTRootNode {

  private final ColorExprNode theExprNode;
  private final String kind;
  
  
//  public static final AbstractSingleNodeFactory factory =
//    new AbstractSingleNodeFactory(
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
  protected ColorExprPromiseNode(int offset, ColorExprNode n, String kind) {
    super(offset);
    theExprNode = n;
    this.kind = kind;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append(kind);
    sb.append('\n');
    sb.append(theExprNode.unparse(debug, indent+2));
    return sb.toString();
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  

  public ColorExprNode getTheExprNode() {
    return theExprNode;
  }
  @Override
  public IAASTNode cloneTree() {
    // TODO Auto-generated method stub
    return null;
  }

}
