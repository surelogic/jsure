package test_implicitThis;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
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
  @RegionEffects("reads InstanceRegion" /* is UNASSOCIATED: Cannot use this on constructor */)
  public C(boolean reads) {}
  
  /**
   * Constructor.
   * region exists.
   * static region.
   * read effect.
   */
  // Used to be bad (Constructor, static region), but as of 2009-08-28 we allow static regions to be named unqualified. 
  @RegionEffects("reads StaticRegion" /* is CONSISTENT */)
  public C(boolean reads, int a) {}
  
  /**
   * Constructor.
   * region exists.
   * Region not from class C!
   * read effect.
   */
  // Used to be bad (Constructor, static region), but as of 2009-08-28 we allow static regions to be named unqualified.  Now it is bad because the region cannot be found because it's not from an ancestor of C.
  @RegionEffects("reads RegionFromD" /* is UNBOUND: region not from C */)
  public C(boolean reads, int a, int b) {}

  /**
   * Constructor.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- Constructor
  @RegionEffects("writes InstanceRegion" /* is UNASSOCIATED: Cannot use this on constructor */)
  public C(Object writes) {}
  
  /**
   * Constructor.
   * region exists.
   * static region.
   * write effect.
   */
  // Used to be bad, but as of 2009-08-28 we allow static regions to be named unqualified. 
  @RegionEffects("writes StaticRegion" /* is CONSISTENT */)
  public C(Object writes, int a) {}
  
  /**
   * Constructor.
   * region exists.
   * Region not from class C!
   * write effect.
   */
  // BAD -- Constructor, region not from C
  @RegionEffects("writes RegionFromD" /* is UNBOUND: Cannot use this on constructor; region not from C */)
  public C(Object writes, int a, int b) {}


  // methods
  
  /**
   * Non-static method.
   * region exists.
   * non-static region.
   * read effect.
   */
  // GOOD
  @RegionEffects("reads InstanceRegion" /* is CONSISTENT */)
  public void good_read_instanceRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * static region.
   * read effect.
   */
  // Used to be bad, but as of 2009-08-28 we allow static regions to be named unqualified. 
  @RegionEffects("reads StaticRegion" /* is CONSISTENT */)
  public void bad_read_staticRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * Region not from class C!
   * read effect.
   */
  // BAD -- region not from C
  @RegionEffects("reads RegionFromD" /* is UNBOUND: no such region */)
  public void bad_read_badRegion() {}

  /**
   * Non-static method.
   * region exists.
   * non-static region.
   * write effect.
   */
  // GOOD
  @RegionEffects("writes InstanceRegion" /* is CONSISTENT */)
  public void good_write_instanceRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * static region.
   * write effect.
   */
  // Used to be bad, but as of 2009-08-28 we allow static regions to be named unqualified. 
  @RegionEffects("writes StaticRegion" /* is CONSISTENT */)
  public void bad_write_staticRegion() {}
  
  /**
   * Non-static method.
   * region exists.
   * Region not from class C!
   * write effect.
   */
  // BAD -- region not from C
  @RegionEffects("writes RegionFromD" /* is UNBOUND: no such region */)
  public void bad_write_badRegion() {}  

  // Static methods
  
  /**
   * static method.
   * region exists.
   * static region.
   * read effect.
   */
  // BAD -- static method
  @RegionEffects("reads InstanceRegion" /* is UNASSOCIATED: Cannot use this on static method */)
  public static void bad_static_read_instanceRegion() {}
  
  /**
   * static method.
   * region exists.
   * static region.
   * read effect.
   */
  // Used to be bad, but as of 2009-08-28 we allow static regions to be named unqualified. 
  @RegionEffects("reads StaticRegion" /* is CONSISTENT */)
  public static void bad_static_read_staticRegion() {}
  
  /**
   * static method.
   * region exists.
   * Region not from class C!
   * read effect.
   */
  // BAD -- static method, region not from C
  @RegionEffects("reads RegionFromD" /* is UNBOUND: Cannot use this on static method; region not from C */)
  public static void bad_static_read_badRegion() {}

  /**
   * static method.
   * region exists.
   * static region.
   * write effect.
   */
  // BAD -- static method
  @RegionEffects("writes InstanceRegion" /* is UNASSOCIATED: Cannot use this on static method */)
  public static void bad_static_write_instanceRegion() {}
  
  /**
   * static method.
   * region exists.
   * static region.
   * write effect.
   */
  // Used to be bad, but as of 2009-08-28 we allow static regions to be named unqualified. 
  @RegionEffects("writes StaticRegion" /* is CONSISTENT */)
  public static void bad_static_write_staticRegion() {}
  
  /**
   * static method.
   * region exists.
   * Region not from class C!
   * write effect.
   */
  // BAD -- static method, region not from C
  @RegionEffects("writes RegionFromD" /* is UNBOUND: Cannot use this on static method; region not from C */)
  public static void bad_static_write_badRegion() {}  



  /**
   * Non-static method.
   * read effect.
   * Public region, public method: Good
   */
  // GOOD
  @RegionEffects("reads PublicRegion" /* is CONSISTENT */)
  public void good_read_publicRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Public region, protected method: Good
   */
  // GOOD
  @RegionEffects("reads PublicRegion" /* is CONSISTENT */)
  protected void bad_read_publicRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Public region, default method: Good
   */
  // GOOD
  @RegionEffects("reads PublicRegion" /* is CONSISTENT */)
  void bad_read_publicRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Public region, private method: Good
   */
  @SuppressWarnings("unused")
  // GOOD
  @RegionEffects("reads PublicRegion" /* is CONSISTENT */)
  private void bad_read_publicRegion_privateMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, public method: Good
   */
  // BAD
  @RegionEffects("reads ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_protectedRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, protected method: Good
   */
  // GOOD
  @RegionEffects("reads ProtectedRegion" /* is CONSISTENT */)
  protected void bad_read_protectedRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, default method: Good
   */
  // GOOD
  @RegionEffects("reads ProtectedRegion" /* is CONSISTENT */)
  void bad_read_protectedRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Protected region, private method: Good
   */
  @SuppressWarnings("unused")
  // GOOD
  @RegionEffects("reads ProtectedRegion" /* is CONSISTENT */)
  private void bad_read_protectedRegion_privateMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, public method: Good
   */
  // BAD
  @RegionEffects("reads DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_defaultRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, protected method: Good
   */
  // BAD
  @RegionEffects("reads DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_read_defaultRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, default method: Good
   */
  // GOOD
  @RegionEffects("reads DefaultRegion" /* is CONSISTENT */)
  void bad_read_defaultRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Default region, private method: Good
   */
  @SuppressWarnings("unused")
  // GOOD
  @RegionEffects("reads DefaultRegion" /* is CONSISTENT */)
  private void bad_read_defaultRegion_privateMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, public method: Good
   */
  // BAD
  @RegionEffects("reads PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_read_privateRegion_publicMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, protected method: Good
   */
  // BAD
  @RegionEffects("reads PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_read_privateRegion_protectedMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, default method: Good
   */
  // BAD
  @RegionEffects("reads PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  void bad_read_privateRegion_defaultMethod() {}

  /**
   * Non-static method.
   * read effect.
   * Private region, private method: Good
   */
  @SuppressWarnings("unused")
  // GOOD
  @RegionEffects("reads PrivateRegion" /* is CONSISTENT */)
  private void bad_read_privateRegion_privateMethod() {}



  /**
   * Non-static method.
   * write effect.
   * Public region, public method: Good
   */
  // GOOD
  @RegionEffects("writes PublicRegion" /* is CONSISTENT */)
  public void good_write_publicRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Public region, protected method: Good
   */
  // GOOD
  @RegionEffects("writes PublicRegion" /* is CONSISTENT */)
  protected void bad_write_publicRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Public region, default method: Good
   */
  // GOOD
  @RegionEffects("writes PublicRegion" /* is CONSISTENT */)
  void bad_write_publicRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Public region, private method: Good
   */
  @SuppressWarnings("unused")
  // GOOD
  @RegionEffects("writes PublicRegion" /* is CONSISTENT */)
  private void bad_write_publicRegion_privateMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, public method: Good
   */
  // BAD
  @RegionEffects("writes ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_protectedRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, protected method: Good
   */
  // GOOD
  @RegionEffects("writes ProtectedRegion" /* is CONSISTENT */)
  protected void bad_write_protectedRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, default method: Good
   */
  // GOOD
  @RegionEffects("writes ProtectedRegion" /* is CONSISTENT */)
  void bad_write_protectedRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Protected region, private method: Good
   */
  @SuppressWarnings("unused")
  // GOOD
  @RegionEffects("writes ProtectedRegion" /* is CONSISTENT */)
  private void bad_write_protectedRegion_privateMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, public method: Good
   */
  // BAD
  @RegionEffects("writes DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_defaultRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, protected method: Good
   */
  // BAD
  @RegionEffects("writes DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_write_defaultRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, default method: Good
   */
  // GOOD
  @RegionEffects("writes DefaultRegion" /* is CONSISTENT */)
  void bad_write_defaultRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Default region, private method: Good
   */
  @SuppressWarnings("unused")
  // GOOD
  @RegionEffects("writes DefaultRegion" /* is CONSISTENT */)
  private void bad_write_defaultRegion_privateMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, public method: Good
   */
  // BAD
  @RegionEffects("writes PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  public void good_write_privateRegion_publicMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, protected method: Good
   */
  // BAD
  @RegionEffects("writes PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  protected void bad_write_privateRegion_protectedMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, default method: Good
   */
  // BAD
  @RegionEffects("writes PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
  void bad_write_privateRegion_defaultMethod() {}

  /**
   * Non-static method.
   * write effect.
   * Private region, private method: Good
   */
  @SuppressWarnings("unused")
  // GOOD
  @RegionEffects("writes PrivateRegion" /* is CONSISTENT */)
  private void bad_write_privateRegion_privateMethod() {}
}
