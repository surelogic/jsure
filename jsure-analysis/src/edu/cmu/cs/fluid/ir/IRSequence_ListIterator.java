/* $Header$ */
package edu.cmu.cs.fluid.ir;

import java.util.*;

import com.surelogic.common.util.*;

/** List Iterator of elements in an IRSequence.
 */
public class IRSequence_ListIterator<T> extends AbstractIterator<T>
implements ListIteratable<T>
{
  //static final IRLocation START = new IRLocation(Integer.MAX_VALUE-1);
  static final IRLocation END = null; // new IRLocation(Integer.MAX_VALUE);
  
  private IRSequence<T> seq;
  private IRLocation last = null;
  private IRLocation loc;

  public IRSequence_ListIterator(IRSequence<T> s, int start) {
    seq = s;
    
    final int size = seq.size();
    if (start > size) {
      throw new IllegalArgumentException(start+" > size "+size);
    }
    else if (start == size) {
      loc = null;
    } 
    else {
      loc = s.location(start);
    }
  }
  
  @Override
  public boolean hasNext() {
    return loc != END;
  }

  @Override
  public T next() throws NoSuchElementException {
    try {
      if (loc == END) {
        throw new NoSuchElementException("end of sequence");
      }
      try {
        last = loc;
        return seq.elementAt(loc);
      } finally {
        loc = seq.nextLocation(loc);
        if (loc == null) {
          loc = END;
        }
      }
    } catch (NullPointerException ex) {
      throw new NoSuchElementException("end of sequence");
    }
  }

  @Override
  public boolean hasPrevious() {
    IRLocation prev;
    if (loc == END) {
      prev = seq.lastLocation();
    } else {
      prev = seq.prevLocation(loc);
    }
    return prev != null;
  }

  @Override
  public T previous() {
    try {      
      if (loc == END) {
        loc = seq.lastLocation();
      } else {
        loc = seq.prevLocation(loc);
      }
      last = loc;
      return seq.elementAt(loc);
    } catch (NullPointerException ex) {
      throw new NoSuchElementException("end of sequence");
    }
  }

  @Override
  public int nextIndex() {
    if (loc == END) {
      throw new IllegalStateException("No next location");
    }
    return seq.locationIndex(loc);
  }

  @Override
  public int previousIndex() {
    IRLocation prev;
    if (loc == END) {
      prev = seq.lastLocation();
    } else {
      prev = seq.prevLocation(loc);
    }
    if (prev == null) {
      throw new IllegalStateException("No previous location");
    }
    return seq.locationIndex(prev);
  }

  @Override
  public void set(T val) {
    if (last == null) {
      throw new IllegalStateException("No location to set");
    }
    seq.setElementAt(val, last);
  }

  @Override
  public void add(T val) {
    if (loc == END) {
      seq.appendElement(val); 
    } else {
      seq.insertElementBefore(val, loc);
    }
    last = null; 
  }
  
  @Override
  public void remove() {
    if (last == null) {
      throw new IllegalStateException("No location to set");
    }
    seq.removeElementAt(last);
    last = null;
  }
}