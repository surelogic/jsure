/*
 * Created on Aug 4, 2004
 *
 */
package edu.cmu.cs.fluid.java.target;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;


/**
 * @author chance
 *
 */
public interface ITargetMatcher {
  static final Logger LOG = SLLogger.getLogger("FLUID.java.target");
  
  boolean match(IRNode decl);
  boolean match(IRNode decl, Operator op);
  
  /**
   * Optional operation, since it may not make any sense
   */
  boolean match(String name);
}
