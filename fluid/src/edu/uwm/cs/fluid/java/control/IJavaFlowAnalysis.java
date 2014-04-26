package edu.uwm.cs.fluid.java.control;

import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.control.LabelList;
import edu.cmu.cs.fluid.control.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.uwm.cs.fluid.control.IFlowAnalysis;
import edu.uwm.cs.fluid.util.Lattice;

public interface IJavaFlowAnalysis<T, L extends Lattice<T>> extends IFlowAnalysis<T, L> {
  public T getInfo(ControlEdge edge, LabelList ll);

  public T getAfter(IRNode node, WhichPort port);  
  
  public void initialize(ControlEdge edge, LabelList ll, T value);
  public void initialize(ControlEdge edge, T value);

  public SubAnalysisFactory<L, T> getSubAnalysisFactory();
}
