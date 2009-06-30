
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class ColorRequireNode extends ColoringAnnotationNode { 
  // Fields
  private final ColorExprNode cExpr;

  public static final AbstractSingleNodeFactory factory =
    new AbstractSingleNodeFactory("ColorRequire") {
      @Override
      @SuppressWarnings("unchecked")      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ColorExprNode cExpr =  (ColorExprNode) _kids.get(0);
        return new ColorRequireNode (_start,
          cExpr        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColorRequireNode(int offset,
                          ColorExprNode cExpr) {
    super(offset);
    if (cExpr == null) { throw new IllegalArgumentException("cExpr is null"); }
    ((AASTNode) cExpr).setParent(this);
    this.cExpr = cExpr;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ColorRequire\n");
    sb.append(getCExpr().unparse(debug, indent+2));
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public ColorExprNode getCExpr() {
    return cExpr;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new ColorRequireNode(getOffset(), (ColorExprNode)getCExpr().cloneTree());
	}
}

