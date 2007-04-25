/* Created on Jun 12, 2005
 */
package inherited_region.good;

/**
 * Adds fields to R, protects Instance
 * @lock L is this protects Instance
 */
public class Good3 extends GoodRoot {
  /** @mapInto R */
  protected int z;
}
