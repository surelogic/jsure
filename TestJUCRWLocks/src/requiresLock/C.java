package requiresLock;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RequiresLock;

@Region("protected R")
@RegionLock("RW is rwLock protects R" /* is INCONSISTENT: has inconsistent lock preconditions */)
public class C {
  protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  
  @InRegion("R")
  private int f1;
  
  @InRegion("R")
  private int f2;
  
  
  
  @RequiresLock("RW.readLock()" /* is CONSISTENT: all call sites are good */)
  protected int getter() {
    // PROTECTED
    return f1 + 
    // PROTECTED
      f2;
  }
  
  @RequiresLock("RW.writeLock()" /* is INCONSISTENT: Not all call sites are good */)
  protected void setter(final int a, final int b) {
    // PROTECTED
    f1 = a;
    // PROTECTED
    f2 = b;
  }
  
  public void doIt() {
    rwLock.readLock().lock();
    try {
      // CORRECT
      Spy.value = getter();
      // BAD: Need writeLock
      setter(10, 100);
    } finally {
      rwLock.readLock().unlock();
    }

    rwLock.writeLock().lock();
    try {
      // CORRECT
      Spy.value = getter();
      // CORRECT
      setter(10, 100);
    } finally {
      rwLock.writeLock().unlock();
    }
  }
}
