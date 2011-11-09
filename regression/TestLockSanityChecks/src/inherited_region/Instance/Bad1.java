/* Created on Jun 12, 2005
 */
package inherited_region.Instance;

import com.surelogic.RegionLock;

// TestResult is UNASSOCIATED : Cannot protect Instance because it has fields in a super class. 
@RegionLock("L is this protects Instance" /* is UNASSOCIATED : Cannot protect Instance because it has fields in a super class. */)
public class Bad1 extends BadRoot1 {

}
