package test_requires_lock;

import com.surelogic.Lock;
import com.surelogic.RequiresLock;

@Lock("L is this protects Instance")
public class TestImprovedMessages {
  @SuppressWarnings("unused")
  private int v;
  /**
   * Test improved labels in the drops.  It used to be that the drop only
   * included the lock name, not the full context of which lock,
   * so both drops generated for this method were for lock "L".  Now it 
   * is for locks "this . L" and "p . L".
   */
  @RequiresLock("this:L, p:L")
  public void bad(final TestImprovedMessages p) {
    // bad
    v = 1;
    // good
    p.v = 1;
  }
}
