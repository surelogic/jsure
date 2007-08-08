package test_requires_lock;

import com.surelogic.Lock;
import com.surelogic.PolicyLock;
import com.surelogic.Region;

@Region("private static S")
@Lock("PrivateRootStaticLock is class protects S")
@PolicyLock("PrivateRootStaticPolicyLock is class")
public class Root {

}
