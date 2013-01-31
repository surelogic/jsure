/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/Operator.java,v 1.34 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.tree;

import java.util.Iterator;
import java.util.Hashtable;
import java.util.*;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.*;

/** Operators contain information about the node, essentially
 * identifying nonterminals and production in the abstract grammar.
 * Operators are declared in a hierarchy that corresponds with the
 * Java hierarchy.  Each operator is a class with a single instance
 * (named "prototype").  All operator classes should be concrete,
 * even those representing nonterminals, because they are needed
 * to specify allowable nodes for children.
 *
 * <p> For example, for Java, we expect the following operators
 * (among many others):
 * <pre>
 *    JavaOperator
 *    Expression
 *    BinopExpression
 *    AndExpression
 * </pre>
 * Only the last operator would be used in nodes with children.
 * (The others may appear as operators of place-filling nodes).
 * </p>
 *
 * <p> We have recently added the capability to <em>instantiate</em>
 * an operator with a particular node.  This permits a syntax tree to be
 * traversed where each node has the type of its oeprator.  Essentially
 * an instantiation is a copy of the operator for a particular node. </p>
 *
 * <p> Since operator classes have a specific form, they can
 * be generated from concise descriptions.  See the script
 * in <tt>lib/perl/create-operator</tt>. </p>
 */
public abstract class Operator implements Cloneable {
  /** Return a textual string naming the abstract nonterminal or production
   * @functional
   */
  public abstract String name();

  /** Return the superoperator for this operator.
   * @functional
   */
  public Operator superOperator() {
    return null;
  }

  /** Return the syntax tree type associated with this operator.
   */
  public abstract SyntaxTreeInterface tree();
  
  /** Create a new node with this operator and with the correct
   * shape of children.
   * @postcondition unique(return)
   */
  public abstract IRNode createNode();
  public abstract IRNode createNode(SyntaxTreeInterface tree);

  /** Return true if this operator is the same as this
   * or if it represents a suboperator.
   * @functional
   */
  public boolean includes(Operator other) {
    // System.out.println("Got "+other);
    while (other != null) {
      if (other == this) return true;
      other = other.superOperator();
    }
    return false;
  }
    
  /** Return true if this node represents a "real"
   * node in the abstract syntax or not.
   */
  public boolean isProduction() {
    return false;
  }

  /** Return the class of nodes acceptable to nodes of this operator,
   * or null if a nonterminal or leaf production.
   * @functional
   */
  public Operator childOperator(int i) {
    return null;
  }
  /** Return the class of nodes acceptable to nodes of this operator,
   * or null if a nonterminal or leaf production.
   * @functional
   */
  public Operator childOperator(IRLocation loc) {
    return childOperator(loc.getID());
  }

	public String childLabel(int i) {
		return "";
	}  

	public String infoType(int i) {
		return "";
	} 
	
	public String infoLabel(int i) {
		return "";
	} 

	public int numInfo() {
		return 0;
	}

	/** Return the class of nodes acceptable to nodes of this operator,
   * if it is a node with variable number of children (otherwise null).
   * @functional
   */
  public Operator variableOperator() {
    return null;
  }
  
  /** Return the number of children, or a negative number
   * if the operator takes a variable number of children.
   */
  public int numChildren() {
    return 0;
  }

  /* We make the clone method private to prevent random clone's */
  private Operator privateClone() {
    try {
      return (Operator)super.clone();
    } catch (CloneNotSupportedException e) {
      return null; // never happens
    }
  }
  @Override
  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException("Do not clone operators directly");
  }

  protected IRNode baseNode = null;

  /** Return a copy of an operator for a particular node of the tree.
   * (The method is an instance method so that it can be overridden.)
   */
  public Operator instantiate(IRNode node) {
    return instantiate(tree(), node);
  }
  public Operator instantiate(SyntaxTreeInterface tree, IRNode node) {
    if (node == null) return null;
    Operator copied = tree.getOperator(node).privateClone();
    copied.baseNode = node;
    return copied;
  }

  /** Return an enumeration of nodes, each of which should be instantiated.
   */
  public Iterator<Operator> instantiate(final Iterator<IRNode> enm) {
    return instantiate(tree(),enm);
  } 
  public Iterator<Operator> instantiate(final SyntaxTreeInterface tree,
      final Iterator<IRNode> enm) {
    return new AbstractRemovelessIterator<Operator>() {
      @Override
      public boolean hasNext() { return enm.hasNext(); }
      @Override
      public Operator next() throws NoSuchElementException {
        return instantiate(tree, enm.next());
      }
    };
  }

  public IRNode getBaseNode() {
    return baseNode;
  }

  private static final Hashtable<String, Operator> allOperators = new Hashtable<String, Operator>();

  public Operator() {
    allOperators.put(internalName(),this);
  }
  final String internalName() {
    return getClass().getName();
  }
  static Operator findOperatorInternal(String name) {
    Operator op = allOperators.get(name);
    if (op == null) {
      try {
        Class.forName(name);
	op = allOperators.get(name);
      } catch (ClassNotFoundException e) {
      }
    }
    if (op == null) {
      throw new FluidRuntimeException("Operator not found for " + name);
    }
    return op;
  }

  /** Write information about this specific instance.
   * If all operators of the same class are identical,
   * this method need not do anything.
   * @see #readInstance
   */
  protected void writeInstance(IROutput out)
  {
    // do nothing
  }

  /** Read information about an instance and return an
   * instance with the information.  If all operators of the same
   * class are identical, this method can simply return 'this.'
   * @see #writeInstance
   */
  protected Operator readInstance(IRInput in)
  {
    return this;
  }

  // help for attributes on nodes:

  /** Return set of attributes that may be set for
   * nodes with this operator.
   * @see #getAttribute
   */
  public java.util.Set<SlotInfo> getAttributes() {
    return Collections.emptySet();
  }

  /** Return an attribute given a name.
   * The attribute may or may not be set on nodes with
   * this operator.
   * @return null or slot info for this name.
   * @param name (case insensitive) of attribute.
   */
  public SlotInfo getAttribute(String name) {
    java.util.Iterator<SlotInfo> attrs = getAttributes().iterator();
    while (attrs.hasNext()) {
      SlotInfo attr = attrs.next();
      if (attr.name().equalsIgnoreCase(name)) return attr;
    }
    return null;
  }

  /** Return set of attribute names that may be set for
   * nodes with this operator (for compatibility with Models).
   * @see #getAttribute
   */
  public java.util.Set getAttributeNames() {
    return ImmutableHashOrderSet.empty;
  }

  /** Return an attribute given a name.
   * The attribute may or may not be set on nodes with
   * this operator.
   * @return null or slot info for this name.
   * @param name of attribute.
   */
  public SlotInfo getAttribute(SyntaxTreeInterface tree, String name) {
    return getAttribute(name);
  }

  /** Return true if the node has all children required and
   * if all required attributes are set.
   * This default method only works if no attributes are required.
   */
  public boolean isComplete(IRNode node) {
    return isComplete(tree(), node);
  }

  public boolean isComplete(SyntaxTreeInterface t, IRNode node) {
    int ch = numChildren();
    if (ch < 0) ch = ~ch;
    for (int i=0; i < ch; ++i) {
      if (!t.hasChild(node,i)) return false;
    }
    return true;
  }

  public static final Operator prototype = new Operator() {
		@Override
    public String name() {		  
		  return "Operator.prototype";
		}

		@Override
    public SyntaxTreeInterface tree() {
			return null;
		}
  
		@Override
    public IRNode createNode() {
			throw new NotImplemented("Not intended to be instantiated");
		}

		@Override
    public IRNode createNode(SyntaxTreeInterface tree) {
			throw new NotImplemented("Not intended to be instantiated");		  
		}

		@Override
    public boolean includes(Operator other) { 
		  // TODO: this may break code that doesn't use include
		  return true;
		}
  };
}


