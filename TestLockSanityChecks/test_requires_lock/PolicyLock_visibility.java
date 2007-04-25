package test_requires_lock;

/**
 * Check lock visibility issues for policy locks and requiresLock promises.
 * Checks that locks required from the receiver are at least as visible as 
 * the method that requires them.  Checks that static locks in class C are at 
 * least as visible as methods in class C that require them.  
 *
 * Test does not apply to locks on parameters because that can only be
 * determined at the call site.  The real issue here is that the required lock
 * should be visible when the method is visible.
 *
 * 
 * 
 * 
 * 
 * 
 * @policyLock PrivateLock is privateField 
 * @policyLock DefaultLock is defaultField 
 * @policyLock ProtectedLock is protectedField 
 * @policyLock PublicLock is publicField 
 * 
 * 
 * 
 * 
 * 
 * 
 * @policyLock PrivateStaticLock is privateStaticField 
 * @policyLock DefaultStaticLock is defaultStaticField 
 * @policyLock ProtectedStaticLock is protectedStaticField 
 * @policyLock PublicStaticLock is publicStaticField 
 */
@SuppressWarnings("unused")
public class PolicyLock_visibility extends Root {
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
  
  /**
   * @requiresLock PrivateLock
   */
  private void good_privateMethod_privateLock() {}
  
  /**
   * @requiresLock DefaultLock
   */
  private void good_privateMethod_defaultLock() {}
  
  /**
   * @requiresLock ProtectedLock
   */
  private void good_privateMethod_protectedLock() {}
  
  /**
   * @requiresLock PublicLock
   */
  private void good_privateMethod_publicLock() {}
  
  
  
  /**
   * @requiresLock PrivateLock
   */
  void bad_defaultMethod_privateLock() {}
  
  /**
   * @requiresLock DefaultLock
   */
  void good_defaultMethod_defaultLock() {}
  
  /**
   * @requiresLock ProtectedLock
   */
  void good_defaultMethod_protectedLock() {}
  
  /**
   * @requiresLock PublicLock
   */
  void good_defaultMethod_publicLock() {}
  
  
  
  /**
   * @requiresLock PrivateLock
   */
  protected void bad_protectedMethod_privateLock() {}
  
  /**
   * @requiresLock DefaultLock
   */
  protected void bad_protectedMethod_defaultLock() {}
  
  /**
   * @requiresLock ProtectedLock
   */
  protected void good_protectedMethod_protectedLock() {}
  
  /**
   * @requiresLock PublicLock
   */
  protected void good_protectedMethod_publicLock() {}
  
  
  
  /**
   * @requiresLock PrivateLock
   */
  public void bad_publicMethod_privateLock() {}
  
  /**
   * @requiresLock DefaultLock
   */
  public void bad_publicMethod_defaultLock() {}
  
  /**
   * @requiresLock ProtectedLock
   */
  public void bad_publicMethod_protectedLock() {}
  
  /**
   * @requiresLock PublicLock
   */
  public void good_publicMethod_publicLock() {}

  
  
  /* ======================================================================
   * == Static method and static locks
   * ====================================================================== */
  
  /**
   * @requiresLock PrivateStaticLock
   */
  private static void good_privateStaticMethod_privateStaticLock() {}
  
  /**
   * @requiresLock DefaultStaticLock
   */
  private static void good_privateStaticMethod_defaultStaticLock() {}
  
  /**
   * @requiresLock ProtectedStaticLock
   */
  private static void good_privateStaticMethod_protectedStaticLock() {}
  
  /**
   * @requiresLock PublicStaticLock
   */
  private static void good_privateStaticMethod_publicStaticLock() {}
  
  
  
  /**
   * @requiresLock PrivateStaticLock
   */
  static void bad_defaultStaticMethod_privateStaticLock() {}
  
  /**
   * @requiresLock DefaultStaticLock
   */
  static void good_defaultStaticMethod_defaultStaticLock() {}
  
  /**
   * @requiresLock ProtectedStaticLock
   */
  static void good_defaultStaticMethod_protectedStaticLock() {}
  
  /**
   * @requiresLock PublicStaticLock
   */
  static void good_defaultStaticMethod_publicStaticLock() {}
  
  
  
  /**
   * @requiresLock PrivateStaticLock
   */
  protected static void bad_protectedStaticMethod_privateStaticLock() {}
  
  /**
   * @requiresLock DefaultStaticLock
   */
  protected static void bad_protectedStaticMethod_defaultStaticLock() {}
  
  /**
   * @requiresLock ProtectedStaticLock
   */
  protected static void good_protectedStaticMethod_protectedStaticLock() {}
  
  /**
   * @requiresLock PublicStaticLock
   */
  protected static void good_protectedStaticMethod_publicStaticLock() {}
  
  
  
  /**
   * @requiresLock PrivateStaticLock
   */
  public static void bad_publicStaticMethod_privateStaticLock() {}
  
  /**
   * @requiresLock DefaultStaticLock
   */
  public static void bad_publicStaticMethod_defaultStaticLock() {}
  
  /**
   * @requiresLock ProtectedStaticLock
   */
  public static void bad_publicStaticMethod_protectedStaticLock() {}
  
  /**
   * @requiresLock PublicStaticLock
   */
  public static void good_publicStaticMethod_publicStaticLock() {}

  
  
  /* ======================================================================
   * == Instance method and static locks
   * ====================================================================== */
  
  /**
   * @requiresLock PrivateStaticLock
   */
  private void good_privateMethod_privateStaticLock() {}
  
  /**
   * @requiresLock DefaultStaticLock
   */
  private void good_privateMethod_defaultStaticLock() {}
  
  /**
   * @requiresLock ProtectedStaticLock
   */
  private void good_privateMethod_protectedStaticLock() {}
  
  /**
   * @requiresLock PublicStaticLock
   */
  private void good_privateMethod_publicStaticLock() {}
  
  
  
  /**
   * @requiresLock PrivateStaticLock
   */
  void bad_defaultMethod_privateStaticLock() {}
  
  /**
   * @requiresLock DefaultStaticLock
   */
  void good_defaultMethod_defaultStaticLock() {}
  
  /**
   * @requiresLock ProtectedStaticLock
   */
  void good_defaultMethod_protectedStaticLock() {}
  
  /**
   * @requiresLock PublicStaticLock
   */
  void good_defaultMethod_publicStaticLock() {}
  
  
  
  /**
   * @requiresLock PrivateStaticLock
   */
  protected void bad_protectedMethod_privateStaticLock() {}
  
  /**
   * @requiresLock DefaultStaticLock
   */
  protected void bad_protectedMethod_defaultStaticLock() {}
  
  /**
   * @requiresLock ProtectedStaticLock
   */
  protected void good_protectedMethod_protectedStaticLock() {}
  
  /**
   * @requiresLock PublicStaticLock
   */
  protected void good_protectedMethod_publicStaticLock() {}
  
  
  
  /**
   * @requiresLock PrivateStaticLock
   */
  public void bad_publicMethod_privateStaticLock() {}
  
  /**
   * @requiresLock DefaultStaticLock
   */
  public void bad_publicMethod_defaultStaticLock() {}
  
  /**
   * @requiresLock ProtectedStaticLock
   */
  public void bad_publicMethod_protectedStaticLock() {}
  
  /**
   * @requiresLock PublicStaticLock
   */
  public void good_publicMethod_publicStaticLock() {}

  
  
  /* ======================================================================
   * == Constructors and static locks
   * ====================================================================== */
  
  /**
   * GOOD
   * @requiresLock PrivateStaticLock
   */
  private PolicyLock_visibility(boolean x, boolean y) {}
  
  /**
   * GOOD
   * @requiresLock DefaultStaticLock
   */
  private PolicyLock_visibility(boolean x, int y) {}
  
  /**
   * GOOD
   * @requiresLock ProtectedStaticLock
   */
  private PolicyLock_visibility(boolean x, float y) {}
  
  /**
   * GOOD
   * @requiresLock PublicStaticLock
   */
  private PolicyLock_visibility(boolean x, Object y) {}
  
  
  
  /**
   * BAD
   * @requiresLock PrivateStaticLock
   */
  PolicyLock_visibility(int x, boolean y) {}
  
  /**
   * GOOD
   * @requiresLock DefaultStaticLock
   */
  PolicyLock_visibility(int x, int y) {}
  
  /**
   * GOOD
   * @requiresLock ProtectedStaticLock
   */
  PolicyLock_visibility(int x, float y) {}
  
  /**
   * GOOD
   * @requiresLock PublicStaticLock
   */
  PolicyLock_visibility(int x, Object y) {}
  
  
  
  /**
   * BAD
   * @requiresLock PrivateStaticLock
   */
  protected PolicyLock_visibility(float x, boolean y) {}
  
  /**
   * BAD
   * @requiresLock DefaultStaticLock
   */
  protected PolicyLock_visibility(float x, int y) {}
  
  /**
   * GOOD
   * @requiresLock ProtectedStaticLock
   */
  protected PolicyLock_visibility(float x, float y) {}
  
  /**
   * GOOD
   * @requiresLock PublicStaticLock
   */
  protected PolicyLock_visibility(float x, Object y) {}
  
  
  
  /**
   * BAD
   * @requiresLock PrivateStaticLock
   */
  public PolicyLock_visibility(Object x, boolean y) {}
  
  /**
   * BAD
   * @requiresLock DefaultStaticLock
   */
  public PolicyLock_visibility(Object x, int y) {}
  
  /**
   * BAD
   * @requiresLock ProtectedStaticLock
   */
  public PolicyLock_visibility(Object x, float y) {}
  
  /**
   * GOOD
   * @requiresLock PublicStaticLock
   */
  public PolicyLock_visibility(Object x, Object y) {}


  
  /**
   * XXX: Currently incorrectly flagged as an error.
   * Don't say anything about the use of locks from parameters.
   * @requiresLock p.PrivateLock
   */
  public void good_parameterTest(final PolicyLock_visibility p) {}
  
  /**
   * Don't say anything about the use of static locks from other classes
   * @requiresLock test_requires_lock.D:PrivateStaticPolicyLock
   */
  public void good_otherClassTest1() {}
  
  /**
   * Don't say anything about the use of static locks from other classes
   * @requiresLock test_requires_lock.D:PrivateStaticPolicyLock
   */
  public static void good_otherClassTest2() {}
  
  /**
   * Don't say anything about the use of static locks from other classes
   * @requiresLock test_requires_lock.D:PrivateStaticPolicyLock
   */
  public PolicyLock_visibility(int x, int y, int z) {}



  /**
   * Don't say anything about the use of static locks from ANCESTOR classes
   * @requiresLock test_requires_lock.Root:PrivateRootStaticPolicyLock
   */
  public void good_ancestorClassTest1() {}
  
  /**
   * Don't say anything about the use of static locks from ANCESTOR classes
   * @requiresLock test_requires_lock.Root:PrivateRootStaticPolicyLock
   */
  public static void good_ancestorClassTest2() {}
  
  /**
   * Don't say anything about the use of static locks from ANCESTOR classes
   * @requiresLock test_requires_lock.Root:PrivateRootStaticPolicyLock
   */
  public PolicyLock_visibility(int x, int y, int z, int w) {}
}
