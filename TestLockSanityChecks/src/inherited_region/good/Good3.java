/* Created on Jun 12, 2005
 */
package inherited_region.good;

import com.surelogic.RegionLock;
import com.surelogic.InRegion;

/**
 * Adds fields to R, protects Instance
 */
// TestResult is CONSISTENT : Adds region to Instance, protects Instance in same class
@RegionLock("L is this protects Instance" /* is CONSISTENT : Adds region to Instance, protects Instance in same class*/)
public class Good3 extends GoodRoot {
  // TestResult is CONSISTENT
  @InRegion("R" /* is CONSISTENT */)
  protected int z;
}
