package test_returns_lock;

/**
 * (These tests are all really Lock Name sanity tests.)
 * 
 * @region R
 * @lock Exists is this protects R
 * 
 * @region static StaticRegion
 * @region NonStaticRegion
 * @lock StaticLock is class protects StaticRegion
 * @lock NonStaticLock is this protects NonStaticRegion
 */
public class StateLock {
  /**
   * BAD: cannot qualify a static lock with this
   * @returnsLock this.StaticLock
   */
  public Object getLock_thisQualified_staticLock() {
    return StateLock.class;
  }
  
  /**
   * GOOD: Can ref static lock from instance method
   * @returnsLock StaticLock
   */
  public Object getLock_implicit_staticLock() {
    return StateLock.class;
  }
  
  /**
   * GOOD: Can ref static lock from instance method
   * @returnsLock test_returns_lock.StateLock:StaticLock
   */
  public Object getLock_typeQualified_staticLock() {
    return StateLock.class;
  }
  
  /**
   * BAD: type-qualified instance lock
   * @returnsLock test_returns_lock.StateLock:Exists
   */
  public Object getLock_typeQualifiedInstance() {
    return null;
  }
  
  /**
   * BAD: Lock doesn't exist
   * @returnsLock DoesntExist
   */
  public Object getLock_doesntExist() {
    return null;
  }
  
  /**
   * GOOD: Lock exists
   * @returnsLock Exists
   */
  public Object getLock_exists() {
    return this;
  }
  
  /**
   * GOOD: Lock Exists.  Test with qualified this
   * @returnsLock this.Exists
   */
  public Object getLock_exists2() {
    return this;
  }

  /**
   * BAD: parameter doesn't exist.
   * @returnsLock p.DoesntExist
   */
  public Object getLock_bad_param(final Object o) {
    return o;
  }
  
  /**
   * BAD: parameter exists, but the lock doesn't exist
   * @returnsLock p.DoesntExist 
   */
  public Object getLock_param_doesntExist(final StateLock p) {
    return p;
  }
    
  /**
   * BAD: parameter exists; lock exists; but param is non-final
   * @returnsLock p.Exists 
   */
  public Object getLock_param_nonfinal(StateLock p) {
    return p;
  }
  
  /**
   * GOOD: parameter exists; lock exists; param is final
   * @returnsLock p.Exists 
   */
  public Object getLock_param_good(final StateLock p) {
    return p;
  }
  
  
  
  
  
  
  
  
  /**
   * BAD: cannot bind "this"!  Irreleveant that lock otherwise exists.
   * This is a syntactic check on the QualifiedLockName.
   * @returnsLock this.NonStaticLock
   */
  public static Object getLock_staticMethod_instanceRegion1() {
    return new Object();
  }

  /**
   * BAD: cannot bind "this"!  Irreleveant that lock otherwise does exist.
   * This is a semantic check on the protected region: Check that it is static.
   *
   * @returnsLock NonStaticLock
   */
  public static Object getLock_staticMethod_instanceRegion1a() {
    return new Object();
  }
  
  /**
   * GOOD: Returns lock that protects a static region.
   * @returnsLock StaticLock 
   */
  public static Object getLock_staticMethod_staticRegion1() {
    return StateLock.class;
  }
  
  /**
   * GOOD: Returns lock that protects a static region.  Here we explicitly 
   * name the class.
   * @returnsLock test_returns_lock.StateLock:StaticLock 
   */
  public static Object getLock_staticMethod_staticRegion1a() {
    return StateLock.class;
  }
  
  /**
   * BAD: cannot bind "this"!  Irreleveant that lock otherwise does not exist.
   * @returnsLock this.UnknownRegion
   */
  public static Object getLock_staticMethod_unknownRegionWithThis() {
    return new Object();
  }

  /**
   * BAD: Region doesn't exist, so doesn't matter if it is static or not. 
   * @returnsLock DoesntExist
   */
  public static Object getLock_staticMethod_unknownRegion() {
    return new Object();
  }
  
  /**
   * BAD: The named class doesn't exist.  (Default package)
   * @returnsLock C:DoesntExist
   */
  public static Object getLock_staticMethod_unknownClass1() {
    return null;
  }
  
  /**
   * BAD: The named class doesn't exist.  (Named package)
   * @returnsLock foo.bar.C:DoesntExist
   */
  public static Object getLock_staticMethod_unknownClass1a() {
    return null;
  }

  
  
  /**
   * GOOD: Static method returns an instance lock of a parameter.
   * @returnsLock p.NonStaticLock
   */
  public static Object getLock_staticMethod_paramLock(final StateLock p) {
    return p;
  }
}
