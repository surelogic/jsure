/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/IteratableHashSet.java,v 1.1 2006/07/20 22:30:55 chance Exp $*/
package edu.cmu.cs.fluid.util;

import java.util.*;

public class IteratableHashSet<T> extends HashSet<T> implements Iteratable<T> {
  Iterator<T> it = null;
  private void init() {
    if (it == null) {
      it = this.iterator();
    }
  }
  
  public boolean hasNext() {
    init();
    return it.hasNext();
  }

  public T next() {
    init();
    return it.next();
  }

  public void remove() {
    init();
    it.remove();
  }
}
