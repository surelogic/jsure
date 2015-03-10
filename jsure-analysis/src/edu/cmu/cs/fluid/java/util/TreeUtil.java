// $Header: /var/cvs/fluid/code/fluid/java/operator/TreeUtil.java,v 1.1 1999/04/02 17:12:24 chance Exp $
package edu.cmu.cs.fluid.java.util;

import java.util.Stack;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaPromise;

public class TreeUtil implements JavaGlobals {
  public static Stack<IRNode> findPathUp(IRNode root, IRNode here) {
    Stack<IRNode> stack = new Stack<IRNode>();
    
    // based on bsi.findRoot
    IRNode parent = JavaPromise.getParentOrPromisedFor(here);
    while (parent != null) {
      if (root.equals(parent)) {
        return stack;
      }
      stack.push(parent);
      here = parent;
      parent = JavaPromise.getParentOrPromisedFor(here);
    }
    return null; // ran out of parents w/o matching root
  }
}
