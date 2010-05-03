
package com.surelogic.aast.promise;


import java.util.*;



import com.surelogic.aast.*;
import com.surelogic.analysis.colors.CBinExpr;
import com.surelogic.analysis.colors.CExpr;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;

public class ColorOrNode extends AASTNode implements ColorExprElem { 
  // Fields
  private final List<ColorOrElem> orElems;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ColorOr") {
      @Override
      @SuppressWarnings("unchecked")
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        List<ColorOrElem> orElems = ((List) _kids);
        return new ColorOrNode (_start,
          orElems        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColorOrNode(int offset,
                     List<ColorOrElem> orElems) {
    super(offset);
    if (orElems == null) { throw new IllegalArgumentException("orElems is null"); }
    for (ColorOrElem _c : orElems) {
      ((AASTNode) _c).setParent(this);
    }
    this.orElems = Collections.unmodifiableList(orElems);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ColorOr\n");
    for(ColorOrElem _n : getOrElemsList()) {
      sb.append(_n.unparse(debug, indent+2));
    }
    return sb.toString();
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<ColorOrElem> getOrElemsList() {
    return orElems;
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
		List<ColorOrElem> orElemsCopy = new ArrayList<ColorOrElem>(orElems.size());
		for (ColorOrElem _n : getOrElemsList()) {
			orElemsCopy.add((ColorOrElem)_n.cloneTree());
		}	
    return new ColorOrNode(getOffset(), orElemsCopy);
	}
	
	public CExpr buildCExpr(IRNode where) {
	  CExpr res = null;
	  boolean first = true;
	  for (ColorOrElem oe: orElems) {
	    if (first) {
	      res = oe.buildCExpr(null);
	      first = false;
	    } else {
	      final CExpr t = oe.buildCExpr(where);
	      res = CBinExpr.cOr(res, t);
	    }
	  }
	  return res;
	}
}

