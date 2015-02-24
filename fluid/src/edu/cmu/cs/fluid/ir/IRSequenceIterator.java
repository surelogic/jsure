/* $Header$ */
package edu.cmu.cs.fluid.ir;

import java.util.*;

import com.surelogic.common.util.*;

/** Iterator of elements in an IRSequence.
 */
public class IRSequenceIterator<T> extends AbstractRemovelessIterator<T> {
  private IRSequence<T> seq;
  private IRLocation next;

  public IRSequenceIterator(IRSequence<T> s) {
    seq = s;
    next = s.firstLocation();
  }

  public boolean hasNext() {
    return next != null;
  }

  public T next() throws NoSuchElementException {
    try {
      try {
        return seq.elementAt(next);
      } finally {
        next = seq.nextLocation(next);
      }
    } catch (NullPointerException ex) {
      throw new NoSuchElementException("end of sequence");
    }
  }
}