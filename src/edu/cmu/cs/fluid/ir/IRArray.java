/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRArray.java,v 1.25 2006/03/31 21:27:20 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;


/**
 * A generic fixed size array.
 * @author boyland
 */
public class IRArray<S,T> extends IRAbstractArray<S,T> {
  private final S[] contents;
  
  private final SlotStorage<S,T> storage;
  @Override
  public SlotStorage<S,T> getSlotStorage() {
    return storage;
  }
  
  @SuppressWarnings("unchecked")
  public IRArray(int size, SlotStorage<S,T> ss) {
    super();
    storage = ss;
    contents = (S[]) new Object[size]; // This is safe in Java 5
    initialize();
  }

  public int size() {
    return contents.length;
  }

  @Override
  protected S getInternal(int i) {
    return contents[i];
  }
  @Override
  protected void setInternal(int i, S slotState) {
    contents[i] = slotState;
  }
  
  public static <T> IRSequence<T> readValue(IRInput in, IRSequence<T> current)
      throws IOException {
    SlotFactory sf = in.readSlotFactory();
    int size = in.readInt();
    if (current != null) {
      if (current.size() != size)
          throw new IOException("re-reading array[" + current.size()
              + "] with length = " + size);
      return current;
    }
    //System.out.println("Array " + size);
    return sf.getOldFactory().newSequence(size);
  }

}

