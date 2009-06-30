
package com.surelogic.aast.promise;


import java.util.*;



import com.surelogic.aast.*;
import com.surelogic.analysis.colors.CBinExpr;
import com.surelogic.analysis.colors.CExpr;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;

public class ColorAndNode extends AASTNode implements ColorOrElem, ColorExprElem { 
  // Fields
  private final List<ColorLit> andElems;

  public static final AbstractSingleNodeFactory factory =
    new AbstractSingleNodeFactory("ColorAnd") {
      @Override
      @SuppressWarnings("unchecked")      
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        List<ColorLit> andElems = ((List) _kids);
        return new ColorAndNode(_start, andElems);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColorAndNode(int offset,
                      List<ColorLit> andElems) {
    super(offset);
    if (andElems == null) { throw new IllegalArgumentException("andElems is null"); }
    for (ColorLit _c : andElems) {
      ((AASTNode) _c).setParent(this);
    }
    this.andElems = Collections.unmodifiableList(andElems);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ColorAnd\n");
    for(ColorLit _n : getAndElemsList()) {
      sb.append(_n.unparse(debug, indent+2));
    }
    return sb.toString();
  }

  
  public CExpr buildCExpr(IRNode where) {
    CExpr res = null;
    for (ColorLit cl: andElems) {
      if (res == null) {
        res = cl.buildCExpr(null);
      } else {
        final CExpr t = cl.buildCExpr(where);
        res = CBinExpr.cAnd(res, t);
      }
    }
    return res;
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<ColorLit> getAndElemsList() {
    return andElems;
  }
  

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	List<ColorLit> andElemsCopy = new ArrayList<ColorLit>(andElems.size());
  	for (ColorLit _n : getAndElemsList()) {
  	  // cast below is guaranteed to succeed if tree is built correctly.
			andElemsCopy.add((ColorLit)_n.cloneTree());
		}
  	return new ColorAndNode(getOffset(), andElemsCopy);
  }
}

