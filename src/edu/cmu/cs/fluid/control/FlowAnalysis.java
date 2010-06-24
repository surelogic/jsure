package edu.cmu.cs.fluid.control;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;


import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.FluidInterruptedException;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeViewer;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.AssocList;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.util.Queue;

/** A class for performing flow analysis over a control-flow graph
 * <p>
 * The technique used here is very general and naive: we
 * start with everything top and then iterate using a worklist
 * until a (greatest) fixpoint is reached.
 * There are currently no provisions for "widening" (narrowing
 * for our lattices would be a better term).
 * </p>
 * @see Component
 * @see ControlNode
 * @see ControlEdge
 * @see ForwardAnalysis
 * @see BackwardAnalysis
 */
public abstract class FlowAnalysis<T> {
  //private static boolean printedOnce = false;
  
  public final String name;
  protected final Lattice<T> lattice;
  private final Queue worklist;

  private static final Logger LOG = SLLogger.getLogger("FLUID.control.FlowAnalysis");
  private final IRNodeViewer nodeViewer;
  
  //private boolean started = false;
  private long iterations = 0;

  private final int maxIterations;
  
  
  public static final class AnalysisGaveUp extends RuntimeException {
    public final int count;
    
    public AnalysisGaveUp(final int c) {
      count = c;
    }
  }
  
  
  
  /** Create a new instance of flow analysis.
   * @param l the lattice of values for the analysis.
   * @see #getInfo
   */
  protected FlowAnalysis(String n, Lattice<T> l, IRNodeViewer nv, int max) {
    name = n;
    lattice = l;
    worklist = new Queue();
    nodeViewer = nv;
    maxIterations = max;
    if (LOG.isLoggable(Level.FINEST)) {
      debug = true;
    }
  }

  public String getName() {
    return name;
  }
  public Lattice<T> getLattice() {
    return lattice;
  }

  /** Initialize the lattice value for this control edge
   * to the default "top" value.
   */
  public void initialize(ControlEdge edge) {
    initialize(edge,lattice.top());
  }
  /** Initialize the lattice value for this control edge
   * as specified.
   */
  public void initialize(ControlEdge edge, Lattice<T> value) {
    setInfo(edge,LabelList.empty,value);
  }
  /** Initialize the lattice value for this control edge
   * and labellist as specified.
   */
  public void initialize(ControlEdge edge, LabelList ll, Lattice<T> value) {
    setInfo(edge,ll,value);
  }

  private boolean debug = false;
  public void debug() {
    debug = true;
  }

  /**
   * Return the analysis results after a particular port of the component for
   * the node. If the node isn't evaluated, this method returns null.
   */
  public final Lattice<T> getAfter(final IRNode node, final WhichPort port) {
    final Component comp = JavaComponentFactory.getComponent(node, true);
    if (comp == null) {
      return null;
    }

    final ControlNode cn = port.getPort(comp);
    if (cn instanceof BlankOutputPort) {
      return null;
    }
    
    Lattice<T> val = null;
    try {
      for (ControlEdgeIterator outs = cn.getOutputs();
        outs.hasNext();
        ) {
        final ControlEdge e = outs.nextControlEdge();
        final Lattice<T> next = this.getInfo(e);
        if (val == null) {
          val = next;
        } else if (next != null) {
          val = val.meet(next);
        }
      }
    } catch (Exception ex) {
      LOG.log(
        Level.SEVERE,
        "Exception occurred for " + DebugUnparser.toString(node),
        ex);
      ex.printStackTrace();
    }
    if (val == null) {
      val = this.getLattice().top();
    }
    return val;
  }

  
  private final Map<ControlEdge,AssocList<LabelList,Lattice<T>>> currentInfo = 
    new HashMap<ControlEdge,AssocList<LabelList,Lattice<T>>>();

  private static final int COUNT_BEFORE_CHECK = 10;
  
  /** Perform the analysis as specified.
   * NB: If new initializations have been performed since
   * the last time analysis was done, they will be
   * now taken into account.
   */
  public void performAnalysis() {
	final IDE ide = IDE.getInstance();
    LOG.finer("About to start analysis: " + this);
    try {
      int globalCount = 0;
      int localCount = 0;
      while (!worklist.isEmpty()) {
        /* should the worklist include label too ? */
        work((ControlEdge)worklist.dequeue());
        localCount++;
        globalCount++;

        if (localCount == COUNT_BEFORE_CHECK) {
          localCount = 0;
        	
        	if (ide.isCancelled()) {        
        		throw new FluidInterruptedException();
        	}
        	if (maxIterations > 0 && globalCount >= maxIterations) {
        	  throw new AnalysisGaveUp(globalCount);
        	}
        }        
      }
    } catch (RuntimeException e1) {
      if (e1 instanceof AnalysisGaveUp) throw e1;
      LOG.log(Level.WARNING, "Problem while analyzing", e1);
    }
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("Analysis is complete.");
      // probably what debug should do, but it seems hard to 
      // use 'debug' without editing code to turn it on and off.
      for (Map.Entry<ControlEdge,AssocList<LabelList,Lattice<T>>> e : currentInfo.entrySet()) {
        ControlEdge edge = e.getKey();
        AssocList<LabelList,Lattice<T>> value = e.getValue();
        ControlNode source = edge.getSource();
        ControlNode sink = edge.getSink();
        IRNode syntax;
        String kind;
        if (sink instanceof EntryPort) {
          kind = "entry";
          syntax = ((ComponentNode)sink).getComponent().getSyntax();
        } else if (source instanceof NormalExitPort) {
          kind = "normal exit";
          syntax = ((ComponentNode)source).getComponent().getSyntax();
        } else if (source instanceof AbruptExitPort) {
          kind = "abrupt exit";
          syntax = ((ComponentNode)source).getComponent().getSyntax();
        } else {
          continue;
        }
        LOG.finer("Information at " + kind + " for " + JJNode.tree.getOperator(syntax) + "(...) = " + nodeViewer.toString(syntax));
        for (Enumeration<LabelList> en = value.keys(); en.hasMoreElements();) {
          LabelList ll = en.nextElement();
          Lattice l = value.get(ll);
          LOG.finer("  " + ll + " -> " + l);
        }
      }
    } else {
      //System.out.println("analysis logger is " + LOG.getLevel());
    }
  }

  /** Re-run the transfer functions for all edges
   * in the graph.  This function can only be called
   * once analysis is complete.
   */
  public void reworkAll() {
    if (!worklist.isEmpty()) {
      // or log something
      throw new FluidError("reworkAll called too soon");
    }
    for (ControlEdge controlEdge : currentInfo.keySet()) {
      work(controlEdge);
      while (!worklist.isEmpty()) {
      	reportMonotonicityError((ControlEdge)worklist.dequeue());
      }
    }    
  }

  protected void work(ControlEdge edge) {
    AssocList<LabelList,Lattice<T>> l = currentInfo.get(edge);
    if (LOG.isLoggable(Level.FINEST)) {
      LOG.finest("Working with " + edge.getSource() + " -> " + edge.getSink());
    }
    /*
    if (!printedOnce && l.size() > 50) {
      printedOnce = true;
      System.out.println("Found a long assoc list");
      System.out.println(l);
    }
    */
    /* assert l != null */
    if (debug) {
      if (iterations >= 2000000) throw new FluidError("near-infinite loop in flow analysis.  Probable bug in lattice or in transfer functions.");
    }
    /* O(n^2) but hopefully n is very small */
    Enumeration<LabelList> keys = l.keys();
    while (keys.hasMoreElements()) {
      LabelList ll = keys.nextElement();
      ++iterations;
      useInfo(edge, ll, l.get(ll));
    }
  }

  /** Return an indication of how long the analysis took.
   * Currently it gives the total number of times an edge
   * in the control-flow graph was visited.
   */
  public long getIterations() {
    return iterations;
  }

  protected void setInfo(ControlEdge edge, LabelList ll, Lattice<T> value) {
    if (edge == null) {
      throw new FluidError("setInfo got null edge");
    }
    if (value == null)
      return; // no information changed
    if (debug) {
      LOG.info("new value computed for label list " +
			 ll + ": " + value);
    }
    if (currentInfo.containsKey(edge)) {
      AssocList<LabelList,Lattice<T>> l = currentInfo.get(edge);
      Lattice<T> vold = l.get(ll);
      if (vold != null)
        value = value.meet(vold);
      if (vold == null || !vold.equals(value)) {
        l.put(ll, value);
        worklist.enqueue(edge);
      } else {
        if (debug) {
          LOG.info("  (but it didn't add anything)");
        }
      }
    } else { // no value yet
      currentInfo.put(edge, new AssocList<LabelList,Lattice<T>>(ll, value));
      worklist.enqueue(edge);
    }
  }

  protected abstract void useInfo(ControlEdge edge,
				  LabelList ll,
				  Lattice<T> value);

  public AssocList<LabelList,Lattice<T>> getRawInfo(ControlEdge edge) {
    return currentInfo.get(edge);
  }

  public Lattice<T> getInfo(ControlEdge edge) {
    AssocList<?,Lattice<T>> l = getRawInfo(edge);
    if (l == null) {
      return lattice.top();
    } else {
      Enumeration<Lattice<T>> values = l.elements();
      Lattice<T> value = lattice.top();
      while (values.hasMoreElements()) {
        value = value.meet(values.nextElement());
      }
      return value;
    }
  }

  /** Get the control-information for a particular edge
   * that was built on a particular label list.
   */
  public Lattice<T> getInfo(ControlEdge edge, LabelList ll) {
    AssocList<LabelList,Lattice<T>> l = getRawInfo(edge);
    if (l == null) {
      return lattice.top();
    } else {
      //! We assume the enumerations run in parallel!
      Enumeration<LabelList> keys = l.keys();
      Enumeration<Lattice<T>> values = l.elements();
      Lattice<T> value = lattice.top();
      while (values.hasMoreElements()) {
        LabelList ll2 = keys.nextElement();
        Lattice<T> val = values.nextElement();
        if (ll2.includes(ll))
          value = value.meet(val);
      }
      return value;
    }
  }
  
  protected void reportMonotonicityError(ControlNode n) {
  	if (n instanceof ComponentNode) {
  		IRNode astNode = ((ComponentNode)n).getComponent().getSyntax();
  		if (astNode != null) {
  			LOG.severe("Monotonicity error in transfer for " + JJNode.tree.getOperator(astNode));
  			return;
  		}
  	}
  	LOG.warning("Monotocity error in analysis at " + n);
  }
  
  protected abstract void reportMonotonicityError(ControlEdge e);
}
