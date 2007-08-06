package test_returns_lock;

import com.surelogic.Lock;
import com.surelogic.Locks;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.ReturnsLock;

/**
 * (These tests are all really Lock Name sanity tests.)
 */
@Regions({
  @Region("R"),
  @Region("static StaticRegion"),
  @Region("NonStaticRegion")
})
@Locks({
  @Lock("Exists is this protects R"),
  @Lock("StaticLock is class protects StaticRegion"),
  @Lock("NonStaticLock is this protects NonStaticRegion")
})
public class StateLock {
  /**
   * BAD: cannot qualify a static lock with this
   */
  @ReturnsLock("this:StaticLock" /*is UNASSOCIATED: Cannot qualify a static lock with this*/)
  public Object getLock_thisQualified_staticLock() {
    return StateLock.class;
  }
  
  /**
   * GOOD: Can ref static lock from instance method
   */
  @ReturnsLock("StaticLock" /* is CONSISTENT */)
  public Object getLock_implicit_staticLock() {
    return StateLock.class;
  }
  
  /**
   * GOOD: Can ref static lock from instance method
   */
  @ReturnsLock("test_returns_lock.StateLock:StaticLock" /* is CONSISTENT */)
  public Object getLock_typeQualified_staticLock() {
    return StateLock.class;
  }
  
  /**
   * BAD: type-qualified instance lock
   */
  @ReturnsLock("test_returns_lock.StateLock:Exists" /*is UNASSOCIATED: Cannot type-qualify an instance lock*/)
  public Object getLock_typeQualifiedInstance() {
    return null;
  }
  
  /**
   * BAD: Lock doesn't exist
   */
  @ReturnsLock("DoesntExist" /*is UNBOUND*/)
  public Object getLock_doesntExist() {
    return null;
  }
  
  /**
   * GOOD: Lock exists
   */
  @ReturnsLock("Exists" /*is CONSISTENT*/)
  public Object getLock_exists() {
    return this;
  }
  
  /**
   * GOOD: Lock Exists.  Test with qualified this
   */
  @ReturnsLock("this:Exists" /*is CONSISTENT*/)
  public Object getLock_exists2() {
    return this;
  }

  /**
   * BAD: parameter doesn't exist.
   */
  @ReturnsLock("p:DoesntExist" /* is UNBOUND: parameter p doesn't exist */)
  public Object getLock_bad_param(final Object o) {
    return o;
  }
  
  /**
   * BAD: parameter exists, but the lock doesn't exist
   */
  @ReturnsLock("p:DoesntExist" /*is UNBOUND*/)
  public Object getLock_param_doesntExist(final StateLock p) {
    return p;
  }
    
  /**
   * BAD: parameter exists; lock exists; but param is non-final
   */
  @ReturnsLock("p:Exists" /*is UNASSOCIATED: parameter is non-final*/)
  public Object getLock_param_nonfinal(StateLock p) {
    return p;
  }
  
  /**
   * GOOD: parameter exists; lock exists; param is final
   */
  @ReturnsLock("p:Exists" /* is CONSISTENT */)
  public Object getLock_param_good(final StateLock p) {
    return p;
  }
  
  
  
  
  
  
  
  
  /**
   * BAD: cannot bind "this"!  Irrelevant that lock otherwise exists.
   * This is a syntactic check on the QualifiedLockName.
   */
  @ReturnsLock("this:NonStaticLock" /*is UNASSOCIATED: Cannot refer to 'this' on static method*/)
  public static Object getLock_staticMethod_instanceRegion1() {
    return new Object();
  }

  /**
   * BAD: cannot bind "this"!  Irrelevant that lock otherwise does exist.
   * This is a semantic check on the protected region: Check that it is static.
   */
  @ReturnsLock("NonStaticLock" /*is UNASSOCIATED: Cannot refer to 'this' (implicitly) on static method*/)
  public static Object getLock_staticMethod_instanceRegion1a() {
    return new Object();
  }
  
  /**
   * GOOD: Returns lock that protects a static region.
   */
  @ReturnsLock("StaticLock" /*is CONSISTENT*/)
  public static Object getLock_staticMethod_staticRegion1() {
    return StateLock.class;
  }
  
  /**
   * GOOD: Returns lock that protects a static region.  Here we explicitly 
   * name the class.
   */
  @ReturnsLock("test_returns_lock.StateLock:StaticLock" /*is CONSISTENT */)
  public static Object getLock_staticMethod_staticRegion1a() {
    return StateLock.class;
  }
  
  /**
   * BAD: Lock does not exist; doesn't matter that we cannot refer to 'this'
   */
  @ReturnsLock("this:DoesntExist" /*is UNBOUND: Lock does not exist*/)
  public static Object getLock_staticMethod_unknownRegionWithThis() {
    return new Object();
  }

  /**
   * BAD: Lock doesn't exist, so doesn't matter if it is static or not.
   */
  @ReturnsLock("DoesntExist" /*is UNBOUND: Lock doesn't exist */)
  public static Object getLock_staticMethod_unknownRegion() {
    return new Object();
  }
  
  /**
   * BAD: The named class doesn't exist.  (Default package)
   */
  @ReturnsLock("NoSuchClass:DoesntExist" /* is UNBOUND: Class doesn't exist (Default package)*/)
  public static Object getLock_staticMethod_unknownClass1() {
    return null;
  }
  
  /**
   * BAD: The named class doesn't exist.  (Named package)
   */
  @ReturnsLock("no.such.pkg.NoSuchClass:DoesntExist" /* is UNBOUND: Package & Class don't exist */)
  public static Object getLock_staticMethod_unknownClass1a() {
    return null;
  }

  /**
   * GOOD: Static method returns an instance lock of a parameter.
   */
  @ReturnsLock("p:NonStaticLock" /* is CONSISTENT */)
  public static Object getLock_staticMethod_paramLock(final StateLock p) {
    return p;
  }
}
