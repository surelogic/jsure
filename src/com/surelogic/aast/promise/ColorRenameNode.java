
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ColorRenameNode extends ColoringAnnotationNode { 
  // Fields
  private final ColorNameNode color;
  private final ColorExprNode cExpr;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ColorRename") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ColorNameNode color =  (ColorNameNode) _kids.get(0);
        ColorExprNode cExpr =  (ColorExprNode) _kids.get(1);
        return new ColorRenameNode (_start,
          color,
          cExpr        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColorRenameNode(int offset,
                         ColorNameNode color,
                         ColorExprNode cExpr) {
    super(offset);
    if (color == null) { throw new IllegalArgumentException("color is null"); }
    ((AASTNode) color).setParent(this);
    this.color = color;
    if (cExpr == null) { throw new IllegalArgumentException("cExpr is null"); }
    ((AASTNode) cExpr).setParent(this);
    this.cExpr = cExpr;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ColorRename\n");
    sb.append(getColor().unparse(debug, indent+2));
    sb.append(getCExpr().unparse(debug, indent+2));
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public ColorNameNode getColor() {
    return color;
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
		return new ColorRenameNode(getOffset(), (ColorNameNode)getColor().cloneTree(), (ColorExprNode)getCExpr().cloneTree());
	}
}

