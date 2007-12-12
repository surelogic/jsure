/* Created on Jun 12, 2005
 */
package inherited_region.good;

/**
 * Adds fields to R, protects R
 * @TestResult is CONSISTENT : Adds region and protects it in the same class
 * @RegionLock L is this protects R
 */
public class Good2 extends GoodRoot {
  /**
   * @TestResult is CONSISTENT
   * @InRegion R
   */
  protected int z;
}
