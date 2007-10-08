package suggest_juc_lock_fields;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.Region;
import com.surelogic.RegionLock;

@Region("protected Region3")
@RegionLock("Lock3 is f_lock3 protects Region3")
public class C3 extends C2 {
  public final Lock f_lock3 = new ReentrantLock();
}

