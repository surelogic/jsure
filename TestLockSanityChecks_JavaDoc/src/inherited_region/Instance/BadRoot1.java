/* Created on Jun 12, 2005
 */
package inherited_region.Instance;

/**
 * Puts fields directly in region Instance.  Now Instance cannot be
 * cannot be protected by a lock in a subclass.
 */
public class BadRoot1 {
  protected int x;
}
