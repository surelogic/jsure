package test_implicitThis;

import com.surelogic.Reads;
import com.surelogic.Region;
import com.surelogic.Regions;
import com.surelogic.Writes;

@Regions({
  @Region("public static StaticRegion"),
  @Region("public InstanceRegion"),
  @Region("public PublicRegion"),
  @Region("protected ProtectedRegion"),
  @Region("DefaultRegion"),
  @Region("private PrivateRegion")
})
public class C {
  // Constructors
  
  /**
   * Constructor.
   * region exists.
   * static region.
   * read effect.
   */
  // BAD -- Constructor
  @Reads("InstanceRegion" /* is UNASSOCIATED: Cannot use this on constructor */)
  public C(boolean reads) {}
  
  /**
   * Constructor.
   * region exists.
   * static region.
   * read effect.
   */
  // BAD -- Constructor, static region
  @Reads("StaticRegion" /* is UNASSOCIATED: Cannot use this, static region on constructor */)
  public C(boolean reads, int a) {}
  
  /**
   * Constructor.
   * region exists.
   * Region not from class C!
   * read effect.
   */
  // BAD -- Constructor, region not from C
  @Reads("RegionFromD" /* is UNBOUND: Cannot use this on constructor; region not from C */)
  public C(boolean reads, int a, int b) {}

  /**
   * Constructor.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- Constructor
  @Writes("InstanceRegion" /* is UNASSOCIATED: Cannot use this on constructor */)
  public C(Object writes) {}
  
  /**
   * Constructor.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- Constructor, static region
  @Writes("StaticRegion" /* is UNASSOCIATED: Cannot use this, static region on constructor */)
  public C(Object writes, int a) {}
  
  /**
   * Constructor.
   * region exists.
   * Region not from class C!
   * write effect.
   */
  // BAD -- Constructor, region not from C
  @Writes("RegionFromD" /* is UNBOUND: Cannot use this on constructor; region not from C */)
  public C(Object writes, int a, int b) {}


  // methods
  
  /**
   * Non-static method.
   * region exists.
   * non-static region.
   * read effect.
   */
  // GOOD
  @Reads("InstanceRegion" /* is CONSISTENT */)
  public void good_read_instanceRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * static region.
   * read effect.
   */
  // BAD --- static region
  @Reads("StaticRegion" /* is UNASSOCIATED: static region */)
  public void bad_read_staticRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * Region not from class C!
   * read effect.
   */
  // BAD -- region not from C
  @Reads("RegionFromD" /* is UNBOUND: no such region */)
  public void bad_read_badRegion() {}

  /**
   * Non-static method.
   * region exists.
   * non-static region.
   * write effect.
   */
  // GOOD
  @Writes("InstanceRegion" /* is CONSISTENT */)
  public void good_write_instanceRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- static region
  @Writes("StaticRegion" /* is UNASSOCIATED: static region */)
  public void bad_write_staticRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * Region not from class C!
   * write effect.
   */
  // BAD -- region not from C
  @Writes("RegionFromD" /* is UNBOUND: no such region */)
  public void bad_write_badRegion() {}  

  // Static methods
  
  /**
   * static method.
   * region exists.
   * static region.
   * read effect.
   */
  // BAD -- static method
  @Reads("InstanceRegion" /* is UNASSOCIATED: Cannot use this on static method */)
  public static void bad_static_read_instanceRegion() {}
  
  /**
   * static method.
   * region exists.
   * static region.
   * read effect.
   */
  // BAD -- static method, static region
  @Reads("StaticRegion" /* is UNASSOCIATED: Cannot use this on static method */)
  public static void bad_static_read_staticRegion() {}
  
  /**
   * static method.
   * region exists.
   * Region not from class C!
   * read effect.
   */
  // BAD -- static method, region not from C
  @Reads("RegionFromD" /* is UNBOUND: Cannot use this on static method; region not from C */)
  public static void bad_static_read_badRegion() {}

  /**
   * static method.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- static method
  @Writes("InstanceRegion" /* is UNASSOCIATED: Cannot use this on static method */)
  public static void bad_static_write_instanceRegion() {}
  
  /**
   * static method.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- static method, static region
  @Writes("StaticRegion" /* is UNASSOCIATED: Cannot use this on static method */)
  public static void bad_static_write_staticRegion() {}
  
  /**
   * static method.
   * region exists.
   * Region not from class C!
   * write effect.
   */
  // BAD -- static method, region not from C
  @Writes("RegionFromD" /* is UNBOUND: Cannot use this on static method; region not from C */)
  public static void bad_static_write_badRegion() {}  



  /**
   * Non-static method.
   * read effect.
   * Public region, public method: Good
   */
  // GOOD
  @Reads("PublicRegion" /* is CONSISTENT */)
  public void good_read_publicRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Public region, protected method: Good
   */
  // GOOD
  @Reads("PublicRegion" /* is CONSISTENT */)
  protected void bad_read_publicRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Public region, default method: Good
   */
  // GOOD
  @Reads("PublicRegion" /* is CONSISTENT */)
  void bad_read_publicRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Public region, private method: Good
   */
  // GOOD
  @Reads("PublicRegion" /* is CONSISTENT */)
  private void bad_read_publicRegion_privateMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, public method: Good
   */
  // BAD
  @Reads("ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_protectedRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, protected method: Good
   */
  // GOOD
  @Reads("ProtectedRegion" /* is CONSISTENT */)
  protected void bad_read_protectedRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, default method: Good
   */
  // GOOD
  @Reads("ProtectedRegion" /* is CONSISTENT */)
  void bad_read_protectedRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, private method: Good
   */
  // GOOD
  @Reads("ProtectedRegion" /* is CONSISTENT */)
  private void bad_read_protectedRegion_privateMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, public method: Good
   */
  // BAD
  @Reads("DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_defaultRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, protected method: Good
   */
  // BAD
  @Reads("DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_read_defaultRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, default method: Good
   */
  // GOOD
  @Reads("DefaultRegion" /* is CONSISTENT */)
  void bad_read_defaultRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, private method: Good
   */
  // GOOD
  @Reads("DefaultRegion" /* is CONSISTENT */)
  private void bad_read_defaultRegion_privateMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, public method: Good
   */
  // BAD
  @Reads("PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_privateRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, protected method: Good
   */
  // BAD
  @Reads("PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_read_privateRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, default method: Good
   */
  // BAD
  @Reads("PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  void bad_read_privateRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, private method: Good
   */
  // GOOD
  @Reads("PrivateRegion" /* is CONSISTENT */)
  private void bad_read_privateRegion_privateMethod() {}



  /**
   * Non-static method.
   * write effect.
   * Public region, public method: Good
   */
  // GOOD
  @Writes("PublicRegion" /* is CONSISTENT */)
  public void good_write_publicRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Public region, protected method: Good
   */
  // GOOD
  @Writes("PublicRegion" /* is CONSISTENT */)
  protected void bad_write_publicRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Public region, default method: Good
   */
  // GOOD
  @Writes("PublicRegion" /* is CONSISTENT */)
  void bad_write_publicRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Public region, private method: Good
   */
  // GOOD
  @Writes("PublicRegion" /* is CONSISTENT */)
  private void bad_write_publicRegion_privateMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, public method: Good
   */
  // BAD
  @Writes("ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_protectedRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, protected method: Good
   */
  // GOOD
  @Writes("ProtectedRegion" /* is CONSISTENT */)
  protected void bad_write_protectedRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, default method: Good
   */
  // GOOD
  @Writes("ProtectedRegion" /* is CONSISTENT */)
  void bad_write_protectedRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, private method: Good
   */
  // GOOD
  @Writes("ProtectedRegion" /* is CONSISTENT */)
  private void bad_write_protectedRegion_privateMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, public method: Good
   */
  // BAD
  @Writes("DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_defaultRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, protected method: Good
   */
  // BAD
  @Writes("DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_write_defaultRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, default method: Good
   */
  // GOOD
  @Writes("DefaultRegion" /* is CONSISTENT */)
  void bad_write_defaultRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, private method: Good
   */
  // GOOD
  @Writes("DefaultRegion" /* is CONSISTENT */)
  private void bad_write_defaultRegion_privateMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, public method: Good
   */
  // BAD
  @Writes("PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_privateRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, protected method: Good
   */
  // BAD
  @Writes("PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_write_privateRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, default method: Good
   */
  // BAD
  @Writes("PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  void bad_write_privateRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, private method: Good
   */
  // GOOD
  @Writes("PrivateRegion" /* is CONSISTENT */)
  private void bad_write_privateRegion_privateMethod() {}
}
