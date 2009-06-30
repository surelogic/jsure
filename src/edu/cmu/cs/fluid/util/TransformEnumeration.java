/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/TransformEnumeration.java,v 1.2 2005/05/25 18:03:35 chance Exp $ */
package edu.cmu.cs.fluid.util;

import java.util.Enumeration;

/** An enumeration that allows us to do something to each element
 * before it is returned.
 */
@Deprecated
@SuppressWarnings("all")
public abstract class TransformEnumeration implements Enumeration {
  private final Enumeration base;
  public TransformEnumeration(Enumeration e) {
    base = e;
  }

  public boolean hasMoreElements() {
    return base.hasMoreElements();
  }

  public Object nextElement() {
    return transform(base.nextElement());
  }

  /** Called on a value to return from the iterator.
   * Return the transformed value.
   */
  protected abstract Object transform(Object o);
}
