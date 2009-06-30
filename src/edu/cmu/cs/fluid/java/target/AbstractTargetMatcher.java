/*
 * Created on Aug 4, 2004
 *
 */
package edu.cmu.cs.fluid.java.target;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * @author chance
 *
 */
public abstract class AbstractTargetMatcher implements ITargetMatcher, JavaGlobals {
  final IRNode target;
  
  public AbstractTargetMatcher(IRNode target) {
    this.target = target;
  }
  
  public AbstractTargetMatcher() {
    this(null);    
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.target.ITargetMatcher#match(edu.cmu.cs.fluid.ir.IRNode)
   */
  public boolean match(IRNode decl) {  
    return match(decl, jtree.getOperator(decl));
  }
  
  public boolean match(String s) {
    throw new UnsupportedOperationException();
  }
}
