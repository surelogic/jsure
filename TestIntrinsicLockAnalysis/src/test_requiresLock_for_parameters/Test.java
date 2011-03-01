package test_requiresLock_for_parameters;

import com.surelogic.Borrowed;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;

/**
 * Tests that requiresLock works on method parameters and that it is properly
 * enforced in that situation.
 */
@RegionLock("LL is this protects Instance")
public class Test {
  private int z;
  
  @Borrowed("this")
  public Test() {
    this.z = 0;
  }
  
  /**
   * Here we test that we actually assure the holding of locks on parameters.
   * We require that the lock on the parameter "c" be held by the caller and 
   * that the lock on the receiver be held by the caller.
   */
  @RequiresLock("LL, c:L")
  public double doIt(final C c) {
    return z * c.getMagnitude(); // GOOD
  }
  
  public static void main(final String[] args) {
    final Test o = new Test();
    final C c = new C();
    
    // Good
    synchronized (o) {
      synchronized (c) {
        o.doIt(c);
      }
    }

    // Bad: Not holding lock of c as is required by doIt()
    synchronized (o) {
      o.doIt(c);
    }

    // Bad: Not holding lock of o as is required by doIt()
    synchronized (c) {
      o.doIt(c);
    }
    
    // Bad: Not holding any locks
    o.doIt(c);
  }
}
