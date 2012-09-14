package edu.cmu.cs.fluid.java;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.control.Component;
import edu.cmu.cs.fluid.control.ComponentFactory;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.parse.JJOperator;
import edu.cmu.cs.fluid.tree.*;

public final class JavaComponentFactory implements ComponentFactory {
  /**
	 * Logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("FLUID.java.control");

  /*
   * TODO Could this be split up into thread-local maps?
   * (if it doesn't matter that they use the same thing and they mostly don't overlap anyways)
   */
  public static final JavaComponentFactory prototype = new JavaComponentFactory();

  /**
	 * Each syntax node potentially has a control component. This information is
	 * stored in the following table. The structures are transient, and so there
	 * is no need to use slots. @type Hashtable[IRNode,Component]
	 */
  private static final ConcurrentMap<IRNode, Component> components = new ConcurrentHashMap<IRNode, Component>();
  
  public static void clearCache() {
	  //checkCache();
	  components.clear();
  }

  public Component getComponent(IRNode node) {
    return getComponent(node, false);
  }
  
  public static Component getComponent(IRNode node, boolean quiet) {
    final Component comp = components.get(node);
    if (comp == null)
      // Requires class lock to be held: method is static synchronized
      return prototype.createComponent(node, quiet);
    else
      return comp;
  }
  
  // Class lock must be held
  private Component createComponent(final IRNode node, boolean quiet) {
    JavaOperator op = (JavaOperator) JJNode.tree.getOperator(node);
    Component comp = op.createComponent(node);
    if (comp != null) {
      if (LOG.isLoggable(Level.WARNING) && !comp.isValid()) {
        LOG.warning("invalid component for");
        JavaNode.dumpTree(System.out, node, 1);
      }
      comp.registerFactory(this);
	  Component old = components.putIfAbsent(node, comp);
	  if (old != null) {
		  // Already created by someone else
		  return old;
	  }
    } else if (!quiet) {
      LOG.warning(
        "Null control-flow component for " + DebugUnparser.toString(node) + " (OP = " + JJNode.tree.getOperator(node) + ")");
      IRNode here = node;
      while ((here = JJNode.tree.getParentOrNull(here)) != null) {
        LOG.warning(
          "  child of " + JJNode.tree.getOperator(here).name() + " node");
      }
      LOG.log(Level.WARNING, "Just want the trace", new FluidError("dummy"));
    }
    return comp;
  }

  public SyntaxTreeInterface tree() {
    return JJOperator.tree;
  }
}
