/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/IntraproceduralAnalysis.java,v
 * 1.21 2003/08/06 21:16:33 chance Exp $
 */
package edu.uwm.cs.fluid.java.analysis;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.control.*;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.*;
import edu.uwm.cs.fluid.util.Lattice;
import edu.uwm.cs.fluid.control.FlowAnalysis;
import edu.uwm.cs.fluid.control.IFlowAnalysis;
import edu.uwm.cs.fluid.control.LabeledLattice.LabeledValue;
import edu.cmu.cs.fluid.version.Version;

/**
 * A general purpose framework for analysis of a single method. It provides a
 * SlotInfo interface and also caches recent analyses (which are kept in an LRU
 * queue).
 * This approach uses lattice poisoning and requires a postpass to find problems.
 */
public abstract class IntraproceduralAnalysis<T> extends DerivedSlotInfo<T> {
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
	 * Allocate a register a new analysis by name.
	 * 
	 * @param name
	 *          The name under which to register the slots.
	 * @exception SlotAlreadyRegisteredException
	 *              If slots have already been registered under this name.
	 *              @precondition nonNull(name)
	 */
  public IntraproceduralAnalysis(String name, IRType<T> type, IBinder b)
    throws SlotAlreadyRegisteredException {
    super(name, type);
    binder = b;
  }

  @Override
  protected boolean valueExists(IRNode n) {
    // for now:
    return true;
  }

  /**
	 * Get the analysis results as a slot, This is a default implementation, it
	 * may be redefined in subclasses.
	 */
  @Override
  public T getSlotValue(IRNode node) {
    return getAnalysisResultsBefore(node);
  }


  /**
	 * Return the analysis results after a particular port of the component for
	 * the node. If the node isn't evaluated, this method returns null.
	 * 
	 * @param port
	 *          one of
	 *          <ul>
	 *          <li>0 (entry port)
	 *          <li>1 (normal exit port)
	 *          <li>2 (abrupt exit port)
	 *          </ul>
	 */
  protected T getAfter(IRNode node, int port) {
    Component comp = JavaComponentFactory.getComponent(node, true);
    if (comp == null)
      return null;
    IRNode flowUnit = edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis.getFlowUnit(node);
    if (flowUnit == null)
      return null;
    IFlowAnalysis<T> a = getAnalysis(flowUnit);
    ControlNode cn;
    switch (port) {
      case 0 :
        cn = comp.getEntryPort();
        break;
      case 1 :
        cn = comp.getNormalExitPort();
        break;
      case 2 :
        cn = comp.getAbruptExitPort();
        break;
      default :
        throw new FluidError("unknown port designator: " + port);
    }
    if (cn instanceof BlankInputPort)
      return null;
    Lattice<T> lattice = a.getLattice();
    T val = null;
    try {
      for (ControlEdgeIterator ins = cn.getInputs();
        ins.hasNext();
        ) {
        ControlEdge e = ins.nextControlEdge();
        T next = a.getInfo(e);
        if (val == null)
          val = next;
        else if (next != null)
          val = lattice.join(val,next);
      }
    } catch (Exception ex) {
      LOG.log(
        Level.SEVERE,
        "Exception occurred for " + DebugUnparser.toString(node),
        ex);
      ex.printStackTrace();
    }
    if (val == null)
      val = lattice.bottom();
    return val;
  }

  /**
	 * Get the analysis results for a particular node. We fetch a completed
	 * analysis for the current version for the method including the node passed.
	 * Then we find the analysis information on the control-flow edge entering
	 * the control-flow component for the node. This value is returned.
	 */
  public T getAnalysisResultsBefore(IRNode node) {
    return getAfter(node, 0);
  }

  /**
	 * Get the analysis results for a particular node. We fetch a completed
	 * analysis for the current version for the method including the node passed.
	 * Then we find the analysis information on the (first)control-flow edge
	 * exiting the control-flow component for the node. This value is returned.
	 */
  public T getAnalysisResultsAfter(IRNode node) {
    return getAfter(node, 1);
  }

  /**
	 * Get the analysis results for a particular node. We fetch a completed
	 * analysis for the current version for the method including the node passed.
	 * Then we find the analysis information on the (first)control-flow edge
	 * abruptly exiting the control-flow component for the node. This value is returned.
	 */
  public T getAnalysisResultsAbrupt(IRNode node) {
    return getAfter(node, 2);
  }

  /*
	 * Start with a cache with a sentinel. This element will eventually be
	 * evicted since it is never used.
	 */
  private IntraproceduralAnalysisCache<T> cache =
    new IntraproceduralAnalysisCache<T>(null, null, null);

  public void clear() {
    cache = new IntraproceduralAnalysisCache<T>(null, null, null);
  }
  
  /**
	 * Return analysis done for a particular method. If it has not yet been
	 * computed, analysis is performed.
	 */
  public FlowAnalysis<T> getAnalysis(IRNode flowUnit) {
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
        fa.initialize(op.getSource(flowUnit));
        fa.initialize(op.getNormalSink(flowUnit));
        fa.initialize(op.getAbruptSink(flowUnit));
        c = new IntraproceduralAnalysisCache<T>(flowUnit, v, fa);

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

  protected void printAllAnalysisResults(FlowAnalysis<T> fa, IRNode body) {
    for (IRNode n : JJNode.tree.topDown(body)) {
      if (true) {
        printAnalysisResults(fa, n);
      }
    }
  }

  protected void printAnalysisResults(FlowAnalysis<T> fa, IRNode node) {
    Component cfgComp = JavaComponentFactory.prototype.getComponent(node);
    if (cfgComp.getEntryPort() instanceof BlankInputPort) return;
    System.out.println("\nNode: " + DebugUnparser.toString(node));
    //System.out.println(" Entry port and dual are " + cfgComp.getEntryPort() + " " + cfgComp.getEntryPort().getDual());
    printAnalysisResults(fa, cfgComp.getEntryPort().getInputs(), "Entry");
    printAnalysisResults(fa, cfgComp.getNormalExitPort().getOutputs(), "Normal exit");
    printAnalysisResults(fa, cfgComp.getAbruptExitPort().getOutputs(), "Abrupt exit");
  }

  protected void printAnalysisResults(FlowAnalysis<T> fa, ControlEdgeIterator edges, String name) {
    for (int i=1; edges.hasNext(); ++i) {
      System.out.print("  " + name + " " + i + ": ");
      printAnalysisResults(fa,(ControlEdge)edges.next());
    }
  }
  
  protected void printAnalysisResults(FlowAnalysis<T> fa, ControlEdge e) {
    Lattice<T> l = fa.getLattice();
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
	 */
  protected abstract FlowAnalysis<T> createAnalysis(IRNode flowUnit);
}

/**
 * A list of recently performed analyses. The last accessed is at the head of
 * the list. The list is circular and doubly linked.
 */
class IntraproceduralAnalysisCache<T> {
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
