/* $Header$ */
package edu.cmu.cs.fluid.control;

import java.util.NoSuchElementException;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Class to represent sequences of control nodes of Java programs.
 * @author John Tang Boyland
 * @see EmptyControlEdgeIterator
 * @see SingleControlEdgeIterator
 * @see PairControlEdgeIterator
 */
public abstract class ControlEdgeIterator extends AbstractRemovelessIterator<IRNode> {
  @Override
  public IRNode next() throws NoSuchElementException {
    return nextControlEdge();
  }
  @Override
  abstract public boolean hasNext();
  abstract public ControlEdge nextControlEdge() throws NoSuchElementException;
}
