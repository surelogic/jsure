/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRAbstractArray.java,v 1.12 2007/11/08 17:45:52 chance Exp $
 */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import edu.cmu.cs.fluid.util.*;
import com.surelogic.Starts;

/**
 * A class that functions as an array.
 * It avoids adding fields.
 * @author boyland
 */
public abstract class IRAbstractArray<S,T> extends IRAbstractSequence<S,T> {
  protected IRAbstractArray() {
  }
  
  /**
   * Get the (unchanging) slot storage for this state.
   * @return the slot storage for this state.
   */
  public abstract SlotStorage<S,T> getSlotStorage();
  
  @Override
  public SlotFactory getSlotFactory() {
    return getSlotStorage().getSlotFactory();
  }

  /**
   * Set up all the slots with the empty slot.
   */
  protected void initialize() {
    int size = size();
    S initial = getSlotStorage().newSlot();
    for (int i=0; i < size; ++i) {
      setInternal(i,initial);
    }
  }

  /**
   * Check whether index is good
   * @param i index in range [0,size) (checked)
   * @throws IRSequenceException if index out of range
   */
  protected final void check(int i) throws IRSequenceException {
    if (i < 0 || size() <= i) {
      throw new IRSequenceException("Index " + i + " out of range [0," + size() + ")");
    }
  }
  
  /**
   * Return the slot state of element i.
   * @param i index in range [0,size) (unchecked)
   * @return slot state for element i
   */
  protected abstract S getInternal(int i);
  
  /**
   * Set the slot state for element i
   * @param i index in range [0,size) (unchecked)
   * @param newState state to set
   */
  protected abstract void setInternal(int i, S newState);
  
  public boolean isVariable() {
    return false;
  }

  public boolean hasElements() {
    return size() > 0;
  }

  public Iteratable<T> elements() {
    return getSlotFactory().newIterator(new MyIterator());
  }

  public boolean validAt(int i) {
    check(i);
    return getSlotStorage().isValid(getInternal(i));
  }
  public boolean validAt(IRLocation loc) {
    return loc != null && validAt(loc.getID());
  }

  public T elementAt(int i) {
    check(i);
    return getSlotStorage().getSlotValue(getInternal(i));
  }
  public T elementAt(IRLocation loc) {
    return elementAt(loc.getID());
  }

  public void setElementAt(T element, int i) {
    check(i);
    // XXX: Race condition:
    setInternal(i,getSlotStorage().setSlotValue(getInternal(i),element));
    noteChanged();
  }
  public void setElementAt(T element, IRLocation loc) {
    setElementAt(element,loc.getID());
  }

  public IRLocation insertElement(T element) {
    throw new IRSequenceException("arrays are fixed size");
  }

  public IRLocation appendElement(T element) {
    throw new IRSequenceException("arrays are fixed size");
  }

  public IRLocation insertElementAfter(T element, IRLocation i) {
    throw new IRSequenceException("arrays are fixed size");
  }

  public IRLocation insertElementBefore(T element, IRLocation i) {
    throw new IRSequenceException("arrays are fixed size");
  }

  public void removeElementAt(IRLocation i) {
    throw new IRSequenceException("arrays are fixed size");
  }

  public IRLocation location(int i) {
    if (0 <= i && i < size()) return IRLocation.get(i);
    return null;
  }

  public int locationIndex(IRLocation loc) {
    return loc.getID();
  }

  public IRLocation firstLocation() {
    return location(0);
  }

  public IRLocation lastLocation() {
    return location(size()-1);
  }

  public IRLocation nextLocation(IRLocation loc) {
    return location(loc.getID() + 1);
  }

  public IRLocation prevLocation(IRLocation loc) {
    return location(loc.getID() - 1);
  }

  public int compareLocations(IRLocation loc1, IRLocation loc2) {
    return loc1.getID() - loc2.getID();
  }

  public void writeValue(IROutput out) throws IOException {
    out.writeSlotFactory(getSlotFactory());
    out.writeInt(size());
  }

  @SuppressWarnings("unchecked")
  public void writeContents(IRCompoundType<IRSequence<T>> t, IROutput out) throws IOException {
    int size = size();
    boolean allValid = true;
    boolean allInvalid = true;
    for (int i = 0; i < size; ++i) {
      if (validAt(i)) {
        allInvalid = false;
      } else {
        allValid = false;
      }
    }
    out.writeByte(allValid ? '+' : (allInvalid ? '-' : '='));
    if (allValid) {
      for (int i = 0; i < size; ++i) {
        S s = getInternal(i);
        getSlotStorage().writeSlotValue(s, t.getType(i), out);
      }
      return;
    }
    if (allInvalid) return;
    for (int i = 0; i < size; ++i) {
      S s = getInternal(i);
      if (getSlotStorage().isValid(s)) {
        out.writeInt(i);
        getSlotStorage().writeSlotValue(s, t.getType(i), out);
      }
    }
    out.writeInt(-1);
  }

  @SuppressWarnings("unchecked")
  public void readContents(IRCompoundType<IRSequence<T>> t, IRInput in) throws IOException {
    int size = size();
    byte kind = '+';
    if (in.getRevision() >= 4) kind = in.readByte();
    if (kind == '+') {
      for (int i = 0; i < size; ++i) {
        setInternal(i, getSlotStorage().readSlotValue(getInternal(i), t.getType(i), in));
      }
    } else if (kind != '-') {
      int i;
      while ((i = in.readInt()) >= 0) {
        setInternal(i, getSlotStorage().readSlotValue(getInternal(i), t.getType(i), in));
      }
    }
  }

  public boolean isChanged() {
    int size = size();
    for (int i = 0; i < size; ++i) {
      if (getSlotStorage().isChanged(getInternal(i))) return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public void writeChangedContents(IRCompoundType<IRSequence<T>> t, IROutput out)
      throws IOException {
    int size = size();
    boolean allChanged = true;
    for (int i = 0; i < size; ++i) {
      S s = getInternal(i);
      if (!getSlotStorage().isChanged(s)) {
        allChanged = false;
        break;
      }
    }
    out.writeBoolean(allChanged);
    for (int i = 0; i < size; ++i) {
      S s = getInternal(i);
      if (allChanged) {
        getSlotStorage().writeSlotValue(s, t.getType(i), out);
      } else if (getSlotStorage().isChanged(s)) {
        out.writeInt(i);
        getSlotStorage().writeSlotValue(s, t.getType(i), out);
      }
    }
    if (!allChanged) out.writeInt(-1);
  }

  @SuppressWarnings("unchecked")
  public void readChangedContents(IRCompoundType<IRSequence<T>> t, IRInput in)
      throws IOException {
    int size = size();
    boolean allChanged = in.readBoolean();
    if (allChanged) {
      for (int i = 0; i < size; ++i) {
        setInternal(i, getSlotStorage().readSlotValue(getInternal(i), t.getType(i), in));
      }
    } else {
      int i;
      while ((i = in.readInt()) != -1) {
        setInternal(i, getSlotStorage().readSlotValue(getInternal(i), t.getType(i), in));
      }
    }
  }

  public void describe(PrintStream out) {
    int n = size();
    out.println("IRArray[" + n + "]");
    for (int i = 0; i < n; ++i) {
      out.print(" " + i + " => ");
      getSlotStorage().describe(getInternal(i), out);
    }
  }
  
  private class MyIterator extends AbstractRemovelessIterator<T> implements ListIteratable<T> {
    int index;
    final int max;
    final SlotStorage<S,T> storage;
    
    MyIterator(int start) {
      index = start;
      max = size();      
      storage = getSlotStorage();
    }
    
    MyIterator() {
      this(0);
    }

    public boolean hasNext() {
      return index < max;
    }

    public T next() throws NoSuchElementException {
      if (index < max) {
        // Changed to eliminate checks / repeated calls
        //T element = elementAt(index);
        T element = storage.getSlotValue(getInternal(index));
        ++index;
        return element;
      } else {
        throw new NoSuchElementException("at end of array");
      }
    }

    //===========================================================
    // Implementation of List
    //===========================================================

    public boolean hasPrevious() {
      return index > 0;
    }

    public T previous() {
      if (index > 0) {
        index--;
        T element = elementAt(index);
        return element;
      } else {
        throw new NoSuchElementException("at beginning of array");
      }
    }

    public int nextIndex() {
      return index;
    }

    public int previousIndex() {
      return index - 1;
    }

    public void set(T val) {
      if (index < max) {
        setElementAt(val, index);
      } else {
        throw new NoSuchElementException("at end of array");
      }
    }

    public void add(T arg0) {
      throw new UnsupportedOperationException("arrays are fixed size");
    }
  }

  //===========================================================
  // Implementation of List
  //===========================================================

  @Starts("nothing")
public boolean contains(Object o) {
    return indexOf(o) >= 0;
  }
  
  @Starts("nothing")
public Object[] toArray() {
    int size   = size();
    Object[] a = new Object[size];
    for(int i=0; i<size; i++) {
      a[i] = elementAt(i);
    }
    return a;
  }

  @SuppressWarnings("unchecked")
  public <E> E[] toArray(E[] a) {
    int size = size();    
    if (a.length < size) {
      a = (E[])java.lang.reflect.Array.
      newInstance(a.getClass().getComponentType(), size);
    }
    for(int i=0; i<size; i++) {
      a[i] = (E) elementAt(i);
    }
    if (a.length > size)
      a[size] = null;
    return a;    
  }

  public boolean add(T arg0) {
    throw new UnsupportedOperationException("arrays are fixed size");
  }

  @Starts("nothing")
public boolean remove(Object arg0) {
    throw new UnsupportedOperationException("arrays are fixed size");
  }

  @Override
  public boolean addAll(Collection<? extends T> arg0) {
    throw new UnsupportedOperationException("arrays are fixed size");
  }

  @Override
  public boolean addAll(int arg0, Collection<? extends T> arg1) {
    throw new UnsupportedOperationException("arrays are fixed size");
  }

  @Override
  public boolean removeAll(Collection<?> arg0) {
    throw new UnsupportedOperationException("arrays are fixed size");
  }

  @Override
  public boolean retainAll(Collection<?> arg0) {
    throw new UnsupportedOperationException("arrays are fixed size");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("arrays are fixed size");
  }

  @Override
  public void add(int arg0, T arg1) {
    throw new UnsupportedOperationException("empty sequence cannot be modified");
  }

  @Override
  public T remove(int arg0) {
    throw new UnsupportedOperationException("empty sequence cannot be modified");
  }

  @Starts("nothing")
public int indexOf(Object o) {
    int size = size();
    if (o == null) {
      for(int i=0; i<size; i++) {
        if (elementAt(i) == null) {
          return i;
        }
      }
    } else {
      for(int i=0; i<size; i++) {
        if (o.equals(elementAt(i))) {
          return i;
        }
      }
    }
    return -1;
  }

  @Starts("nothing")
public int lastIndexOf(Object o) {
    int size = size();
    if (o == null) {
      for(int i=size-1; i>=0; i--) {
        if (elementAt(i) == null) {
          return i;
        }
      }
    } else {
      for(int i=size-1; i>=0; i--) {
        if (o.equals(elementAt(i))) {
          return i;
        }
      }
    }
    return -1;
  }

  @Override
  protected ListIteratable<T> createListIterator(int i) {
    return new MyIterator(i);
  }
}
