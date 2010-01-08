/*
 * Created on Jul 10, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.FlowAnalysis;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.CachedSet;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.util.SetException;
import edu.cmu.cs.fluid.java.analysis.LocationGenerator.SimpleLocation;
/**
 * Simple analysis to manipulate locations, show that they work (more or less)
 */
@Deprecated
public class TestLocationAnalysis extends TrackingIntraproceduralAnalysis {

	/**
	 * @param b
	 */
	public TestLocationAnalysis(IBinder b) {
		super(b);
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis#createAnalysis(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
  protected FlowAnalysis createAnalysis(IRNode flowUnit) {
		LocationGenerator gen = new LocationGenerator(this);
		FlowUnit op = (FlowUnit)tree.getOperator(flowUnit);
		IRNode methodDecl = getRawFlowUnit(flowUnit);
		LocationMap lm = new LocationMap(methodDecl,binder,gen);
		FlowAnalysis analysis =
			new TrackingForwardAnalysis("test of location analysis",
					lm,new TestLocationTransfer(this, binder));

		ImmutableHashOrderSet paramset = getParams(methodDecl,CachedSet.getEmpty());
		try {
			ILocationMap start = (ILocationMap)lm.bottom();
			int n = paramset.size();
			for (int i=0; i < n; ++i) {
				IRNode local = (IRNode)paramset.elementAt(i);
				start = start.replaceLocation(local,gen.getLocation(local));
			}
			analysis.initialize(op.getSource(flowUnit).getOutput(),start);
			//analysis.debug();
			return analysis;
		} catch (SetException e) {
			throw new FluidRuntimeException("infinite number of locals?");
		}
	}

	public ImmutableHashOrderSet getParams(IRNode methodDecl, ImmutableHashOrderSet s) {
		//!! does not work for class initialization methods:
		Iterator e = tree.bottomUp(methodDecl);
		try {
			while (true) {
				IRNode node = ((IRNode)e.next());
				Operator op = tree.getOperator(node);
				if (op instanceof ParameterDeclaration)
					s = s.addElement(node);
				}
		} catch (NoSuchElementException ex) {
			return s;
		}
		
	}

}
@Deprecated
class TestLocationTransfer extends JavaForwardTransfer{
	
	/*
	 * @param base
	 * @param binder
	 */
	public TestLocationTransfer(IntraproceduralAnalysis base, IBinder binder) {
		super(base, binder);
	}

	@Override
  public Lattice transferAssignment(IRNode node, Lattice value) {
//			LOG.debug(DebugUnparser.toString(node) + " " + value);
      AssignmentInterface op = (AssignmentInterface)tree.getOperator(node);
			IRNode lhs = op.getTarget(node);
			Operator lhsOp = tree.getOperator(lhs);
			LocationMap lm = (LocationMap)value;
			if (lhsOp instanceof VariableUseExpression) {
				IRNode decl = binder.getBinding(lhs);
				SimpleLocation l = lm.getLocation(op.getSource(node));
				return lm.replaceLocation(decl,l);
			}
			return value;
		}
		@Override
    public Lattice transferInitialization(IRNode node, Lattice value) {
			LocationMap lm = (LocationMap)value;
			if (tree.getOperator(node) instanceof VariableDeclarator) {
				IRNode source = VariableDeclarator.getInit(node);
				if(tree.getOperator(source) instanceof Initialization){
					source = Initialization.getValue(source);
				}
				SimpleLocation l = lm.getLocation(source);
				return lm.replaceLocation(node,l);
			}
			return value;
		}
}
