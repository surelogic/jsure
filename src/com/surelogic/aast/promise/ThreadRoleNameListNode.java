/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorNameListNode.java,v 1.1 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.surelogic.aast.*;

public abstract class ThreadRoleNameListNode extends ThreadRoleAnnotationNode {
//Fields
  protected final List<ThreadRoleNameNode> tRoles;
  private final String kind;

  public ThreadRoleNameListNode(int offset,
                        List<ThreadRoleNameNode> tRoles,
                        String kind) {
    super(offset);
    if (tRoles == null) { throw new IllegalArgumentException("tRoles is null"); }
    for (ThreadRoleNameNode _t : tRoles) {
      ((AASTNode) _t).setParent(this);
    }
    this.tRoles = Collections.unmodifiableList(tRoles);
    this.kind = kind;
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<ThreadRoleNameNode> getThreadRoleList() {
    return tRoles;
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
    boolean first = true;
    for(AASTNode _n : tRoles) {
      if (debug) {
    	  sb.append(_n.unparse(debug, indent+2));
      } else {
    	  if (first) {
    		  first = false;
    	  } else {
    	      sb.append(", ");
    	  }
    	  sb.append(_n.unparse(debug, indent));
      }
    }
    return sb.toString();
  }

  protected List<ThreadRoleNameNode> cloneTRoleList() {
    List<ThreadRoleNameNode> tRolesCopy = new ArrayList<ThreadRoleNameNode>(tRoles.size());
    for (ThreadRoleNameNode tRoleNameNode : tRoles) {
      tRolesCopy.add((ThreadRoleNameNode)tRoleNameNode.cloneTree());
    } 
    return tRolesCopy;
  }
}
