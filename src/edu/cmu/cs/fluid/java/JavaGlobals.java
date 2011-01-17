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

  @SuppressWarnings("rawtypes")
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
  static final Category REGION_CAT = Category.getInstance2(100);
  static final Category LOCK_ASSURANCE_CAT = Category.getInstance2(101);
  static final Category LOCK_REQUIRESLOCK_CAT = Category.getInstance2(102);
  static final Category UNIQUENESS_CAT = Category.getInstance2(103);
  static final Category EFFECTS_CAT = Category.getInstance2(104);
  static final Category NULL_CAT = Category.getInstance2(105);
  static final Category THREAD_EFFECTS_CAT = Category.getInstance2(106);
  static final Category THREAD_ROLES_CAT = Category.getInstance2(107);
  static final Category THREAD_ROLE_REPORT_REGION_CAT = Category.getInstance2(108);
  static final Category THREAD_ROLE_CONSTRAINED_REGION_CAT = Category.getInstance2(109);
  static final Category USES_CAT = Category.getInstance2(110);
  static final Category PROMISE_CAT = Category.getInstance2(111);
  static final Category VOUCH_CAT = Category.getInstance2(112);
  static final Category ASSUME_CAT = Category.getInstance2(113);
  static final Category PROMISE_SCRUBBER = Category.getInstance2(114);
  static final Category PROMISE_PARSER_PROBLEM = PROMISE_SCRUBBER;
  static final Category PROMISE_PARSER_WARNING = Category.getInstance2(115);
  static final Category CONVERT_TO_IR = Category.getInstance2(116);
  static final Category UNCATEGORIZED = Category.getInstance2(117);
  static final Category MODULE_CAT = Category.getInstance2(118);
}