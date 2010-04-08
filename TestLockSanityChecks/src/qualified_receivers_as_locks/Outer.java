package qualified_receivers_as_locks;

import com.surelogic.PolicyLock;
import com.surelogic.PolicyLocks;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;

/**
 * Test that the lock promise scrubber rejects annotations that use a
 * qualified receiver for the lock implementation.  This feature is not
 * yet supported by the lock analysis (see bug 992).  The analysis implementation
 * currently assumes that lock fields come from the same object as the region
 * that is protected, and using qualified receivers violates this assumption.
 * This is something to be fixed in the future.
 *
 * @author aarong
 *
 */
public class Outer {
  @SuppressWarnings("unused")
  private final Object field = new Object();

  @SuppressWarnings("unused")
  @RegionLocks({
    // Rejected with warning
    @RegionLock("L is qualified_receivers_as_locks.Outer.this protects f" /* is CONSISTENT */),
    // Rejected with warning
    @RegionLock("L2 is qualified_receivers_as_locks.Outer.this.field protects g" /* is CONSISTENT */)
  })
  @PolicyLocks({
    // Rejected with warning
    @PolicyLock("P is qualified_receivers_as_locks.Outer.this" /* is CONSISTENT */),
    // Rejected with warning
    @PolicyLock("P2 is qualified_receivers_as_locks.Outer.this.field" /* is CONSISTENT */)
  })
  private class Inner {
    private int f;
    private int g;

    public void set(int v) {
      f = v;
    }
  }
}
