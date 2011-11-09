package returnsLock_and_JUC;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.RegionLock;
import com.surelogic.ReturnsLock;

@RegionLock("L is lock protects Instance")
public class C {
  public final ReadWriteLock lock = new ReentrantReadWriteLock();


  
  @ReturnsLock("L" /* is CONSISTENT */)
  public ReadWriteLock getLock() {
    return lock;
  }
  
  @ReturnsLock("L.readLock()" /* is UNPARSEABLE */)
  public Lock getReadLock() {
    return lock.readLock();
  }

  @ReturnsLock("L.writeLock()" /* is UNPARSEABLE */)
  public Lock getWriteLock() {
    return lock.writeLock();
  }
}
