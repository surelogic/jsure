package edu.cmu.cs.fluid.control;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Defines an enumerator for collections of control edges.
 * It operates by aggregating the <emph>iterator</emph> for the underlying
 * collection.
 *  
 * @author Scott Wisniewski
 */
class GeneralControlEdgeIterator extends VariableInputControlEdgeIterator {
	
	private Iterator<ControlEdge> sequence_;
	
	/**
	 * Constructs a GeneralControlEdgeIterator instance.
	 * 
	 * @param sequence The iterator for the underlying
	 * sequence of control nodes.
	 */
	public GeneralControlEdgeIterator(Iterator<ControlEdge> sequence) {
		sequence_ = sequence;
	}
	
	/** 
	 * Returns true if the current position
	 * of the enumerator is not beyond the end
	 * of the enumerated sequence.
	 * 
	 * @see java.util.Enumeration#hasMoreElements()
	 */
  @Override
	public boolean hasNext() {
		return sequence_.hasNext();
	}
	
	/**
	 * Provides a strongly typed version of nextElement().
	 * 
	 * @see edu.cmu.cs.fluid.control.ControlEdgeEnumeration#nextControlEdge()
	 */
  @Override
	public ControlEdge nextControlEdge() throws NoSuchElementException {	
		return nextVariableInputControlEdge();
	}
	/**
	 * @see edu.cmu.cs.fluid.control.VariableInputControlEdgeIterator#nextVariableInputControlEdge()
	 */
  @Override
	public VariableInputControlEdge nextVariableInputControlEdge() throws NoSuchElementException {
		return (VariableInputControlEdge) sequence_.next();
	}
}
