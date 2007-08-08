/* Created on Jun 12, 2005
 */
package inherited_region.good;

import com.surelogic.Lock;
import com.surelogic.MapInto;

/**
 * Adds fields to R, protects Instance
 */
// TestResult is CONSISTENT : Adds region to Instance, protects Instance in same class
@Lock("L is this protects Instance" /* is CONSISTENT : Adds region to Instance, protects Instance in same class*/)
public class Good3 extends GoodRoot {
  // TestResult is CONSISTENT
  @MapInto("R" /* is CONSISTENT */)
  protected int z;
}
