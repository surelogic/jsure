package test_requires_lock;

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
   * @requiresLock this.SL
   */
  public void bad1() {}

  /**
   * BAD: instance-qualified static lock
   * @requiresLock p.SL
   */
  public void bad2(final C p) {}
  
  /**
   * GOOD: implicitly class-qualified static lock
   * @requiresLock SL
   */
  public void good1() {}

  /**
   * Good: instance-qualified instance lock
   * @requiresLock this.IL
   */
  public void good2() {}

  /**
   * Good: parameter-qualified instance lock
   * @requiresLock p.IL
   */
  public void good3(final C p) {}
  

  

  /**
   * @lock InnerLock is this protects Instance
   */
  public class Inner1 {
    
    public class Inner2 {
      /**
       * GOOD: Qualified receiver exists, names instance lock.
       * @requiresLock test_requires_lock.C.this:IL
       */
      public void good1() {}

// COMMENTED OUT FOR NOW: THIS IS BUG 501.  THE PROMISE PARSER CURRENTLY
// WON'T PARSE THE "test_requires_lock.C.Inner1.this"
// REENABLE THIS WHEN BUG 501 IS FIXED.
//      /**
//       * GOOD: Qualified receiver exists, names instance lock.
//       * @returnsLock test_requires_lock.C.Inner1.this:InnerLock
//       */
//      public Object getInnerLock_good() {
//        return Inner1.this;
//      }
    }
    
    /**
     * GOOD: Qualified receiver exists, names instance lock.
     * @requiresLock test_requires_lock.C.this:IL
     */
    public void good1() {}
    
    /**
     * BAD: qualified receiver doesn't exist
     * @requiresLock test.CC.this:SL
     */
    public void bad1() {}

    /**
     * BAD: instance-qualified static lock
     * @requiresLock test_requires_lock.C.this:SL
     */
    public void bad2() {}

    /**
     * GOOD: Class-qualified static lock
     * @requiresLock test_requires_lock.C:SL
     */
    public void good2() {}
  }
}
