/* Created on Jun 12, 2005
 */
package inherited_region.Subregion;

import com.surelogic.MapInto;
import com.surelogic.Region;

/**
 * Puts fields into new region R, a subregion of Instance.
 * Instance cannot be protected in a subclass because it now indirectly contains
 * fields.
 */
// TestResult is CONSISTENT
@Region("public R")
public class BadRoot2 {
  // TestResult is CONSISTENT
  @MapInto("R")
  protected int x;
}
