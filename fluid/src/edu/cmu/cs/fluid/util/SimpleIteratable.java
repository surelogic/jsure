/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/SimpleIteratable.java,v 1.1 2006/07/20 22:30:55 chance Exp $*/
package edu.cmu.cs.fluid.util;

import java.util.Iterator;

/**
 * Makes an Iterator into an Iteratable
 * 
 * @author chance
 */
public class SimpleIteratable<T> implements Iteratable<T> {
  Iterator<T> it;
    
  public SimpleIteratable(Iterator<T> it) {
    this.it = it;
  }
  
  public boolean hasNext() {
    return it != null && it.hasNext();
  }

  public T next() {
    if (it != null) {
      return it.next();
    }
    return null;
  }

  public void remove() {
    if (it != null) {
      it.remove();
    }
  }

  public Iterator<T> iterator() {
    Iterator<T> rv = it;
    it = null;
    return rv;
  }

}
