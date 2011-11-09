/* Created on Jun 12, 2005
 */
package inherited_region.good;

import com.surelogic.Region;

/**
 * Declares region R, but doesn't put anything in it.  Subclasses
 * can protect Instance, can protect R
 */
// TestRegion is VALID
@Region("public R" /* is VALID */)
public class GoodRoot {
  
}
