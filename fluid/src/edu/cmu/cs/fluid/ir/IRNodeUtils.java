/*
 * Created on Sep 21, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.ir;

public class IRNodeUtils {
  /**
   * Copied from JDK 1.4 HashMap
   * 
   * Returns a hash value for the specified object.  In addition to 
   * the object's own hashCode, this method applies a "supplemental
   * hash function," which defends against poor quality hash functions.
   * This is critical because HashMap uses power-of two length 
   * hash tables.<p>
   *
   * The shift distances in this function were chosen as the result
   * of an automated search over the entire four-dimensional search space.
   */
  public static int hash(int h) {
    h += ~(h << 9);
    h ^=  (h >>> 14);
    h +=  (h << 4);
    h ^=  (h >>> 10);
    return h;
  }
}
