package edu.cmu.cs.fluid.control;

import java.util.NoSuchElementException;

/**
 * @author Scott Wisniewski
 */
public abstract class VariableInputControlEdgeIterator extends ControlEdgeIterator {
	public abstract VariableInputControlEdge nextVariableInputControlEdge() throws NoSuchElementException;
	
}
