package suggest_juc_lock_fields;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Region;
import com.surelogic.RegionLock;

@Region("protected Region2")
@RegionLock("Lock2 is f_lock2 protects Region2")
public class C2 extends C1 {
  public final Lock f_lock2 = new ReentrantLock();
}

