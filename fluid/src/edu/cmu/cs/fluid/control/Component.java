/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/Component.java,v 1.19 2005/06/10 20:33:24 chance Exp $ */
package edu.cmu.cs.fluid.control;

import java.util.*;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.*;

/** A region of control nodes in the graph corresponding to a 
 * syntactic entity.  Regions are strictly nested and control cannot
 * enter or leave the region except through special edges
 * identified by Ports.
 * @see Port
 * @see Subcomponent
 */

public class Component {
  /** The syntax node for this component. */
  protected IRNode syntax;

  ComponentFactory factory;

  /** The syntax tree for control-flow graph component nodes.
   * <b>This should be a parameter, not a constant.</b>
   */
  protected static final SyntaxTreeInterface tree = edu.cmu.cs.fluid.parse.JJNode.tree;

  public void registerFactory(ComponentFactory cf) {
    factory = cf;
  }

  public Component getComponent(IRNode node) {
    return factory.getComponent(node);
  }

  /** The three ports: one for entry, one for normal exit
   * and one for exceptions (abrupt exit).
   */
  protected ComponentPort entryPort, normalExitPort, abruptExitPort;

  public Component(IRNode node) {
    syntax = node;
  }

  public Component(IRNode node, int inputs, int outputs, int abrupts) {
    this(node);
    if (inputs == 0) {
      entryPort = new ComponentBlankEntryPort(this); // redundant assignment
    } else if (inputs == 1) {
      entryPort = new ComponentEntryPort(this);
    } else if (inputs == 2) {
        entryPort = new ComponentBooleanEntryPort(this);
    } else {
      throw new FluidRuntimeException("bad number of entry ports");
    }
    if (outputs == 2) {
      normalExitPort = new ComponentBooleanExitPort(this);
    } else if (outputs == 1) {
      normalExitPort = new ComponentNormalExitPort(this);
    } else if (outputs == 0) {
      normalExitPort = new ComponentBlankNormalExitPort(this);
    } else {
      throw new FluidRuntimeException("bad number of normal exit ports");
    }
    if (abrupts == 0) {
      abruptExitPort = new ComponentBlankAbruptExitPort(this);
    } else if (abrupts == 1) {
      abruptExitPort = new ComponentAbruptExitPort(this);
    } else {
      throw new FluidRuntimeException("bad number of abrupt exit ports");
    }
  }

  void registerEntryPort(ComponentPort entry) {
    entryPort = entry;
  }

  void registerNormalExitPort(ComponentPort normalExit) {
    normalExitPort = normalExit;
  }

  void registerAbruptExitPort(ComponentPort abruptExit) {
    abruptExitPort = abruptExit;
  }

  /** Subcomponents within the region.
   * Each subcomponent represents a wrapper.
   */
  protected Hashtable<IRLocation,ISubcomponent> subcomponents;

  void registerSubcomponent(IRLocation loc, ISubcomponent sub) {
    if (subcomponents == null)
      subcomponents = new Hashtable<IRLocation,ISubcomponent>();
    if (loc == null)
      return; // the variable subcomponent
    subcomponents.put(loc, sub);
  }

  /** Return the subcomponent associated with the given location.
   * For variable arity nodes, this method should be overridden
   * in language specific subclasses.
   * @param loc
   * The location associated with this subcomponent.
   */
  public ISubcomponent getSubcomponent(IRLocation loc) {
    if (subcomponents == null) return null;
    ISubcomponent sub = subcomponents.get(loc);
    return sub;
  }

  protected Collection<ComponentNode> componentNodes;
  
  void registerComponentNode(ComponentNode node) {
	  if (componentNodes == null) componentNodes = new ArrayList<ComponentNode>();
	  componentNodes.add(node);
  }
  
  /** Return the subcomponent associated with the start
   * and end of the sequence in a variable-arity node.
   * (Override in language specific, node specific component classes.)
   */
  public VariableSubcomponent getVariableSubcomponent() {
    return null;
  }

  /** Return the subcomponent in the component of our parent. */
  public ISubcomponent getSubcomponentInParent() {
	final IRNode parent;
	final IRLocation loc;	
	synchronized (syntax) { //: JTB I don't understand why locking is needed 
		parent = tree.getParent(syntax);
		if (parent == null) {
			return null;
		}
		loc = tree.getLocation(syntax);
	}	
    return getComponent(parent).getSubcomponent(loc);
    
  }

  /** Return the node associated with this component */
  public IRNode getSyntax() {
    return syntax;
  }

  /** Return the start port. */
  public ComponentPort getEntryPort() {
    return entryPort;
  }

  /** Return the normal exit port. */
  public ComponentPort getNormalExitPort() {
    return normalExitPort;
  }

  /** Return the abrupt exit port */
  public ComponentPort getAbruptExitPort() {
    return abruptExitPort;
  }

  /** Verify a component that all pieces are put together correctly.
   * Currently we check simply that no edges are null.
   * We traverse forward from input ports and backward from output ports.
   */
  public boolean isValid() {
    Stack<ControlNode> s = new Stack<ControlNode>();
    // cannot be hashtable because of proxies
    Map<ControlNode,ControlNode> visited = new IdentityHashMap<ControlNode,ControlNode>(); 

    s.push(getEntryPort());
    s.push(getNormalExitPort());
    s.push(getAbruptExitPort());
    outer : while (!s.empty()) {
      ControlNode node = s.pop();
      if (node == null)
        return false;
      /** cannot use contains because of proxies: **/
      /*
      for (int i = visited.size(); i > 0; --i) {
        if (visited.elementAt(i - 1) == node)
          continue outer;
      }
      */
      if (visited.containsKey(node)) {
        continue outer;
      }
        
      visited.put(node, node);
      if (!(node instanceof IInputPort)) {
        ControlEdgeIterator more = node.getInputs();
        while (more.hasNext()) {
          ControlEdge e = more.nextControlEdge();
          if (e == null)
            return false;
          s.push(e.getSource());
        }
      }
      if (!(node instanceof IOutputPort)) {
        ControlEdgeIterator more = node.getOutputs();
        while (more.hasNext()) {
          ControlEdge e = more.nextControlEdge();
          if (e == null)
            return false;
          s.push(e.getSink());
        }
      }
    }
    return true;
  }
}
