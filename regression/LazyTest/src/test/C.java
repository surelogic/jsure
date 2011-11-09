package test;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.surelogic.RegionLock;

/* The behavior of this class can be verified by setting breakpoints on the
 * getResultFor() and getSubANalysisQuery() methods of the delegate classes
 * in AbstractJavaFlowAnalysisQuery.
 */
@RegionLock("L is lock protects Instance")
public class C {
  public final Lock lock = new ReentrantLock();
  
  @SuppressWarnings("unused")
  private int x;
  
  {
    /* For this section we have a ThunkedSubAnalysis, where the
     * original analysis is that from the (implicit) constructor. 
     */
    lock.lock();
    try {
      x = 100;
    } finally {
      lock.unlock();
    }

    new Object() {
      {
        /* For this section we have a ThunkedSubAnalysis where the 
         * original analysis is the thunked analysis from the initializer
         * (above) whose original analysis is from the (implicit) constructor
         * for class C.
         */
        lock.lock();
        try {
          x = 1000;
        } finally {
          lock.unlock();
        }
      }
    };
  }
  
  public void method() {
    // Here we use a regular ThunkedAnalysis
    lock.lock();
    try {
      x = 10;
    } finally {
      lock.unlock();
    }
  }
  
  public void method2() {
    /* This method uses MustHoldAnalysis.EMPTY_HELD_LOCKS_QUERY because
     * it doesn't use any JUC methods.
     */
  }
}
