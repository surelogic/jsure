package test_requires_lock;

import com.surelogic.RegionLock;
import com.surelogic.PolicyLock;
import com.surelogic.Region;

@Region("private static S")
@RegionLock("PrivateRootStaticLock is class protects S")
@PolicyLock("PrivateRootStaticPolicyLock is class")
public class Root {

}
