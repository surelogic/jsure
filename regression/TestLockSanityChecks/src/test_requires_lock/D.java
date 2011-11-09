package test_requires_lock;

import com.surelogic.RegionLock;
import com.surelogic.PolicyLock;
import com.surelogic.Region;

@Region("private static S")
@RegionLock("PrivateStaticLock is class protects S")
@PolicyLock("PrivateStaticPolicyLock is class")
public class D {

}
