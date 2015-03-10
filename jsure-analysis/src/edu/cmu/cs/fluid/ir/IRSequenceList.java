/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRSequenceList.java,v 1.5 2007/05/17 18:57:53 chance Exp $*/
package edu.cmu.cs.fluid.ir;

import java.util.*;
import com.surelogic.Starts;

/**
 * A wrapper that adapts an IRSequence as a {@link java.util.List}.
 * @author boyland
 */
public class IRSequenceList<T> extends AbstractSequentialList<T> implements List<T> {
  private final IRSequence<T> underlying;
  
  public IRSequenceList(IRSequence<T> seq) {
    underlying = seq;
  }

  private class Iterator implements ListIterator<T> {
    IRLocation loc;
    boolean forward = true;
    
    public Iterator(IRLocation l) {
      loc = l;
    }
    
    /* (non-Javadoc)
     * @see java.util.ListIterator#hasNext()
     */
    @Override
    public boolean hasNext() {
      if (loc == null) return underlying.firstLocation() != null;
      return underlying.nextLocation(loc) != null;
    }
    
    /* (non-Javadoc)
     * @see java.util.ListIterator#next()
     */
    @Override
    public T next() {
      forward = true;
      if (loc == null) loc = underlying.firstLocation();
      else loc = underlying.nextLocation(loc);
      if (loc == null) {
        throw new NoSuchElementException("next() at end of IRSequenceList");
      }
      return underlying.elementAt(loc);
    }
    
    /* (non-Javadoc)
     * @see java.util.ListIterator#hasPrevious()
     */
    @Override
    public boolean hasPrevious() {
      return loc != null;
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#previous()
     */
    @Override
    public T previous() {
      forward = false;
      if (loc == null) {
        throw new NoSuchElementException("previous() at start of IRSequenceList");
      }
      T result = underlying.elementAt(loc);
      loc = underlying.prevLocation(loc);
      return result;
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#nextIndex()
     */
    @Override
    public int nextIndex() {
      if (loc == null) return 0;
      return underlying.locationIndex(loc)+1;
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#previousIndex()
     */
    @Override
    public int previousIndex() {
      if (loc == null) return -1;
      return underlying.locationIndex(loc);
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#remove()
     */
    @Override
    public void remove() {
      if (forward) {
        if (loc == null) throw new IllegalStateException("next() not called yet.");
        IRLocation prevLoc = underlying.prevLocation(loc);
        underlying.removeElementAt(loc);
        loc = prevLoc;
      } else {
        IRLocation nextLoc;
        if (loc == null) {
          nextLoc = underlying.firstLocation();
        } else {
          nextLoc = underlying.nextLocation(loc);
        }
        if (nextLoc == null) throw new IllegalStateException("already removed");
        underlying.removeElementAt(nextLoc);
      }
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#set(E)
     */
    @Override
    public void set(T o) {
      if (forward) {
        if (loc == null) throw new IllegalStateException("next() not called yet.");
        underlying.setElementAt(o,loc);
      } else {
        IRLocation nextLoc;
        if (loc == null) {
          nextLoc = underlying.firstLocation();
        } else {
          nextLoc = underlying.nextLocation(loc);
        }
        if (nextLoc == null) throw new IllegalStateException("previous() not called yet.");
        underlying.setElementAt(o,nextLoc);
      }
    }
    /* (non-Javadoc)
     * @see java.util.ListIterator#add(E)
     */
    @Override
    public void add(T o) {
      if (loc == null) loc = underlying.insertElement(o);
      else loc = underlying.insertElementAfter(o,loc);
    }
  }
  
  /* (non-Javadoc)
   * @see java.util.AbstractSequentialList#listIterator(int)
   */
  @Starts("nothing")
@Override
  public ListIterator<T> listIterator(int index) {
    if (index == 0) return new Iterator(null);
    else return new Iterator(underlying.location(index-1));
  }

  /* (non-Javadoc)
   * @see java.util.AbstractCollection#size()
   */
  @Starts("nothing")
@Override
  public int size() {
    return underlying.size();
  }
  
}