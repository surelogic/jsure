/*$Header: /cvs/fluid/fluid/src/edu/uwm/cs/fluid/control/ForwardAnalysis.java,v 1.11 2008/06/24 19:13:16 thallora Exp $*/
package edu.uwm.cs.fluid.control;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

//import sun.security.krb5.internal.p;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.control.AddLabel;
import edu.cmu.cs.fluid.control.BlankInputPort;
import edu.cmu.cs.fluid.control.Choice;
import edu.cmu.cs.fluid.control.ComponentChoice;
import edu.cmu.cs.fluid.control.ComponentFlow;
import edu.cmu.cs.fluid.control.ComponentPort;
import edu.cmu.cs.fluid.control.ComponentSource;
import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.control.DoubleInputPort;
import edu.cmu.cs.fluid.control.DoubleOutputPort;
import edu.cmu.cs.fluid.control.DynamicSplit;
import edu.cmu.cs.fluid.control.Flow;
import edu.cmu.cs.fluid.control.Fork;
import edu.cmu.cs.fluid.control.InputPort;
import edu.cmu.cs.fluid.control.Join;
import edu.cmu.cs.fluid.control.LabelList;
import edu.cmu.cs.fluid.control.LabelTest;
import edu.cmu.cs.fluid.control.LoopMerge;
import edu.cmu.cs.fluid.control.Merge;
import edu.cmu.cs.fluid.control.NoOperation;
import edu.cmu.cs.fluid.control.OutputPort;
import edu.cmu.cs.fluid.control.PendingLabelStrip;
import edu.cmu.cs.fluid.control.SimpleInputPort;
import edu.cmu.cs.fluid.control.SimpleOutputPort;
import edu.cmu.cs.fluid.control.Sink;
import edu.cmu.cs.fluid.control.Source;
import edu.cmu.cs.fluid.control.Split;
import edu.cmu.cs.fluid.control.SubcomponentFlow;
import edu.cmu.cs.fluid.control.TrackLabel;
import edu.cmu.cs.fluid.control.TrackedDemerge;
import edu.cmu.cs.fluid.control.TrackedMerge;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeViewer;
import edu.uwm.cs.fluid.util.Lattice;


/**
 * Forward control-flow analysis over control-flow graphs. We specialize the
 * control-flow analysis class to perform analysis in the same direction as the
 * control-flow. The analysis is generic; it must be parameterized with a
 * strategy for how to transfer analysis values over specific node types.
 * 
 * @see ForwardTransfer
 * @see edu.uwm.cs.fluid.util.Lattice
 */
public class ForwardAnalysis<T> extends FlowAnalysis<T> {
  /** Logger instance for debugging. */
  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis");
  
  protected final ForwardTransfer<T> trans;
  
  // a whole family of transfer operations to be used with labeled lattices.
  
  final LabeledLattice.UnaryOp<T,IRNode> conditionalTrueTransfer = 
    new LabeledLattice.UnaryOp<T,IRNode>() {
    public T operate(T x, IRNode arg) {
      LOG.finer("calling componentTrueTransfer");
      return trans.transferConditional(arg,true,x);
    }
  };
  final LabeledLattice.UnaryOp<T,IRNode> conditionalFalseTransfer = 
    new LabeledLattice.UnaryOp<T,IRNode>() {
    public T operate(T x, IRNode arg) {
      LOG.finer("calling componentFalseTransfer");
      return trans.transferConditional(arg,false,x);
    }
  };
  
  final LabeledLattice.UnaryOp<T,ComponentFlow> componentFlowTransfer =
    new LabeledLattice.UnaryOp<T,ComponentFlow>() {
    public T operate(T x, ComponentFlow arg) {
      LOG.finer("calling componentFlowTransfer");
      return trans.transferComponentFlow(arg.getComponent().getSyntax(),arg.getInfo(),x);
    }
  };
  final LabeledLattice.UnaryOp<T,SubcomponentFlow> subcomponentFlowTransfer =
    new LabeledLattice.UnaryOp<T,SubcomponentFlow>() {
    public T operate(T x, SubcomponentFlow arg) {
      LOG.finer("calling subcomponentFlowTransfer");
      return trans.transferComponentFlow(arg.getSyntax(),arg.getInfo(),x);
    }
  };
  final LabeledLattice.UnaryOp<T,ComponentChoice> componentChoiceTrueTransfer =
    new LabeledLattice.UnaryOp<T,ComponentChoice>() {
    public T operate(T x, ComponentChoice arg) {
      LOG.finer("calling componentChoiceTrueTransfer");
      return trans.transferComponentChoice(arg.getSyntax(),arg.getInfo(),true,x);
    }
  };
  final LabeledLattice.UnaryOp<T,ComponentChoice> componentChoiceFalseTransfer =
    new LabeledLattice.UnaryOp<T,ComponentChoice>() {
    public T operate(T x, ComponentChoice arg) {
      LOG.finer("calling componentChoiceFalseTransfer");
      return trans.transferComponentChoice(arg.getSyntax(),arg.getInfo(),false,x);
    }
  };
  final LabeledLattice.LabelOp<ControlLabel> addLabelOp =
    new LabeledLattice.LabelOp<ControlLabel>() {
    public LabelList operate(LabelList ll, ControlLabel arg) {
      LOG.finer("calling addLabelOp");
      return ll.addLabel(arg);
    }
  };
  final LabeledLattice.LabelOp<ControlLabel> dropLabelOp =
    new LabeledLattice.LabelOp<ControlLabel>() {
    public LabelList operate(LabelList ll, ControlLabel arg) {
      LOG.finer("calling dropLabelOp");
      return ll.dropLabel(arg);
    }
  };
  final LabeledLattice.LabelOp<LabelTest> testLabelTrueOp =
    new LabeledLattice.LabelOp<LabelTest>() {
    public LabelList operate(LabelList ll, LabelTest arg) {
      LOG.finer("calling testLabelTrueOp");
      ControlLabel label = ll.firstLabel();
      if (label == null) { // cannot strip anything!
        return null;
      } else if (trans.transferLabelTest(arg.getSyntax(),arg.getInfo(),label,true)) {
        return ll.dropLabel();
      } else {
        return null;
      }
    }
  };
  final LabeledLattice.LabelOp<LabelTest> testLabelFalseOp =
    new LabeledLattice.LabelOp<LabelTest>() {
    public LabelList operate(LabelList ll, LabelTest arg) {
      LOG.finer("calling testLabelFalseOp");
      ControlLabel label = ll.firstLabel();
      if (label == null) { // cannot strip anything!
        return null;
      } else if (trans.transferLabelTest(arg.getSyntax(),arg.getInfo(),label,false)) {
        return ll;
      } else {
        return null;
      }
    }
  };
  
  final LabeledLattice.LabelOp<Void> pendingLabelStripOp =
    new LabeledLattice.LabelOp<Void>() {
    public LabelList operate(LabelList ll, Void arg) {
      LOG.finer("calling pendingLabelStripOp");
      ControlLabel saved = ll.firstLabel();
      LabelList ll2 = ll.dropLabel();
      ControlLabel track = ll2.firstLabel();
      LabelList ll3 = ll2.dropLabel();
      if (track == TrackLabel.trueTrack) {
        return  ll3.addLabel(saved);
      } else if (track == TrackLabel.falseTrack) {
        if (ll3 == LabelList.empty) {
          LOG.warning("pending label strip applied to " + ll);
          return null;
        } else {
          return ll3.dropLabel().addLabel(saved);
        }
      }
      return null;
    }
  };
  final LabeledLattice.Combiner<T,LoopMerge> loopMergeOp =
    new LabeledLattice.AbstractCombiner<T,LoopMerge>() {
    public T combine(T value, T otherValue, LoopMerge node) {
      LOG.finer("calling loopMergeOp");
      IRNode loop = node.getComponent().getSyntax();
      return trans.transferLoopMerge(loop,value,otherValue);
    }
  };
  
  /**
   * Create an instance of forward control-flow analysis.
   * 
   * @param t
   *          the transfer functions for semantics-specific nodes.
   * @see FlowAnalysis
   */
  public ForwardAnalysis(String name, Lattice<T> l, ForwardTransfer<T> t, IRNodeViewer nv) {
    super(name, l, nv);
    trans = t;
  }
  
  
  @Override
  protected void transferFlow(Flow node) {
    ControlEdge before = node.getInput();
    ControlEdge after = node.getOutput();
    if (node instanceof NoOperation) {
      doNOPtransfer(before,after);
    } else if (node instanceof AddLabel) {
      doTransfer(before,after,addLabelOp,((AddLabel)node).getLabel());
    } else if (node instanceof ComponentFlow) {
      ComponentFlow cf = (ComponentFlow) node;
      doTransfer(before,after,componentFlowTransfer,cf);
    } else if (node instanceof SubcomponentFlow) {
      SubcomponentFlow scf = (SubcomponentFlow) node;
      doTransfer(before,after,subcomponentFlowTransfer,scf);
    } else if (node instanceof PendingLabelStrip) {
      doTransfer(before,after,pendingLabelStripOp,null);
    } else {
      throw new FluidError("unknown Flow " + node);
    }
  }
  
  
  @Override
  protected void transferJoin(Join node) {
    ControlEdge in1 = node.getInput(false);
    ControlEdge in2 = node.getInput(true);
    ControlEdge out = node.getOutput();
    if (node instanceof Merge) {
      if (node instanceof LoopMerge) {
        doTransfer(in1,in2,out,loopMergeOp,(LoopMerge)node);
      } else {
        doTransfer(in1,in2,out,infoLattice.joinCombiner,null);
      }
    } else if (node instanceof TrackedMerge) {
      doTransfer(in1,in2,out,
          addLabelOp,TrackLabel.trueTrack,
          addLabelOp,TrackLabel.falseTrack);
    } else {
      throw new FluidError("unknown Join " + node);
    }
    
  }
  
  
  @Override
  protected void transferSink(Sink n) {
    // do nothing!
  }
  
  
  @Override
  protected void transferSource(Source n) {
    ControlEdge edge = n.getOutput();
    T val = trans.transferComponentSource(((ComponentSource)n).getComponent().getSyntax());
    setInfo(edge, new LabeledLattice.LabeledValue<T>(LabelList.empty,val,null));
  }
  
  
  @Override
  protected void transferSplit(Split node) {
    ControlEdge in = node.getInput();
    ControlEdge out1 = node.getOutput1();
    ControlEdge out2 = node.getOutput2();
    if (node instanceof Fork) {
      doNOPtransfer(in,out1);
      doNOPtransfer(in,out2);
    } else if (node instanceof Choice) {
      if (node instanceof LabelTest) {
        doTransfer(in,out1,testLabelTrueOp,(LabelTest)node);
        doTransfer(in,out2,testLabelFalseOp,(LabelTest)node);
      } else if (node instanceof ComponentChoice) {
        doTransfer(in,out1,componentChoiceTrueTransfer,(ComponentChoice)node);
        doTransfer(in,out2,componentChoiceFalseTransfer,(ComponentChoice)node);
      } else {
        LOG.severe("Unknown Choice node " + node);
      }
    } else if (node instanceof DynamicSplit) {
      DynamicSplit ds = (DynamicSplit) node;
      // control could go both ways!
      if (ds.test(true)) {
        doNOPtransfer(in,out1);
      }
      if (ds.test(false)) {
        doNOPtransfer(in,out2);
      }
    } else if (node instanceof TrackedDemerge) {
      doTransfer(in,out1,dropLabelOp,TrackLabel.trueTrack);
      doTransfer(in,out2,dropLabelOp,TrackLabel.falseTrack);
    } else {
      LOG.severe("unknown Split " + node);
    }  
  }
  
  @Override
  protected void transferPort(
      OutputPort port,
      InputPort dual) {
    if (port instanceof SimpleOutputPort) {
      ControlEdge in = ((SimpleOutputPort)port).getInput();
      if (dual instanceof SimpleInputPort) {
        doNOPtransfer(in,((SimpleInputPort)dual).getOutput());
      } else if (dual instanceof DoubleInputPort) {
        ControlEdge out1 = ((DoubleInputPort) dual).getOutput1();
        ControlEdge out2 = ((DoubleInputPort) dual).getOutput2();
        if (port instanceof ComponentPort) {
          IRNode node = ((ComponentPort) port).getComponent().getSyntax();
          doTransfer(in,out1,conditionalTrueTransfer,node);
          doTransfer(in,out2,conditionalFalseTransfer,node);
        } else {
          //? I do not think this happens:
          LOG.warning("unexpected port split: " + port);
          doNOPtransfer(in,out1);
          doNOPtransfer(in,out2);
        }
      } else if (dual instanceof BlankInputPort) {
        /* do nothing */
      } else {
        LOG.severe("unknown InputPort " + dual);
      }
    } else if (port instanceof DoubleOutputPort) {
      DoubleOutputPort dop = ((DoubleOutputPort)port);
      ControlEdge in1 = dop.getInput1();
      ControlEdge in2 = dop.getInput2();
      if (dual instanceof SimpleInputPort) {
        doTransfer(in1,in2,((SimpleInputPort)dual).getOutput(),infoLattice.joinCombiner,null);
      } else if (dual instanceof DoubleInputPort) {
        DoubleInputPort dip = ((DoubleInputPort)dual);
        doNOPtransfer(in1,dip.getOutput1());
        doNOPtransfer(in2,dip.getOutput2());
      } else if (dual instanceof BlankInputPort) {
        /* do nothing */
      } else {
        LOG.severe("unknown InputPort " + dual);
      }
    } else {
      LOG.severe("unknown OutputPort " + port);
    }
  }
  
  @Override
  protected void reportMonotonicityError(ControlEdge e) {
    reportMonotonicityError(e.getSource());
  }
}
