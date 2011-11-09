package test_explicitThis;

import com.surelogic.RegionEffects;
import com.surelogic.Region;
import com.surelogic.Regions;

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
  @RegionEffects("reads this:InstanceRegion" /* is UNASSOCIATED: Cannot use this on constructor */)
  public C(boolean reads) {}
  
  /**
   * Constructor.
   * region exists.
   * static region.
   * read effect.
   */
  // BAD -- Constructor, static region
  @RegionEffects("reads this:StaticRegion" /* is UNASSOCIATED: Cannot use this, static region on constructor */)
  public C(boolean reads, int a) {}
  
  /**
   * Constructor.
   * region exists.
   * Region not from class C!
   * read effect.
   */
  // BAD -- Constructor, region not from C
  @RegionEffects("reads this:RegionFromD" /* is UNBOUND: Cannot use this on constructor; region not from C */)
  public C(boolean reads, int a, int b) {}

  /**
   * Constructor.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- Constructor
  @RegionEffects("writes this:InstanceRegion" /* is UNASSOCIATED: Cannot use this on constructor */)
  public C(Object writes) {}
  
  /**
   * Constructor.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- Constructor, static region
  @RegionEffects("writes this:StaticRegion" /* is UNASSOCIATED: Cannot use this, static region on constructor */)
  public C(Object writes, int a) {}
  
  /**
   * Constructor.
   * region exists.
   * Region not from class C!
   * write effect.
   */
  // BAD -- Constructor, region not from C
  @RegionEffects("writes this:RegionFromD" /* is UNBOUND: Cannot use this on constructor; region not from C */)
  public C(Object writes, int a, int b) {}


  // methods
  
  /**
   * Non-static method.
   * region exists.
   * non-static region.
   * read effect.
   */
  // GOOD
  @RegionEffects("reads this:InstanceRegion" /* is CONSISTENT */)
  public void good_read_instanceRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * static region.
   * read effect.
   */
  // BAD --- static region
  @RegionEffects("reads this:StaticRegion" /* is UNASSOCIATED: static region */)
  public void bad_read_staticRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * Region not from class C!
   * read effect.
   */
  // BAD -- region not from C
  @RegionEffects("reads this:RegionFromD" /* is UNBOUND: no such region */)
  public void bad_read_badRegion() {}

  /**
   * Non-static method.
   * region exists.
   * non-static region.
   * write effect.
   */
  // GOOD
  @RegionEffects("writes this:InstanceRegion" /* is CONSISTENT */)
  public void good_write_instanceRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- static region
  @RegionEffects("writes this:StaticRegion" /* is UNASSOCIATED: static region */)
  public void bad_write_staticRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * Region not from class C!
   * write effect.
   */
  // BAD -- region not from C
  @RegionEffects("writes this:RegionFromD" /* is UNBOUND: no such region */)
  public void bad_write_badRegion() {}  

  // Static methods
  
  /**
   * static method.
   * region exists.
   * static region.
   * read effect.
   */
  // BAD -- static method
  @RegionEffects("reads this:InstanceRegion" /* is UNASSOCIATED: Cannot use this on static method */)
  public static void bad_static_read_instanceRegion() {}
  
  /**
   * static method.
   * region exists.
   * static region.
   * read effect.
   */
  // BAD -- static method
  @RegionEffects("reads this:StaticRegion" /* is UNASSOCIATED: Cannot use this on static method */)
  public static void bad_static_read_staticRegion() {}
  
  /**
   * static method.
   * region exists.
   * Region not from class C!
   * read effect.
   */
  // BAD -- static method, region not from C
  @RegionEffects("reads this:RegionFromD" /* is UNBOUND: Cannot use this on static method; region not from C */)
  public static void bad_static_read_badRegion() {}

  /**
   * static method.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- static method
  @RegionEffects("writes this:InstanceRegion" /* is UNASSOCIATED: Cannot use this on static method */)
  public static void bad_static_write_instanceRegion() {}
  
  /**
   * static method.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- static method
  @RegionEffects("writes this:StaticRegion" /* is UNASSOCIATED: Cannot use this on static method */)
  public static void bad_static_write_staticRegion() {}
  
  /**
   * static method.
   * region exists.
   * Region not from class C!
   * write effect.
   */
  // BAD -- static method, region not from C
  @RegionEffects("writes this:RegionFromD" /* is UNBOUND: Cannot use this on static method; region not from C */)
  public static void bad_static_write_badRegion() {}  




  /**
   * Non-static method.
   * read effect.
   * Public region, public method: Good
   */
  // GOOD
  @RegionEffects("reads this:PublicRegion" /* is CONSISTENT */)
  public void good_read_publicRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Public region, protected method: Good
   */
  // GOOD
  @RegionEffects("reads this:PublicRegion" /* is CONSISTENT */)
  protected void bad_read_publicRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Public region, default method: Good
   */
  // GOOD
  @RegionEffects("reads this:PublicRegion" /* is CONSISTENT */)
  void bad_read_publicRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Public region, private method: Good
   */
  // GOOD
  @RegionEffects("reads this:PublicRegion" /* is CONSISTENT */)
  @SuppressWarnings("unused")
  private void bad_read_publicRegion_privateMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, public method: Good
   */
  // BAD
  @RegionEffects("reads this:ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_protectedRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, protected method: Good
   */
  // GOOD
  @RegionEffects("reads this:ProtectedRegion" /* is CONSISTENT */)
  protected void bad_read_protectedRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, default method: Good
   */
  // GOOD
  @RegionEffects("reads this:ProtectedRegion" /* is CONSISTENT */)
  void bad_read_protectedRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, private method: Good
   */
  // GOOD
  @RegionEffects("reads this:ProtectedRegion" /* is CONSISTENT */)
  @SuppressWarnings("unused")
  private void bad_read_protectedRegion_privateMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, public method: Good
   */
  // BAD
  @RegionEffects("reads this:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_defaultRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, protected method: Good
   */
  // BAD
  @RegionEffects("reads this:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_read_defaultRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, default method: Good
   */
  // GOOD
  @RegionEffects("reads this:DefaultRegion" /* is CONSISTENT */)
  void bad_read_defaultRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, private method: Good
   */
  // GOOD
  @RegionEffects("reads this:DefaultRegion" /* is CONSISTENT */)
  @SuppressWarnings("unused")
  private void bad_read_defaultRegion_privateMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, public method: Good
   */
  // BAD
  @RegionEffects("reads this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_privateRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, protected method: Good
   */
  // BAD
  @RegionEffects("reads this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_read_privateRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, default method: Good
   */
  // BAD
  @RegionEffects("reads this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  void bad_read_privateRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, private method: Good
   */
  // GOOD
  @RegionEffects("reads this:PrivateRegion" /* is CONSISTENT */)
  @SuppressWarnings("unused")
  private void bad_read_privateRegion_privateMethod() {}



  /**
   * Non-static method.
   * write effect.
   * Public region, public method: Good
   */
  // GOOD
  @RegionEffects("writes this:PublicRegion" /* is CONSISTENT */)
  public void good_write_publicRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Public region, protected method: Good
   */
  // GOOD
  @RegionEffects("writes this:PublicRegion" /* is CONSISTENT */)
  protected void bad_write_publicRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Public region, default method: Good
   */
  // GOOD
  @RegionEffects("writes this:PublicRegion" /* is CONSISTENT */)
  void bad_write_publicRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Public region, private method: Good
   */
  // GOOD
  @RegionEffects("writes this:PublicRegion" /* is CONSISTENT */)
  @SuppressWarnings("unused")
  private void bad_write_publicRegion_privateMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, public method: Good
   */
  // BAD
  @RegionEffects("writes this:ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_protectedRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, protected method: Good
   */
  // GOOD
  @RegionEffects("writes this:ProtectedRegion" /* is CONSISTENT */)
  protected void bad_write_protectedRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, default method: Good
   */
  // GOOD
  @RegionEffects("writes this:ProtectedRegion" /* is CONSISTENT */)
  void bad_write_protectedRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, private method: Good
   */
  // GOOD
  @RegionEffects("writes this:ProtectedRegion" /* is CONSISTENT */)
  @SuppressWarnings("unused")
  private void bad_write_protectedRegion_privateMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, public method: Good
   */
  // BAD
  @RegionEffects("writes this:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_defaultRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, protected method: Good
   */
  // BAD
  @RegionEffects("writes this:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_write_defaultRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, default method: Good
   */
  // GOOD
  @RegionEffects("writes this:DefaultRegion" /* is CONSISTENT */)
  void bad_write_defaultRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, private method: Good
   */
  // GOOD
  @RegionEffects("writes this:DefaultRegion" /* is CONSISTENT */)
  @SuppressWarnings("unused")
  private void bad_write_defaultRegion_privateMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, public method: Good
   */
  // BAD
  @RegionEffects("writes this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_privateRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, protected method: Good
   */
  // BAD
  @RegionEffects("writes this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_write_privateRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, default method: Good
   */
  // BAD
  @RegionEffects("writes this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  void bad_write_privateRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, private method: Good
   */
  // GOOD
  @RegionEffects("writes this:PrivateRegion" /* is CONSISTENT */)
  @SuppressWarnings("unused")
  private void bad_write_privateRegion_privateMethod() {}
}
