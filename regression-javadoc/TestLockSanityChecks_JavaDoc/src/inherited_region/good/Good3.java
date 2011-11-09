/* Created on Jun 12, 2005
 */
package inherited_region.good;

/**
 * Adds fields to R, protects Instance
 * @TestResult is CONSISTENT : Adds region to Instance, protects Instance in same class
 * @RegionLock L is this protects Instance
 */
public class Good3 extends GoodRoot {
  /** 
   * @TestResult is CONSISTENT
   * @InRegion R
   */
  protected int z;
}
