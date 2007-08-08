package test_unique_names;

import com.surelogic.Lock;
import com.surelogic.PolicyLock;
import com.surelogic.Region;

@Region("public R1")
@Lock("L1 is this protects R1" /* is CONSISTENT: First use of L1 in hierarchy */)
@PolicyLock("P1 is class" /* is CONSISTENT: First use of P1 in hierarchy */)
public class C1 {

}
