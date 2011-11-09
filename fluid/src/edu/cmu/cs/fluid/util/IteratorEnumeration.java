/*
 * Created on May 14, 2003
 *
 */
package edu.cmu.cs.fluid.util;

import java.util.Iterator;

/**
 * @author Edwin Chan
 */
@Deprecated
@SuppressWarnings("all")
public class IteratorEnumeration extends SimpleEnumeration {
  private Iterator it;

  public IteratorEnumeration(Iterator it) {
    this.it = it;
  }
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.util.SimpleEnumeration#computeNextElement()
	 */
	@Override
  protected Object computeNextElement() {
    if (it.hasNext()) {
      return it.next();
    }
    return noElement;
	}
}
