/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/IntraproceduralAnalysis.java,v
 * 1.21 2003/08/06 21:16:33 chance Exp $
 */
package edu.uwm.cs.fluid.java.analysis;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.util.IThunk;
import com.surelogic.util.Thunk;

import edu.cmu.cs.fluid.control.*;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.*;
import edu.uwm.cs.fluid.util.Lattice;
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.control.LabeledLattice.LabeledValue;
import edu.cmu.cs.fluid.version.Version;

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
    super();
    binder = b;
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
    final A a = getAnalysis(
        edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis.getFlowUnit(
            node, constructorContext));
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
  private IntraproceduralAnalysisCache<T, L, A> cache =
    new IntraproceduralAnalysisCache<T, L, A>(null, null, null);

  public void clear() {
    cache = new IntraproceduralAnalysisCache<T, L, A>(null, null, null);
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
    IntraproceduralAnalysisCache<T, L, A> c = cache;
    int cached = 0;
    while (c.flowUnit != flowUnit || c.version != v) {
      // System.out.println("Rejecting " + c.toString());
      c = c.getNext();
      if (c == cache) { // back to the front; no analysis found
        FlowUnit op = (FlowUnit) tree.getOperator(flowUnit);
        A fa = createAnalysis(flowUnit);
        fa.initialize(op.getSource(flowUnit));
        fa.initialize(op.getNormalSink(flowUnit));
        fa.initialize(op.getAbruptSink(flowUnit));
        c = new IntraproceduralAnalysisCache<T, L, A>(flowUnit, v, fa);

        try {
          if (debug) {
            LOG.fine("Performing " + c.toString() + " ...");
          }
          v.clamp();
          fa.performAnalysis();
          //printAllAnalysisResults(fa,flowUnit);
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

  public final IThunk<A> getAnalysisThunk(final IRNode flowUnit) {
    return new Thunk<A>() {
      @Override
      protected A evaluate() { return getAnalysis(flowUnit); }
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
    Component cfgComp = JavaComponentFactory.prototype.getComponent(node);
    if (cfgComp.getEntryPort() instanceof BlankInputPort) return;
    System.out.println("\nNode: " + DebugUnparser.toString(node));
    //System.out.println(" Entry port and dual are " + cfgComp.getEntryPort() + " " + cfgComp.getEntryPort().getDual());
    printAnalysisResults(fa, cfgComp.getEntryPort().getInputs(), "Entry");
    printAnalysisResults(fa, cfgComp.getNormalExitPort().getOutputs(), "Normal exit");
    printAnalysisResults(fa, cfgComp.getAbruptExitPort().getOutputs(), "Abrupt exit");
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
