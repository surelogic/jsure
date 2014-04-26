package edu.cmu.cs.fluid.control;

import java.util.ArrayList;
import java.util.Collection;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;

/** A region of control nodes in the graph corresponding to the
 * child of a syntactic entity.  This structure serves as a wrapper
 * around the region of the child node itself (a Component).
 * We do not refer directly to the component, leaving that to
 * be computed on demand.
 * <P>  This structure maintains a set of ports that are used as
 * proxies of the ports on the child's component.  The ports
 * are all of type SubcomponentPort. </P>
 * @see Port
 * @see Component
 * @see VariableSubcomponent
 */

public class Subcomponent implements ISubcomponent {
  /** The component this subcomponent is nested in. */
  protected Component component;

  /** The location within the component that this subcomponent
   * occupies.
   */
  protected IRLocation location;

  /** The three ports: one for entry, one for normal exit
   * and one for exceptions (abrupt exit).
   */
  protected Port entryPort, normalExitPort, abruptExitPort;

  /** The syntax tree for control-flow graph subcomponent nodes.
   * <b>This should be a parameter, not a constant.</b>
   */
  protected static final SyntaxTreeInterface tree = edu.cmu.cs.fluid.parse.JJNode.tree;

  public Subcomponent(Component comp, IRLocation loc) {
    component = comp;
    location = loc;
    component.registerSubcomponent(loc, this);
  }

  public Subcomponent(
    Component comp,
    IRLocation loc,
    int inputs,
    int outputs,
    int abrupts) {
    this(comp, loc);
    if (inputs == 1) {
      entryPort = new SubcomponentEntryPort(this); // redundant assignment
    } else if (inputs == 2) {
    	entryPort = new SubcomponentBooleanEntryPort(this);
    } else {
      throw new FluidRuntimeException("bad number of entry ports");
    }
    if (outputs == 2) {
      normalExitPort = new SubcomponentBooleanExitPort(this);
    } else if (outputs == 1) {
      normalExitPort = new SubcomponentNormalExitPort(this);
    } else {
      throw new FluidRuntimeException("bad number of normal exit ports");
    }
    if (abrupts == 1) {
      abruptExitPort = new SubcomponentAbruptExitPort(this);
    } else {
      throw new FluidRuntimeException("bad number of abrupt exit ports");
    }
  }

  void registerEntryPort(SubcomponentPort entry) {
    entryPort = entry;
  }

  void registerNormalExitPort(SubcomponentPort normalExit) {
    normalExitPort = normalExit;
  }

  void registerAbruptExitPort(SubcomponentPort abruptExit) {
    abruptExitPort = abruptExit;
  }

  /* (non-Javadoc)
 * @see edu.cmu.cs.fluid.control.ISubcomponent#getComponent()
 */
  @Override
public Component getComponent() {
    return component;
  }

  /* (non-Javadoc)
 * @see edu.cmu.cs.fluid.control.ISubcomponent#getLocation()
 */
  @Override
public IRLocation getLocation() {
    return location;
  }

  /* (non-Javadoc)
 * @see edu.cmu.cs.fluid.control.ISubcomponent#getComponentInChild()
 */
  @Override
public Component getComponentInChild() {
    IRNode child = getSyntax();
    if (child == null)
      return null;
    return component.getComponent(child);
  }

  /* (non-Javadoc)
 * @see edu.cmu.cs.fluid.control.ISubcomponent#getSyntax()
 */
  @Override
public IRNode getSyntax() {
    IRNode node = component.getSyntax();
    IRNode child = tree.getChild(node, location);
    return child;
  }

  /* (non-Javadoc)
 * @see edu.cmu.cs.fluid.control.ISubcomponent#getEntryPort()
 */
  @Override
public Port getEntryPort() {
    return entryPort;
  }

  /* (non-Javadoc)
 * @see edu.cmu.cs.fluid.control.ISubcomponent#getNormalExitPort()
 */
  @Override
public Port getNormalExitPort() {
    return normalExitPort;
  }

  /* (non-Javadoc)
 * @see edu.cmu.cs.fluid.control.ISubcomponent#getAbruptExitPort()
 */
  @Override
public Port getAbruptExitPort() {
    return abruptExitPort;
  }

  Collection<SubcomponentNode> nodes;
  
  @Override
  public void registerSubcomponentNode(SubcomponentNode n) {
	  if (nodes == null) nodes = new ArrayList<SubcomponentNode>();
	  nodes.add(n);
  }
  
  /* (non-Javadoc)
 * @see edu.cmu.cs.fluid.control.ISubcomponent#getVariableEdge(int, boolean)
 */
  @Override
public VariableSubcomponentControlEdge getVariableEdge(
    int index,
    boolean isEntry) {
    return null;
  }
}
