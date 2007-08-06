package test_unique_names;

import com.surelogic.Lock;
import com.surelogic.PolicyLock;
import com.surelogic.Region;

/**
 * Bad: Reuses lock names L1 and P1
 */
@Region("public R2")
@Lock("L1 is this protects R2" /* is UNASSOCIATED: L1 is already inherited from C1 */)
@PolicyLock("P1 is class" /* is UNASSOCIATED: P1 is already inherited from C1 */)
public class BadC2 extends C1 {

}
