package test_unique_names;

import com.surelogic.Lock;
import com.surelogic.PolicyLock;
import com.surelogic.Region;

@Region("public R3")
@Lock("L3 is this protects R3" /* is CONSISTENT: First use of L3 in hierarchy */)
@PolicyLock("P3 is class" /* is CONSISTENT: First use of P3 in hierarchy */)
public class GoodC3 extends GoodC2 {

}
