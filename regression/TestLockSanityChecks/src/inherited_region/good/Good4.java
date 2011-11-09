/* Created on Jun 12, 2005
 */
package inherited_region.good;

import com.surelogic.RegionLock;

/**
 * Adds fields to Instance, protects Instance
 */
// TestResult is CONSISTENT : Adds field to Instance, protects Instance in same class
@RegionLock("L is this protects Instance" /* is CONSISTENT : Adds field to Instance, protects Instance in same class */)
public class Good4 extends GoodRoot {
  protected int z;
}
