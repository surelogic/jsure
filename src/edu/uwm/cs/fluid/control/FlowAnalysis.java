/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/control/FlowAnalysis.java,v 1.16 2008/06/24 19:13:16 thallora Exp $*/
package edu.uwm.cs.fluid.control;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.FluidInterruptedException;
import edu.cmu.cs.fluid.control.BlankInputPort;
import edu.cmu.cs.fluid.control.Component;
import edu.cmu.cs.fluid.control.ComponentNode;
import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.control.ControlEdgeIterator;
import edu.cmu.cs.fluid.control.ControlNode;
import edu.cmu.cs.fluid.control.Flow;
import edu.cmu.cs.fluid.control.InputPort;
import edu.cmu.cs.fluid.control.Join;
import edu.cmu.cs.fluid.control.LabelList;
import edu.cmu.cs.fluid.control.OutputPort;
import edu.cmu.cs.fluid.control.Port;
import edu.cmu.cs.fluid.control.Sink;
import edu.cmu.cs.fluid.control.Source;
import edu.cmu.cs.fluid.control.Split;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeViewer;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.uwm.cs.fluid.util.Lattice;
import edu.uwm.cs.fluid.control.LabeledLattice.Combiner;
import edu.uwm.cs.fluid.control.LabeledLattice.LabelOp;
import edu.uwm.cs.fluid.control.LabeledLattice.LabeledValue;
import edu.uwm.cs.fluid.control.LabeledLattice.UnaryOp;

/** A class for performing flow analysis over a control-flow graph
 * <p>
 * The technique used here is very general and naive: we
 * start with everything bottom and then iterate using a worklist
 * until a (least) fixpoint is reached.
 * </p>
 * @see Component
 * @see ControlNode
 * @see ControlEdge
 * @see ForwardAnalysis
 * @see BackwardAnalysis
 */
public abstract class FlowAnalysis<T, L extends Lattice<T>> implements Cloneable, IFlowAnalysis<T, L> {
  public final String name;
  protected final L lattice;
  protected final LabeledLattice<T> infoLattice;
  private Worklist worklist;

  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis");
  
//  private static final String VALUE = "{(<unknown>:<[NOTNULL],{e}>)}";
  
  //private boolean started = false;
  private long iterations = 0;

  private final IRNodeViewer nodeViewer;
  
  /** Create a new instance of flow analysis.
   * @param l the lattice of values for the analysis.
   * @see #getInfo
   */
  protected FlowAnalysis(String n, L l, IRNodeViewer nv) {
    name = n;
    lattice = l;
    infoLattice = new LabeledLattice<T>(l);
    worklist = createWorklist();
    nodeViewer = (nv == null) ? IRNodeViewer.defaultViewer : nv;
  }
  
  /**
   * Create the worklist to be used by this analysis.  Called from the
   * constructor, so don't do anything crazy.
   */
  protected abstract Worklist createWorklist();
  
  @Override
  @SuppressWarnings("unchecked")
  public IFlowAnalysis<T, L> clone() {
    try {
      FlowAnalysis<T, L> copy = (FlowAnalysis<T, L>) super.clone();
      copy.worklist = copy.worklist.clone();
      copy.infoMap = new HashMap<ControlEdge,LabeledLattice.LabeledValue<T>>();
      copy.iterations = 0;
      return copy;
    } catch (CloneNotSupportedException ex) {
      // won't happen
      return null;
    }
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.IFlowAnalysis#getName()
   */
  public String getName() {
    return name;
  }
  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.IFlowAnalysis#getLattice()
   */
  public L getLattice() {
    return lattice;
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.IFlowAnalysis#initialize(edu.cmu.cs.fluid.control.ControlNode)
   */
  public void initialize(ControlNode n) {
    worklist.initialize();
    worklist.add(n);
  }
  
  /** Initialize the lattice value for this control edge
   * to the default "bottom" value.
   * @deprecated use initialize(edge.getSource())
   */
  @Deprecated
  public void initialize(ControlEdge edge) {
    initialize(edge,lattice.bottom());
  }
  /** Initialize the lattice value for this control edge
   * as specified.
   */
  public void initialize(ControlEdge edge, T value) {
    initialize(edge,LabelList.empty,value);
  }
  /** Initialize the lattice value for this control edge
   * and labellist as specified.
   */
  public void initialize(ControlEdge edge, LabelList ll, T value) {
    worklist.initialize();
    setInfo(edge,ll,value);
  }

  private boolean debug = false;
  public void debug() {
    debug = true;
  }

  public final T getAfter(final IRNode node, final WhichPort port) {
    Component comp = JavaComponentFactory.getComponent(node, true);
    if (comp == null) {
      return null;
    }
    
    final ControlNode cn = port.getPort(comp);
    if (cn instanceof BlankInputPort) {
      return null;
    }
    
    T val = null;
    try {
      for (ControlEdgeIterator ins = cn.getInputs();
        ins.hasNext();
        ) {
        final ControlEdge e = ins.nextControlEdge();
        final T next = this.getInfo(e);
        if (val == null) {
          val = next;
        } else if (next != null) {
          val = lattice.join(val,next);
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
      val = lattice.bottom();
    }
    return val;
  }

  
  // should be treated as private!
  protected Map<ControlEdge,LabeledLattice.LabeledValue<T>> infoMap =
    new HashMap<ControlEdge,LabeledLattice.LabeledValue<T>>();
  
  public LabeledValue<T> getRawInfo(ControlEdge edge) {
    return infoMap.get(edge);
  }
  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.IFlowAnalysis#getInfo(edu.cmu.cs.fluid.control.ControlEdge)
   */
  public T getInfo(ControlEdge edge) {
    return infoLattice.joinAll(getRawInfo(edge));
  }
  public T getInfo(ControlEdge edge, LabelList ll) {
    return infoLattice.getValue(getRawInfo(edge),ll,lattice.bottom());
  }
  protected void setInfo(ControlEdge edge, LabeledLattice.LabeledValue<T> lv) {
    if (lv == null) return; // assume transfers are strict
    LabeledLattice.LabeledValue<T> old = infoMap.get(edge);
//    final String oldString = infoLattice.toString(old);
//    final String newString = infoLattice.toString(lv);
    
//    if (newString.equals(VALUE)) {
//      System.out.println("Found it");
//    }
//    
//    System.out.println("setInfo: " + newString);
    if (old != null) {
      if (infoLattice.equals(old,lv)) return;
      if (debug) {
        if (!infoLattice.lessEq(old,lv)) {
          this.reportMonotonicityError(edge);
          final String oldString = infoLattice.toString(old);
          final String newString = infoLattice.toString(lv);
          LOG.severe("Monotonicity error: was " + oldString + "; now " + newString);
        }
      }
    }
    if (debug && LOG.isLoggable(Level.FINER)) {
      LOG.finer("new value '" + infoLattice.toString(lv) + "' replaces '" + infoLattice.toString(old) + "'");
    }
    infoMap.put(edge,lv);
    worklist.add(getNodeFromEdgeForWorklist(edge));
  }

  protected abstract ControlNode getNodeFromEdgeForWorklist(ControlEdge edge);
  
  protected void setInfo(ControlEdge edge, LabelList ll, T value) {
    if (edge == null) {
      throw new FluidError("setInfo got null edge");
    }
    if (value == null || value == lattice.bottom())
      return; // no information changed, assume strict
    if (debug && LOG.isLoggable(Level.INFO)) {
      LOG.info("new value computed for label list " +
             ll + ": " + lattice.toString(value));
    }
    LabeledValue<T> old = infoMap.get(edge);
    old = infoLattice.setValue(old,ll,value);
    setInfo(edge,old);
  }

  private static final int COUNT_BEFORE_CHECK = 10;

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.IFlowAnalysis#performAnalysis()
   */
  public void performAnalysis() {
    final IDE ide = IDE.getInstance();
    worklist.start();
    int count = COUNT_BEFORE_CHECK;
    while (worklist.hasNext()) {
      if (count == 0) {
        count = COUNT_BEFORE_CHECK;
        if (ide.isCancelled()) {
          throw new FluidInterruptedException();
        }
      }
      work(worklist.next());
      count -= 1;
    }
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.IFlowAnalysis#reworkAll()
   */
  public void reworkAll() {
    if (worklist.hasNext()) {
      // or log something
      throw new FluidError("reworkAll called too soon");
    }
    Set<ControlNode> nodes = new HashSet<ControlNode>();
    for (ControlEdge e : infoMap.keySet()) {
      nodes.add(e.getSource());
      nodes.add(e.getSink());
    }    
    for (ControlNode n : nodes) {
      work(n);
    }
    nodes.clear();
  }

  protected void work(ControlNode node) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine(iterations + ": working with " + node);
      if (node instanceof ComponentNode) {
        ComponentNode cn = (ComponentNode)node;
        IRNode syntax = cn.getComponent().getSyntax();
        LOG.fine("  operator = " + JJNode.tree.getOperator(syntax) + ", syntax = " + nodeViewer.toString(syntax));
      }
    }
    if (debug) {
      ++iterations;
      if (iterations >= 1000000) throw new FluidError("near-infinite loop in flow analysis.  Probable bug in lattice or in transfer functions.");
    }
    if (node instanceof Port) {
      Port port = (Port)node;
      Port dual = port.getDual();
      if (port instanceof InputPort) {
        transferPort((OutputPort)dual,(InputPort)port);
      } else {
        transferPort((OutputPort)port,(InputPort)dual);
      }
    } else if (node instanceof Flow) {
      transferFlow((Flow)node);
    } else if (node instanceof Split) {
      transferSplit((Split)node);
    } else if (node instanceof Join) {
      transferJoin((Join)node);
    } else if (node instanceof Source) {
      transferSource((Source)node);
    } else if (node instanceof Sink) {
      transferSink((Sink)node);
    }
  }
  
  protected abstract void transferPort(OutputPort p1, InputPort p2);
  protected abstract void transferFlow(Flow n);
  protected abstract void transferSplit(Split n);
  protected abstract void transferJoin(Join n);
  protected abstract void transferSource(Source n);
  protected abstract void transferSink(Sink n);
  
  protected void doNOPtransfer(ControlEdge e1, ControlEdge e2) {
    setInfo(e2,infoMap.get(e1));
  }
  protected <U> void doTransfer(ControlEdge e1, ControlEdge e2,
                                  UnaryOp<T,U> op, U arg) {
    LabeledValue<T> lv1 = infoMap.get(e1);
    LabeledValue<T> lv2 = infoMap.get(e2);
    LabeledValue<T> result = infoLattice.map(lv1,op,arg,lv2);

//    final String s1 = lv1 == null ? "null" : infoLattice.toString(lv1);
//    final String s2 = lv2 == null ? "null" : infoLattice.toString(lv2);
//    final String rs = result == null ? "null" : infoLattice.toString(result);
//    if (rs.equals(VALUE)) {
//      infoLattice.map(lv1,op,arg,lv2);
//    }
//    if (!infoLattice.lessEq(lv2, result)) {
//      infoLattice.map(lv1,op,arg,lv2);
//    }

    if (debug && LOG.isLoggable(Level.FINE)) {
      LOG.fine("map " + op + "(" + arg + ") on " + lv1 + " with cache = " + lv2 + " to get " + result);
    }
    setInfo(e2,result);
  }
  protected <U> void doTransfer(ControlEdge e1, ControlEdge e2, ControlEdge e3,
                                 Combiner<T,U> combiner, U arg) {
    LabeledValue<T> lv1 = infoMap.get(e1);
    LabeledValue<T> lv2 = infoMap.get(e2);
    LabeledValue<T> lv3 = infoMap.get(e3);
    LabeledValue<T> result = infoLattice.merge(lv1,lv2,combiner,arg,lv3);

//    final String s1 = lv1 == null ? "null" : infoLattice.toString(lv1);
//    final String s2 = lv2 == null ? "null" : infoLattice.toString(lv2);
//    final String s3 = lv3 == null ? "null" : infoLattice.toString(lv3);
//    final String rs = result == null ? "null" : infoLattice.toString(result);
//    if (rs.equals(VALUE)) {
//      infoLattice.merge(lv1,lv2,combiner,arg,lv3);
//    }
//    if (!infoLattice.lessEq(lv3,result)) {
//      infoLattice.merge(lv1,lv2,combiner,arg,lv3);
//    }
    
    if (debug && LOG.isLoggable(Level.FINE)) {
      String in1 = lv1 == null ? "null" : lv1.toString(lattice);
      String in2 = lv2 == null ? "null" : lv2.toString(lattice);
      String out = result == null ? "null" : result.toString(lattice);
      LOG.fine("merge " + in1 + " and " + in2 + " to get " + out);
    }
    setInfo(e3,result);
  }
  protected <U> void doTransfer(ControlEdge e1, ControlEdge e2,
                                 LabelOp<U> op, U arg) {
    LabeledValue<T> lv1 = infoMap.get(e1);
    LabeledValue<T> lv2 = infoMap.get(e2);
    LabeledValue<T> result = infoLattice.labelMap(lv1,op,arg,lv2);

//    final String s1 = lv1 == null ? "null" : infoLattice.toString(lv1);
//    final String s2 = lv2 == null ? "null" : infoLattice.toString(lv2);
//    final String rs = result == null ? "null" : infoLattice.toString(result);
//    if (rs.equals(VALUE)) {
//      infoLattice.labelMap(lv1,op,arg,lv2);
//    }
//    if (!infoLattice.lessEq(lv2, result)) {
//      infoLattice.labelMap(lv1,op,arg,lv2);
//    }
    
    if (debug && LOG.isLoggable(Level.FINE)) {
      String in = lv1 == null ? "null" : lv1.toString(lattice);
      String out = result == null ? "null" : result.toString(lattice);
      LOG.fine("labelMap " + op + " over " + in + " to get " + out);
    }
    setInfo(e2,result);
  }
  protected <U> void doTransfer(ControlEdge e1, ControlEdge e2, ControlEdge e3,
                                  LabelOp<U> op1,  U arg1, LabelOp<U> op2, U arg2) {
    LabeledValue<T> lv1 = infoMap.get(e1);
    LabeledValue<T> lv2 = infoMap.get(e2);
    LabeledValue<T> lv3 = infoMap.get(e3);
    LabeledValue<T> merged = infoLattice.labelMap2(lv1,op1,arg1,lv2,op2,arg2,lv3);
    
//    final String s1 = lv1 == null ? "null" : infoLattice.toString(lv1);
//    final String s2 = lv2 == null ? "null" : infoLattice.toString(lv2);
//    final String s3 = lv3 == null ? "null" : infoLattice.toString(lv3);
//    final String rs = merged == null ? "null" : infoLattice.toString(merged);
//    if (rs.equals(VALUE)) {
//      infoLattice.labelMap2(lv1,op1,arg1,lv2,op2,arg2,lv3);
//    }
//    if (!infoLattice.lessEq(lv3,merged)) {
//      infoLattice.labelMap2(lv1,op1,arg1,lv2,op2,arg2,lv3);
//    }

    if (debug && LOG.isLoggable(Level.FINE)) {
      LOG.fine("labelMap2 merging " + lv1 + " and " + lv2 + " to get " + merged);
    }
    setInfo(e3,merged);
  }

  /* (non-Javadoc)
   * @see edu.uwm.cs.fluid.control.IFlowAnalysis#getIterations()
   */
  public long getIterations() {
    return iterations;
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
