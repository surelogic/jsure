package suggest_juc_lock_fields;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Region;
import com.surelogic.RegionLock;

@Region("protected Region1")
@RegionLock("Lock1 is f_lock1 protects Region1")
public class C1 {
  public final Lock f_lock1 = new ReentrantLock();
}

