package test_returns_lock;

/**
 * Tests that static state locks cannot be referenced through "this" or 
 * parameter names.
 * (These tests are all really Lock Name sanity tests.)
 * 
 * @region I
 * @region static S
 * 
 * @lock IL is this protects I
 * @lock SL is class protects S
 *
 */
public class C {
  /**
   * BAD: instance-qualified static lock
   * @returnsLock this.SL
   */
  public Object getLock() {
    return C.class;
  }

  /**
   * BAD: instance-qualified static lock
   * @returnsLock p.SL
   */
  public Object getLock(final C p) {
    return C.class;
  }
  
  /**
   * GOOD: implicitly class-qualified static lock
   * @returnsLock SL
   */
  public Object getLock2() {
    return C.class;
  }

  /**
   * Good: instance-qualified instance lock
   * @returnsLock this.IL
   */
  public Object getInstanceLock() {
    return this;
  }

  /**
   * Good: parameter-qualified instance lock
   * @returnsLock p.IL
   */
  public Object getInstanceLock(final C p) {
    return p;
  }
  

  

  /**
   * @lock InnerLock is this protects Instance
   */
  public class Inner1 {
    
    public class Inner2 {
      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * @returnsLock test_returns_lock.C.this:IL
       */
      public Object getLock_good() {
        return C.this;
      }

      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * BUT THE METOD IS BAD: It returns the wrong lock
       * @returnsLock test_returns_lock.C.this:IL
       */
      public Object getLock_bad() {
        return Inner1.this;
      }

// COMMENTED OUT FOR NOW: THIS IS BUG 501.  THE PROMISE PARSER CURRENTLY
// WON'T PARSE THE "test_returns_lock.C.Inner1.this"
// REENABLE THIS WHEN BUG 501 IS FIXED.
//      /**
//       * GOOD: Qualified receiver exists, names instance lock.
//       * @returnsLock test_returns_lock.C.Inner1.this:InnerLock
//       */
//      public Object getInnerLock_good() {
//        return Inner1.this;
//      }
    }
    
    /**
     * GOOD: Qualified receiver exists, names instance lock.
     * @returnsLock test_returns_lock.C.this:IL
     */
    public Object getLock_good() {
      return C.this;
    }
    
    /**
     * BAD: qualified receiver doesn't exist
     * @returnsLock test.CC.this:SL
     */
    public Object getLock_badQualifiedReceiver() {
      return C.class;
    }

    /**
     * BAD: instance-qualified static lock
     * @returnsLock test_returns_lock.C.this:SL
     */
    public Object getLock() {
      return C.class;
    }

    /**
     * GOOD: Class-qualified static lock
     * @returnsLock test_returns_lock.C:SL
     */
    public Object getLock2() {
      return C.class;
    }
  }
}
