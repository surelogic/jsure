package test_requires_lock;

import com.surelogic.Lock;
import com.surelogic.PolicyLock;
import com.surelogic.Region;

@Region("private static S")
@Lock("PrivateStaticLock is class protects S")
@PolicyLock("PrivateStaticPolicyLock is class")
public class D {

}
