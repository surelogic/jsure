
package com.surelogic.aast.promise;


import java.util.*;



import com.surelogic.aast.*;
import com.surelogic.analysis.threadroles.TRBinExpr;
import com.surelogic.analysis.threadroles.TRExpr;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;

public class ThreadRoleOrNode extends AASTNode implements ThreadRoleExprElem { 
  // Fields
  private final List<ThreadRoleOrElem> orElems;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ThreadRoleOr") {
      @Override
      @SuppressWarnings("unchecked")
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        @SuppressWarnings("rawtypes")
        List<ThreadRoleOrElem> orElems = ((List) _kids);
        return new ThreadRoleOrNode (_start,
          orElems        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleOrNode(int offset,
                     List<ThreadRoleOrElem> orElems) {
    super(offset);
    if (orElems == null) { throw new IllegalArgumentException("orElems is null"); }
    for (ThreadRoleOrElem _tro : orElems) {
      ((AASTNode) _tro).setParent(this);
    }
    this.orElems = Collections.unmodifiableList(orElems);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); 
      sb.append("ThreadRoleOr\n");
      for(ThreadRoleOrElem _n : getOrElemsList()) {
    	  sb.append(_n.unparse(debug, indent+2));
      }
    } else if (getOrElemsList().size() == 1) {
      return getOrElemsList().get(0).unparse(debug, indent);
    } else {
      boolean first = true;
      for(ThreadRoleOrElem _n : getOrElemsList()) {
    	  if (first) {
    		  first = false;
    	  } else {
    		  sb.append(" | ");
    	  }
    	  sb.append(_n.unparse(debug, indent+2));
      }
    }
    return sb.toString();
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<ThreadRoleOrElem> getOrElemsList() {
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
		List<ThreadRoleOrElem> orElemsCopy = new ArrayList<ThreadRoleOrElem>(orElems.size());
		for (ThreadRoleOrElem _n : getOrElemsList()) {
			orElemsCopy.add((ThreadRoleOrElem)_n.cloneTree());
		}	
    return new ThreadRoleOrNode(getOffset(), orElemsCopy);
	}
	
	public TRExpr buildTRExpr(IRNode where) {
	  TRExpr res = null;
	  boolean first = true;
	  for (ThreadRoleOrElem oe: orElems) {
	    if (first) {
	      res = oe.buildTRExpr(null);
	      first = false;
	    } else {
	      final TRExpr t = oe.buildTRExpr(where);
	      res = TRBinExpr.cOr(res, t);
	    }
	  }
	  return res;
	}
}

