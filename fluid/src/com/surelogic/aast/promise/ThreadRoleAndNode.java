
package com.surelogic.aast.promise;


import java.util.*;



import com.surelogic.aast.*;
import com.surelogic.analysis.threadroles.TRBinExpr;
import com.surelogic.analysis.threadroles.TRExpr;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;

public class ThreadRoleAndNode extends AASTNode implements ThreadRoleOrElem, ThreadRoleExprElem { 
  // Fields
  private final List<TRoleLit> andElems;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ThreadRoleAnd") {
      @Override
      @SuppressWarnings("unchecked")      
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        @SuppressWarnings("rawtypes")
        List<TRoleLit> andElems = ((List) _kids);
        return new ThreadRoleAndNode(_start, andElems);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleAndNode(int offset,
                      List<TRoleLit> andElems) {
    super(offset);
    if (andElems == null) { throw new IllegalArgumentException("andElems is null"); }
    for (TRoleLit _c : andElems) {
      ((AASTNode) _c).setParent(this);
    }
    this.andElems = Collections.unmodifiableList(andElems);
  }

  @Override
  public String unparse(boolean debug, int indent) {
	  StringBuilder sb = new StringBuilder();
	  if (debug) { 
		  indent(sb, indent); 
		  sb.append("ThreadRoleAnd\n");
		  for(TRoleLit _n : getAndElemsList()) {
			  sb.append(_n.unparse(debug, indent+2));
		  }	  
	  } else if (getAndElemsList().size() == 1) {
		  return getAndElemsList().get(0).unparse(debug, indent);
	  } else {
		  boolean first = true;
		  for(TRoleLit _n : getAndElemsList()) {
			  if (first) {
				  first = false;
			  } else {
				  sb.append(" & ");
			  }
			  sb.append(_n.unparse(debug, indent+2));
		  }
  }
    return sb.toString();
  }

  
  @Override
  public TRExpr buildTRExpr(IRNode where) {
    TRExpr res = null;
    for (TRoleLit cl: andElems) {
      if (res == null) {
        res = cl.buildTRExpr(null);
      } else {
        final TRExpr t = cl.buildTRExpr(where);
        res = TRBinExpr.cAnd(res, t);
      }
    }
    return res;
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<TRoleLit> getAndElemsList() {
    return andElems;
  }
  

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	List<TRoleLit> andElemsCopy = new ArrayList<TRoleLit>(andElems.size());
  	for (TRoleLit _n : getAndElemsList()) {
  	  // cast below is guaranteed to succeed if tree is built correctly.
			andElemsCopy.add((TRoleLit)_n.cloneTree());
		}
  	return new ThreadRoleAndNode(getOffset(), andElemsCopy);
  }
}

