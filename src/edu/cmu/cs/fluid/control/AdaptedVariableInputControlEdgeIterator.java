package edu.cmu.cs.fluid.control;

import java.util.NoSuchElementException;

/**
 * TODO Add Comments
 * @author Scott Wisniewski
 */
class AdaptedVariableInputControlEdgeIterator extends VariableInputControlEdgeIterator {
	private ControlEdgeIterator wrapped_;
	
	public AdaptedVariableInputControlEdgeIterator(ControlEdgeIterator wrapped) {
		wrapped_ = wrapped;
	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.VariableInputControlEdgeIterator#nextVariableInputControlEdge()
	 */
  @Override
	public VariableInputControlEdge nextVariableInputControlEdge() throws NoSuchElementException {
		return (VariableInputControlEdge)wrapped_.nextControlEdge();
	}
	
	/**
	 * @see java.util.Iterator#hasNext()
	 */
  @Override
	public boolean hasNext() {
		return wrapped_.hasNext();
	}
	
	/**
	 * @see edu.cmu.cs.fluid.control.ControlEdgeIterator#nextControlEdge()
	 */
  @Override
	public ControlEdge nextControlEdge() throws NoSuchElementException {
		return nextVariableInputControlEdge();
	}
}
