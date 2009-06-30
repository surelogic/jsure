
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.analysis.colors.CExpr;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;

public class ColorNotNode extends AASTNode implements ColorLit, ColorOrElem { 
  // Fields
  private final ColorNameNode target;

  public static final AbstractSingleNodeFactory factory =
    new AbstractSingleNodeFactory("ColorNot") {
      @Override
      @SuppressWarnings("unchecked")      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ColorNameNode target =  (ColorNameNode) _kids.get(0);
        return new ColorNotNode (_start,
          target        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColorNotNode(int offset,
                      ColorNameNode target) {
    super(offset);
    if (target == null) { throw new IllegalArgumentException("target is null"); }
    ((AASTNode) target).setParent(this);
    this.target = target;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ColorNot\n");
    sb.append(getTarget().unparse(debug, indent+2));
    return sb.toString();
  }
  
  public CExpr buildCExpr(IRNode where) {
    CExpr res = target.buildCExpr(null);
    return res;
  }

  /**
   * @return A non-null node
   */
  public ColorNameNode getTarget() {
    return target;
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
		return new ColorNotNode(getOffset(), (ColorNameNode)getTarget().cloneTree());
	}
}

