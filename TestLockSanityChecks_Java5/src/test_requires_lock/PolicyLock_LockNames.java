package test_requires_lock;

import com.surelogic.PolicyLock;
import com.surelogic.PolicyLocks;
import com.surelogic.RequiresLock;

/**
 * (These tests are all really Lock Name sanity tests.)
 */
@PolicyLocks({
  @PolicyLock("Exists is this"),
  @PolicyLock("StaticLock is class"),
  @PolicyLock("NonStaticLock is this")
})
public class PolicyLock_LockNames {
  /**
   * BAD: cannot qualify a static lock with this
   */
  @RequiresLock("this:StaticLock" /*is UNASSOCIATED: cannot qualify a static lock with this*/)
  public void bad_thisQualified_staticLock() {}
  
  /**
   * GOOD: Can ref static lock from instance method
   */
  @RequiresLock("StaticLock" /* is CONSISTENT */)
  public void good_implicit_staticLock() {}
  
  /**
   * GOOD: Can ref static lock from instance method
   */
  @RequiresLock("test_requires_lock.PolicyLock_LockNames:StaticLock" /* is CONSISTENT */)
  public void good_typeQualified_staticLock() {}
  
  /**
   * BAD: type-qualified instance lock
   */
  @RequiresLock("test_requires_lock.PolicyLock_LockNames:Exists" /*is UNASSOCIATED: type-qualified instance lock*/)
  public void bad_typeQualifiedInstance() {}
  
  /**
   * BAD: Lock doesn't exist
   */
  @RequiresLock("DoesntExist" /* is UNBOUND: Lock doesn't exist*/)
  public void bad_doesntExist() {}
  
  /**
   * GOOD: Lock exists
   */
  @RequiresLock("Exists" /* is CONSISTENT */)
  public void good_exists_implicit_this() {}
  
  /**
   * GOOD: Lock Exists.  Test with qualified this
   */
  @RequiresLock("this:Exists" /* is CONSISTENT */)
  public void good_exists_explicit_this() {}

  /**
   * BAD: parameter doesn't exist.
   */
  @RequiresLock("p:DoesntExist" /*is UNBOUND: Parameter doesn't exist*/)
  public void bad_param_doesnt_exist(final Object o) {}
  
  /**
   * BAD: parameter exists, but the lock doesn't exist
   */
  @RequiresLock("p:DoesntExist" /* is UNBOUND: Lock doesn't exist*/)
  public void bad_param_lock_doesnt_exist(final PolicyLock_LockNames p) {}
    
  /**
   * BAD: parameter exists; lock exists; but param is non-final
   */
  @RequiresLock("p:Exists" /* is UNASSOCIATED: parameter is non-final */)
  public void bad_param_nonfinal(PolicyLock_LockNames p) {}
  
  /**
   * GOOD: parameter exists; lock exists; param is final
   */
  @RequiresLock("p:Exists" /* is CONSISTENT */)
  public void good_param_is_good(final PolicyLock_LockNames p) {}
  
  
  
  
  
  
  
  
  /**
   * BAD: cannot bind "this" for a static method!  Irrelevant that lock otherwise exists.
   * This is a syntactic check on the QualifiedLockName.
   */
  @RequiresLock("this:NonStaticLock" /*is UNASSOCIATED: cannot use 'this' on static method*/)
  public static void bad_static_use_of_explicit_this() {}

  /**
   * BAD: cannot bind "this" for a static method!  Irrelevant that lock otherwise does exist.
   * This is a semantic check on the protected region: Check that it is static.
   */
  @RequiresLock("NonStaticLock" /*is UNASSOCIATED: cannot use 'this' (implicit) on static method*/)
  public static void bad_static_use_of_implicit_this() {}
  
  /** 
   * GOOD: Returns lock that protects a static region.
   */
  @RequiresLock("StaticLock" /*is CONSISTENT*/)
  public static void good_staticMethod_implicit_staticLock() {}
  
  /**
   * GOOD: Returns lock that protects a static region.  Here we explicitly 
   * name the class.
   */
  @RequiresLock("test_requires_lock.PolicyLock_LockNames:StaticLock" /* is CONSISTENT */)
  public static void good_staticMethod_typeQualified_staticLock() {}
  
  /**
   * BAD: Lock does not exist.  Irrelevant that there is a static use of receiver.
   */
  @RequiresLock("this:DoesntExist" /*is UNBOUND: static use of this with unknown region*/)
  public static void bad_staticMethod_unknownRegionWithThis() {}

  /**
   * BAD: Lock does not exist.  Irrelevant that there is a static use of receiver.
   */
  @RequiresLock("DoesntExist" /*is UNBOUND: static use of this (implicit) with unknown region*/)
  public static void bad_staticMethod_unknownRegionImplicitThis() {}
  
  /**
   * BAD: The named class doesn't exist.  (Default package)
   */
  @RequiresLock("NoSuchClass:DoesntExist" /*is UNBOUND: No such class (default package)*/)
  public static void bad_staticMethod_unknownClass1() {}
  
  /**
   * BAD: The named class doesn't exist.  (Named package)
   */
  @RequiresLock("no.such.pkg.NoSuchClass:DoesntExist" /*is UNBOUND: No such class (non-existent package)*/)
  public static void bad_staticMethod_unknownClass1a() {}

  
  
  /**
   * GOOD: Static method requires an instance lock of a parameter.
   */
  @RequiresLock("p:NonStaticLock" /* is CONSISTENT */)
  public static void staticMethod_paramLock(final PolicyLock_LockNames p) {}
}
