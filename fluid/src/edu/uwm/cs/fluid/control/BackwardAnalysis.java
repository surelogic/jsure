/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/control/BackwardAnalysis.java,v 1.11 2008/06/24 19:13:16 thallora Exp $*/
package edu.uwm.cs.fluid.control;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.control.AddLabel;
import edu.cmu.cs.fluid.control.BlankOutputPort;
import edu.cmu.cs.fluid.control.Choice;
import edu.cmu.cs.fluid.control.ComponentChoice;
import edu.cmu.cs.fluid.control.ComponentFlow;
import edu.cmu.cs.fluid.control.ComponentSink;
import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.control.ControlNode;
import edu.cmu.cs.fluid.control.DoubleInputPort;
import edu.cmu.cs.fluid.control.DoubleOutputPort;
import edu.cmu.cs.fluid.control.DynamicSplit;
import edu.cmu.cs.fluid.control.Flow;
import edu.cmu.cs.fluid.control.Fork;
import edu.cmu.cs.fluid.control.InputPort;
import edu.cmu.cs.fluid.control.Join;
import edu.cmu.cs.fluid.control.LabelList;
import edu.cmu.cs.fluid.control.LabelTest;
import edu.cmu.cs.fluid.control.Merge;
import edu.cmu.cs.fluid.control.NoOperation;
import edu.cmu.cs.fluid.control.OutputPort;
import edu.cmu.cs.fluid.control.PendingLabelStrip;
import edu.cmu.cs.fluid.control.SimpleInputPort;
import edu.cmu.cs.fluid.control.SimpleOutputPort;
import edu.cmu.cs.fluid.control.Sink;
import edu.cmu.cs.fluid.control.Source;
import edu.cmu.cs.fluid.control.Split;
import edu.cmu.cs.fluid.control.SubcomponentChoice;
import edu.cmu.cs.fluid.control.SubcomponentFlow;
import edu.cmu.cs.fluid.control.TrackLabel;
import edu.cmu.cs.fluid.control.TrackedDemerge;
import edu.cmu.cs.fluid.control.TrackedMerge;
import edu.cmu.cs.fluid.control.UnknownLabel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeViewer;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.uwm.cs.fluid.control.LabeledLattice.UnaryOp;
import edu.uwm.cs.fluid.util.Lattice;


/** Backward control-flow analysis over control-flow graphs.
 * We specialize the control-flow analysis class to perform
 * analysis in the opposite direction as the control-flow.
 * The analysis is generic; it must be parameterized with a strategy
 * for how to transfer analysis values over specific node types.
 * 
 * @see BackwardTransfer
 * @see edu.uwm.cs.fluid.util.Lattice
 */
public class BackwardAnalysis<T, L extends Lattice<T>, XFER extends BackwardTransfer<T>> extends FlowAnalysis<T, L> {
  /** Logger instance for debugging. */
  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis");

  /* We use a generic type for this so that subclasses of BackwardAnalysis
   * specialized for different languages or environments can expect to have
   * specialized transfer functions.
   */
  protected final XFER trans;

  /** Create an instance of backward control-flow analysis.
   * @param t the transfer function for semantics-specific nodes.
   * @see FlowAnalysis
   */
  public BackwardAnalysis(
      final String name, final L l, final XFER t, final IRNodeViewer nv) {
    this(name, l, t, nv, false);
  }

  public BackwardAnalysis(
      final String name, final L l, final XFER t, final IRNodeViewer nv,
      final boolean timeOut) {
    super(name, l, nv, timeOut);
    trans = t;
  }
  

  @Override
  protected final Worklist createWorklist() {
    return Worklist.Factory.makeWorklist(false);
  }
  
  @Override
  protected final ControlNode getNodeFromEdgeForWorklist(
      final ControlEdge edge) {
    return edge.getSource();
  }
  
  @Override
  public void performAnalysis() {
    realPerformAnalysis();
    // for debugging: find all nodes whose outgoing edges have
    // information but whose incoming edges have no such information
    /*
    Set<ControlNode> sources = new HashSet<ControlNode>();
    Set<ControlEdge> edges = infoMap.keySet();
    for (ControlEdge e : edges) {
      sources.add(e.getSource());
    }
    for (ControlNode n : sources) {
      for (IRNode e : n.getInputs()) {
        if (infoMap.get(e) == null) {
          System.out.println("CFG analysis stopped at " + n);
          break;
        }
      }
    }
    */
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.control.FlowAnalysis#reportMonotonicityError(edu.cmu.cs.fluid.control.ControlEdge)
   */
  @Override
  protected void reportMonotonicityError(ControlEdge e) {
    reportMonotonicityError(e.getSink());
  }

  @Override
  protected void transferPort(OutputPort dual, InputPort port) {
    if (port instanceof SimpleInputPort) {
      ControlEdge out = ((SimpleInputPort)port).getOutput();
      if (dual instanceof SimpleOutputPort) {
        doNOPtransfer(out,((SimpleOutputPort) dual).getInput());
      } else if (dual instanceof DoubleOutputPort) {
        doNOPtransfer(out,((DoubleOutputPort) dual).getInput1());
        doNOPtransfer(out,((DoubleOutputPort) dual).getInput2());
      } else if (dual instanceof BlankOutputPort) {
        /* do nothing */
      } else {
        LOG.severe("unknown OutputPort " + dual + ", dual of " + port + " for " + DebugUnparser.toString(port.getSyntax()));
      }
    } else if (port instanceof DoubleInputPort) {
      ControlEdge out1 = ((DoubleInputPort)port).getOutput1();
      ControlEdge out2 = ((DoubleInputPort)port).getOutput2();
      if (dual instanceof SimpleOutputPort) {
        doTransfer(out1,out2,((SimpleOutputPort) dual).getInput(),
                   conditionalCombiner,dual.getSyntax());
      } else if (dual instanceof DoubleOutputPort) {
        doNOPtransfer(out1,((DoubleOutputPort) dual).getInput1());
        doNOPtransfer(out2,((DoubleOutputPort) dual).getInput2());
      } else if (dual instanceof BlankOutputPort) {
        /* do nothing */
      } else {
        LOG.severe("unknown OutputPort " + dual);
      }
    } else {
      LOG.severe("unknown InputPort " + port);
    }
  }

  final LabeledLattice.LabelOp<ControlLabel> unAddLabelOp =
    new LabeledLattice.LabelOp<ControlLabel>() {
    public LabelList operate(LabelList ll, ControlLabel arg) {
      ControlLabel label = ll.firstLabel();
      if (label != null
          && (label instanceof UnknownLabel || 
                 trans.testAddLabel(arg,label))) {
        return ll.dropLabel();
      }
     return null;
    }
  };
  final LabeledLattice.UnaryOp<T,ComponentFlow> componentFlowTransfer =
    new LabeledLattice.UnaryOp<T,ComponentFlow>() {
    public T operate(T x, ComponentFlow arg) {
      return trans.transferComponentFlow(arg.getComponent().getSyntax(),arg.getInfo(),x);
    }
  };
  final LabeledLattice.UnaryOp<T,SubcomponentFlow> subcomponentFlowTransfer =
    new LabeledLattice.UnaryOp<T,SubcomponentFlow>() {
    public T operate(T x, SubcomponentFlow arg) {
      return trans.transferComponentFlow(arg.getSyntax(),arg.getInfo(),x);
    }
  };
  
  final LabeledLattice.LabelOp<Boolean> unPendingLabelStripOp =
    new LabeledLattice.LabelOp<Boolean>() {
    public LabelList operate(LabelList ll, Boolean arg) {
      ControlLabel saved = ll.firstLabel();
      if (saved == null) return null;
      if (arg.booleanValue()) {
        return ll.dropLabel().addLabel(TrackLabel.trueTrack).addLabel(saved);
      } else {
        return ll.dropLabel().addLabel(UnknownLabel.prototype).addLabel(
          TrackLabel.falseTrack).addLabel(saved);
      }
    }
  };
  
  @Override
  protected void transferFlow(Flow node) {
    ControlEdge in = node.getInput();
    ControlEdge out = node.getOutput();
    if (node instanceof NoOperation) {
      doNOPtransfer(out,in);
    } else if (node instanceof AddLabel) {
      doTransfer(out,in,unAddLabelOp,((AddLabel) node).getLabel());
    } else if (node instanceof ComponentFlow) {
      doTransfer(out,in,componentFlowTransfer,(ComponentFlow)node);
    } else if (node instanceof SubcomponentFlow) {
      doTransfer(out,in,subcomponentFlowTransfer,(SubcomponentFlow)node);
    } else if (node instanceof PendingLabelStrip) {
      doTransfer(out,out,in,unPendingLabelStripOp,Boolean.TRUE,
                                             unPendingLabelStripOp,Boolean.FALSE);
    } else {
      LOG.severe("unknown Flow " + node);
    }
  }

  final LabeledLattice.Combiner<T,ComponentChoice> componentChoiceCombiner =
    new LabeledLattice.Combiner<T,ComponentChoice>() {
     public final UnaryOp<T,ComponentChoice> leftBottom = new UnaryOp<T,ComponentChoice>() {
       public T operate(T x, ComponentChoice arg) { 
         IRNode node = arg.getSyntax();
         Object info = arg.getInfo();
         return trans.transferComponentChoice(node,info,false,x);
       }
     };
     public final UnaryOp<T,ComponentChoice> rightBottom = new UnaryOp<T,ComponentChoice>() {
       public T operate(T x, ComponentChoice arg) { 
         IRNode node = arg.getSyntax();
         Object info = arg.getInfo();
         T r1 = trans.transferComponentChoice(node,info,true,x);
         return r1; 
       }
     };
     public UnaryOp<T,ComponentChoice> bindLeftBottom() {
       return leftBottom;
     }
     public UnaryOp<T,ComponentChoice> bindRightBottom() {
       return rightBottom;
     }
     public T combine(T x, T y, ComponentChoice arg) {
        IRNode node = arg.getSyntax();
        Object info = arg.getInfo();
        T r1 = trans.transferComponentChoice(node,info,true,x);
        T r2 = trans.transferComponentChoice(node,info,false,y);
        if (r1 == null) r1 = lattice.bottom();
        if (r2 == null) r2 = lattice.bottom();
        return lattice.join(r1,r2);
      }
  };
  final LabeledLattice.Combiner<T,SubcomponentChoice> subcomponentChoiceCombiner =
      new LabeledLattice.Combiner<T,SubcomponentChoice>() {
       public final UnaryOp<T,SubcomponentChoice> leftBottom = new UnaryOp<T,SubcomponentChoice>() {
         public T operate(T x, SubcomponentChoice arg) { 
           IRNode node = arg.getSyntax();
           Object info = arg.getInfo();
           return trans.transferComponentChoice(node,info,false,x);
         }
       };
       public final UnaryOp<T,SubcomponentChoice> rightBottom = new UnaryOp<T,SubcomponentChoice>() {
         public T operate(T x, SubcomponentChoice arg) { 
           IRNode node = arg.getSyntax();
           Object info = arg.getInfo();
           T r1 = trans.transferComponentChoice(node,info,true,x);
           return r1; 
         }
       };
       public UnaryOp<T,SubcomponentChoice> bindLeftBottom() {
         return leftBottom;
       }
       public UnaryOp<T,SubcomponentChoice> bindRightBottom() {
         return rightBottom;
       }
       public T combine(T x, T y, SubcomponentChoice arg) {
          IRNode node = arg.getSyntax();
          Object info = arg.getInfo();
          T r1 = trans.transferComponentChoice(node,info,true,x);
          T r2 = trans.transferComponentChoice(node,info,false,y);
          if (r1 == null) r1 = lattice.bottom();
          if (r2 == null) r2 = lattice.bottom();
          return lattice.join(r1,r2);
        }
    };
  final LabeledLattice.Combiner<T, IRNode> conditionalCombiner =
    new LabeledLattice.Combiner<T,IRNode>() {
      public final UnaryOp<T,IRNode> leftBottom = new UnaryOp<T,IRNode>() {
        public T operate(T x, IRNode arg) { 
          return trans.transferConditional(arg,false,x);
        }
      };
      public final UnaryOp<T,IRNode> rightBottom = new UnaryOp<T,IRNode>() {
        public T operate(T x, IRNode arg) { 
          T r1 = trans.transferConditional(arg,true,x);
          return r1; 
        }
      };
      public UnaryOp<T,IRNode> bindLeftBottom() {
        return leftBottom;
      }
      public UnaryOp<T,IRNode> bindRightBottom() {
        return rightBottom;
      }
    public T combine(T x, T y, IRNode arg) {
      T xp = trans.transferConditional(arg, true, x);
      T yp = trans.transferConditional(arg, false, y);
      if (xp == null) return yp;
      if (yp == null) return xp;
      return lattice.join(xp,yp);
    }
  };

  /*
   * Updated to this 2011-02-03 based on John's help.  This was a large 
   * part of the problem for bug 1647!
   * 
   * If you read all the comments in LabelledLattice.merge, you will see that
   * the code goes to great lengths to force the combiner to be called even when
   * one of the inputs is null/bottom, BUT the code in AbstractCombiner presumes
   * that null is the identity of the function: f(BOT,x) = f(X,BOT) = x but that
   * is DEFINITELY not the case for Dynamic split. Depending on the argument,
   * one may have f(BOT,x) = BOT for all x.
   * 
   * This then gets the WRONG value pressed through the system and then a
   * monotonicity error.
   * 
   * was:
   * 
   *  LabeledLattice.Combiner<T, DynamicSplit> dynamicSplitCombiner = new LabeledLattice.AbstractCombiner<T,DynamicSplit>() {
   *      public T combine(T x, T y, DynamicSplit arg) {
   *        if (!arg.test(true)) x= lattice.bottom();
   *        if (!arg.test(false)) y = lattice.bottom();
   *        return lattice.join(x,y);
   *      }
   *  };
   */
  final LabeledLattice.Combiner<T, DynamicSplit> dynamicSplitCombiner = new LabeledLattice.Combiner<T, DynamicSplit>() {
    public final UnaryOp<T, DynamicSplit> leftBottom = new UnaryOp<T, DynamicSplit>() {
      public T operate(T x, DynamicSplit arg) {
        return combine(lattice.bottom(), x, arg);
      }
    };

    public final UnaryOp<T, DynamicSplit> rightBottom = new UnaryOp<T, DynamicSplit>() {
      public T operate(T x, DynamicSplit arg) {
        return combine(x, lattice.bottom(), arg);
      }
    };

    public UnaryOp<T, DynamicSplit> bindLeftBottom() {
      return leftBottom;
    }

    public UnaryOp<T, DynamicSplit> bindRightBottom() {
      return rightBottom;
    }

    public T combine(T x, T y, DynamicSplit arg) {
      if (!arg.test(true))
        x = lattice.bottom();
      if (!arg.test(false))
        y = lattice.bottom();
      return lattice.join(x, y);
    }
  };

//  LabeledLattice.Combiner<T, DynamicSplit> dynamicSplitCombiner = new LabeledLattice.AbstractCombiner<T,DynamicSplit>() {
//      public T combine(T x, T y, DynamicSplit arg) {
//        if (!arg.test(true)) x= lattice.bottom();
//        if (!arg.test(false)) y = lattice.bottom();
//        return lattice.join(x,y);
//      }
//  };
  
  final LabeledLattice.LabelOp<ControlLabel> addLabelOp =
    new LabeledLattice.LabelOp<ControlLabel>() {
    public LabelList operate(LabelList ll, ControlLabel arg) {
      return ll.addLabel(arg);
    }
  };
  final LabeledLattice.LabelOp<ControlLabel> nopLabelOp =
    new LabeledLattice.LabelOp<ControlLabel>() {
    public LabelList operate(LabelList ll, ControlLabel arg) {
      return ll;
    }
  };

  @Override
  protected void transferSplit(Split node) {    
    ControlEdge in = node.getInput();
    ControlEdge out1 = node.getOutput1();
    ControlEdge out2 = node.getOutput2();
    
    // the difficulty here is that (unlike forward analysis),
    // splits *CAN* have non-trivial transfer functions.
    // This means we sometimes need to run two transfer functions (see Choice below).
    if (node instanceof Fork) {
      doTransfer(out1,out2,in,infoLattice.joinCombiner,null);
    } else if (node instanceof Choice) {
      if (node instanceof LabelTest) {
        ControlLabel label = ((LabelTest)node).getTestLabel();
        doTransfer(out1,out2,in,addLabelOp,label,nopLabelOp,null);
      } else if (node instanceof ComponentChoice) {
        doTransfer(out1,out2,in,componentChoiceCombiner,(ComponentChoice)node);
      } else if (node instanceof SubcomponentChoice) {
        doTransfer(out1, out2, in, subcomponentChoiceCombiner, (SubcomponentChoice) node);
      } else {
        LOG.severe("Unknown Choice " + node);
      }
    } else if (node instanceof DynamicSplit) {
      doTransfer(out1,out2,in,dynamicSplitCombiner,(DynamicSplit)node);
    } else if (node instanceof TrackedDemerge) {
      doTransfer(out1,out2,in,addLabelOp,TrackLabel.trueTrack,addLabelOp,TrackLabel.falseTrack);
    } else {
      LOG.severe("unknown Split " + node);
    }
  }

  final LabeledLattice.LabelOp<ControlLabel> dropLabelOp =
    new LabeledLattice.LabelOp<ControlLabel>() {
    public LabelList operate(LabelList ll, ControlLabel arg) {
      return ll.dropLabel(arg);
    }
  };
  
  @Override
  protected void transferJoin(Join node) {
    ControlEdge out = node.getOutput();
    ControlEdge in1 = node.getInput1();
    ControlEdge in2 = node.getInput2();
    if (node instanceof Merge) {
      doNOPtransfer(out,in1);
      doNOPtransfer(out,in2);
    } else if (node instanceof TrackedMerge) {
      doTransfer(out,in1,dropLabelOp,TrackLabel.trueTrack);
      doTransfer(out,in2,dropLabelOp,TrackLabel.falseTrack);
    } else {
      LOG.severe("unknown Join " + node);
    }
  }

  @Override
  protected void transferSource(Source n) {
    // do nothing
  }

  @Override
  protected void transferSink(Sink n) {
    ControlEdge in = n.getInput();
    if (n instanceof ComponentSink) {
      ComponentSink cs = (ComponentSink)n;
      Boolean normal = (Boolean)cs.getInfo();
      T value = trans.transferComponentSink(n,normal);
      LabelList ll = LabelList.empty;
      if (!normal) ll = ll.addLabel(UnknownLabel.prototype);
      setInfo(in,ll,value);
    }  
  }
}
