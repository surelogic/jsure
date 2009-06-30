/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/IJavaMemberTable.java,v 1.2 2007/04/25 20:39:26 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;

public interface IJavaMemberTable {
  Iterator<IRNode> getDeclarationsFromUse(String info, IRNode overrider);
  
  IJavaScope asScope(AbstractJavaBinder binder);
}
