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
 * @Region private PrivateRegion
 * @Region DefaultRegion
 * @Region protected ProtectedRegion
 * @Region public PublicRegion
 * 
 * @Lock PrivateLock is privateField protects PrivateRegion
 * @Lock DefaultLock is defaultField protects DefaultRegion
 * @Lock ProtectedLock is protectedField protects ProtectedRegion
 * @Lock PublicLock is publicField protects PublicRegion
 * 
 * @Region private static PrivateStaticRegion
 * @Region static DefaultStaticRegion
 * @Region protected static ProtectedStaticRegion
 * @Region public static PublicStaticRegion
 * 
 * @Lock PrivateStaticLock is privateStaticField protects PrivateStaticRegion
 * @Lock DefaultStaticLock is defaultStaticField protects DefaultStaticRegion
 * @Lock ProtectedStaticLock is protectedStaticField protects ProtectedStaticRegion
 * @Lock PublicStaticLock is publicStaticField protects PublicStaticRegion
 */
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
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PrivateLock
   */
  private void good_privateMethod_privateLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock DefaultLock
   */
  private void good_privateMethod_defaultLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedLock
   */
  private void good_privateMethod_protectedLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicLock
   */
  private void good_privateMethod_publicLock() {}
  
  
  
  /**
   * @TestResult is UNASSOCIATED: default method requires private lock
   * @RequiresLock PrivateLock
   */
  void bad_defaultMethod_privateLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock DefaultLock
   */
  void good_defaultMethod_defaultLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedLock
   */
  void good_defaultMethod_protectedLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicLock
   */
  void good_defaultMethod_publicLock() {}
  
  
  
  /**
   * @TestResult is UNASSOCIATED: protected method requires private lock
   * @RequiresLock PrivateLock
   */
  protected void bad_protectedMethod_privateLock() {}
  
  /**
   * @TestResult is UNASSOCIATED: protected method requires default lock
   * @RequiresLock DefaultLock
   */
  protected void bad_protectedMethod_defaultLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedLock
   */
  protected void good_protectedMethod_protectedLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicLock
   */
  protected void good_protectedMethod_publicLock() {}
  
  
  
  /**
   * @TestResult is UNASSOCIATED: Public method requires private lock
   * @RequiresLock PrivateLock
   */
  public void bad_publicMethod_privateLock() {}
  
  /**
   * @TestResult is UNASSOCIATED: Public method requires default lock
   * @RequiresLock DefaultLock
   */
  public void bad_publicMethod_defaultLock() {}
  
  /**
   * @TestResult is UNASSOCIATED: Public method requires protected lock
   * @RequiresLock ProtectedLock
   */
  public void bad_publicMethod_protectedLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicLock
   */
  public void good_publicMethod_publicLock() {}

  
  
  /* ======================================================================
   * == Static method and static locks
   * ====================================================================== */
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PrivateStaticLock
   */
  private static void good_privateStaticMethod_privateStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock DefaultStaticLock
   */
  private static void good_privateStaticMethod_defaultStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedStaticLock
   */
  private static void good_privateStaticMethod_protectedStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  private static void good_privateStaticMethod_publicStaticLock() {}
  
  
  
  /**
   * @TestResult is UNASSOCIATED: default method requires private lock
   * @RequiresLock PrivateStaticLock
   */
  static void bad_defaultStaticMethod_privateStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock DefaultStaticLock
   */
  static void good_defaultStaticMethod_defaultStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedStaticLock
   */
  static void good_defaultStaticMethod_protectedStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  static void good_defaultStaticMethod_publicStaticLock() {}
  
  
  
  /**
   * @TestResult is UNASSOCIATED: protected method requires private lock
   * @RequiresLock PrivateStaticLock
   */
  protected static void bad_protectedStaticMethod_privateStaticLock() {}
  
  /**
   * @TestResult is UNASSOCIATED: protected method requires default lock
   * @RequiresLock DefaultStaticLock
   */
  protected static void bad_protectedStaticMethod_defaultStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedStaticLock
   */
  protected static void good_protectedStaticMethod_protectedStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  protected static void good_protectedStaticMethod_publicStaticLock() {}
  
  
  
  /**
   * @TestResult is UNASSOCIATED: public method requires private lock
   * @RequiresLock PrivateStaticLock
   */
  public static void bad_publicStaticMethod_privateStaticLock() {}
  
  /**
   * @TestResult is UNASSOCIATED: public method requires default lock
   * @RequiresLock DefaultStaticLock
   */
  public static void bad_publicStaticMethod_defaultStaticLock() {}
  
  /**
   * @TestResult is UNASSOCIATED: public method requires protected lock
   * @RequiresLock ProtectedStaticLock
   */
  public static void bad_publicStaticMethod_protectedStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  public static void good_publicStaticMethod_publicStaticLock() {}

  
  
  /* ======================================================================
   * == Instance method and static locks
   * ====================================================================== */
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PrivateStaticLock
   */
  private void good_privateMethod_privateStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock DefaultStaticLock
   */
  private void good_privateMethod_defaultStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedStaticLock
   */
  private void good_privateMethod_protectedStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  private void good_privateMethod_publicStaticLock() {}
  
  
  
  /**
   * @TestResult is UNASSOCIATED: default method requires private lock
   * @RequiresLock PrivateStaticLock
   */
  void bad_defaultMethod_privateStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock DefaultStaticLock
   */
  void good_defaultMethod_defaultStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedStaticLock
   */
  void good_defaultMethod_protectedStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  void good_defaultMethod_publicStaticLock() {}
  
  
  
  /**
   * @TestResult is UNASSOCIATED: protected method requires private lock
   * @RequiresLock PrivateStaticLock
   */
  protected void bad_protectedMethod_privateStaticLock() {}
  
  /**
   * @TestResult is UNASSOCIATED: protected method requires default lock
   * @RequiresLock DefaultStaticLock
   */
  protected void bad_protectedMethod_defaultStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedStaticLock
   */
  protected void good_protectedMethod_protectedStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  protected void good_protectedMethod_publicStaticLock() {}
  
  
  
  /**
   * @TestResult is UNASSOCIATED: public method requires private lock
   * @RequiresLock PrivateStaticLock
   */
  public void bad_publicMethod_privateStaticLock() {}
  
  /**
   * @TestResult is UNASSOCIATED: public method requires default lock
   * @RequiresLock DefaultStaticLock
   */
  public void bad_publicMethod_defaultStaticLock() {}
  
  /**
   * @TestResult is UNASSOCIATED: public method requires protected lock
   * @RequiresLock ProtectedStaticLock
   */
  public void bad_publicMethod_protectedStaticLock() {}
  
  /**
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  public void good_publicMethod_publicStaticLock() {}

  
  
  /* ======================================================================
   * == Constructors and static locks
   * ====================================================================== */
  
  /**
   * GOOD
   * @TestResult is CONSISTENT
   * @RequiresLock PrivateStaticLock
   */
  private StateLock_visibility(boolean x, boolean y) {}
  
  /**
   * GOOD
   * @TestResult is CONSISTENT
   * @RequiresLock DefaultStaticLock
   */
  private StateLock_visibility(boolean x, int y) {}
  
  /**
   * GOOD
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedStaticLock
   */
  private StateLock_visibility(boolean x, float y) {}
  
  /**
   * GOOD
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  private StateLock_visibility(boolean x, Object y) {}
  
  
  
  /**
   * BAD
   * @TestResult is UNASSOCIATED: default constructor requires private lock
   * @RequiresLock PrivateStaticLock
   */
  StateLock_visibility(int x, boolean y) {}
  
  /**
   * GOOD
   * @TestResult is CONSISTENT
   * @RequiresLock DefaultStaticLock
   */
  StateLock_visibility(int x, int y) {}
  
  /**
   * GOOD
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedStaticLock
   */
  StateLock_visibility(int x, float y) {}
  
  /**
   * GOOD
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  StateLock_visibility(int x, Object y) {}
  
  
  
  /**
   * BAD
   * @TestResult is UNASSOCIATED: protected constructor requires private lock
   * @RequiresLock PrivateStaticLock
   */
  protected StateLock_visibility(float x, boolean y) {}
  
  /**
   * BAD
   * @TestResult is UNASSOCIATED: protected constructor requires default lock
   * @RequiresLock DefaultStaticLock
   */
  protected StateLock_visibility(float x, int y) {}
  
  /**
   * GOOD
   * @TestResult is CONSISTENT
   * @RequiresLock ProtectedStaticLock
   */
  protected StateLock_visibility(float x, float y) {}
  
  /**
   * GOOD
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  protected StateLock_visibility(float x, Object y) {}
  
  
  
  /**
   * BAD
   * @TestResult is UNASSOCIATED: public constructor requires private lock
   * @RequiresLock PrivateStaticLock
   */
  public StateLock_visibility(Object x, boolean y) {}
  
  /**
   * BAD
   * @TestResult is UNASSOCIATED: public constructor requires default lock
   * @RequiresLock DefaultStaticLock
   */
  public StateLock_visibility(Object x, int y) {}
  
  /**
   * BAD
   * @TestResult is UNASSOCIATED: public constructor requires protected lock
   * @RequiresLock ProtectedStaticLock
   */
  public StateLock_visibility(Object x, float y) {}
  
  /**
   * GOOD
   * @TestResult is CONSISTENT
   * @RequiresLock PublicStaticLock
   */
  public StateLock_visibility(Object x, Object y) {}


  
  /**
   * Don't say anything about the use of locks from parameters.
   * @TestResult is CONSISTENT
   * @RequiresLock p:PrivateLock
   */
  public void good_parameterTest(final StateLock_visibility p) {}
  
  /**
   * Don't say anything about the use of static locks from other classes
   * @TestResult is CONSISTENT
   * @RequiresLock test_requires_lock.D:PrivateStaticLock
   */
  public void good_otherClassTest1() {}
  
  /**
   * Don't say anything about the use of static locks from other classes
   * @TestResult is CONSISTENT
   * @RequiresLock test_requires_lock.D:PrivateStaticLock
   */
  public static void good_otherClassTest2() {}
  
  /**
   * Don't say anything about the use of static locks from other classes
   * @TestResult is CONSISTENT
   * @RequiresLock test_requires_lock.D:PrivateStaticLock
   */
  public StateLock_visibility(int x, int y, int z) {}



  /**
   * Don't say anything about the use of static locks from ANCESTOR classes
   * @TestResult is CONSISTENT
   * @RequiresLock test_requires_lock.Root:PrivateRootStaticLock
   */
  public void good_ancestorClassTest1() {}
  
  /**
   * Don't say anything about the use of static locks from ANCESTOR classes
   * @TestResult is CONSISTENT
   * @RequiresLock test_requires_lock.Root:PrivateRootStaticLock
   */
  public static void good_ancestorClassTest2() {}
  
  /**
   * Don't say anything about the use of static locks from ANCESTOR classes
   * @TestResult is CONSISTENT
   * @RequiresLock test_requires_lock.Root:PrivateRootStaticLock
   */
  public StateLock_visibility(int x, int y, int z, int w) {}
}
