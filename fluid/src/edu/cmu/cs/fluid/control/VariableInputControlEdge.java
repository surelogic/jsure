package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.NotImplemented;


/**
 * Defines an edge between two ArbitaryInputControlNodes.
 * 
 * @author Scott Wisniewski
 */
public class VariableInputControlEdge extends ControlEdge {

	private ControlNode source_;
	private ControlNode sink_;
	private boolean sourceIsSecondary_;
	
	public VariableInputControlEdge(ControlNode source, ControlNode sink) {
		super();
		attach(source, sink);
	}
	
	/** 
	 * @see edu.cmu.cs.fluid.control.ControlEdge#getSource()
	 */
	@Override public ControlNode getSource() {
		return source_;
	}
	
	/** 
	 * @see edu.cmu.cs.fluid.control.ControlEdge#getSink()
	 */
	@Override public ControlNode getSink() {
		return sink_;

	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.ControlEdge#sourceIsSecondary()
	 */
	@Override public boolean sourceIsSecondary() {
		return sourceIsSecondary_;
	}
	
	/**
	 * Indicates wether or not this edge is the second edge to go
	 * into the sink. For this class, this method always returns false.
	 * The notion of "the the seconnd incoming edge" is meaningless
	 * for AribitraryInputControlNodes because they can have an arbitrary number
	 * of in-edges. 
	 * @return false
	 * @see edu.cmu.cs.fluid.control.ControlEdge#sinkIsSecondary()
	 */
	@Override public boolean sinkIsSecondary() {
		return false;
	}
	
	/** 
	 * @see edu.cmu.cs.fluid.control.ControlEdge#setSource(edu.cmu.cs.fluid.control.ControlNode, boolean)
	 */
	@Override
  protected void setSource(ControlNode source, boolean secondary) {
		source_ = source;
		sourceIsSecondary_ = secondary;
	}

	/**
	 * @see edu.cmu.cs.fluid.control.ControlEdge#setSink(edu.cmu.cs.fluid.control.ControlNode, boolean)
	 */
	@Override
  protected void setSink(ControlNode sink, boolean secondary) {
			sink_ = sink;
	}
	
	/** 
	 * @see edu.cmu.cs.fluid.control.ControlEdge#attachSink(edu.cmu.cs.fluid.control.ControlNode)
	 */
	protected void attachSink(ControlNode source, ControlNode sink) throws EdgeLinkageError {
		setSink(sink, false);
		if (sink != null)
			((VariableInputControlNode)sink).indicateInputEdge(source, this);
	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.ControlEdge#attachSource(edu.cmu.cs.fluid.control.ControlNode)
	 */
	protected void attachSource(ControlNode source, ControlNode sink) throws EdgeLinkageError {
		if (source instanceof OneOutput) {
			OneOutput typedSource = (OneOutput)source;
			if (typedSource.getOutput() != null)
				throw new EdgeLinkageError(SOURCE_ALREADY_TAKEN);
			else {
				typedSource.setOutput(this);
				setSource(source, false);
			}
		}
		else if (source instanceof TwoOutput) {
			TwoOutput typedSource = (TwoOutput)source;
			if (typedSource.getOutput1() != null && typedSource.getOutput2() != null)
				throw new EdgeLinkageError(SOURCE_FULL);
			else if (typedSource.getOutput1() == null) {
				typedSource.setOutput1(this);
				setSource(source, false);
			}
			else {
				typedSource.setOutput2(this);
				setSource(source, true);
			}
		}
		else if (source instanceof VariableOutput) {
			VariableOutput typedSource = (VariableOutput)source;
			typedSource.indicateOutputEdge(sink, this);
			setSource(source, false);
		}
		else if (source == null)
			setSource(source, false);
		else
			throw new EdgeLinkageError(SOURCE_TYPE_NOT_SUPPORTED);
	}	
	
	/**
	 * TODO document this method 
	 * @see edu.cmu.cs.fluid.control.ControlEdge#attachSink(edu.cmu.cs.fluid.control.ControlNode)
	 */
	@Override
  protected void attachSink(ControlNode sink) throws EdgeLinkageError {
		throw new NotImplemented();
	}
	
	/**
	 * TODO document this method
	 * @see edu.cmu.cs.fluid.control.ControlEdge#attachSource(edu.cmu.cs.fluid.control.ControlNode)
	 */
	@Override
  protected void attachSource(ControlNode source) throws EdgeLinkageError {
		throw new NotImplemented();
	}
	
	/**
	 * TODO document this method
	 * @see edu.cmu.cs.fluid.control.ControlEdge#attach(edu.cmu.cs.fluid.control.ControlNode, edu.cmu.cs.fluid.control.ControlNode)
	 */
	@Override
  protected void attach(ControlNode source, ControlNode sink) throws EdgeLinkageError {
		attachSource(source, sink);
		attachSink(source, sink);
	}	
}
