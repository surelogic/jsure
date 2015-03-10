/*
 * Created on Jul 14, 2003
 */
package edu.cmu.cs.fluid.ir;

import java.util.NoSuchElementException;

import com.surelogic.common.util.*;

/**
 * Iterator over IRSequences that skips over invalid entries.
 * Supports removal of elements using the {@link #remove} method.
 */
public final class IRSequenceValidatingIterator<T> extends AbstractIterator<T> {
  /** The sequence being iterated over */
  private IRSequence seq;
  /** The location that will be used by remove */
  private IRLocation remove;
  /** The location that will be used by next */
  private IRLocation next;

  public IRSequenceValidatingIterator( final IRSequence s) {
    seq = s;
    remove = null;
    next = nextValidLocation( s.firstLocation() );
  }

  /**
   * Get the next valid location in the sequence starting at the
   * given location.  (That is, if <code>loc</code> is valid, return 
   * <code>loc</code>, otherwise find the next location that is valid.)
   * Returns <code>null</code> if their is no such location.
   * <em>Assumes <code>loc</code> is non-null!</em>
   */
  private IRLocation nextValidLocation( IRLocation loc ) {
    while( (loc != null) && !seq.validAt(loc) ) {
      loc = seq.nextLocation( loc );
    }
    return loc;
  }

  public boolean hasNext() {
    return next != null;
  }

  @SuppressWarnings("unchecked")
  public T next() throws NoSuchElementException {
    if( next == null ) {
      throw new NoSuchElementException( "End of Sequence");
    } else {
      final Object elt = seq.elementAt( next );
      remove = next;
      next = nextValidLocation( seq.nextLocation( next ) );
      return (T) elt;
    }
  }

  public void remove() {
    seq.removeElementAt(remove);
  }
}
