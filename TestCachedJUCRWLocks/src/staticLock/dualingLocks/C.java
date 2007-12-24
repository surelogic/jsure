package staticLock.dualingLocks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.RegionLock;

@RegionLock("DataLock is rwLock protects data" /* is CONSISTENT */)
public class C {
  protected static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  protected static final Lock wLock = rwLock.writeLock();
  
  protected static int data;
}
