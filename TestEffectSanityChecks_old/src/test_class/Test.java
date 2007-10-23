package test_class;

import com.surelogic.Reads;
import com.surelogic.Writes;

public class Test {
  /**
   * Constructor: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is static.
   * read effect.
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:StaticRegion" /* is CONSISTENT */)
  public Test(boolean read) {}

  /**
   * Constructor: allowed to have class targets.
   * Class exists (fully qualified, different package).
   * region exists.
   * region is static.
   * read effect.
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Reads("test_class.nested.C:StaticRegion" /* is CONSISTENT */)
  public Test(boolean read, int a) {}

  /**
   * Constructor: allowed to have class targets.
   * Class exists (Unqualified).
   * region exists.
   * region is static.
   * read effect.
   */
  // XXX: Bug 1053, binder doesn't handle this yet
  // GOOD
  @SuppressWarnings("deprecation")
  @Reads("C:StaticRegion" /* is CONSISTENT */)
  public Test(boolean read, int a, int b) {}

  /**
   * Constructor: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is non-static.
   * read effect.
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:InstanceRegion" /* is UNASSOCIATED */)
  public Test(boolean read, int a, int b, int c) {}

  /**
   * Constructor: allowed to have class targets.
   * Class does not exist.
   * region exists.
   * region is static.
   * read effect.
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Reads("test_class.NoSuchClass:StaticRegion" /* is UNBOUND */)
  public Test(boolean read, int a, int b, int c, int d) {}

  /**
   * Constructor: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region does not exist.
   * read effect.
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:NoSuchRegion" /* is UNBOUND */)
  public Test(boolean read, int a, int b, int c, int d, int e) {}

  /**
   * Constructor: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists, but not as part of C
   * read effect.
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:RegionFromD" /* is UNBOUND */)
  public Test(boolean read, int a, int b, int c, int d, int e, int f) {}




  /**
   * Constructor: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is static.
   * write effect.
   */
  /// GOOD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:StaticRegion" /* is CONSISTENT */)
  public Test(Object write) {}

  /**
   * Constructor: allowed to have class targets.
   * Class exists (fully qualified, different package).
   * region exists.
   * region is static.
   * write effect.
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Writes("test_class.nested.C:StaticRegion" /* is CONSISTENT */)
  public Test(Object write, int a) {}

  /**
   * Constructor: allowed to have class targets.
   * Class exists (Unqualified).
   * region exists.
   * region is static.
   * write effect.
   */
  // XXX: Bug 1053, binder doesn't handle this yet
  // GOOD
  @SuppressWarnings("deprecation")
  @Writes("C:StaticRegion" /* is CONSISTENT */)
  public Test(Object write, int a, int b) {}

  /**
   * Constructor: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is non-static.
   * write effect.
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:InstanceRegion" /* is UNASSOCIATED */)
  public Test(Object write, int a, int b, int c) {}

  /**
   * Constructor: allowed to have class targets.
   * Class does not exist.
   * region exists.
   * region is static.
   * write effect.
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Writes("test_class.NoSuchClass:StaticRegion" /* is UNBOUND */)
  public Test(Object write, int a, int b, int c, int d) {}

  /**
   * Constructor: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region does not exist.
   * write effect.
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:NoSuchRegion" /* is UNBOUND */)
  public Test(Object write, int a, int b, int c, int d, int e) {}

  /**
   * Constructor: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists, but not as part of C
   * write effect.
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:RegionFromD" /* is UNBOUND */)
  public Test(Object write, int a, int b, int c, int d, int e, int f) {}

  
  
  //===
  
  /**
   * Method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is static.
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:StaticRegion" /* is CONSISTENT */)
  public void good_read_qualifiedClassName() {}

  /**
   * Method: allowed to have class targets.
   * Class exists (fully qualified, different package).
   * region exists.
   * region is static.
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.nested.C:StaticRegion" /* is CONSISTENT */)
  public void good_read_goodQualfiedClassName2() {}

  /**
   * Method: allowed to have class targets.
   * Class exists (Unqualified).
   * region exists.
   * region is static.
   * read effect.
   */
  // XXX: Bug 1053, binder doesn't handle this yet
  @SuppressWarnings("deprecation")
  @Reads("C:StaticRegion" /* is CONSISTENT */)
  public void good_read_unqualifiedClassName() {}

  /**
   * Method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is non-static.
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:InstanceRegion" /* is UNASSOCIATED */)
  public void bad_read_staticRegion() {}

  /**
   * Method: allowed to have class targets.
   * Class does not exist.
   * region exists.
   * region is static.
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.NoSuchClass:StaticRegion" /* is UNBOUND */)
  public void bad_read_noSuchClass() {}

  /**
   * Method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region does not exist.
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:NoSuchRegion" /* is UNBOUND */)
  public void bad_read_noSuchRegion() {}

  /**
   * Method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists, but not as part of C
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:RegionFromD" /* is UNBOUND */)
  public void bad_read_noSuchRegion2() {}




  /**
   * Method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is static.
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:StaticRegion" /* is CONSISTENT */)
  public void good_write_qualifiedClassName() {}

  /**
   * Method: allowed to have class targets.
   * Class exists (fully qualified, different package).
   * region exists.
   * region is static.
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.nested.C:StaticRegion" /* is CONSISTENT */)
  public void good_write_goodQualfiedClassName2() {}

  /**
   * Method: allowed to have class targets.
   * Class exists (Unqualified).
   * region exists.
   * region is static.
   * write effect.
   */
  // XXX: Bug 1053, binder doesn't handle this yet
  @SuppressWarnings("deprecation")
  @Writes("C:StaticRegion" /* is CONSISTENT */)
  public void good_write_unqualifiedClassName() {}

  /**
   * Method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is non-static.
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:InstanceRegion" /* is UNASSOCIATED */)
  public void bad_write_staticRegion() {}

  /**
   * Method: allowed to have class targets.
   * Class does not exist.
   * region exists.
   * region is static.
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.NoSuchClass:StaticRegion" /* is UNBOUND */)
  public void bad_write_noSuchClass() {}

  /**
   * Method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region does not exist.
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:NoSuchRegion" /* is UNBOUND */)
  public void bad_write_noSuchRegion() {}

  /**
   * Method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists, but not as part of C
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:RegionFromD" /* is UNBOUND */)
  public void bad_write_noSuchRegion2() {}

  // Static methods
  
  
  /**
   * Static method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is static.
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:StaticRegion" /* is CONSISTENT */)
  public static void good_static_read_qualifiedClassName() {}

  /**
   * Static method: allowed to have class targets.
   * Class exists (fully qualified, different package).
   * region exists.
   * region is static.
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.nested.C:StaticRegion" /* is CONSISTENT */)
  public static void good_static_read_goodQualfiedClassName2() {}

  /**
   * Static method: allowed to have class targets.
   * Class exists (Unqualified).
   * region exists.
   * region is static.
   * read effect.
   */
  // XXX: Bug 1053, binder doesn't handle this yet
  @SuppressWarnings("deprecation")
  @Reads("C:StaticRegion" /* is CONSISTENT */)
  public static void good_static_read_unqualifiedClassName() {}

  /**
   * Static method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is non-static.
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:InstanceRegion" /* is UNASSOCIATED */)
  public static void bad_static_read_staticRegion() {}

  /**
   * Static method: allowed to have class targets.
   * Class does not exist.
   * region exists.
   * region is static.
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.NoSuchClass:StaticRegion" /* is UNBOUND */)
  public static void bad_static_read_noSuchClass() {}

  /**
   * Static method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region does not exist.
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:NoSuchRegion" /* is UNBOUND */)
  public static void bad_static_read_noSuchRegion() {}

  /**
   * Static method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists, but not as part of C
   * read effect.
   */
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:RegionFromD" /* is UNBOUND */)
  public static void bad_static_read_noSuchRegion2() {}




  /**
   * Static method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is static.
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:StaticRegion" /* is CONSISTENT */)
  public static void good_static_write_qualifiedClassName() {}

  /**
   * Static method: allowed to have class targets.
   * Class exists (fully qualified, different package).
   * region exists.
   * region is static.
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.nested.C:StaticRegion" /* is CONSISTENT */)
  public static void good_static_write_goodQualfiedClassName2() {}

  /**
   * Static method: allowed to have class targets.
   * Class exists (Unqualified).
   * region exists.
   * region is static.
   * write effect.
   */
  // XXX: Bug 1053, binder doesn't handle this yet
  @SuppressWarnings("deprecation")
  @Writes("C:StaticRegion" /* is CONSISTENT */)
  public static void good_static_write_unqualifiedClassName() {}

  /**
   * Static method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists.
   * region is non-static.
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:InstanceRegion" /* is UNASSOCIATED */)
  public static void bad_static_write_staticRegion() {}

  /**
   * Static method: allowed to have class targets.
   * Class does not exist.
   * region exists.
   * region is static.
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.NoSuchClass:StaticRegion" /* is UNBOUND */)
  public static void bad_static_write_noSuchClass() {}

  /**
   * Static method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region does not exist.
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:NoSuchRegion" /* is UNBOUND */)
  public static void bad_static_write_noSuchRegion() {}

  /**
   * Static method: allowed to have class targets.
   * Class exists (fully qualified, same package).
   * region exists, but not as part of C
   * write effect.
   */
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:RegionFromD" /* is UNBOUND */)
  public static void bad_static_write_noSuchRegion2() {}




  /**
   * Static method.
   * read effect.
   * Public region, public method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:PublicRegion" /* is CONSISTENT */)
  public static void good_read_publicRegion_publicMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Public region, protected method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:PublicRegion" /* is CONSISTENT */)
  protected void bad_read_publicRegion_protectedMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Public region, default method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:PublicRegion" /* is CONSISTENT */)
  void bad_read_publicRegion_defaultMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Public region, private method: Good
   */
  // GOOD
  @SuppressWarnings({ "deprecation", "unused" })
  @Reads("test_class.C:PublicRegion" /* is CONSISTENT */)
  private void bad_read_publicRegion_privateMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Protected region, public method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
  public static void good_read_protectedRegion_publicMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Protected region, protected method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:ProtectedRegion" /* is CONSISTENT */)
  protected void bad_read_protectedRegion_protectedMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Protected region, default method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:ProtectedRegion" /* is CONSISTENT */)
  void bad_read_protectedRegion_defaultMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Protected region, private method: Good
   */
  // GOOD
  @SuppressWarnings({ "deprecation", "unused" })
  @Reads("test_class.C:ProtectedRegion" /* is CONSISTENT */)
  private void bad_read_protectedRegion_privateMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Default region, public method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  public static void good_read_defaultRegion_publicMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Default region, protected method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_read_defaultRegion_protectedMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Default region, default method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:DefaultRegion" /* is CONSISTENT */)
  void bad_read_defaultRegion_defaultMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Default region, private method: Good
   */
  // GOOD
  @SuppressWarnings({ "deprecation", "unused" })
  @Reads("test_class.C:DefaultRegion" /* is CONSISTENT */)
  private void bad_read_defaultRegion_privateMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Private region, public method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  public static void good_read_privateRegion_publicMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Private region, protected method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_read_privateRegion_protectedMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Private region, default method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Reads("test_class.C:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  void bad_read_privateRegion_defaultMethod(C c) {}

  /**
   * Static method.
   * read effect.
   * Private region, private method: Good
   */
  // GOOD
  @SuppressWarnings({ "deprecation", "unused" })
  @Reads("test_class.C:PrivateRegion" /* is CONSISTENT */)
  private void bad_read_privateRegion_privateMethod(C c) {}



  /**
   * Static method.
   * write effect.
   * Public region, public method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:PublicRegion" /* is CONSISTENT */)
  public static void good_write_publicRegion_publicMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Public region, protected method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:PublicRegion" /* is CONSISTENT */)
  protected void bad_write_publicRegion_protectedMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Public region, default method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:PublicRegion" /* is CONSISTENT */)
  void bad_write_publicRegion_defaultMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Public region, private method: Good
   */
  // GOOD
  @SuppressWarnings({ "deprecation", "unused" })
  @Writes("test_class.C:PublicRegion" /* is CONSISTENT */)
  private void bad_write_publicRegion_privateMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Protected region, public method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
  public static void good_write_protectedRegion_publicMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Protected region, protected method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:ProtectedRegion" /* is CONSISTENT */)
  protected void bad_write_protectedRegion_protectedMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Protected region, default method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:ProtectedRegion" /* is CONSISTENT */)
  void bad_write_protectedRegion_defaultMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Protected region, private method: Good
   */
  // GOOD
  @SuppressWarnings({ "deprecation", "unused" })
  @Writes("test_class.C:ProtectedRegion" /* is CONSISTENT */)
  private void bad_write_protectedRegion_privateMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Default region, public method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  public static void good_write_defaultRegion_publicMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Default region, protected method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_write_defaultRegion_protectedMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Default region, default method: Good
   */
  // GOOD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:DefaultRegion" /* is CONSISTENT */)
  void bad_write_defaultRegion_defaultMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Default region, private method: Good
   */
  // GOOD
  @SuppressWarnings({ "deprecation", "unused" })
  @Writes("test_class.C:DefaultRegion" /* is CONSISTENT */)
  private void bad_write_defaultRegion_privateMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Private region, public method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  public static void good_write_privateRegion_publicMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Private region, protected method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_write_privateRegion_protectedMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Private region, default method: Good
   */
  // BAD
  @SuppressWarnings("deprecation")
  @Writes("test_class.C:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  void bad_write_privateRegion_defaultMethod(C c) {}

  /**
   * Static method.
   * write effect.
   * Private region, private method: Good
   */
  // GOOD
  @SuppressWarnings({ "deprecation", "unused" })
  @Writes("test_class.C:PrivateRegion" /* is CONSISTENT */)
  private void bad_write_privateRegion_privateMethod(C c) {}
}
