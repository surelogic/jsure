/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/EmptyIRSequence.java,v 1.27 2008/06/27 19:10:35 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import edu.cmu.cs.fluid.util.*;

public final class EmptyIRSequence<T> extends IRAbstractState<T> implements IRSequence<T>, List<T> {
  @SuppressWarnings("unchecked")
  static public final EmptyIRSequence prototype = new EmptyIRSequence();

  @SuppressWarnings("unchecked")
  public static <T> IRSequence<T> prototype() { return prototype; }  
  
  public EmptyIRSequence() {
  }

  public int size() {
    return 0;
  }

  public boolean isVariable() {
    return false;
  }

  public boolean hasElements() {
    return false;
  }
  public Iteratable<T> elements() {
    return EmptyIterator.prototype();
  }

  public boolean validAt(int i) {
    throw new IRSequenceException("empty sequence is empty");
  }
  public boolean validAt(IRLocation loc) {
    throw new IRSequenceException("empty sequence is empty");
  }
  public T elementAt(int i) {
    throw new IRSequenceException("empty sequence is empty");
  }
  public T elementAt(IRLocation loc) {
    throw new IRSequenceException("empty sequence is empty");
  }
  public void setElementAt(T element, int i) {
    throw new IRSequenceException("empty sequence is empty");
  }
  public void setElementAt(T element, IRLocation loc) {
    throw new IRSequenceException("empty sequence is empty");
  }

  public IRLocation insertElement(T element) {
    throw new IRSequenceException("empty sequence cannot be added to");
  }
  public IRLocation appendElement(T element) {
    throw new IRSequenceException("empty sequence cannot be added to");
  }
  public IRLocation insertElementAfter(T element, IRLocation i) {
    throw new IRSequenceException("empty sequence cannot be added to");
  }
  public IRLocation insertElementBefore(T element, IRLocation i) {
    throw new IRSequenceException("empty sequence cannot be added to");
  }
  public void removeElementAt(IRLocation i) {
    throw new IRSequenceException("empty sequence cannot be added to");
  }

  public IRLocation location(int i) {
    throw new IllegalArgumentException("empty sequence has no locations");
  }
  public int locationIndex(IRLocation loc) {
    throw new IllegalArgumentException("empty sequence has no locations");
  }

  public IRLocation firstLocation() {
    return null;
  }
  public IRLocation lastLocation() {
    return null;
  }
  public IRLocation nextLocation(IRLocation loc) {
    throw new IllegalArgumentException("empty sequence has no locations");
  }
  public IRLocation prevLocation(IRLocation loc) {
    throw new IllegalArgumentException("empty sequence has no locations");
  }

  public int compareLocations(IRLocation loc1, IRLocation loc2) {
    throw new IllegalArgumentException("empty sequence has no locations");
  }

  public void writeValue(IROutput out) {
  }

  public IRSequence<T> readValue(IRInput in, IRSequence<T> current)
    throws IOException {
    if (current != null) {
      if (current.isVariable() || current.size() != 0)
        throw new IOException("re-reading sequence as empty!");
    }
    return prototype();
  }

  public void writeContents(IRCompoundType<IRSequence<T>> ty, IROutput out) {
  }
  public void readContents(IRCompoundType<IRSequence<T>> ty, IRInput in) {
  }

  public boolean isChanged() {
    return false;
  }
  public void writeChangedContents(IRCompoundType<IRSequence<T>> ty, IROutput out) {
  }
  public void readChangedContents(IRCompoundType<IRSequence<T>> ty, IRInput in) {
  }
  
  public void describe(PrintStream out) {
    out.println("EmptyIRSequence");
  }

  /* (non-Javadoc)
   * not actually mutable.  parent is null.
   * @see edu.cmu.cs.fluid.ir.IRState#getParent()
   */
  @Override
  public void setParent(IRState st) {
    // do nothing
  }
  @Override
  protected SlotFactory getSlotFactory() {
    return ConstantSlotFactory.prototype;
  }

  //===========================================================
  // Implementation of List
  //===========================================================

  public boolean isEmpty() {
    return true;
  }

  public boolean contains(Object arg0) {
    return false;
  }

  public Iterator<T> iterator() {
    return EmptyIterator.prototype();
  }
  
  public Object[] toArray() {
    return noObjects;
  }

  public <E> E[] toArray(E[] a) {
    return a;
  }

  public boolean add(T arg0) {
    throw new UnsupportedOperationException("empty sequence cannot be modified");
  }

  public boolean remove(Object arg0) {
    throw new UnsupportedOperationException("empty sequence cannot be modified");
  }

  public boolean containsAll(Collection<?> c) {
    return c.isEmpty();
  }

  public boolean addAll(Collection<? extends T> arg0) {
    throw new UnsupportedOperationException("empty sequence cannot be modified");
  }

  public boolean addAll(int arg0, Collection<? extends T> arg1) {
    throw new UnsupportedOperationException("empty sequence cannot be modified");
  }

  public boolean removeAll(Collection<?> arg0) {
    throw new UnsupportedOperationException("empty sequence cannot be modified");
  }

  public boolean retainAll(Collection<?> arg0) {
    return false;
  }

  public void clear() {
    throw new UnsupportedOperationException("empty sequence cannot be modified");
  }

  public T get(int i) {
    throw new IllegalArgumentException("No such element: "+i);
  }

  public T set(int arg0, T arg1) {
    throw new UnsupportedOperationException("empty sequence cannot be modified");
  }

  public void add(int arg0, T arg1) {
    throw new UnsupportedOperationException("empty sequence cannot be modified");
  }

  public T remove(int arg0) {
    throw new UnsupportedOperationException("empty sequence cannot be modified");
  }

  public int indexOf(Object arg0) {
    return -1;
  }

  public int lastIndexOf(Object arg0) {
    return -1;
  }

  public ListIterator<T> listIterator() {
    return EmptyIterator.listIterator();
  }

  public ListIterator<T> listIterator(int arg0) {
    return EmptyIterator.listIterator();
  }

  public List<T> subList(int i1, int i2) {
    /*
    if (i1 != i2) {
      throw new IllegalArgumentException(i1+" != "+i2);
    }
    */
    return this;
  }
}
