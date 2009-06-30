/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRCompound.java,v 1.6 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;

/** Compound values are containers whose contents can change
 * without affecting equals(...) on the container.
 * NB: If a compound has internal slots, then when read from a file,
 * the "old" factory should be used.
 * A concrete class that implements this interface should extend 
 * {@link java.util.Observable}
 * @see IRAbstractState
 */
public interface IRCompound<T> extends IRState {
  /** Write the contents of the container to output. */
  public void writeContents(IRCompoundType<T> t, IROutput out) throws IOException;

  /** Read the contents of a container from input. */
  public void readContents(IRCompoundType<T> t, IRInput in) throws IOException;

  /** Have the contents changed from their "previous values"
   * (initial values) ?
   */
  public boolean isChanged();

  /** Write the contents of a changed container to output. */
  public void writeChangedContents(IRCompoundType<T> t, IROutput out)
       throws IOException;

  /** Read the contents of a changed container from input. */
  public void readChangedContents(IRCompoundType<T> t, IRInput in)
       throws IOException;
}
  
