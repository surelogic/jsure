package test.MethodCall.NoNesting;

import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;

@RegionLocks({
  @RegionLock("F1 is this protects f1"),
  @RegionLock("F2 is TestDefinitionAndSynchronization.this protects f2") // unsupported
})
public class TestDefinitionAndSynchronization {
  public int f1;
  public int f2;

  public void testSynchronization() {
    synchronized (this) {
      f1 = 1; // Assures
      f2 = 2; // N/A
    }
    
    synchronized (TestDefinitionAndSynchronization.this) {
      f1 = 3; // Assures
      f2 = 4; // N/A
    }
  }
}
