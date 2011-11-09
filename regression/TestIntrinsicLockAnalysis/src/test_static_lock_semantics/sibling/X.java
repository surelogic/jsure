package test_static_lock_semantics.sibling;

import com.surelogic.RegionEffects;
import com.surelogic.RegionLock;
import com.surelogic.Starts;

/**
 * Field lock is used as a lock by subclasess.
 * Point is that we should find that for o.lock, when o is statically
 * of type X, the lock implementation is the locks of all its subclasses.
 * 
 * <p>We also test out using 'this' as a lock locally.
 */
@RegionLock("XLock is this protects foo")
public class X {
  // Used as a lock by classes Y and Z
  protected final Object lock = new Object();
  
  // GOOD: protected by single-threaded constructor
  public int foo = 0;
  
  @RegionEffects("none")
  @Starts("nothing")
  public X() {}

  public synchronized int good() {
    // GOOD
    return foo;
  }
}
