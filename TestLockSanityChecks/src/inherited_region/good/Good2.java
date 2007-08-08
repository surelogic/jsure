/* Created on Jun 12, 2005
 */
package inherited_region.good;

import com.surelogic.Lock;
import com.surelogic.MapInto;

/**
 * Adds fields to R, protects R
 */
// TestResult is CONSISTENT : Adds region and protects it in the same class
@Lock("L is this protects R" /* is CONSISTENT : Adds region and protects it in the same class */)
public class Good2 extends GoodRoot {
  // TestResult is CONSISTENT
  @MapInto("R" /* is CONSISTENT */)
  protected int z;
}
