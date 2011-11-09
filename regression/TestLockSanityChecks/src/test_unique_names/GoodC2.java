package test_unique_names;

import com.surelogic.RegionLock;
import com.surelogic.PolicyLock;
import com.surelogic.Region;

@Region("public R2")
@RegionLock("L2 is this protects R2" /* is CONSISTENT: First use of L2 in hierarchy */)
@PolicyLock("P2 is class" /* is CONSISTENT: First use of P2 in hierarchy */)
public class GoodC2 extends C1 {

}
