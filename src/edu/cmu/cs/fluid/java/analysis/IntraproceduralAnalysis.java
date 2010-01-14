/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/IntraproceduralAnalysis.java,v
 * 1.21 2003/08/06 21:16:33 chance Exp $
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.control.*;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.*;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.version.Version;

/**
 * A general purpose framework for analysis of a single method. It 
 * caches recent analyses (which are kept in an LRU queue).
 */
public abstract class IntraproceduralAnalysis<T,V> {
  /** Logger instance for debugging. */
  protected static final Logger LOG = SLLogger.getLogger("FLUID.analysis.flow");

  /* The maximum number of analyses cached. */
  public static final int maxCached = 40;

  /**
	 * A mapping from names to declarations, made public for convenience.
	 */
  public final IBinder binder;

  public static final SyntaxTreeInterface tree = JJNode.tree;

  /** Allocate an anonymous analysis. */
  protected IntraproceduralAnalysis(IBinder b) {
    super();
    binder = b;
  }

  /**
   * Get the flow unit that contains the given node, corrected to current
   * constructor context if necessary. Specifically, if the given node turns out
   * to be inside an instance field initializer or an instance initialization
   * block, then {@value context} is returned instead of the actual flow unit
   * (which would be in the InitDeclaration pseudo-method). This allows the node
   * to be properly integrated into the flow control because the flow graph for
   * every constructor includes the flow through the instance field initializers
   * and instance initialization blocks, so we need to use the flow graph for
   * the correct constructor.
   * 
   * <p>
   * If {@value context} null} is {@value null} or the node is not in an
   * instance field initializer or instance initializer block then this method
   * returns the same value as {@code getRawFlowUnit(node)}.
   * 
   * <p>
   * The one instance where we actually want to use the InitDeclaration as the
   * flow unit is when analyzing the constructor of an anonymous class
   * expression. In this case there is exactly one unnamed constructor, and
   * there is no other flow unit that makes sense to use at this point. The
   * caller of this method is responsible for supplying a null} context in this
   * situation.
   * 
   * @param node
   *          The node whose flow unit should be returned.
   * @param context
   *          The ConstructorDeclaration of the constructor currently being
   *          analyzed, or <code>null</code> if no constructor is currently
   *          being analyzed.
   * @return The flow unit that contains the given node, as described above.
   */
  public static IRNode getFlowUnit(final IRNode node, final IRNode context) {
    final IRNode flowUnit = getRawFlowUnit(node);
    if (InitDeclaration.prototype.includes(flowUnit) && context != null) {
      return context;
    } else {
      return flowUnit;
    }
  }

  // XXX Rename getFlowUnit(IRNode) to getRawFlowUnit(IRNode) when done
  public static IRNode getRawFlowUnit(final IRNode n) {
    return getFlowUnit(n);
  }
  
  /** return the FlowUnit node that includes this node's component. */
  public static IRNode getFlowUnit(final IRNode n) {
    /* We have a problem: The ClassBodyDeclInterface test below triggers a
     * match for anonymous class expressions.  This is not what we want if our 
     * starting node 'n' is the anonymous class expression.  In that case, we
     * still want the flow unit that 'n' is a part of, not the flow unit that 'n'
     * is.  So if 'n' is an AnonClassExpression, we start the root walk from the 
     * parent of 'n'.  
     * 
     * We also need to check if 'n' is part of an argument to the ACE
     * Note that we only want to skip the first ACE
     * 
     * We search until we hit a FlowUnit (hopefully a method or constructor
     * declaration) or a ClassBodyDeclInterface.  The latter happens when 'n'
     * occurs in a field declaration or a class/instance initializer block.  When
     * that happens we return the appropriate promise node for the single 
     * Class initializer method or instance initializer method.
     */
    boolean skippedACE = false;
    IRNode start = n;
    if (AnonClassExpression.prototype.includes(n)) {
      start = JJNode.tree.getParent(n);
      skippedACE = true;
    }

    final Iterator<IRNode> e = tree.rootWalk(start);
    Operator lastOp = null;
    Operator last2Op = null;
    while (e.hasNext()) {
      final IRNode node = e.next();
      final Operator op = tree.getOperator(node);
      if (op instanceof FlowUnit)
        return node;
      if (op instanceof ClassBodyDeclInterface) {
        if (!skippedACE && AnonClassExpression.prototype.includes(op)) {
          // Check if we're part of the arguments to the ACE
          if (!NewExpression.prototype.includes(lastOp)
              || !(last2Op == null || Arguments.prototype.includes(last2Op))) {
            LOG.warning("Trying to get flow unit from ACE's " + lastOp.name()
                + ", " + last2Op.name());
          }
          skippedACE = true;
          last2Op = lastOp;
          lastOp = op;
          continue;
        }

        final IRNode classDecl = tree.getParent(tree.getParent(node));
        final Operator cdOp = tree.getOperator(classDecl);
        final boolean isInterface =
          InterfaceDeclaration.prototype.includes(cdOp);
        if (JavaNode.getModifier(node, JavaNode.STATIC) || isInterface) {
          /*
           * We found a static field/method in a class or an (implicitly) static
           * field in an interface.
           */
          return ClassInitDeclaration.getClassInitMethod(classDecl);
        } else {
          return InitDeclaration.getInitMethod(classDecl);
        }
      }
      last2Op = lastOp;
      lastOp = op;
    }
    return null;
  }

  /**
	 * Enumerate the tree nodes within a flow graph. It omits nodes within
	 * nested flow units. This enumeration is not protected from changes; it is
	 * intended only to be used side-effect free analyses.
	 */
  public static Iterator<IRNode> bottomUp(IRNode decl) {
    return new JavaPromiseTreeIterator(decl, true) {
      // NB: mark is *not* called on the first node.
      @Override
      protected boolean mark(IRNode node) {
        if (node == null)
          return false;
        Operator op = tree.getOperator(node);
        if (op instanceof MethodDeclaration
          || op instanceof ConstructorDeclaration
          || op instanceof ClassBody)
          return false;
        return true;
      }
    };
  }
  
  /**
	 * Create an array of all locals accessible in this fluid unit. We include
	 * those in nested blocks, but not those in nested method bodies.
	 * Constructors include locals mentioned in the instance initializer. NB:
	 * This method may return locals that do <em>not</em> actually occur in the
	 * flow unit, but it makes an effort to include all those which <em>are</em>
	 * in the flow unit.
	 */
  public static IRNode[] flowUnitLocals(IRNode flowNode, 
		  boolean keepPrimitiveVars, IBinder binder) {
    FlowUnit op = (FlowUnit) tree.getOperator(flowNode);
    // System.out.println("Getting locals for a " + op);
    Vector<IRNode> locals = new Vector<IRNode>();
    // first add all those inside:
    for (Iterator<IRNode> e = bottomUp(flowNode); e.hasNext();) {
      IRNode node = e.next();
      Operator op2 = tree.getOperator(node);
      if (op2 instanceof VariableDeclarator || op2 instanceof ParameterDeclaration) {
    	  if (keepPrimitiveVars || binder.getJavaType(node) instanceof IJavaReferenceType) {
    		  locals.addElement(node);
    	  } else {
    		  //System.out.println("Rejecting "+DebugUnparser.toString(node));
    	  }
      }
    }

    // then grab receiver or return values
    IRNode temp = null;
    temp = JavaPromise.getReturnNodeOrNull(flowNode);
    if (temp != null) {
    	if (keepPrimitiveVars || binder.getJavaType(temp) instanceof IJavaReferenceType) {
    		locals.addElement(temp);
  	  } else {
		  //System.out.println("Rejecting "+DebugUnparser.toString(temp));
	  }
    }
    temp = JavaPromise.getReceiverNodeOrNull(flowNode);
    if (temp != null) {
      locals.addElement(temp);
    }
    if (op instanceof ClassBody) {
      // XXX: Where is the receiver declaration?
      temp = JavaPromise.getReceiverNodeOrNull(tree.getParent(flowNode));
      if (temp == null) {
        for (IRNode d : tree.children(flowNode)) {
          if (tree.getOperator(d) instanceof ConstructorDeclaration) {
            temp = JavaPromise.getReceiverNode(d);
            break;
          }
        }
      }
      if (temp != null) {
        locals.addElement(temp);
        LOG.fine("Added 'this' to instance initializer locals: " + temp);
      } 
    }

    // then, if we are a constructor, we append those locals
    // from the instance initializer
    // (flow node ClassBody)
    if (op instanceof ConstructorDeclaration) {
      IRNode[] moreLocals = flowUnitLocals(tree.getParent(flowNode), keepPrimitiveVars, binder);
      locals.ensureCapacity(locals.size() + moreLocals.length);
      for (int i = 0; i < moreLocals.length; ++i) {
        locals.addElement(moreLocals[i]);
      }
    }
    // similarly for the two kinds of initializer promises:
    if (op instanceof ClassInitDeclaration || op instanceof InitDeclaration) {
      IRNode classdecl = JavaPromise.getPromisedFor(flowNode);
      IRNode[] moreLocals = flowUnitLocals(VisitUtil.getClassBody(classdecl), keepPrimitiveVars, binder);
      locals.ensureCapacity(locals.size() + moreLocals.length);
      for (int i = 0; i < moreLocals.length; ++i) {
        locals.addElement(moreLocals[i]);
      }
    }

    // Conditions are always relative to something:
    if (op instanceof Condition) {
      IRNode pnode = tree.getParentOrNull(flowNode);
      if (pnode == null)
        pnode = JavaPromise.getPromisedFor(flowNode);
      Operator pop = tree.getOperator(pnode);
      if (pop instanceof InvariantDeclaration) {
        IRNode cnode = JavaPromise.getPromisedFor(pnode);
        IRNode cinode = JavaPromise.getClassInitMethod(cnode);
        locals.addElement(JavaPromise.getReceiverNode(cinode));
      } else { // precondition, postcondition, or throw condition
        if (Type.prototype.includes(pop)) {
          pnode = tree.getParent(tree.getParent(pnode));
          pop = tree.getOperator(pnode);
        }
        IRNode params;
        if (pop instanceof MethodDeclaration) {
          params = MethodDeclaration.getParams(pnode);
        } else if (pop instanceof ConstructorDeclaration) {
          params = ConstructorDeclaration.getParams(pnode);
        } else {
          throw new FluidError("unknown Condition parent " + pop);
        }
        IRNode receiverNode = JavaPromise.getReceiverNode(pnode);
        if (receiverNode != null) {
          locals.addElement(receiverNode);
        }
        for (Iterator<IRNode> e = tree.children(params); e.hasNext();) {
        	IRNode n = e.next();        
        	if (keepPrimitiveVars || binder.getJavaType(n) instanceof IJavaReferenceType) {
        		locals.addElement(n);
      	  } else {
    		  //System.out.println("Rejecting "+DebugUnparser.toString(n));
    	  }
        }
      }
    }

    // make sure we don't have duplicates
    Set<IRNode> localSet = new HashSet<IRNode>(locals);
    // create an array and return:
    IRNode[] localArray = new IRNode[localSet.size()];
    localSet.toArray(localArray);

    for (IRNode x : localArray) {
      if (x == null) throw new NullPointerException("Got a null!");
    }
    return localArray;
  }

  /**
	 * Return the analysis results after a particular port of the component for
	 * the node. If the node isn't evaluated, this method returns null.
   * 
   * @param constructorContext
   *          The constructor declaration, if any, that is currently being
   *          analyzed. if non-<code>null</code>, this is used as the flow unit
   *          if it turns out that the node <code>node</code> is part of an
   *          instance field initializer or instance initialization block.
	 */
  private final Lattice<T> getAfter(
      final IRNode node, final IRNode constructorContext, final WhichPort port) {
    final FlowAnalysis<T> a = getAnalysis(getFlowUnit(node, constructorContext));
    return a == null ? null : a.getAfter(node, port);
  }

  /**
	 * Get the analysis results for a particular node. We fetch a completed
	 * analysis for the current version for the method including the node passed.
	 * Then we find the analysis information on the control-flow edge entering
	 * the control-flow component for the node. This value is returned.
	 * 
   * @param constructorContext
   *          The constructor declaration, if any, that is currently being
   *          analyzed. if non-<code>null</code>, this is used as the flow unit
   *          if it turns out that the node <code>node</code> is part of an
   *          instance field initializer or instance initialization block.
	 */
  public final Lattice<T> getAnalysisResultsBefore(
      final IRNode node, final IRNode constructorContext) {
    return getAfter(node, constructorContext, WhichPort.ENTRY);
  }

  /**
	 * Get the analysis results for a particular node. We fetch a completed
	 * analysis for the current version for the method including the node passed.
	 * Then we find the analysis information on the (first)control-flow edge
	 * exiting the control-flow component for the node. This value is returned.
   * 
   * @param constructorContext
   *          The constructor declaration, if any, that is currently being
   *          analyzed. if non-<code>null</code>, this is used as the flow unit
   *          if it turns out that the node <code>node</code> is part of an
   *          instance field initializer or instance initialization block.
	 */
  public final Lattice<T> getAnalysisResultsAfter(
      final IRNode node, final IRNode constructorContext) {
    return getAfter(node, constructorContext, WhichPort.NORMAL_EXIT);
  }

  /**
	 * Get the analysis results for a particular node. We fetch a completed
	 * analysis for the current version for the method including the node passed.
	 * Then we find the analysis information on the (first)control-flow edge
	 * abruptly exiting the control-flow component for the node. This value is returned.
   * 
   * @param constructorContext
   *          The constructor declaration, if any, that is currently being
   *          analyzed. if non-<code>null</code>, this is used as the flow unit
   *          if it turns out that the node <code>node</code> is part of an
   *          instance field initializer or instance initialization block.
	 */
  public final Lattice<T> getAnalysisResultsAbrupt(
      final IRNode node, final IRNode constructorContext) {
    return getAfter(node, constructorContext, WhichPort.ABRUPT_EXIT);
  }

  // ========================================================================
  // === Temporary
  // ========================================================================

//  public final Lattice<T> getAnalysisResultsBefore(final IRNode node) {
//    return getAfter(node, null, WhichPort.ENTRY);
//  }
//
//  public final Lattice<T> getAnalysisResultsAfter(final IRNode node) {
//    return getAfter(node, null, WhichPort.NORMAL_EXIT);
//  }
//
//  public final Lattice<T> getAnalysisResultsAbrupt(final IRNode node) {
//    return getAfter(node, null, WhichPort.ABRUPT_EXIT);
//  }

  // ========================================================================
  // === End Temporary
  // ========================================================================

  /*
	 * Start with a cache with a sentinel. This element will eventually be
	 * evicted since it is never used.
	 */
  private IntraproceduralAnalysisCache<T> cache =
    new IntraproceduralAnalysisCache<T>(null, null, null);

  /**
	 * Information for debugging.
	 */
  public static String flowUnitName(IRNode flowUnit) {
    if (flowUnit == null)
      return " sentinel";
    FlowUnit op = (FlowUnit) tree.getOperator(flowUnit);
    String name;
    if (op instanceof MethodBody) { //! anachronism
      name = " for " + JJNode.getInfo(tree.getParent(flowUnit));
    } else if (op instanceof MethodDeclaration) {
      name = " for " + JJNode.getInfo(flowUnit);
    } else if (op instanceof ClassBody) {
      name = " for instances of " + JJNode.getInfo(tree.getParent(flowUnit));
    } else if (op instanceof TypeDeclInterface) {
      name = " for " + JJNode.getInfo(flowUnit);
    } else {
      name = "";
    }
    return name;
  }

  public void clear() {
    cache = new IntraproceduralAnalysisCache<T>(null, null, null);
  }
  
  /**
	 * Return analysis done for a particular method. If it has not yet been
	 * computed, analysis is performed.  
	 * @param flowUnit The flow unit whose analysis component should be 
	 * returned.  It is assumed the flow unit has already been corrected for
	 * the correct constructor context: see {@link #getFlowUnit(IRNode, IRNode)}.
	 */
  public final FlowAnalysis<T> getAnalysis(IRNode flowUnit) {
    final boolean debug = LOG.isLoggable(Level.FINE);
    
    Version v = Version.getVersion();
    IntraproceduralAnalysisCache<T> c = cache;
    int cached = 0;
    while (c.flowUnit != flowUnit || c.version != v) {
      // System.out.println("Rejecting " + c.toString());
      c = c.getNext();
      if (c == cache) { // back to the front; no analysis found
        FlowUnit op = (FlowUnit) tree.getOperator(flowUnit);
        FlowAnalysis<T> fa = createAnalysis(flowUnit);
        fa.initialize(op.getSource(flowUnit).getOutput());
        fa.initialize(op.getNormalSink(flowUnit).getInput());
        fa.initialize(op.getAbruptSink(flowUnit).getInput(),
		      LabelList.empty.addLabel(UnknownLabel.prototype),
		      fa.getLattice().top());
        c = new IntraproceduralAnalysisCache<T>(flowUnit, v, fa);

        try {
          if (debug) {
            LOG.fine("Performing " + c.toString() + " ...");
          }
          v.clamp();
          fa.performAnalysis();
        } catch (SlotUndefinedException e) {
          LOG.log(Level.SEVERE, "Got exception", e);
        } finally {
          v.unclamp();
        }
        if (debug) {
          LOG.fine(" (" + fa.getIterations() + " iterations)");
        }
        if (cached >= maxCached) {
          // System.out.println("Flushing " + cache.getPrev().toString());
          cache.getPrev().unlink();
        }
        break;
      }
      ++cached;
    }
    if (c != cache) { // put in front.
      c.unlink();
      c.link(cache);
      cache = c;
    }
    // System.out.println("Found " + c.toString());
    return c.analysis;
  }

  /**
	 * Create the appropriate flow analysis instance. Any interesting
	 * initialization should be done as well. (The input and output ports will be
	 * initialized in any case.)
   * @param flowUnit The flow unit whose analysis component should be 
   * returned.  It is assumed the flow unit has already been corrected for
   * the correct constructor context: see {@link #getFlowUnit(IRNode, IRNode)}.
	 */
  protected abstract FlowAnalysis<T> createAnalysis(IRNode flowUnit);
}

/**
 * A list of recently performed analyses. The last accessed is at the head of
 * the list. The list is circular and doubly linked.
 */
final class IntraproceduralAnalysisCache<T> {
  final IRNode flowUnit;
  final Version version;
  final FlowAnalysis<T> analysis;

  private static SyntaxTreeInterface tree = JJNode.tree;

  private IntraproceduralAnalysisCache<T> prev = null, next = null;

  IntraproceduralAnalysisCache(IRNode unit, Version v, FlowAnalysis<T> a) {
    flowUnit = unit;
    version = v;
    analysis = a;
    prev = next = this;
  }

  IntraproceduralAnalysisCache<T> getNext() {
    return next;
  }
  IntraproceduralAnalysisCache<T> getPrev() {
    return prev;
  }

  void unlink() {
    if (prev != this) {
      prev.next = next;
      next.prev = prev;
      prev = next = this;
    }
  }

  void link(IntraproceduralAnalysisCache<T> before) {
    if (before != null) {
      prev = before.prev;
      next = before;
      prev.next = this;
      next.prev = this;
    }
  }

  /**
	 * Information for debugging.
	 */
  public static String flowUnitName(IRNode flowUnit) {
    if (flowUnit == null)
      return " sentinel";
    FlowUnit op = (FlowUnit) tree.getOperator(flowUnit);
    String name = "";
    try {
      if (op instanceof MethodBody) {
        name = " for " + JJNode.getInfo(tree.getParent(flowUnit));
      } else if (op instanceof ClassBody) {
        name =
          " for instances of " + JJNode.getInfo(tree.getParent(flowUnit));
      } else if (op instanceof TypeDeclInterface) {
        name = " for " + JJNode.getInfo(flowUnit);
      } else if (op instanceof SomeFunctionDeclaration) {
        name = " for " + JavaNames.genMethodConstructorName(flowUnit);
      } else {
        name = " for " + DebugUnparser.toString(flowUnit);
      }
      return name;
    } catch (Exception e) {
      return "<exception " + e.getMessage() + ">";
    }
  }

  @Override
  public String toString() {
    if (version == null && analysis == null && flowUnit == null)
      return "sentinel";
    return version.toString() + " " + analysis.name + flowUnitName(flowUnit);
  }
}
