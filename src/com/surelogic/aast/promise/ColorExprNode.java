
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ColorExprNode extends AASTRootNode { 
  private final ColorExprElem theExpr;
  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory(
      "ColorExpr") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
        String _id, int _dims, List<AASTNode> _kids) {

      return new ColorExprNode(_start, (ColorExprElem) _kids.get(0));
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be
   * 
   * @unique
   */
  public ColorExprNode(int offset, ColorExprElem elem) {
    super(offset);
    theExpr = elem;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ColorExpr\n");
    sb.append(theExpr.unparse(debug, indent+2));
    return sb.toString();
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  

	public ColorExprElem getTheExpr() {
    return theExpr;
  }
	
  /* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		return new ColorExprNode(getOffset(), (ColorExprElem) theExpr.cloneTree());
	}
}

