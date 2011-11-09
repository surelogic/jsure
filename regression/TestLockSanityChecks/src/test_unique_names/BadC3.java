package test_unique_names;

import com.surelogic.RegionLock;
import com.surelogic.PolicyLock;
import com.surelogic.Region;

/**
 * Bad: Reuses lock names L1 and P1
 */
@Region("public R3")
@RegionLock("L1 is this protects R3" /* is UNASSOCIATED: L1 is already inherited from C1 */)
@PolicyLock("P1 is class" /* is UNASSOCIATED: P1 is already inherited from C1 */)
public class BadC3 extends GoodC2 {

}
