/* Created on Jun 12, 2005
 */
package inherited_region.good;

/**
 * Adds fields to R, protects R
 * @lock L is this protects R
 */
public class Good2 extends GoodRoot {
  /** @mapInto R */
  protected int z;
}
