/* Created on Jun 12, 2005
 */
package inherited_region.Subregion;

import com.surelogic.Lock;

// TestResult is UNASSOCIATED: Cannot protect R because it has fields in a super class.
@Lock("L is this protects R" /* is UNASSOCIATED: Cannot protect R because it has fields in a super class. */)
public class Bad3 extends BadRoot2 {

}
