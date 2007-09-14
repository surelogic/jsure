package requiresLock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.Region;
import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;
import com.surelogic.Regions;
import com.surelogic.RequiresLock;


/**
 * Tests sanity checking of read-write locks in RequiresLock annotations.
 */
@Regions({
  @Region("protected R1"),
  @Region("protected R2"),
  @Region("protected R3"),
  @Region("protected static S1"),
  @Region("protected static S2"),
  @Region("protected static S3")
})
@RegionLocks({
  @RegionLock("L is intrinsicLock protects R1"),
  @RegionLock("JUC is jucLock protects R2"),
  @RegionLock("RW is rwLock protects R3"),  
  @RegionLock("SL is staticIntrinsicLock protects S1"),
  @RegionLock("SJUC is staticJucLock protects S2"),
  @RegionLock("SRW is staticRwLock protects S3")  
})
public class Warnings {
  protected final Object intrinsicLock = new Object();
  protected final Lock jucLock = new ReentrantLock();
  protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
  protected static final Object staticIntrinsicLock = new Object();
  protected static final Lock staticJucLock = new ReentrantLock();
  protected static final ReadWriteLock staticRwLock = new ReentrantReadWriteLock();
  

  
  @RequiresLock("L" /* is CONSISTENT: Can require an intrinsic lock */)
  protected void intrinsic_lock() {}
  
  @RequiresLock("L.readLock()" /* is UNASSOCIATED: Intrinsic locks do not have read components */)
  protected void intrinsic_readLock() {}
  
  @RequiresLock("L.writeLock()" /* is UNASSOCIATED: Intrinsic locks do not have write components */)
  protected void intrinsic_writeLock() {}
  
  

  
  @RequiresLock("JUC" /* is CONSISTENT: Can require a java.util.concurrent.locks.Lock */)
  protected void juc_lock() {}
  
  @RequiresLock("JUC.readLock()" /* is UNASSOCIATED: java.util.concurrent.locks.Lock does not have a read component */)
  protected void juc_readLock() {}
  
  @RequiresLock("JUC.writeLock()" /* is UNASSOCIATED: java.util.concurrent.locks.Lock does not have a write component */)
  protected void juc_writeLock() {}
  

  
  @RequiresLock("RW" /* is UNASSOCIATED: Cannot require a java.util.concurrent.locks.ReadWriteLock */)
  protected void rw_lock() {}
  
  @RequiresLock("RW.readLock()" /* is CONSISTENT: java.util.concurrent.locks.ReadWriteLock has a read component */)
  protected void rw_readLock() {}
  
  @RequiresLock("RW.writeLock()" /* is CONSISTENT: java.util.concurrent.locks.ReadWriteLock has a write component */)
  protected void rw_writeLock() {}
  

  
  @RequiresLock("SL" /* is CONSISTENT: Can require an intrinsic lock */)
  protected void static_intrinsic_lock() {}
  
  @RequiresLock("SL.readLock()" /* is UNASSOCIATED: Intrinsic locks do not have read components */)
  protected void static_intrinsic_readLock() {}
  
  @RequiresLock("SL.writeLock()" /* is UNASSOCIATED: Intrinsic locks do not have write components */)
  protected void static_intrinsic_writeLock() {}
  
  

  
  @RequiresLock("SJUC" /* is CONSISTENT: Can require a java.util.concurrent.locks.Lock */)
  protected void static_juc_lock() {}
  
  @RequiresLock("SJUC.readLock()" /* is UNASSOCIATED: java.util.concurrent.locks.ReadWriteLock has a read component */)
  protected void static_juc_readLock() {}
  
  @RequiresLock("SJUC.writeLock()" /* is UNASSOCIATED: java.util.concurrent.locks.ReadWriteLock has a write component */)
  protected void static_juc_writeLock() {}
  

  
  @RequiresLock("SRW" /* is UNASSOCIATED: Cannot require a java.util.concurrent.locks.ReadWriteLock */)
  protected void static_rw_lock() {}
  
  @RequiresLock("SRW.readLock()" /* is CONSISTENT: java.util.concurrent.locks.ReadWriteLock has a read component */)
  protected void static_rw_readLock() {}
  
  @RequiresLock("SRW.writeLock()" /* is CONSISTENT: java.util.concurrent.locks.ReadWriteLock has a write component */)
  protected void static_rw_writeLock() {}
}
