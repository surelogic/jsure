package test_requires_lock;

/**
 * (These tests are all really Lock Name sanity tests.)
 * 
 * @Region R
 * @Lock Exists is this protects R
 * 
 * @Region static StaticRegion
 * @Lock StaticLock is class protects StaticRegion
 * @Region NonStaticRegion
 * @Lock NonStaticLock is this protects NonStaticRegion
 */
public class StateLock_LockNames {
  /**
   * BAD: cannot qualify a static lock with this
   * @TestResult is UNASSOCIATED: cannot qualify a static lock with this
   * @RequiresLock this:StaticLock
   */
  public void bad_thisQualified_staticLock() {}
  
  /**
   * GOOD: Can ref static lock from instance method
   * @TestResult is CONSISTENT
   * @RequiresLock StaticLock
   */
  public void good_implicit_staticLock() {}
  
  /**
   * GOOD: Can ref static lock from instance method
   * @TestResult is CONSISTENT
   * @RequiresLock test_requires_lock.StateLock_LockNames:StaticLock
   */
  public void good_typeQualified_staticLock() {}
  
  /**
   * BAD: type-qualified instance lock
   * @TestResult is UNASSOCIATED: type-qualified instance lock
   * @RequiresLock test_requires_lock.StateLock_LockNames:Exists
   */
  public void bad_typeQualifiedInstance() {}
  
  /**
   * BAD: Lock doesn't exist
   * @TestResult is UNBOUND: Lock doesn't exist
   * @RequiresLock DoesntExist
   */
  public void bad_doesntExist() {}
  
  /**
   * GOOD: Lock exists
   * @TestResult is CONSISTENT
   * @RequiresLock Exists
   */
  public void good_exists_implicit_this() {}
  
  /**
   * GOOD: Lock Exists.  Test with qualified this
   * @TestResult is CONSISTENT
   * @RequiresLock this:Exists
   */
  public void good_exists_explicit_this() {}

  /**
   * BAD: parameter doesn't exist.
   * @TestResult is UNBOUND: Parameter doesn't exist
   * @RequiresLock p:DoesntExist
   */
  public void bad_param_doesnt_exist(final Object o) {}
  
  /**
   * BAD: parameter exists, but the lock doesn't exist
   * @TestResult is UNBOUND: Lock doesn't exist
   * @RequiresLock p:DoesntExist 
   */
  public void bad_param_lock_doesnt_exist(final StateLock_LockNames p) {}
    
  /**
   * BAD: parameter exists; lock exists; but param is non-final
   * @TestResult is UNASSOCIATED: parameter is non-final
   * @RequiresLock p:Exists 
   */
  public void bad_param_nonfinal(StateLock_LockNames p) {}
  
  /**
   * GOOD: parameter exists; lock exists; param is final
   * @TestResult is CONSISTENT
   * @RequiresLock p:Exists 
   */
  public void good_param_is_good(final StateLock_LockNames p) {}
  
  
  
  
  
  
  
  
  /**
   * BAD: cannot bind "this" for a static method!  Irrelevant that lock otherwise exists.
   * This is a syntactic check on the QualifiedLockName.
   * @TestResult is UNASSOCIATED: cannot use 'this' on static method
   * @RequiresLock this:NonStaticLock
   */
  public static void bad_static_use_of_explicit_this() {}

  /**
   * BAD: cannot bind "this" for a static method!  Irrelevant that lock otherwise does exist.
   * This is a semantic check on the protected region: Check that it is static.
   * @TestResult is UNASSOCIATED: cannot use 'this' (implicit) on static method
   * @RequiresLock NonStaticLock
   */
  public static void bad_static_use_of_implicit_this() {}
  
  /** 
   * GOOD: Returns lock that protects a static region.
   * @TestResult is CONSISTENT
   * @RequiresLock StaticLock 
   */
  public static void good_staticMethod_implicit_staticLock() {}
  
  /**
   * GOOD: Returns lock that protects a static region.  Here we explicitly 
   * name the class.
   * @TestResult is CONSISTENT
   * @RequiresLock test_requires_lock.StateLock_LockNames:StaticLock 
   */
  public static void good_staticMethod_typeQualified_staticLock() {}
  
  /**
   * BAD: Lock does not exist.  Irrelevant that there is a static use of receiver.
   * @TestResult is UNBOUND: static use of this with unknown region
   * @RequiresLock this:DoesntExist
   */
  public static void bad_staticMethod_unknownRegionWithThis() {}

  /**
   * BAD: Lock does not exist.  Irrelevant that there is a static use of receiver.
   * @TestResult is UNBOUND: static use of this (implicit) with unknown region
   * @RequiresLock DoesntExist
   */
  public static void bad_staticMethod_unknownRegionImplicitThis() {}
  
  /**
   * BAD: The named class doesn't exist.  (Default package)
   * @TestResult is UNBOUND: No such class (default package)
   * @RequiresLock NoSuchClass:DoesntExist
   */
  public static void bad_staticMethod_unknownClass1() {}
  
  /**
   * BAD: The named class doesn't exist.  (Named package)
   * @TestResult is UNBOUND: No such class (non-existent package)
   * @RequiresLock no.such.pkg.NoSuchClass:DoesntExist
   */
  public static void bad_staticMethod_unknownClass1a() {}

  
  
  /**
   * GOOD: Static method requires an instance lock of a parameter.
   * @TestResult is CONSISTENT
   * @RequiresLock p:NonStaticLock
   */
  public static void staticMethod_paramLock(final StateLock_LockNames p) {}
}
