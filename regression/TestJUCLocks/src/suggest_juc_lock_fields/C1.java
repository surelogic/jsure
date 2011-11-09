package suggest_juc_lock_fields;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class C1 {
  public final Lock f_lock1 = new ReentrantLock();
}

