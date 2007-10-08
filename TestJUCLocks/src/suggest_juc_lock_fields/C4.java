package suggest_juc_lock_fields;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.surelogic.Region;
import com.surelogic.RegionLock;

@Region("protected Region4")
@RegionLock("Lock4 is f_lock4 protects Region4")
public class C4 extends C3 {
  public final ReadWriteLock f_lock4 = new ReentrantReadWriteLock();
}

