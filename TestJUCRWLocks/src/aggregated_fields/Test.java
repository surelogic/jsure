package aggregated_fields;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.Aggregate;
import com.surelogic.Borrowed;
import com.surelogic.Lock;
import com.surelogic.Locks;
import com.surelogic.MapInto;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.SingleThreaded;
import com.surelogic.Unique;

@Locks({
  @Lock("RW1 is rwLock1 protects Region1"),
  @Lock("RW2 is rwLock2 protects Region2"),
  @Lock("RW3 is rwLock3 protects Region3")
})
@Regions({
  @Region("private Region1"),
  @Region("private Region2"),
  @Region("private Region3")
})
public class Test {
  private final ReadWriteLock rwLock1 = new ReentrantReadWriteLock();
  private final ReadWriteLock rwLock2 = new ReentrantReadWriteLock();
  private final ReadWriteLock rwLock3 = new ReentrantReadWriteLock();

  // [f] Good: needs RW1.writeLock()
  @Unique
  @MapInto("Region1")
  @Aggregate("f1 into Region2, f2 into Region3")
  private Inner f; // = new Inner();
  
  @SingleThreaded
  @Borrowed("this")
  public Test() {
    // do stuff
    f = new Inner();
  }
  
  public void doStuff() {
    rwLock1.writeLock().lock();
    try {
      // HOLDS RW1.writeLock()
      // [f] Good
      f = new Inner();
    } finally {
      // HOLDS RW1.writeLock()
      rwLock1.writeLock().unlock();
    }

    rwLock1.readLock().lock();
    try {
      // HOLDS RW1.readLock()
      // [f] GOOD
      Inner i = f;
    } finally {
      // HOLDS RW1.readLock()
      rwLock1.readLock().unlock();
    }

    rwLock1.writeLock().lock();
    try {
      // HOLDS RW1.writeLock()
      rwLock2.writeLock().lock();
      try {
        // HOLDS RW1.writeLock(), RW2.writeLock()
        rwLock3.writeLock().lock();
        try {
          // HOLDS RW1.writeLock(), RW2.writeLock(), RW3.writeLock()
          int x = 
            // [f] Good: Needs RW1.readLock()
            // [f1] Good: Needs RW2.readLock()
            f.f1 +
            // [f] Good: Needs RW1.readLock()
            // [f2] Good: Needs RW3.readLock()
            f.f2;
          // HOLDS RW1.writeLock(), RW2.writeLock(), RW3.writeLock()
          int y =
            // HOLDS RW1.writeLock(), RW2.writeLock(), RW3.writeLock()
            // [f] GOOD: Needs RW1.readLock()
            // [f1 f2] sum() Good: Needs RW2.readLock(), RW3.readLock() 
            f.sum(); 
          // HOLDS RW1.writeLock(), RW2.writeLock(), RW3.writeLock()
          if (x != y) { }
        } finally {
          // HOLDS RW1.writeLock(), RW2.writeLock(), RW3.writeLock()
          rwLock3.writeLock().unlock();
        }
      } finally {
        // HOLDS RW1.writeLock(), RW2.writeLock()
        rwLock2.writeLock().unlock();
      }
    } finally {
      // HOLDS RW1.writeLock()
      rwLock1.writeLock().unlock();
    }

    rwLock1.readLock().lock();
    try {
      // HOLDS RW1.readLock()
      rwLock2.readLock().lock();
      try {
        // HOLDS RW1.readLock(), RW2.readLock()
        rwLock3.readLock().lock();
        try {
          // HOLDS RW1.readLock(), RW2.readLock(), RW3.readLock()
          int x = 
            // [f] GOOD: Needs RW1.readLock()
            // [f1] GOOD: Needs RW2.readLock()
            f.f1 + 
            // [f] GOOD: Needs RW1.readLock()
            // [f2] GOOD: Needs RW3.readLock()
            f.f2;
          // HOLDS RW1.readLock(), RW2.readLock(), RW3.readLock()
          int y = 
            // [f] GOOD: Needs RW1.readLock()
            // [f1 f2] sum() GOOD: Needs RW2.readLock(), RW3.readLock() 
            f.sum(); 
          // HOLDS RW1.readLock(), RW2.readLock(), RW3.readLock()
          if (x != y) { }
        } finally {
          // HOLDS RW1.readLock(), RW2.readLock(), RW3.readLock()
          rwLock3.readLock().unlock();
        }
      } finally {
        // HOLDS RW1.readLock(), RW2.readLock()
        rwLock2.readLock().unlock();
      }
    } finally {
      // HOLDS RW1.readLock()
      rwLock1.readLock().unlock();
    }

    rwLock1.readLock().lock();
    try {
      // HOLDS RW1.readLock()
      rwLock2.writeLock().lock();
      try {
        // HOLDS RW1.readLock(), RW2.writeLock()
        rwLock3.writeLock().lock();
        try {
          // HOLDS RW1.readLock(), RW2.writeLock(), RW3.writeLock()
          // [f] GOOD: Needs RW1.readLock()
          // [f1] GOOD: Needs RW2.writeLock()
          f.f1 = 10;
          // HOLDS RW1.readLock(), RW2.writeLock(), RW3.writeLock()
          // [f] GOOD: Needs RW1.readLock()
          // [f2] GOOD: Needs RW3.writeLock()
          f.f2 = 10;
          // HOLDS RW1.readLock(), RW2.writeLock(), RW3.writeLock()
          // [f] GOOD: Needs RW1.readLock()
          // [f1 f2] setBoth() GOOD: Needs RW2.writeLock(), RW3.writeLock()
          f.setBoth(5, 5);
        } finally {
          // HOLDS RW1.readLock(), RW2.writeLock(), RW3.writeLock()
          rwLock3.writeLock().unlock();
        }
      } finally {
        // HOLDS RW1.readLock(), RW2.writeLock()
        rwLock2.writeLock().unlock();
      }
    } finally {
      // HOLDS RW1.readLock()
      rwLock1.readLock().unlock();
    }

    rwLock1.readLock().lock();
    try {
      // HOLDS RW1.readLock()
      rwLock2.readLock().lock();
      try {
        // HOLDS RW1.readLock(), RW2.readLock()
        rwLock3.readLock().lock();
        try {
          // HOLDS RW1.readLock(), RW2.readLock(), RW3.readLock()
          // [f] GOOD: Needs RW1.readLock()
          // [f1] BAD: Needs RW2.writeLock()
          f.f1 = 10;
          // HOLDS RW1.readLock(), RW2.readLock(), RW3.readLock()
          // [f] GOOD: Needs RW1.readLock()
          // [f2] BAD: Needs RW3.writeLock()
          f.f2 = 10;
          // HOLDS RW1.readLock(), RW2.readLock(), RW3.readLock()
          // [f] GOOD: Needs RW1.readLock()
          // [f1 f2] setBoth() BAD: Needs RW2.writeLock(), RW3.writeLock()
          f.setBoth(5, 5); 
        } finally {
          // HOLDS RW1.readLock(), RW2.readLock(), RW3.readLock()
          rwLock3.readLock().unlock();
        }
      } finally {
        // HOLDS RW1.readLock(), RW2.readLock()
        rwLock2.readLock().unlock();
      }
    } finally {
      // HOLDS RW1.readLock()
      rwLock1.readLock().unlock();
    }
  }
}
