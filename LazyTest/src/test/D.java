package test;

import com.surelogic.RegionLock;

/* We test JUC queries when there is no use of JUC classes.  In this case the
 * thunked queries are created, but the thunks are never evaluated and the
 * queries are never asked for answers.
 */
@RegionLock("L is this protects Instance")
public class D {
  @SuppressWarnings("unused")
  private int x;
  
  {
    /* For this section we have a ThunkedSubAnalysis, where the
     * original analysis is that from the (implicit) constructor. 
     */
    synchronized (this) {
      x = 100;
    }

    new Object() {
      {
        /* For this section we have a ThunkedSubAnalysis where the 
         * original analysis is the thunked analysis from the initializer
         * (above) whose original analysis is from the (implicit) constructor
         * for class C.
         */
        synchronized (D.this) {
          x = 1000;
        }
      }
    };
  }
  
  public void method() {
    // Here we use a regular ThunkedAnalysis
    synchronized (this) {
      x = 10;
    }
  }
  
  public void method2() {
    /* This method uses MustHoldAnalysis.EMPTY_HELD_LOCKS_QUERY because
     * it doesn't use any JUC methods.
     */
  }
}
