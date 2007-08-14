package returnsLock_and_JUC;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.Lock;
import com.surelogic.MapInto;
import com.surelogic.Region;
import com.surelogic.ReturnsLock;


@Lock("RW is rwLock protects Region")
@Region("private Region")
public class Test2 {
  private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  
  @MapInto("Region")
  private int value;
  
  
  
  @ReturnsLock("RW")
  public ReadWriteLock getLock() {
    // GOOD: Returns the correct lock
    return rwLock;
  }
  
  public void bad_hasRead_needsWrite() {
    getLock().readLock().lock();
    try {
      // HOLDS RW.readLock()
      // BAD: needs RW.writeLock()
      value = 10;
    } finally {
      // HOLDS RW.readLock()
      getLock().readLock().unlock();
    }
  }
  
  public void good_hasWrite_needsWrite() {
    getLock().writeLock().lock();
    try {
      // HOLDS RW.writeLock()
      // GOOD: needs RW.writeLock()
      value = 10;
    } finally {
      // HOLDS RW.writeLock()
      getLock().writeLock().unlock();
    }
  }
  
  public int good_hasRead_needsRead() {
    getLock().readLock().lock();
    try {
      // HOLDS RW.readLock()
      // GOOD: needs RW.readLock()
      return value;
    } finally {
      // HOLDS RW.readLock()
      getLock().readLock().unlock();
    }
  }  
  
  public int good_hasWrite_needsRead() {
    getLock().writeLock().lock();
    try {
      // HOLDS RW.writeLock()
      // GOOD: needs RW.readLock()
      return value;
    } finally {
      // HOLDS RW.writeLock()
      getLock().writeLock().unlock();
    }
  }
}
