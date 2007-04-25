package test_requires_lock;

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
public class StateLock_LockNames {
  /**
   * BAD: cannot qualify a static lock with this
   * @requiresLock this.StaticLock
   */
  public void bad_thisQualified_staticLock() {}
  
  /**
   * GOOD: Can ref static lock from instance method
   * @requiresLock StaticLock
   */
  public void good_implicit_staticLock() {}
  
  /**
   * GOOD: Can ref static lock from instance method
   * @requiresLock test_requires_lock.StateLock_LockNames:StaticLock
   */
  public void good_typeQualified_staticLock() {}
  
  /**
   * BAD: type-qualified instance lock
   * @requiresLock test_requires_lock.StateLock_LockNames:Exists
   */
  public void bad_typeQualifiedInstance() {}
  
  /**
   * BAD: Lock doesn't exist
   * @requiresLock DoesntExist
   */
  public void bad_doesntExist() {}
  
  /**
   * GOOD: Lock exists
   * @requiresLock Exists
   */
  public void good_exists_implicit_this() {}
  
  /**
   * GOOD: Lock Exists.  Test with qualified this
   * @requiresLock this.Exists
   */
  public void good_exists_explicit_this() {}

  /**
   * BAD: parameter doesn't exist.
   * @requiresLock p.DoesntExist
   */
  public void bad_param_doesnt_exist(final Object o) {}
  
  /**
   * BAD: parameter exists, but the lock doesn't exist
   * @requiresLock p.DoesntExist 
   */
  public void bad_param_lock_doesnt_exist(final StateLock_LockNames p) {}
    
  /**
   * BAD: parameter exists; lock exists; but param is non-final
   * @requiresLock p.Exists 
   */
  public void bad_param_nonfinal(StateLock_LockNames p) {}
  
  /**
   * GOOD: parameter exists; lock exists; param is final
   * @requiresLock p.Exists 
   */
  public void good_param_is_good(final StateLock_LockNames p) {}
  
  
  
  
  
  
  
  
  /**
   * BAD: cannot bind "this" for a static method!  Irreleveant that lock otherwise exists.
   * This is a syntactic check on the QualifiedLockName.
   * @requiresLock this.NonStaticLock
   */
  public static void bad_static_use_of_explicit_this() {}

  /**
   * BAD: cannot bind "this" for a static method!  Irreleveant that lock otherwise does exist.
   * This is a semantic check on the protected region: Check that it is static.
   *
   * @requiresLock NonStaticLock
   */
  public static void bad_static_use_of_implicit_this() {}
  
  /** 
   * GOOD: Returns lock that protects a static region.
   * @requiresLock StaticLock 
   */
  public static void good_staticMethod_implicit_staticLock() {}
  
  /**
   * GOOD: Returns lock that protects a static region.  Here we explicitly 
   * name the class.
   * @requiresLock test_requires_lock.StateLock_LockNames:StaticLock 
   */
  public static void good_staticMethod_typeQualified_staticLock() {}
  
  /**
   * BAD: cannot bind "this"!  Irreleveant that lock otherwise does not exist.
   * @requiresLock this.UnknownRegion
   */
  public static void bad_staticMethod_unknownRegionWithThis() {}

  /**
   * BAD: Region doesn't exist, so doesn't matter if it is static or not. 
   * @requiresLock DoesntExist
   */
  public static void bad_staticMethod_unknownRegionImplicitThis() {}
  
  /**
   * BAD: The named class doesn't exist.  (Default package)
   * @requiresLock C:DoesntExist
   */
  public static void bad_staticMethod_unknownClass1() {}
  
  /**
   * BAD: The named class doesn't exist.  (Named package)
   * @requiresLock foo.bar.C:DoesntExist
   */
  public static void bad_staticMethod_unknownClass1a() {}

  
  
  /**
   * GOOD: Static method requires an instance lock of a parameter.
   * @requiresLock p.NonStaticLock
   */
  public static void staticMethod_paramLock(final StateLock_LockNames p) {}
}
