package test_static_lock_semantics.sibling;

import com.surelogic.Lock;
import com.surelogic.SingleThreaded;
import com.surelogic.Starts;
import com.surelogic.Writes;

/**
 * Field lock is used as a lock by subclasess.
 * Point is that we should find that for o.lock, when o is statically
 * of type X, the lock implementation is the locks of all its subclasses.
 * 
 * <p>We also test out using 'this' as a lock locally.
 */
@Lock("XLock is this protects foo")
public class X {
  // Used as a lock by classes Y and Z
  protected final Object lock = new Object();
  
  // GOOD: protected by single-threaded constructor
  public int foo = 0;
  
  @SingleThreaded
  @Writes("nothing")
  @Starts("nothing")
  public X() {}

  public synchronized int good() {
    // GOOD
    return foo;
  }
}
