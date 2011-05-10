package test.anyInstance;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public static S"),
  @Region("public A extends S"),
  @Region("public B extends A"),
  @Region("public C extends B"),
  
  @Region("public static T"),
  @Region("public X extends T")
})
public class Root {
  // Same as @RegionEffects("writes All")
  public void unannotated() {}

  
  
  // === Write effects
  
  // implicit receiver
  
  @RegionEffects("writes any(Root):B")
  public void writesImplicitThisSuperRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesImplicitThisSameRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesImplicitThisSubRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesImplicitThisNewSubRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesImplicitThisUnrelatedRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  @RegionEffects("writes any(Root):B")
  public void writesExplicitThisSuperRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesExplicitThisSameRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesExplicitThisSubRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesExplicitThisNewSubRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesExplicitThisUnrelatedRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver
  
  @RegionEffects("writes any(Root):B")
  public void writes0thQualifiedThisSuperRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writes0thQualifiedThisSameRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writes0thQualifiedThisSubRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writes0thQualifiedThisNewSubRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writes0thQualifiedThisUnrelatedRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writes0thQualifiedThisNewUnrelatedRegion() {}
  
  // implicit static class
  
  @RegionEffects("writes any(Root):B")
  public void writesImplicitStaticSuperRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesImplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesImplicitStaticNewUnrelatedRegion() {}
  
  // explicit static class
  
  @RegionEffects("writes any(Root):B")
  public void writesExplicitStaticSuperRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesExplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writesExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
  
  @RegionEffects("writes any(Root):B")
  public void writesFormalArgumentSuperRegion(final Root p) {}
  
  @RegionEffects("writes any(Root):B")
  public void writesFormalArgumentSameRegion(final Root p) {}
  
  @RegionEffects("writes any(Root):B")
  public void writesFormalArgumentSubRegion(final Root p) {}
  
  @RegionEffects("writes any(Root):B")
  public void writesFormalArgumentUnrelatedRegion(final Root p) {}
  
  // Qualified receiver
  
  @RegionEffects("writes any(Root):B")
  public void writes1stQualifiedThisSuperRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writes1stQualifiedThisSameRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writes1stQualifiedThisSubRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writes1stQualifiedThisUnrelatedRegion() {}


  
  @RegionEffects("writes any(Root):B")
  public void writes2ndQualifiedThisSuperRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writes2ndQualifiedThisSameRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writes2ndQualifiedThisSubRegion() {}
  
  @RegionEffects("writes any(Root):B")
  public void writes2ndQualifiedThisUnrelatedRegion() {}

  
  
  // === Read effects
  
  // implicit receiver
  
  @RegionEffects("reads any(Root):B")
  public void readsImplicitThisSuperRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsImplicitThisSameRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsImplicitThisSubRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsImplicitThisNewSubRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsImplicitThisUnrelatedRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  @RegionEffects("reads any(Root):B")
  public void readsExplicitThisSuperRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsExplicitThisSameRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsExplicitThisSubRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsExplicitThisNewSubRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsExplicitThisUnrelatedRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver
  
  @RegionEffects("reads any(Root):B")
  public void reads0thQualifiedThisSuperRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void reads0thQualifiedThisSameRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void reads0thQualifiedThisSubRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void reads0thQualifiedThisNewSubRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void reads0thQualifiedThisUnrelatedRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void reads0thQualifiedThisNewUnrelatedRegion() {}
  
  // implicit static class
  
  @RegionEffects("reads any(Root):B")
  public void readsImplicitStaticSuperRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsImplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsImplicitStaticNewUnrelatedRegion() {}
  
  // explicit static class
  
  @RegionEffects("reads any(Root):B")
  public void readsExplicitStaticSuperRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsExplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void readsExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
  
  @RegionEffects("reads any(Root):B")
  public void readsFormalArgumentSuperRegion(final Root p) {}
  
  @RegionEffects("reads any(Root):B")
  public void readsFormalArgumentSameRegion(final Root p) {}
  
  @RegionEffects("reads any(Root):B")
  public void readsFormalArgumentSubRegion(final Root p) {}
  
  @RegionEffects("reads any(Root):B")
  public void readsFormalArgumentUnrelatedRegion(final Root p) {}
  
  // Qualified receiver
  
  @RegionEffects("reads any(Root):B")
  public void reads1stQualifiedThisSuperRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void reads1stQualifiedThisSameRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void reads1stQualifiedThisSubRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void reads1stQualifiedThisUnrelatedRegion() {}


  
  @RegionEffects("reads any(Root):B")
  public void reads2ndQualifiedThisSuperRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void reads2ndQualifiedThisSameRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void reads2ndQualifiedThisSubRegion() {}
  
  @RegionEffects("reads any(Root):B")
  public void reads2ndQualifiedThisUnrelatedRegion() {}


  
  // === Nested class
  
  public class Writes1stQualifiedThis extends Root {
    // BAD: wrong region
    @Override
    @RegionEffects("writes Root.this:A")
    public void writes1stQualifiedThisSuperRegion() {}
    
    // GOOD: More specific
    @Override
    @RegionEffects("writes Root.this:B")
    public void writes1stQualifiedThisSameRegion() {}
    
    // GOOD: More specific
    @Override
    @RegionEffects("writes Root.this:C")
    public void writes1stQualifiedThisSubRegion() {}
    
    // BAD: wrong region
    @Override
    @RegionEffects("writes Root.this:X")
    public void writes1stQualifiedThisUnrelatedRegion() {}

    
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("writes Root.this:A")
    public void reads1stQualifiedThisSuperRegion() {}
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("writes Root.this:B")
    public void reads1stQualifiedThisSameRegion() {}
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("writes Root.this:C")
    public void reads1stQualifiedThisSubRegion() {}
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("writes Root.this:X")
    public void reads1stQualifiedThisUnrelatedRegion() {}

  
  
    public class Writes2ndQualifiedThis extends Root {
      // BAD: wrong region
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:A")
      public void writes1stQualifiedThisSuperRegion() {}
      
      // GOOD: more specific
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:B")
      public void writes1stQualifiedThisSameRegion() {}
      
      // GOOD: more specific
      @Override
         @RegionEffects("writes Writes1stQualifiedThis.this:C")
      public void writes1stQualifiedThisSubRegion() {}
      
      // BAD: Wrong region
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:X")
      public void writes1stQualifiedThisUnrelatedRegion() {}

    
      // BAD: wrong region
      @Override
      @RegionEffects("writes Root.this:A")
      public void writes2ndQualifiedThisSuperRegion() {}
      
      // GOOD: More specific
      @Override
      @RegionEffects("writes Root.this:B")
      public void writes2ndQualifiedThisSameRegion() {}
      
      // GOOD: More specific
      @Override
      @RegionEffects("writes Root.this:C")
      public void writes2ndQualifiedThisSubRegion() {}
      
      // BAD: wrong region
      @Override
      @RegionEffects("writes Root.this:X")
      public void writes2ndQualifiedThisUnrelatedRegion() {}

    
    
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:A")
      public void reads1stQualifiedThisSuperRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:B")
      public void reads1stQualifiedThisSameRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:C")
      public void reads1stQualifiedThisSubRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:X")
      public void reads1stQualifiedThisUnrelatedRegion() {}

    
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Root.this:A")
      public void reads2ndQualifiedThisSuperRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Root.this:B")
      public void reads2ndQualifiedThisSameRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Root.this:C")
      public void reads2ndQualifiedThisSubRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Root.this:X")
      public void reads2ndQualifiedThisUnrelatedRegion() {}
    }
  }



  
  public class Reads1stQualifiedThis extends Root {
    // BAD: wrong region
    @Override
    @RegionEffects("reads Root.this:A")
    public void writes1stQualifiedThisSuperRegion() {}
    
    // GOOD: more specific
    @Override
    @RegionEffects("reads Root.this:B")
    public void writes1stQualifiedThisSameRegion() {}
    
    // GOOD: more specific
    @Override
    @RegionEffects("reads Root.this:C")
    public void writes1stQualifiedThisSubRegion() {}
    
    // BAD: wrong region
    @Override
    @RegionEffects("reads Root.this:X")
    public void writes1stQualifiedThisUnrelatedRegion() {}

    
    
    // BAD: wrong region
    @Override
    @RegionEffects("reads Root.this:A")
    public void reads1stQualifiedThisSuperRegion() {}
    
    // GOOD: more specific
    @Override
    @RegionEffects("reads Root.this:B")
    public void reads1stQualifiedThisSameRegion() {}
    
    // GOOD: more specific
    @Override
    @RegionEffects("reads Root.this:C")
    public void reads1stQualifiedThisSubRegion() {}
    
    // BAD: wrong region
    @Override
    @RegionEffects("reads Root.this:X")
    public void reads1stQualifiedThisUnrelatedRegion() {}

  
  
    public class Reads2ndQualifiedThis extends Root {
      // BAD: wrong region
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:A")
      public void writes1stQualifiedThisSuperRegion() {}
      
      // GOOD: more specific
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:B")
      public void writes1stQualifiedThisSameRegion() {}
      
      // GOOD: more specific
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:C")
      public void writes1stQualifiedThisSubRegion() {}
      
      // BAD: wrong region
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:X")
      public void writes1stQualifiedThisUnrelatedRegion() {}

    
      // BAD: wrong region
      @Override
      @RegionEffects("reads Root.this:A")
      public void writes2ndQualifiedThisSuperRegion() {}
      
      // GOOD: more specific
      @Override
      @RegionEffects("reads Root.this:B")
      public void writes2ndQualifiedThisSameRegion() {}
      
      // GOOD: more specific
      @Override
         @RegionEffects("reads Root.this:C")
      public void writes2ndQualifiedThisSubRegion() {}
      
      // BAD: wrong region
      @Override
      @RegionEffects("reads Root.this:X")
      public void writes2ndQualifiedThisUnrelatedRegion() {}

    
    
      // BAD: wrong region
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:A")
      public void reads1stQualifiedThisSuperRegion() {}
      
      // GOOD: more specific
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:B")
      public void reads1stQualifiedThisSameRegion() {}
      
      // GOOD: more specific
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:C")
      public void reads1stQualifiedThisSubRegion() {}
      
      // BAD: wrong region
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:X")
      public void reads1stQualifiedThisUnrelatedRegion() {}

    
      // BAD: wrong region
      @Override
      @RegionEffects("reads Root.this:A")
      public void reads2ndQualifiedThisSuperRegion() {}
      
      // GOOD: more specific
      @Override
      @RegionEffects("reads Root.this:B")
      public void reads2ndQualifiedThisSameRegion() {}
      
      // GOOD: more specific
      @Override
      @RegionEffects("reads Root.this:C")
      public void reads2ndQualifiedThisSubRegion() {}
      
      // BAD: wrong region
      @Override
      @RegionEffects("reads Root.this:X")
      public void reads2ndQualifiedThisUnrelatedRegion() {}
    }
  }
}