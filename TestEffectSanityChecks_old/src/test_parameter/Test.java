package test_parameter;

import com.surelogic.Reads;
import com.surelogic.Writes;

public class Test {
  /* Constructor
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * Instance Region.
   */
  @Reads("c:InstanceRegion" /* is CONSISTENT */)
  public void Test(C c, boolean reads) {}

  /* Constructor
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * static Region.
   */
  @Reads("c:StaticRegion" /* is UNASSOCIATED: Region is static */)
  public void Test(C c, boolean reads, int a) {}

  /* Constructor
   * Parameter exists.
   * Non-primitive type.
   * Region is from wrong class.
   * Instance Region.
   */
  @Reads("c:RegionFromD" /* is UNBOUND: Region is not from C */)
  public void Test(C c, boolean reads, int a, int b) {}

  /* Constructor
   * Parameter exists.
   * Non-primitive type.
   * Region does not exist.
   */
  @Reads("c:NoSuchRegion" /* is UNBOUND: No such region */)
  public void Test(C c, boolean reads, int a, int b, int d) {}

  /* Constructor
   * Parameter exists.
   * Primitive type.
   */
  @Reads("c:InstanceRegion" /* is UNBOUND: C is primitive, so the region lookup fails */)
  public void Test(int c, boolean reads, int a, int b, int d, int e) {}

  /* Constructor
   * Parameter does not exist.
   */
  @Reads("bad:InstanceRegion" /* is UNBOUND: No such parameter */)
  public void Test(C c, boolean reads, int a, int b, int d, int e, int f) {}

  /* Constructor
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * Instance Region.
   */
  @Reads("c:InstanceRegion" /* is CONSISTENT */)
  public void Test(C c, Object writes) {}

  /* Constructor
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * static Region.
   */
  @Reads("c:StaticRegion" /* is UNASSOCIATED: Region is static */)
  public void Test(C c, Object writes, int a) {}

  /* Constructor
   * Parameter exists.
   * Non-primitive type.
   * Region is from wrong class.
   * Instance Region.
   */
  @Reads("c:RegionFromD" /* is UNBOUND: Region is not from C */)
  public void Test(C c, Object writes, int a, int b) {}

  /* Constructor
   * Parameter exists.
   * Non-primitive type.
   * Region does not exist.
   */
  @Reads("c:NoSuchRegion" /* is UNBOUND: No such region */)
  public void Test(C c, Object writes, int a, int b, int d) {}

  /* Constructor
   * Parameter exists.
   * Primitive type.
   */
  @Reads("c:InstanceRegion" /* is UNBOUND: C is primitive, so the region lookup fails */)
  public void Test(int c, Object writes, int a, int b, int d, int e) {}

  /* Constructor
   * Parameter does not exist.
   */
  @Reads("bad:InstanceRegion" /* is UNBOUND: No such parameter */)
  public void Test(C c, Object writes, int a, int b, int d, int e, int f) {}
  
  
  
  /* Instance method.
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * Instance Region.
   */
  @Reads("c:InstanceRegion" /* is CONSISTENT */)
  public void good_reads(C c) {}

  /* Instance method.
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * static Region.
   */
  @Reads("c:StaticRegion" /* is UNASSOCIATED: Region is static */)
  public void bad_reads_staticRegion(C c) {}

  /* Instance method.
   * Parameter exists.
   * Non-primitive type.
   * Region is from wrong class.
   * Instance Region.
   */
  @Reads("c:RegionFromD" /* is UNBOUND: Region is not from C */)
  public void bad_reads_badRegion(C c) {}

  /* Instance method.
   * Parameter exists.
   * Non-primitive type.
   * Region does not exist.
   */
  @Reads("c:NoSuchRegion" /* is UNBOUND: No such region */)
  public void bad_reads_noSuchRegion(C c) {}

  /* Instance method.
   * Parameter exists.
   * Primitive type.
   */
  @Reads("c:InstanceRegion" /* is UNBOUND: C is primitive, so the region lookup fails */)
  public void bad_reads_primitive(int c) {}

  /* Instance method.
   * Parameter does not exist.
   */
  @Reads("bad:InstanceRegion" /* is UNBOUND: No such parameter */)
  public void bad_reads_noSuchParam(C c) {}

  /* Instance method.
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * Instance Region.
   */
  @Reads("c:InstanceRegion" /* is CONSISTENT */)
  public void good_writes(C c) {}

  /* Instance method.
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * static Region.
   */
  @Reads("c:StaticRegion" /* is UNASSOCIATED: Region is static */)
  public void bad_writes_staticRegion(C c) {}

  /* Instance method.
   * Parameter exists.
   * Non-primitive type.
   * Region is from wrong class.
   * Instance Region.
   */
  @Reads("c:RegionFromD" /* is UNBOUND: Region is not from C */)
  public void bad_writes_badRegion(C c) {}

  /* Instance method.
   * Parameter exists.
   * Non-primitive type.
   * Region does not exist.
   */
  @Reads("c:NoSuchRegion" /* is UNBOUND: No such region */)
  public void bad_writes_noSuchRegion(C c) {}

  
  /* Instance method.
   * Parameter exists.
   * Primitive type.
   */
  @Reads("c:InstanceRegion" /* is UNBOUND: C is primitive, so the region lookup fails */)
  public void bad_writes_primitive(int c) {}

  /* Instance method.
   * Parameter does not exist.
   */
  @Reads("bad:InstanceRegion" /* is UNBOUND: No such parameter */)
  public void bad_writes_noSuchParam(C c) {}



  /* Static method.
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * Instance Region.
   */
  @Reads("c:InstanceRegion" /* is CONSISTENT */)
  public static void good_static_reads(C c) {}

  /* Static method.
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * static Region.
   */
  @Reads("c:StaticRegion" /* is UNASSOCIATED: Region is static */)
  public static void bad_static_reads_staticRegion(C c) {}

  /* Static method.
   * Parameter exists.
   * Non-primitive type.
   * Region is from wrong class.
   * Instance Region.
   */
  @Reads("c:RegionFromD" /* is UNBOUND: Region is not from C */)
  public static void bad_static_reads_badRegion(C c) {}

  /* Static method.
   * Parameter exists.
   * Non-primitive type.
   * Region does not exist.
   */
  @Reads("c:NoSuchRegion" /* is UNBOUND: No such region */)
  public static void bad_static_reads_noSuchRegion(C c) {}

  /* Static method.
   * Parameter exists.
   * Primitive type.
   */
  @Reads("c:InstanceRegion" /* is UNBOUND: C is primitive, so the region lookup fails */)
  public static void bad_static_reads_primitive(int c) {}

  /* Static method.
   * Parameter does not exist.
   */
  @Reads("bad:InstanceRegion" /* is UNBOUND: No such parameter */)
  public static void bad_static_reads_noSuchParam(C c) {}

  /* Static method.
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * Instance Region.
   */
  @Reads("c:InstanceRegion" /* is CONSISTENT */)
  public static void good_static_writes(C c) {}

  /* Static method.
   * Parameter exists.
   * Non-primitive type.
   * Region exists.
   * static Region.
   */
  @Reads("c:StaticRegion" /* is UNASSOCIATED: Region is static */)
  public static void bad_static_writes_staticRegion(C c) {}

  /* Static method.
   * Parameter exists.
   * Non-primitive type.
   * Region is from wrong class.
   * Instance Region.
   */
  @Reads("c:RegionFromD" /* is UNBOUND: Region is not from C */)
  public static void bad_static_writes_badRegion(C c) {}

  /* Static method.
   * Parameter exists.
   * Non-primitive type.
   * Region does not exist.
   */
  @Reads("c:NoSuchRegion" /* is UNBOUND: No such region */)
  public static void bad_static_writes_noSuchRegion(C c) {}

  /* Static method.
   * Parameter exists.
   * Primitive type.
   */
  @Reads("c:InstanceRegion" /* is UNBOUND: C is primitive, so the region lookup fails */)
  public static void bad_static_writes_primitive(int c) {}

  /* Static method.
   * Parameter does not exist.
   */
  @Reads("bad:InstanceRegion" /* is UNBOUND: No such parameter */)
  public static void bad_static_writes_noSuchParam(C c) {}



  /**
   * Non-static method.
   * read effect.
   * Public region, public method: Good
   */
  // GOOD
  @Reads("c:PublicRegion" /* is CONSISTENT */)
  public void good_read_publicRegion_publicMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Public region, protected method: Good
   */
  // GOOD
  @Reads("c:PublicRegion" /* is CONSISTENT */)
  protected void bad_read_publicRegion_protectedMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Public region, default method: Good
   */
  // GOOD
  @Reads("c:PublicRegion" /* is CONSISTENT */)
  void bad_read_publicRegion_defaultMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Public region, private method: Good
   */
  // GOOD
  @Reads("c:PublicRegion" /* is CONSISTENT */)
  private void bad_read_publicRegion_privateMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, public method: Good
   */
  // BAD
  @Reads("c:ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_protectedRegion_publicMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, protected method: Good
   */
  // GOOD
  @Reads("c:ProtectedRegion" /* is CONSISTENT */)
  protected void bad_read_protectedRegion_protectedMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, default method: Good
   */
  // GOOD
  @Reads("c:ProtectedRegion" /* is CONSISTENT */)
  void bad_read_protectedRegion_defaultMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, private method: Good
   */
  // GOOD
  @Reads("c:ProtectedRegion" /* is CONSISTENT */)
  private void bad_read_protectedRegion_privateMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Default region, public method: Good
   */
  // BAD
  @Reads("c:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_defaultRegion_publicMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Default region, protected method: Good
   */
  // BAD
  @Reads("c:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_read_defaultRegion_protectedMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Default region, default method: Good
   */
  // GOOD
  @Reads("c:DefaultRegion" /* is CONSISTENT */)
  void bad_read_defaultRegion_defaultMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Default region, private method: Good
   */
  // GOOD
  @Reads("c:DefaultRegion" /* is CONSISTENT */)
  private void bad_read_defaultRegion_privateMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Private region, public method: Good
   */
  // BAD
  @Reads("c:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_privateRegion_publicMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Private region, protected method: Good
   */
  // BAD
  @Reads("c:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_read_privateRegion_protectedMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Private region, default method: Good
   */
  // BAD
  @Reads("c:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  void bad_read_privateRegion_defaultMethod(C c) {}

  /**
   * Non-static method.
   * read effect.
   * Private region, private method: Good
   */
  // GOOD
  @Reads("c:PrivateRegion" /* is CONSISTENT */)
  private void bad_read_privateRegion_privateMethod(C c) {}



  /**
   * Non-static method.
   * write effect.
   * Public region, public method: Good
   */
  // GOOD
  @Writes("c:PublicRegion" /* is CONSISTENT */)
  public void good_write_publicRegion_publicMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Public region, protected method: Good
   */
  // GOOD
  @Writes("c:PublicRegion" /* is CONSISTENT */)
  protected void bad_write_publicRegion_protectedMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Public region, default method: Good
   */
  // GOOD
  @Writes("c:PublicRegion" /* is CONSISTENT */)
  void bad_write_publicRegion_defaultMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Public region, private method: Good
   */
  // GOOD
  @Writes("c:PublicRegion" /* is CONSISTENT */)
  private void bad_write_publicRegion_privateMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, public method: Good
   */
  // BAD
  @Writes("c:ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_protectedRegion_publicMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, protected method: Good
   */
  // GOOD
  @Writes("c:ProtectedRegion" /* is CONSISTENT */)
  protected void bad_write_protectedRegion_protectedMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, default method: Good
   */
  // GOOD
  @Writes("c:ProtectedRegion" /* is CONSISTENT */)
  void bad_write_protectedRegion_defaultMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, private method: Good
   */
  // GOOD
  @Writes("c:ProtectedRegion" /* is CONSISTENT */)
  private void bad_write_protectedRegion_privateMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Default region, public method: Good
   */
  // BAD
  @Writes("c:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_defaultRegion_publicMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Default region, protected method: Good
   */
  // BAD
  @Writes("c:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_write_defaultRegion_protectedMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Default region, default method: Good
   */
  // GOOD
  @Writes("c:DefaultRegion" /* is CONSISTENT */)
  void bad_write_defaultRegion_defaultMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Default region, private method: Good
   */
  // GOOD
  @Writes("c:DefaultRegion" /* is CONSISTENT */)
  private void bad_write_defaultRegion_privateMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Private region, public method: Good
   */
  // BAD
  @Writes("c:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_privateRegion_publicMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Private region, protected method: Good
   */
  // BAD
  @Writes("c:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_write_privateRegion_protectedMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Private region, default method: Good
   */
  // BAD
  @Writes("c:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  void bad_write_privateRegion_defaultMethod(C c) {}

  /**
   * Non-static method.
   * write effect.
   * Private region, private method: Good
   */
  // GOOD
  @Writes("c:PrivateRegion" /* is CONSISTENT */)
  private void bad_write_privateRegion_privateMethod(C c) {}
}
