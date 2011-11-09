package suggest_juc_lock_fields;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class C3 extends C2 {
  public final ReadWriteLock f_lock3 = new ReentrantReadWriteLock();
}

