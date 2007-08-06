package test_requires_lock;

import com.surelogic.Lock;
import com.surelogic.Locks;
import com.surelogic.PolicyLock;
import com.surelogic.PolicyLocks;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.RequiresLock;

/**
 * Check lock visibility issues for policy locks and requiresLock promises.
 * Checks that locks required from the receiver are at least as visible as 
 * the method that requires them.  Checks that static locks in class C are at 
 * least as visible as methods in class C that require them.  
 *
 * Test does not apply to locks on parameters because that can only be
 * determined at the call site.  The real issue here is that the required lock
 * should be visible when the method is visible.
 */
@Regions({
  @Region("private PrivateRegion"),
  @Region("DefaultRegion"),
  @Region("protected ProtectedRegion"),
  @Region("public PublicRegion"),
  @Region("private static PrivateStaticRegion"),
  @Region("static DefaultStaticRegion"),
  @Region("protected static ProtectedStaticRegion"),
  @Region("public static PublicStaticRegion")
})
@Locks({
  @Lock("PrivateLock is privateField protects PrivateRegion"), 
  @Lock("DefaultLock is defaultField protects DefaultRegion"), 
  @Lock("ProtectedLock is protectedField protects ProtectedRegion"), 
  @Lock("PublicLock is publicField protects PublicRegion"), 
  @Lock("PrivateStaticLock is privateStaticField protects PrivateStaticRegion"), 
  @Lock("DefaultStaticLock is defaultStaticField protects DefaultStaticRegion"), 
  @Lock("ProtectedStaticLock is protectedStaticField protects ProtectedStaticRegion"), 
  @Lock("PublicStaticLock is publicStaticField protects PublicStaticRegion")
})
@SuppressWarnings("unused")
public class StateLock_visibility extends Root {
  private static final Object privateStaticField = new Object();
  static final Object defaultStaticField = new Object();
  protected static final Object protectedStaticField = new Object();
  public static final Object publicStaticField = new Object();

  private final Object privateField = new Object();
  final Object defaultField = new Object();
  protected final Object protectedField = new Object();
  public final Object publicField = new Object();

  
  
  /* ======================================================================
   * == Instance method and instance locks
   * ====================================================================== */
  
  @RequiresLock("PrivateLock" /*is CONSISTENT*/)
  private void good_privateMethod_privateLock() {}
  
  @RequiresLock("DefaultLock" /*is CONSISTENT*/)
  private void good_privateMethod_defaultLock() {}
  
  @RequiresLock("ProtectedLock" /*is CONSISTENT*/)
  private void good_privateMethod_protectedLock() {}
  
  @RequiresLock("PublicLock" /*is CONSISTENT*/)
  private void good_privateMethod_publicLock() {}
  
  
  
  @RequiresLock("PrivateLock" /*is UNASSOCIATED: default method requires private lock*/)
  void bad_defaultMethod_privateLock() {}
  
  @RequiresLock("DefaultLock" /*is CONSISTENT*/)
  void good_defaultMethod_defaultLock() {}
  
  @RequiresLock("ProtectedLock" /*is CONSISTENT*/)
  void good_defaultMethod_protectedLock() {}
  
  @RequiresLock("PublicLock" /*is CONSISTENT*/)
  void good_defaultMethod_publicLock() {}
  
  
  
  @RequiresLock("PrivateLock" /*is UNASSOCIATED: protected method requires private lock*/)
  protected void bad_protectedMethod_privateLock() {}
  
  @RequiresLock("DefaultLock" /*is UNASSOCIATED: protected method requires default lock*/)
  protected void bad_protectedMethod_defaultLock() {}
  
  @RequiresLock("ProtectedLock" /*is CONSISTENT*/)
  protected void good_protectedMethod_protectedLock() {}
  
  @RequiresLock("PublicLock" /*is CONSISTENT*/)
  protected void good_protectedMethod_publicLock() {}
  
  
  
  @RequiresLock("PrivateLock" /*is UNASSOCIATED: Public method requires private lock*/)
  public void bad_publicMethod_privateLock() {}
  
  @RequiresLock("DefaultLock" /*is UNASSOCIATED: Public method requires default lock*/)
  public void bad_publicMethod_defaultLock() {}
  
  @RequiresLock("ProtectedLock" /*is UNASSOCIATED: Public method requires protected lock*/)
  public void bad_publicMethod_protectedLock() {}
  
  @RequiresLock("PublicLock" /*is CONSISTENT*/)
  public void good_publicMethod_publicLock() {}

  
  
  /* ======================================================================
   * == Static method and static locks
   * ====================================================================== */
  
  @RequiresLock("PrivateStaticLock" /*is CONSISTENT*/)
  private static void good_privateStaticMethod_privateStaticLock() {}
  
  @RequiresLock("DefaultStaticLock" /*is CONSISTENT*/)
  private static void good_privateStaticMethod_defaultStaticLock() {}
  
  @RequiresLock("ProtectedStaticLock" /*is CONSISTENT*/)
  private static void good_privateStaticMethod_protectedStaticLock() {}
  
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  private static void good_privateStaticMethod_publicStaticLock() {}
  
  
  
  @RequiresLock("PrivateStaticLock" /*is UNASSOCIATED: default method requires private lock*/)
  static void bad_defaultStaticMethod_privateStaticLock() {}
  
  @RequiresLock("DefaultStaticLock" /*is CONSISTENT*/)
  static void good_defaultStaticMethod_defaultStaticLock() {}
  
  @RequiresLock("ProtectedStaticLock" /*is CONSISTENT*/)
  static void good_defaultStaticMethod_protectedStaticLock() {}
  
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  static void good_defaultStaticMethod_publicStaticLock() {}
  
  
  
  @RequiresLock("PrivateStaticLock" /*is UNASSOCIATED: protected method requires private lock*/)
  protected static void bad_protectedStaticMethod_privateStaticLock() {}
  
  @RequiresLock("DefaultStaticLock" /*is UNASSOCIATED: protected method requires default lock*/)
  protected static void bad_protectedStaticMethod_defaultStaticLock() {}
  
  @RequiresLock("ProtectedStaticLock" /*is CONSISTENT*/)
  protected static void good_protectedStaticMethod_protectedStaticLock() {}
  
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  protected static void good_protectedStaticMethod_publicStaticLock() {}
  
  
  
  @RequiresLock("PrivateStaticLock" /*is UNASSOCIATED: public method requires private lock*/)
  public static void bad_publicStaticMethod_privateStaticLock() {}
  
  @RequiresLock("DefaultStaticLock" /*is UNASSOCIATED: public method requires default lock*/)
  public static void bad_publicStaticMethod_defaultStaticLock() {}
  
  @RequiresLock("ProtectedStaticLock" /*is UNASSOCIATED: public method requires protected lock*/)
  public static void bad_publicStaticMethod_protectedStaticLock() {}
  
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  public static void good_publicStaticMethod_publicStaticLock() {}

  
  
  /* ======================================================================
   * == Instance method and static locks
   * ====================================================================== */
  
  @RequiresLock("PrivateStaticLock" /*is CONSISTENT*/)
  private void good_privateMethod_privateStaticLock() {}
  
  @RequiresLock("DefaultStaticLock" /*is CONSISTENT*/)
  private void good_privateMethod_defaultStaticLock() {}
  
  @RequiresLock("ProtectedStaticLock" /*is CONSISTENT*/)
  private void good_privateMethod_protectedStaticLock() {}
  
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  private void good_privateMethod_publicStaticLock() {}
  
  
  
  @RequiresLock("PrivateStaticLock" /*is UNASSOCIATED: default method requires private lock*/)
  void bad_defaultMethod_privateStaticLock() {}
  
  @RequiresLock("DefaultStaticLock" /*is CONSISTENT*/)
  void good_defaultMethod_defaultStaticLock() {}
  
  @RequiresLock("ProtectedStaticLock" /*is CONSISTENT*/)
  void good_defaultMethod_protectedStaticLock() {}
  
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  void good_defaultMethod_publicStaticLock() {}
  
  
  
  @RequiresLock("PrivateStaticLock" /*is UNASSOCIATED: protected method requires private lock*/)
  protected void bad_protectedMethod_privateStaticLock() {}
  
  @RequiresLock("DefaultStaticLock" /*is UNASSOCIATED: protected method requires default lock*/)
  protected void bad_protectedMethod_defaultStaticLock() {}
  
  @RequiresLock("ProtectedStaticLock" /*is CONSISTENT*/)
  protected void good_protectedMethod_protectedStaticLock() {}
  
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  protected void good_protectedMethod_publicStaticLock() {}
  
  
  
  @RequiresLock("PrivateStaticLock" /*is UNASSOCIATED: public method requires private lock*/)
  public void bad_publicMethod_privateStaticLock() {}
  
  @RequiresLock("DefaultStaticLock" /*is UNASSOCIATED: public method requires default lock*/)
  public void bad_publicMethod_defaultStaticLock() {}
  
  @RequiresLock("ProtectedStaticLock" /*is UNASSOCIATED: public method requires protected lock*/)
  public void bad_publicMethod_protectedStaticLock() {}
  
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  public void good_publicMethod_publicStaticLock() {}

  
  
  /* ======================================================================
   * == Constructors and static locks
   * ====================================================================== */
  
  /** Good */
  @RequiresLock("PrivateStaticLock" /*is CONSISTENT*/)
  private StateLock_visibility(boolean x, boolean y) {}
  
  /** Good */
  @RequiresLock("DefaultStaticLock" /*is CONSISTENT*/)
  private StateLock_visibility(boolean x, int y) {}
  
  /** Good */
  @RequiresLock("ProtectedStaticLock" /*is CONSISTENT*/)
  private StateLock_visibility(boolean x, float y) {}
  
  /** Good */
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  private StateLock_visibility(boolean x, Object y) {}
  
  
  
  /** Bad */
  @RequiresLock("PrivateStaticLock" /*is UNASSOCIATED: default constructor requires private lock*/)
  StateLock_visibility(int x, boolean y) {}
  
  /** Good */
  @RequiresLock("DefaultStaticLock" /*is CONSISTENT*/)
  StateLock_visibility(int x, int y) {}
  
  /** Good */
  @RequiresLock("ProtectedStaticLock" /*is CONSISTENT*/)
  StateLock_visibility(int x, float y) {}
  
  /** Good */
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  StateLock_visibility(int x, Object y) {}
  
  
  
  /** Bad */
  @RequiresLock("PrivateStaticLock" /*is UNASSOCIATED: protected constructor requires private lock*/)
  protected StateLock_visibility(float x, boolean y) {}
  
  /** Bad */
  @RequiresLock("DefaultStaticLock" /*is UNASSOCIATED: protected constructor requires default lock*/)
  protected StateLock_visibility(float x, int y) {}
  
  /** Good */
  @RequiresLock("ProtectedStaticLock" /*is CONSISTENT*/)
  protected StateLock_visibility(float x, float y) {}
  
  /** Good */
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  protected StateLock_visibility(float x, Object y) {}
  
  
  
  /** Bad */
  @RequiresLock("PrivateStaticLock" /*is UNASSOCIATED: public constructor requires private lock*/)
  public StateLock_visibility(Object x, boolean y) {}
  
  /** Bad */
  @RequiresLock("DefaultStaticLock" /*is UNASSOCIATED: public constructor requires default lock*/)
  public StateLock_visibility(Object x, int y) {}
  
  /** Bad */
  @RequiresLock("ProtectedStaticLock" /*is UNASSOCIATED: public constructor requires protected lock*/)
  public StateLock_visibility(Object x, float y) {}
  
  /** Good */
  @RequiresLock("PublicStaticLock" /*is CONSISTENT*/)
  public StateLock_visibility(Object x, Object y) {}


  
  /**
   * Don't say anything about the use of locks from parameters.
   */
  @RequiresLock("p:PrivateLock" /*is CONSISTENT*/)
  public void good_parameterTest(final StateLock_visibility p) {}
  
  /**
   * Don't say anything about the use of static locks from other classes
   */
  @RequiresLock("test_requires_lock.D:PrivateStaticLock" /*is CONSISTENT*/)
  public void good_otherClassTest1() {}
  
  /**
   * Don't say anything about the use of static locks from other classes
   */
  @RequiresLock("test_requires_lock.D:PrivateStaticLock" /*is CONSISTENT*/)
  public static void good_otherClassTest2() {}
  
  /**
   * Don't say anything about the use of static locks from other classes
   */
  @RequiresLock("test_requires_lock.D:PrivateStaticLock" /*is CONSISTENT*/)
  public StateLock_visibility(int x, int y, int z) {}



  /**
   * Don't say anything about the use of static locks from ANCESTOR classes
   */
  @RequiresLock("test_requires_lock.Root:PrivateRootStaticLock" /*is CONSISTENT*/)
  public void good_ancestorClassTest1() {}
  
  /**
   * Don't say anything about the use of static locks from ANCESTOR classes
   */
  @RequiresLock("test_requires_lock.Root:PrivateRootStaticLock" /*is CONSISTENT*/)
  public static void good_ancestorClassTest2() {}
  
  /**
   * Don't say anything about the use of static locks from ANCESTOR classes
   */
  @RequiresLock("test_requires_lock.Root:PrivateRootStaticLock" /*is CONSISTENT*/)
  public StateLock_visibility(int x, int y, int z, int w) {}
}
