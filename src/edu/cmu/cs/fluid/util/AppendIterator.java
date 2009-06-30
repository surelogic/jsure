/* $Header$ */
package edu.cmu.cs.fluid.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class AppendIterator<T> extends AbstractRemovelessIterator<T> {
  private Iterator<? extends T> enm;
  private final Iterator<? extends T> enm2;
  
  public AppendIterator(Iterator<? extends T> e1, Iterator<? extends T> e2) {
    enm  = e1;
    enm2 = e2;
  }
  
  public static <T> Iterator<? extends T> append(Iterator<? extends T> e1, Iterator<? extends T> e2) {
    if (e1 == null || !e1.hasNext()) return e2;
    if (e2 == null || !e2.hasNext()) return e1;
    return new AppendIterator<T>(e1,e2);
  }
  
  public boolean hasNext() {
    if (enm.hasNext()) {
      return true;
    }
    enm = enm2;
    return enm.hasNext();
  }

  public T next() {
    if (hasNext()) {
      return enm.next();
    }
    throw new NoSuchElementException();
  }

  Iterator<? extends T> simplify() {
    if (enm == enm2) {
      if (enm2 instanceof AppendIterator)
	return ((AppendIterator<? extends T>)enm2).simplify();
      else
	return enm2;
    } else {
      return this;
    }
  }
}
