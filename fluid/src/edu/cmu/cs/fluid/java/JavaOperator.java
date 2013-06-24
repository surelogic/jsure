/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/JavaOperator.java,v 1.30
 * 2003/09/25 20:09:08 chance Exp $
 */
package edu.cmu.cs.fluid.java;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.control.Component;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.operator.ParenExpression;
import edu.cmu.cs.fluid.parse.Ellipsis;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.parse.JJOperator;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.unparse.Keyword;
import edu.cmu.cs.fluid.unparse.OpenClose;
import edu.cmu.cs.fluid.unparse.Token;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

/**
 * The operator class for all operators used for Java IRNodes. It contains a
 * hashtable to map operator names to prototypes.
 * 
 * @see JavaNode
 */
public class JavaOperator extends JJOperator {
  /** Logger instance for debugging. */
  protected static final Logger LOG = SLLogger.getLogger("FLUID.java.operator");

  /**
	 * A hashtable from strings to operator names. 
	 */
  static Map<String,JavaOperator> operatorTable = new Hashtable<String,JavaOperator>();
  
  /**
   * a Map from operator to Collection direct children
   */
  static Map<Operator,Collection<Operator>> childrenMap = new HashMap<Operator,Collection<Operator>>();
  
  /**
	 * Compute a name for an operator. By default this is the class name (with
	 * all prefixes removed).
	 */
  @Override
  public String name() {
    String complete = getClass().getName();
    return complete.substring(complete.lastIndexOf('.') + 1);
  }

  public JavaOperator() {
    // System.out.println("Loaded " + name());

    String n = name();
    operatorTable.put(n, this);
    
    Operator sop = superOperator();
    synchronized (childrenMap) {
    	if (sop != null) {
    		//Works for Operators that have a superOperator() and
    		//also for Operators who have children
    		Collection<Operator> c = childrenMap.get(sop);
    		if (c == null) {
    			c = new Vector<Operator>();
    			childrenMap.put(sop, c);
    		}
    		c.add(this);
    	}

    	//Works for Operators without a superOperator() and
    	//without children (top-level ops with no children)
    	Collection<Operator> m = childrenMap.get(this);
    	if (m == null){
    		childrenMap.put(this, null);
    	}
    }
  }

  /**
   * @return An iterator over all the JavaOperators (in no particular order)
   */
  public static Iterator<Operator> allOperators() {
    return childrenMap.keySet().iterator();
  }
  
  public Iterator<Operator> subOperators() {
  	Collection<Operator> c = new ArrayList<Operator>();
  	subOperators(this, c);
  	return c.iterator();
  }
  
  private void subOperators(Operator op, Collection<Operator> c) {
    Collection<Operator> children = childrenMap.get(this);
    if (children == null) {
      return;
    }
    
		c.addAll(children);

    Iterator<Operator> it = children.iterator();
    while (it.hasNext()) {
    	Operator cop = it.next();
    	if (!c.contains(cop)) {
      	subOperators(cop, c);
    	}
    }
		
  }
  
  public static JavaOperator findOperator(String name) {
    JavaOperator op = operatorTable.get(name);
    if (op == null) {
      throw new FluidRuntimeException("No operator " + name + " loaded");
    }
    return op;
  }

  /**
	 * A routine called indirectly by the parser and which creates a JavaNode for
	 * this operator
	 * 
	 * @see JJOperator#createNode
	 */
  public final IRNode jjtCreate() {
    return JavaNode.makeJavaNode(this);
  }
  
  public final IRNode createNode(SyntaxTreeInterface tree, Operator op, IRNode[] children) {
    return JavaNode.makeJavaNode(tree, op, children);
  }
  
  public final IRNode createNode(Operator op, IRNode[] children) {
    return JavaNode.makeJavaNode(JJNode.tree, op, children);
  }

  /**
	 * This method is called to create a control-flow graph component for the
	 * node with this operator. This method is not called more than once for
	 * every IRNode. It should be overridden by nodes that need to create
	 * control-flow graph components.
	 */
  public Component createComponent(IRNode node) {
    // by default: return a CFG component which must never be executed.
    return new Component(node, 0, 0, 0);
  }

  protected static final Token OPENTOKEN = new OpenClose(true);
  protected static final Token CLOSETOKEN = new OpenClose(false);

  /**
	 * This method is called to unparse a node. It is overridden by hand-written
	 * code to perform special actions around the basic unparsing actions.
	 * 
	 * @see #unparse
	 */
  public void unparseWrapper(IRNode node, JavaUnparser u) {
    OPENTOKEN.emit(u, node);
    JavaPromise.unparsePromises(node, u);
    if (JavaNode.getModifier(node, JavaNode.HAS_PARENS)) {
    	// Modified from ParenExpression
        ParenExpression.openParen().emit(u,node);
        u.getStyle().getPAREN().emit(u,node);
        u.unparse(tree.getChild(node,0));
        u.getStyle().getENDPAREN().emit(u,node);
        ParenExpression.closeParen().emit(u,node);
    } else {
    	unparse(node, u);
    }
    CLOSETOKEN.emit(u, node);
  }

  /**
	 * This method is called to perform the basic unparsing action for a node. It
	 * is overridden by each production with automatically generated code that
	 * does not call super. In order to insert hand-written changes around
	 * unparsings for particular nodes, one must override the "wrapper" method.
	 * 
	 * @see #unparseWrapper
	 */
  public void unparse(IRNode node, JavaUnparser u) {
    /* by default, do nothing */
  }

  // UNPARSE TOKENS
  public boolean isMissingTokensWrapper(IRNode node) {
    return isMissingTokens(node);
  }

  public boolean isMissingTokens(IRNode node) {
    return false; // by default
  }

  public Vector<Token>[] missingTokensWrapper(IRNode node) {
    return missingTokens(node);
  }

  //private static final Vector<Token>[] noVectors = new Vector[0];
  
  public Vector<Token>[] missingTokens(IRNode node) {
    return null; // by default
  }

  private static Token defaultToken = new Keyword("<operator>");

  public Token asToken() {
    return defaultToken;
  }

  @Override
  public boolean includes(Operator other) {
    return super.includes(other)
      || (other == Ellipsis.prototype)
      || (this == JavaOperator.prototype);
  }
  
  public boolean includes(IRNode node) {
	if (node == null) {
		return false;
	}
    return includes(JJNode.tree.getOperator(node));
  }

  // attributes:

  private static Set<SlotInfo> attributes = null;

  @Override
  @SuppressWarnings("unchecked")
  public Set<SlotInfo> getAttributes() {
    if (attributes == null) {
      // something of a hack since these are private:
      Bundle here = JavaNode.getBundle();
      int num_here = here.getNumAttributes();

      Object[] a = new Object[num_here + 1];
      for (int i = 0; i < num_here; ++i) {
        SlotInfo si = here.getAttribute(i + 1);
        a[i] = new RootNamedSlotInfoWrapper(si);
      }

      try {
        SlotInfo nodeInfo = SlotInfo.findSlotInfo("JJNode.info");
        a[num_here] = new RootNamedSlotInfoWrapper(nodeInfo);
      } catch (SlotNotRegisteredException ex) {
        throw new FluidError("JJNode.info not registered");
      }

      attributes = new ImmutableHashOrderSet(a);
    }
    return attributes;
  }

  public static final JavaOperator prototype = new JavaOperator();
}

class RootNamedSlotInfoWrapper<T> extends SlotInfoWrapper<T> {
  private final String name;
  public RootNamedSlotInfoWrapper(SlotInfo<T> si) {
    super(si);
    name = root(si.name());
  }

  private static String root(String name) {
    int afterDot = name.lastIndexOf('.');
    return name.substring(afterDot + 1);
  }

  @Override
  public String name() {
    return name;
  }
}
