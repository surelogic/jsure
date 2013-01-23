/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/IntraproceduralAnalysis.java,v
 * 1.21 2003/08/06 21:16:33 chance Exp $
 */
package edu.uwm.cs.fluid.java.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.RequiresLock;
import com.surelogic.common.Pair;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.util.IThunk;
import com.surelogic.util.Thunk;

import edu.cmu.cs.fluid.control.BlankInputPort;
import edu.cmu.cs.fluid.control.Component;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.control.ControlEdgeIterator;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.Arguments;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassBodyDeclInterface;
import edu.cmu.cs.fluid.java.operator.FlowUnit;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodBody;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.version.Version;
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.control.LabeledLattice.LabeledValue;
import edu.uwm.cs.fluid.util.Lattice;

/**
 * A general purpose framework for analysis of a single method. It
 * caches recent analyses (which are kept in an LRU
 * queue).
 * This approach uses lattice poisoning and requires a postpass to find problems.
 */
public abstract class IntraproceduralAnalysis<T, L extends Lattice<T>, A extends FlowAnalysis<T, L>> {
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
	  this(b, false);
  }
  
  protected IntraproceduralAnalysis(IBinder b, boolean useMapCache) {
    super();
    binder = b;

    this.useMapCache = useMapCache;
    if (useMapCache) {
    	mapCache = new HashMap<Pair<IRNode,Version>, A>();
    	cache = null;
    } else {
    	mapCache = null;    
    	cache = new IntraproceduralAnalysisCache<T, L, A>(null, null, null);
    }
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
    final IRNode flowUnit = getFlowUnit(node);
    if (InitDeclaration.prototype.includes(flowUnit) && context != null) {
      return context;
    } else {
      return flowUnit;
    }
  }
  
  /** return the FlowUnit node that includes this node's component. */
  @SuppressWarnings("null") // for last2Op
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
          return JavaPromise.getClassInitOrNull(classDecl);
        } else {
          return JavaPromise.getInitMethodOrNull(classDecl);
        }
      }
      last2Op = lastOp;
      lastOp = op;
    }
    return null;
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
  private final T getAfter(final IRNode node, final IRNode constructorContext, final WhichPort port) {
    final A a = getAnalysis(getFlowUnit(node, constructorContext));
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
  public final T getAnalysisResultsBefore(
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
  public final T getAnalysisResultsAfter(
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
  public final T getAnalysisResultsAbrupt(
      final IRNode node, final IRNode constructorContext) {
    return getAfter(node, constructorContext, WhichPort.ABRUPT_EXIT);
  }
  
  /*
	 * Start with a cache with a sentinel. This element will eventually be
	 * evicted since it is never used.
	 */
  private IntraproceduralAnalysisCache<T, L, A> cache; 

  private final HashMap<Pair<IRNode, Version>, A> mapCache;
  
  private final boolean useMapCache;
	  
  public void clear() {
	  if (useMapCache) {
		  synchronized (mapCache) {
			  mapCache.clear();
		  }
	  } else {
		  cache = new IntraproceduralAnalysisCache<T, L, A>(null, null, null);
	  }
  }
  
  /**
	 * Return analysis done for a particular method. If it has not yet been
	 * computed, analysis is performed.
   * @param flowUnit The flow unit whose analysis component should be 
   * returned.  It is assumed the flow unit has already been corrected for
   * the correct constructor context: see
   * {@link edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis#getFlowUnit(IRNode, IRNode)}.
	 */
  public final A getAnalysis(IRNode flowUnit) {
    final boolean debug = LOG.isLoggable(Level.FINE);
    
    Version v = Version.getVersion();
    if (useMapCache) {
    	final A fa;
    	synchronized (mapCache) {
    	  final Pair<IRNode, Version> key = Pair.getInstance(flowUnit, v);
    		A temp = mapCache.get(key);
    		if (temp == null) {
    			// Start with uncomputed analysis
    			temp = createAnalysis(flowUnit);
    			mapCache.put(key, temp);
    		}
    		fa = temp;
		}    
    	synchronized (fa) {
        	// Compute if necessary
    		if (!fa.isComputed()) {
    			computeAnalysis(flowUnit, v, debug, fa);
    			fa.setComputed();
    		}
    	}
    	return fa;
    }    
    IntraproceduralAnalysisCache<T, L, A> c = cache;
    int cached = 0;
    while (c.flowUnit != flowUnit || c.version != v) {
      // System.out.println("Rejecting " + c.toString());
      c = c.getNext();
      if (c == cache) { // back to the front; no analysis found
    	A fa = computeAnalysis(flowUnit, v, debug);
        c = new IntraproceduralAnalysisCache<T, L, A>(flowUnit, v, fa);
        
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

  private A computeAnalysis(final IRNode flowUnit, final Version v, final boolean debug) {
	  A fa = createAnalysis(flowUnit);
	  synchronized (fa) {
		  return computeAnalysis(flowUnit, v, debug, fa);
	  }
  }
  
  /**
   * @param fa expected to be newly created and never used
   */
  @RequiresLock("fa:ComputeLock")
  private A computeAnalysis(final IRNode flowUnit, final Version v, final boolean debug, final A fa) {	  
	  final FlowUnit op = (FlowUnit) tree.getOperator(flowUnit);
	  final JavaComponentFactory factory = JavaComponentFactory.startUse();	  
	  fa.initialize(op.getSource(flowUnit, factory));
	  fa.initialize(op.getNormalSink(flowUnit, factory));
	  fa.initialize(op.getAbruptSink(flowUnit, factory));
 
      try {
        if (debug) {
          LOG.fine("Performing " + toString(v, fa, flowUnit) + " ...");
        }
        v.clamp();
        fa.performAnalysis();
        //printAllAnalysisResults(fa,flowUnit);
      } catch (SlotUndefinedException e) {
        LOG.log(Level.SEVERE, "Got exception", e);
      } finally {
        v.unclamp();
        JavaComponentFactory.finishUse(factory);
      }
      if (debug) {
        LOG.fine(" (" + fa.getIterations() + " iterations)");
      }
      return fa;
  }
  
  public final IThunk<A> getAnalysisThunk(final IRNode flowUnit) {
    return new Thunk<A>() {
      @Override
      protected A evaluate() {
        return getAnalysis(flowUnit); }
    };
  }
  
  protected void printAllAnalysisResults(A fa, IRNode body) {
    for (IRNode n : JJNode.tree.topDown(body)) {
      if (true) {
        printAnalysisResults(fa, n);
      }
    }
  }

  protected void printAnalysisResults(A fa, IRNode node) {
	final JavaComponentFactory factory = JavaComponentFactory.startUse();
	try {
    Component cfgComp = factory.getComponent(node);
    if (cfgComp.getEntryPort() instanceof BlankInputPort) return;
    System.out.println("\nNode: " + DebugUnparser.toString(node));
    //System.out.println(" Entry port and dual are " + cfgComp.getEntryPort() + " " + cfgComp.getEntryPort().getDual());
    printAnalysisResults(fa, cfgComp.getEntryPort().getInputs(), "Entry");
    printAnalysisResults(fa, cfgComp.getNormalExitPort().getOutputs(), "Normal exit");
    printAnalysisResults(fa, cfgComp.getAbruptExitPort().getOutputs(), "Abrupt exit");
	} finally {
		JavaComponentFactory.finishUse(factory);
	}
  }

  protected void printAnalysisResults(A fa, ControlEdgeIterator edges, String name) {
    for (int i=1; edges.hasNext(); ++i) {
      System.out.print("  " + name + " " + i + ": ");
      printAnalysisResults(fa,(ControlEdge)edges.next());
    }
  }
  
  protected void printAnalysisResults(A fa, ControlEdge e) {
    L l = fa.getLattice();
    LabeledValue<T> rawInfo = fa.getRawInfo(e);
    if (rawInfo == null) {
      System.out.println();
    } else {
      System.out.println(rawInfo.toString(l));
    }
  }

  /**
	 * Create the appropriate flow analysis instance. Any interesting
	 * initialization should be done as well. (The input and output ports will be
	 * initialized in any case.)
   * @param flowUnit The flow unit whose analysis component should be 
   * returned.  It is assumed the flow unit has already been corrected for
   * the correct constructor context: see
   * {@link edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis#getFlowUnit(IRNode, IRNode)}.
	 */
  protected abstract A createAnalysis(IRNode flowUnit);
  
  String toString(Version v, A analysis, IRNode flowUnit) {
	  return v.toString() + " " + analysis.name + IntraproceduralAnalysisCache.flowUnitName(flowUnit);
  }
}

/**
 * A list of recently performed analyses. The last accessed is at the head of
 * the list. The list is circular and doubly linked.
 */
final class IntraproceduralAnalysisCache<T, L extends Lattice<T>, A extends FlowAnalysis<T, L>> {
  final IRNode flowUnit;
  final Version version;
  final A analysis;

  private static SyntaxTreeInterface tree = JJNode.tree;

  private IntraproceduralAnalysisCache<T, L, A> prev = null, next = null;

  IntraproceduralAnalysisCache(IRNode unit, Version v, A a) {
    flowUnit = unit;
    version = v;
    analysis = a;
    prev = next = this;
  }

  IntraproceduralAnalysisCache<T, L, A> getNext() {
    return next;
  }
  IntraproceduralAnalysisCache<T, L, A> getPrev() {
    return prev;
  }

  void unlink() {
    if (prev != this) {
      prev.next = next;
      next.prev = prev;
      prev = next = this;
    }
  }

  void link(IntraproceduralAnalysisCache<T, L, A> before) {
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
