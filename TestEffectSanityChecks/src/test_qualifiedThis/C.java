package test_qualifiedThis;

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
  @SuppressWarnings("unused")
  private static class StaticInner {
    @RegionEffects("reads test_qualifiedThis.C.this:InstanceRegion" /* is UNASSOCIATED: Static inner class */)
    public StaticInner(boolean reads) {} 

    @RegionEffects("writes test_qualifiedThis.C.this:InstanceRegion" /* is UNASSOCIATED: Static inner class */)
    public StaticInner(Object writes) {}

    @RegionEffects("reads test_qualifiedThis.C.this:InstanceRegion" /* is UNASSOCIATED: Static inner class */)
    public void bad_reads_staticInnerClass() {} 

    @RegionEffects("writes test_qualifiedThis.C.this:InstanceRegion" /* is UNASSOCIATED: Static inner class */)
    public void bad_writes_staticInnerClass() {} 

    @RegionEffects("reads test_qualifiedThis.C.this:InstanceRegion" /* is UNBOUND: Static inner class */)
    public static void bad_static_reads_staticInnerClass() {} 

    @RegionEffects("writes test_qualifiedThis.C.this:InstanceRegion" /* is UNBOUND: Static inner class */)
    public static void bad_static_writes_staticInnerClass() {} 
  }
  
  @SuppressWarnings("unused")
  private class Inner {
    /* Non-static inner type: fully qualified.
     * Constructor.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("reads test_qualifiedThis.C.this:InstanceRegion" /* is CONSISTENT */)
    public Inner(boolean reads) {}

    /* Non-static inner type: unqualified
     * Constructor.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    // XXX: Bug 1053, binder doesn't handle this yet
    @RegionEffects("reads C.this:InstanceRegion" /* is CONSISTENT */)
    public Inner(boolean reads, int a) {}
  
    /* Non-static inner type: Not an outer class.
     * Constructor.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("reads test_qualifiedThis.D.this:InstanceRegion" /* is UNBOUND: Not an outer class */)
    public Inner(boolean reads, int a, int b) {}
    
    /* Non-static inner type: named class doesn't exist.
     * Constructor.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("reads test_qualifiedThis.NoSuchClass.this:InstanceRegion" /* is UNBOUND: No such class */)
    public Inner(boolean reads, int a, int b, int c) {}
    
    /* Non-static inner type: fully qualified.
     * Constructor.
     * Outer class exists and contains this class.
     * region exists.
     * region is static.
     */
    @RegionEffects("reads test_qualifiedThis.C.this:StaticRegion" /* is UNASSOCIATED: static region */)
    public Inner(boolean reads, int a, int b, int c, int d) {}
    
    /* Non-static inner type: fully qualified.
     * Constructor.
     * Outer class exists and contains this class.
     * region exists, but from different class
     */
    @RegionEffects("reads test_qualifiedThis.C.this:RegionFromD" /* is UNBOUND: Region is from different class */)
    public Inner(boolean reads, int a, int b, int c, int d, int e) {}
    
    /* Non-static inner type: fully qualified.
     * Constructor.
     * Outer class exists and contains this class.
     * region doesn't exist
     */
    @RegionEffects("reads test_qualifiedThis.C.this:NoSuchRegion" /* is UNBOUND: Region doesn't exist */)
    public Inner(boolean reads, int a, int b, int c, int d, int e, int f) {}

  
  
    /* Non-static inner type: fully qualified.
     * Constructor.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("writes test_qualifiedThis.C.this:InstanceRegion" /* is CONSISTENT */)
    public Inner(Object writes) {}

    /* Non-static inner type: unqualified
     * Constructor.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    // XXX: Bug 1053, binder doesn't handle this yet
    @RegionEffects("writes C.this:InstanceRegion" /* is CONSISTENT */)
    public Inner(Object writes, int a) {}
  
    /* Non-static inner type: Not an outer class.
     * Constructor.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("writes test_qualifiedThis.D.this:InstanceRegion" /* is UNBOUND: Not an outer class */)
    public Inner(Object writes, int a, int b) {}
    
    /* Non-static inner type: named class doesn't exist.
     * Constructor.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("writes test_qualifiedThis.NoSuchClass.this:InstanceRegion" /* is UNBOUND: No such class */)
    public Inner(Object writes, int a, int b, int c) {}
    
    /* Non-static inner type: fully qualified.
     * Constructor.
     * Outer class exists and contains this class.
     * region exists.
     * region is static.
     */
    @RegionEffects("writes test_qualifiedThis.C.this:StaticRegion" /* is UNASSOCIATED: static region */)
    public Inner(Object writes, int a, int b, int c, int d) {}
    
    /* Non-static inner type: fully qualified.
     * Constructor.
     * Outer class exists and contains this class.
     * region exists, but from different class
     */
    @RegionEffects("writes test_qualifiedThis.C.this:RegionFromD" /* is UNBOUND: Region is from different class */)
    public Inner(Object writes, int a, int b, int c, int d, int e) {}
    
    /* Non-static inner type: fully qualified.
     * Constructor.
     * Outer class exists and contains this class.
     * region doesn't exist
     */
    @RegionEffects("writes test_qualifiedThis.C.this:NoSuchRegion" /* is UNBOUND: Region doesn't exist */)
    public Inner(Object writes, int a, int b, int c, int d, int e, int f) {}
    
    
    
    
    /* Non-static inner type: fully qualified.
     * Instance method.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("reads test_qualifiedThis.C.this:InstanceRegion" /* is CONSISTENT */)
    public void good_reads_qualified() {}

    /* Non-static inner type: unqualified
     * Instance method.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    // XXX: Bug 1053, binder doesn't handle this yet
    @RegionEffects("reads C.this:InstanceRegion" /* is CONSISTENT */)
    public void good_reads_unqualified() {}
  
    /* Non-static inner type: Not an outer class.
     * Instance method.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("reads test_qualifiedThis.D.this:InstanceRegion" /* is UNBOUND: Not an outer class */)
    public void bad_reads_notOuterClass() {}
    
    /* Non-static inner type: named class doesn't exist.
     * Instance method.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("reads test_qualifiedThis.NoSuchClass.this:InstanceRegion" /* is UNBOUND: No such class */)
    public void bad_reads_noSuchClass() {}
    
    /* Non-static inner type: fully qualified.
     * Instance method.
     * Outer class exists and contains this class.
     * region exists.
     * region is static.
     */
    @RegionEffects("reads test_qualifiedThis.C.this:StaticRegion" /* is UNASSOCIATED: static region */)
    public void bad_reads_staticRegion() {}
    
    /* Non-static inner type: fully qualified.
     * Instance method.
     * Outer class exists and contains this class.
     * region exists, but from different class
     */
    @RegionEffects("reads test_qualifiedThis.C.this:RegionFromD" /* is UNBOUND: Region is from different class */)
    public void bad_reads_badRegion() {}
    
    /* Non-static inner type: fully qualified.
     * Instance method.
     * Outer class exists and contains this class.
     * region doesn't exist
     */
    @RegionEffects("reads test_qualifiedThis.C.this:NoSuchRegion" /* is UNBOUND: Region doesn't exist */)
    public void bad_reads_noSuchRegion() {}

  
  
    /* Non-static inner type: fully qualified.
     * Instance method.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("writes test_qualifiedThis.C.this:InstanceRegion" /* is CONSISTENT */)
    public void good_writes_qualified() {}

    /* Non-static inner type: unqualified
     * Instance method.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    // XXX: Bug 1053, binder doesn't handle this yet
    @RegionEffects("writes C.this:InstanceRegion" /* is CONSISTENT */)
    public void good_writes_unqualified() {}
  
    /* Non-static inner type: Not an outer class.
     * Instance method.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("writes test_qualifiedThis.D.this:InstanceRegion" /* is UNBOUND: Not an outer class */)
    public void bad_writes_notOuterClass() {}
    
    /* Non-static inner type: named class doesn't exist.
     * Instance method.
     * Outer class exists and contains this class.
     * region exists.
     * region is non-static.
     */
    @RegionEffects("writes test_qualifiedThis.NoSuchClass.this:InstanceRegion" /* is UNBOUND: No such class */)
    public void bad_writes_noSuchClass() {}
    
    /* Non-static inner type: fully qualified.
     * Instance method.
     * Outer class exists and contains this class.
     * region exists.
     * region is static.
     */
    @RegionEffects("writes test_qualifiedThis.C.this:StaticRegion" /* is UNASSOCIATED: static region */)
    public void bad_writes_staticRegion() {}
    
    /* Non-static inner type: fully qualified.
     * Instance method.
     * Outer class exists and contains this class.
     * region exists, but from different class
     */
    @RegionEffects("writes test_qualifiedThis.C.this:RegionFromD" /* is UNBOUND: Region is from different class */)
    public void bad_writes_badRegion() {}
    
    /* Non-static inner type: fully qualified.
     * Instance method.
     * Outer class exists and contains this class.
     * region doesn't exist
     */
    @RegionEffects("writes test_qualifiedThis.C.this:NoSuchRegion" /* is UNBOUND: Region doesn't exist */)
    public void bad_writes_noSuchRegion() {}




    /**
     * Non-static method.
     * read effect.
     * Public region, public method: Good
     */
    // GOOD
    @RegionEffects("reads test_qualifiedThis.C.this:PublicRegion" /* is CONSISTENT */)
    public void good_read_publicRegion_publicMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Public region, protected method: Good
     */
    // GOOD
    @RegionEffects("reads test_qualifiedThis.C.this:PublicRegion" /* is CONSISTENT */)
    protected void bad_read_publicRegion_protectedMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Public region, default method: Good
     */
    // GOOD
    @RegionEffects("reads test_qualifiedThis.C.this:PublicRegion" /* is CONSISTENT */)
    void bad_read_publicRegion_defaultMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Public region, private method: Good
     */
    // GOOD
    @RegionEffects("reads test_qualifiedThis.C.this:PublicRegion" /* is CONSISTENT */)
    private void bad_read_publicRegion_privateMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Protected region, public method: Good
     */
    // BAD
    @RegionEffects("reads test_qualifiedThis.C.this:ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
    public void good_read_protectedRegion_publicMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Protected region, protected method: Good
     */
    // GOOD
    @RegionEffects("reads test_qualifiedThis.C.this:ProtectedRegion" /* is CONSISTENT */)
    protected void bad_read_protectedRegion_protectedMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Protected region, default method: Good
     */
    // GOOD
    @RegionEffects("reads test_qualifiedThis.C.this:ProtectedRegion" /* is CONSISTENT */)
    void bad_read_protectedRegion_defaultMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Protected region, private method: Good
     */
    // GOOD
    @RegionEffects("reads test_qualifiedThis.C.this:ProtectedRegion" /* is CONSISTENT */)
    private void bad_read_protectedRegion_privateMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Default region, public method: Good
     */
    // BAD
    @RegionEffects("reads test_qualifiedThis.C.this:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
    public void good_read_defaultRegion_publicMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Default region, protected method: Good
     */
    // BAD
    @RegionEffects("reads test_qualifiedThis.C.this:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
    protected void bad_read_defaultRegion_protectedMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Default region, default method: Good
     */
    // GOOD
    @RegionEffects("reads test_qualifiedThis.C.this:DefaultRegion" /* is CONSISTENT */)
    void bad_read_defaultRegion_defaultMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Default region, private method: Good
     */
    // GOOD
    @RegionEffects("reads test_qualifiedThis.C.this:DefaultRegion" /* is CONSISTENT */)
    private void bad_read_defaultRegion_privateMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Private region, public method: Good
     */
    // BAD
    @RegionEffects("reads test_qualifiedThis.C.this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
    public void good_read_privateRegion_publicMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Private region, protected method: Good
     */
    // BAD
    @RegionEffects("reads test_qualifiedThis.C.this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
    protected void bad_read_privateRegion_protectedMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Private region, default method: Good
     */
    // BAD
    @RegionEffects("reads test_qualifiedThis.C.this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
    void bad_read_privateRegion_defaultMethod() {}

    /**
     * Non-static method.
     * read effect.
     * Private region, private method: Good
     */
    // GOOD
    @RegionEffects("reads test_qualifiedThis.C.this:PrivateRegion" /* is CONSISTENT */)
    private void bad_read_privateRegion_privateMethod() {}



    /**
     * Non-static method.
     * write effect.
     * Public region, public method: Good
     */
    // GOOD
    @RegionEffects("writes test_qualifiedThis.C.this:PublicRegion" /* is CONSISTENT */)
    public void good_write_publicRegion_publicMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Public region, protected method: Good
     */
    // GOOD
    @RegionEffects("writes test_qualifiedThis.C.this:PublicRegion" /* is CONSISTENT */)
    protected void bad_write_publicRegion_protectedMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Public region, default method: Good
     */
    // GOOD
    @RegionEffects("writes test_qualifiedThis.C.this:PublicRegion" /* is CONSISTENT */)
    void bad_write_publicRegion_defaultMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Public region, private method: Good
     */
    // GOOD
    @RegionEffects("writes test_qualifiedThis.C.this:PublicRegion" /* is CONSISTENT */)
    private void bad_write_publicRegion_privateMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Protected region, public method: Good
     */
    // BAD
    @RegionEffects("writes test_qualifiedThis.C.this:ProtectedRegion" /* is UNASSOCIATED: region is less visible than method */)
    public void good_write_protectedRegion_publicMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Protected region, protected method: Good
     */
    // GOOD
    @RegionEffects("writes test_qualifiedThis.C.this:ProtectedRegion" /* is CONSISTENT */)
    protected void bad_write_protectedRegion_protectedMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Protected region, default method: Good
     */
    // GOOD
    @RegionEffects("writes test_qualifiedThis.C.this:ProtectedRegion" /* is CONSISTENT */)
    void bad_write_protectedRegion_defaultMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Protected region, private method: Good
     */
    // GOOD
    @RegionEffects("writes test_qualifiedThis.C.this:ProtectedRegion" /* is CONSISTENT */)
    private void bad_write_protectedRegion_privateMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Default region, public method: Good
     */
    // BAD
    @RegionEffects("writes test_qualifiedThis.C.this:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
    public void good_write_defaultRegion_publicMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Default region, protected method: Good
     */
    // BAD
    @RegionEffects("writes test_qualifiedThis.C.this:DefaultRegion" /* is UNASSOCIATED: region is less visible than method */)
    protected void bad_write_defaultRegion_protectedMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Default region, default method: Good
     */
    // GOOD
    @RegionEffects("writes test_qualifiedThis.C.this:DefaultRegion" /* is CONSISTENT */)
    void bad_write_defaultRegion_defaultMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Default region, private method: Good
     */
    // GOOD
    @RegionEffects("writes test_qualifiedThis.C.this:DefaultRegion" /* is CONSISTENT */)
    private void bad_write_defaultRegion_privateMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Private region, public method: Good
     */
    // BAD
    @RegionEffects("writes test_qualifiedThis.C.this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
    public void good_write_privateRegion_publicMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Private region, protected method: Good
     */
    // BAD
    @RegionEffects("writes test_qualifiedThis.C.this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
    protected void bad_write_privateRegion_protectedMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Private region, default method: Good
     */
    // BAD
    @RegionEffects("writes test_qualifiedThis.C.this:PrivateRegion" /* is UNASSOCIATED: region is less visible than method */)
    void bad_write_privateRegion_defaultMethod() {}

    /**
     * Non-static method.
     * write effect.
     * Private region, private method: Good
     */
    // GOOD
    @RegionEffects("writes test_qualifiedThis.C.this:PrivateRegion" /* is CONSISTENT */)
    private void bad_write_privateRegion_privateMethod() {}
  }
}
