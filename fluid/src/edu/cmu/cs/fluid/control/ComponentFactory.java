package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.*;

/** A component factory creates a component for a node.
 * It should be subclassed with something that knows how to
 * create a component for a given IRNode.  Each Component
 * is assigned the factory that created it.
 */
public interface ComponentFactory {
  /** Return the component for this node, creating (and registering it)
   * as necessary.  The component should be created at most once.
   */
  public Component getComponent(IRNode node);
  
  /** Return the syntax tree instance for the nodes here. */
  public SyntaxTreeInterface tree();
}
