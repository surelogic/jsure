// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/JavaGlobals.java,v 1.12
// 2003/07/02 20:19:58 thallora Exp $
package edu.cmu.cs.fluid.java;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.control.ControlFlowGraph;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

public interface JavaGlobals {

  static final ControlFlowGraph cfg = ControlFlowGraph.prototype;

  static final SyntaxTreeInterface jtree = JJNode.tree;

  static final ImmutableHashOrderSet empty = ImmutableHashOrderSet.empty;

  // mostly used for creating code
  static final IRNode[] noNodes = new IRNode[0];

  static final IRNode[] oneNode = new IRNode[1];
  
  static final IJavaType[] noTypes = new IJavaType[0];

  static final IJavaType[] oneType = new IJavaType[1];

  static final Logger JAVA = SLLogger.getLogger("JAVA");

  static final Logger SEARCH = SLLogger.getLogger("JAVA.searchCP");

  static final Logger BIND = SLLogger.getLogger("JAVA.bind");

  static final Logger PARSE = SLLogger.getLogger("JAVA.parse");

  static final Logger XFORM = SLLogger.getLogger("JAVA.xform");

  // drop-sea categories
  static final Category REGION_CAT = Category
      .getResultInstance("Regions");

  static final Category LOCK_ASSURANCE_CAT = Category
      .getResultInstance("Concurrency");

  static final Category LOCK_REQUIRESLOCK_CAT = Category
      .getInstance("lock precondition(s)");

  static final Category UNIQUENESS_CAT = Category
      .getResultInstance("Uniqueness");

  static final Category EFFECTS_CAT = Category.getResultInstance("Effects");

  static final Category NULL_CAT = Category.getResultInstance("Null values");

  static final Category THREAD_EFFECTS_CAT = Category
      .getResultInstance("Thread effects");

  static final Category THREAD_COLORING_CAT = Category
      .getResultInstance("Thread coloring");
  
  static final Category COLORIZED_REGION_CAT = 
    Category.getResultInstance("Regions marked as colorized");
  
  static final Category COLOR_CONSTRAINED_REGION_CAT = 
    Category.getResultInstance("Color Constrained Regions");

  static final Category USES_CAT = Category.getResultInstance("Structure");

  static final Category PROMISE_CAT = Category
      .getResultInstance("Scoped promises");

  static final Category ASSUME_CAT = Category
      .getResultInstance("Scoped assumptions");

  static final Category PROMISE_SCRUBBER = Category
      .getResultInstance("Modeling problems");

  static final Category PROMISE_PARSER_PROBLEM = PROMISE_SCRUBBER;

  static final Category PROMISE_PARSER_WARNING = Category
      .getResultInstance("Javadoc warnings");

  static final Category CONVERT_TO_IR = Category
      .getResultInstance("Convert to IR");
  
  static final Category UNCATEGORIZED = Category
  .getResultInstance("Uncategorized");
  
  static final Category MODULE_CAT = Category.getResultInstance("Modules");
}