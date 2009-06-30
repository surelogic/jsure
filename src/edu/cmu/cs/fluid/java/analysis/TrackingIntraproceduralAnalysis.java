/*
 * Created on Dec 9, 2003
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRType;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.control.ControlEdge;
import edu.cmu.cs.fluid.util.Lattice;
import edu.cmu.cs.fluid.control.ForwardAnalysis;
import edu.cmu.cs.fluid.control.BackwardAnalysis;
import edu.cmu.cs.fluid.control.ForwardTransfer;
import edu.cmu.cs.fluid.control.BackwardTransfer;
import edu.cmu.cs.fluid.control.LabelList;

/**
 *  IntraproceduralAnalysis that keeps track of incoming control edge for meets
 */
/*
 * Not really the right way to do this.  We should expand FlowAnalysis to be able to
 * override it so that it tracks this information itself (e.g. add a method doMeet() for 
 * meets that has the extra info.  But it seems to work for now.
 */
public abstract class TrackingIntraproceduralAnalysis<T,V> extends IntraproceduralAnalysis<T,V> {

	/*
	 * I assume we never interleave executions of more than one flow analysis within the same
	 * Intraprocedural analysis.
	 */
	protected ControlEdge latest;

    protected IRNode userSet;
  
	/**
	 * @param b
	 */
	public TrackingIntraproceduralAnalysis(IBinder b) {
		super(b);
		latest = null;
		userSet = null;
    }

	/**
	 * @param name
	 * @param type
	 * @param b
	 * @throws SlotAlreadyRegisteredException
	 */
	public TrackingIntraproceduralAnalysis(String name, IRType<V> type, IBinder b)
		throws SlotAlreadyRegisteredException {
		super(name, type, b);
		latest = null;
	}

	public ControlEdge getLatestEdge(){
		return latest;
	}

	public IRNode getLatestSink(){
		return (latest != null)?latest.getSink():null;
	}
    
    public IRNode getUserNode(){
      return userSet;
    }
  
    public void setUserNode(IRNode u){
      userSet = u;
    }
  
	public class TrackingForwardAnalysis extends ForwardAnalysis<T> {
		public TrackingForwardAnalysis(String name, Lattice<T> l, ForwardTransfer<T> t) {
			super(name, l, t, DebugUnparser.viewer);
			latest = null;
		}
		
		@Override
    public Lattice<T> getInfo(ControlEdge edge, LabelList ll) {
			latest = edge;
			return super.getInfo(edge, ll);
		}

		@Override
    public Lattice<T> getInfo(ControlEdge edge) {
			latest = edge;
			return super.getInfo(edge);
		}

		@Override
    protected void setInfo(ControlEdge edge, LabelList ll, Lattice<T> value) {
			latest = edge;
			super.setInfo(edge, ll, value);
		}

	}
	
	public class TrackingBackwardAnalysis extends BackwardAnalysis<T> {
		public TrackingBackwardAnalysis(String name, Lattice<T> l, BackwardTransfer<T> t) {
			super(name, l, t, DebugUnparser.viewer);
			latest = null;
		}
		
		@Override
    public Lattice<T> getInfo(ControlEdge edge, LabelList ll) {
			latest = edge;
			return super.getInfo(edge, ll);
		}

		@Override
    public Lattice<T> getInfo(ControlEdge edge) {
			latest = edge;
			return super.getInfo(edge);
		}

		@Override
    protected void setInfo(ControlEdge edge, LabelList ll, Lattice<T> value) {
			latest = edge;
			super.setInfo(edge, ll, value);
		}

	}

}
