/* Created on Jun 12, 2005
 */
package inherited_region.Subregion;

/**
 * Puts fields into new region R, a subregion of Instance.
 * Instance cannot be protected in a subclass because it now indirectly contains
 * fields.
 * @region public R
 */
public class BadRoot2 {
  /** @mapInto R */
  protected int x;
}
