package testSanityChecks;

import com.surelogic.ThreadSafe;

/* Bad: superclass is neither java.lang.Object, nor @ThreadSafe */
@ThreadSafe
class Y extends X {
}
