// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/JavaGlobals.java,v 1.12
// 2003/07/02 20:19:58 thallora Exp $
package edu.cmu.cs.fluid.java;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.control.ControlFlowGraph;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

public interface JavaGlobals {
  static final String JLObject = "java.lang.Object";
	
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
  static final int REGION_CAT = 100;
  static final int LOCK_ASSURANCE_CAT = 101;
  static final int LOCK_REQUIRESLOCK_CAT = 102;
  static final int UNIQUENESS_CAT = 103;
  static final int EFFECTS_CAT = 104;
  static final int NULL_CAT = 105;
  static final int THREAD_EFFECTS_CAT = 106;
  static final int THREAD_ROLES_CAT = 107;
  static final int THREAD_ROLE_REPORT_REGION_CAT = 108;
  static final int THREAD_ROLE_CONSTRAINED_REGION_CAT = 109;
  static final int SCOPED_PROMISE_CAT = 111;
  static final int VOUCH_CAT = 112;
  static final int MODULE_CAT = 118;
  static final int UTILITY_CAT = 119;
  static final int SINGLETON_CAT = 120;
  static final int ANNO_BOUNDS_CAT = 121;
}