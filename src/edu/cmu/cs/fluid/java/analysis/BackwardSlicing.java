/*
 * Created on May 18, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.BackwardAnalysis;
import edu.cmu.cs.fluid.control.FlowAnalysis;
import edu.cmu.cs.fluid.control.LabelList;
import edu.cmu.cs.fluid.control.UnknownLabel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.FlowUnit;
import edu.cmu.cs.fluid.util.BooleanLattice;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.util.PairLattice;
import edu.cmu.cs.fluid.util.StackLattice;
import edu.cmu.cs.fluid.util.UnionLattice;
/**
 * @author mbendary
 */
public class BackwardSlicing<V> extends IntraproceduralAnalysis<PairLattice.Type<IRNode,Boolean>,V> {
  /**
	 * @param b
	 */
	public BackwardSlicing(IBinder b) 
	{
		super(b);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis#createAnalysis(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
  protected FlowAnalysis<PairLattice.Type<IRNode,Boolean>> createAnalysis(IRNode flowUnit)
	{
		StackLattice<Boolean> stack = new StackLattice<Boolean>(BooleanLattice.orLattice); 
		Lattice<PairLattice.Type<IRNode,Boolean>> latt = new PairLattice<IRNode,Boolean>(new UnionLattice<IRNode>(),stack);   
		FlowAnalysis<PairLattice.Type<IRNode,Boolean>> analysis =
	        new BackwardAnalysis<PairLattice.Type<IRNode,Boolean>>("backward slicing analysis",
				     latt,new BackwardSlicingTransfer<V>(this,binder), DebugUnparser.viewer, 0);
	    FlowUnit op = (FlowUnit)tree.getOperator(flowUnit);
	    
	    PairLattice<IRNode,Boolean> init = new PairLattice<IRNode,Boolean>(new UnionLattice<IRNode>(),
                                                                         new StackLattice<Boolean>(BooleanLattice.orLattice).empty());
		analysis.initialize(op.getSource(flowUnit).getOutput(),init);
		analysis.initialize(op.getNormalSink(flowUnit).getInput(),init);
	    analysis.initialize(op.getAbruptSink(flowUnit).getInput(),
			      LabelList.empty.addLabel(UnknownLabel.prototype),
			      init);
		return analysis;
	} 
}
