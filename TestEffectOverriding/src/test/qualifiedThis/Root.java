package test.qualifiedThis;

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
  
  @RegionEffects("writes Root.this:B")
  public void writesImplicitThisSuperRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesImplicitThisSameRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesImplicitThisSubRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesImplicitThisNewSubRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesImplicitThisUnrelatedRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  @RegionEffects("writes Root.this:B")
  public void writesExplicitThisSuperRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesExplicitThisSameRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesExplicitThisSubRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesExplicitThisNewSubRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesExplicitThisUnrelatedRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver
  
  @RegionEffects("writes Root.this:B")
  public void writes0thQualifiedThisSuperRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writes0thQualifiedThisSameRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writes0thQualifiedThisSubRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writes0thQualifiedThisNewSubRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writes0thQualifiedThisUnrelatedRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writes0thQualifiedThisNewUnrelatedRegion() {}
  
  // implicit static class
  
  @RegionEffects("writes Root.this:B")
  public void writesImplicitStaticSuperRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesImplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesImplicitStaticNewUnrelatedRegion() {}
  
  // explicit static class
  
  @RegionEffects("writes Root.this:B")
  public void writesExplicitStaticSuperRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesExplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writesExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
  
  @RegionEffects("writes Root.this:B")
  public void writesFormalArgumentSuperRegion(final Root p) {}
  
  @RegionEffects("writes Root.this:B")
  public void writesFormalArgumentSameRegion(final Root p) {}
  
  @RegionEffects("writes Root.this:B")
  public void writesFormalArgumentSubRegion(final Root p) {}
  
  @RegionEffects("writes Root.this:B")
  public void writesFormalArgumentUnrelatedRegion(final Root p) {}
  
  // Qualified receiver
  
  @RegionEffects("writes Root.this:B")
  public void writes1stQualifiedThisSuperRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writes1stQualifiedThisSameRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writes1stQualifiedThisSubRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writes1stQualifiedThisUnrelatedRegion() {}


  
  @RegionEffects("writes Root.this:B")
  public void writes2ndQualifiedThisSuperRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writes2ndQualifiedThisSameRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writes2ndQualifiedThisSubRegion() {}
  
  @RegionEffects("writes Root.this:B")
  public void writes2ndQualifiedThisUnrelatedRegion() {}

  
  
  // === Read effects
  
  // implicit receiver
  
  @RegionEffects("reads Root.this:B")
  public void readsImplicitThisSuperRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsImplicitThisSameRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsImplicitThisSubRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsImplicitThisNewSubRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsImplicitThisUnrelatedRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  @RegionEffects("reads Root.this:B")
  public void readsExplicitThisSuperRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsExplicitThisSameRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsExplicitThisSubRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsExplicitThisNewSubRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsExplicitThisUnrelatedRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver
  
  @RegionEffects("reads Root.this:B")
  public void reads0thQualifiedThisSuperRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void reads0thQualifiedThisSameRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void reads0thQualifiedThisSubRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void reads0thQualifiedThisNewSubRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void reads0thQualifiedThisUnrelatedRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void reads0thQualifiedThisNewUnrelatedRegion() {}
  
  // implicit static class
  
  @RegionEffects("reads Root.this:B")
  public void readsImplicitStaticSuperRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsImplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsImplicitStaticNewUnrelatedRegion() {}
  
  // explicit static class
  
  @RegionEffects("reads Root.this:B")
  public void readsExplicitStaticSuperRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsExplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void readsExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
  
  @RegionEffects("reads Root.this:B")
  public void readsFormalArgumentSuperRegion(final Root p) {}
  
  @RegionEffects("reads Root.this:B")
  public void readsFormalArgumentSameRegion(final Root p) {}
  
  @RegionEffects("reads Root.this:B")
  public void readsFormalArgumentSubRegion(final Root p) {}
  
  @RegionEffects("reads Root.this:B")
  public void readsFormalArgumentUnrelatedRegion(final Root p) {}
  
  // Qualified receiver
  
  @RegionEffects("reads Root.this:B")
  public void reads1stQualifiedThisSuperRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void reads1stQualifiedThisSameRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void reads1stQualifiedThisSubRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void reads1stQualifiedThisUnrelatedRegion() {}


  
  @RegionEffects("reads Root.this:B")
  public void reads2ndQualifiedThisSuperRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void reads2ndQualifiedThisSameRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void reads2ndQualifiedThisSubRegion() {}
  
  @RegionEffects("reads Root.this:B")
  public void reads2ndQualifiedThisUnrelatedRegion() {}


  
  // === Nested class
  
  public class Writes1stQualifiedThis extends Root {
    // BAD: wrong receiver
    @Override
    @RegionEffects("writes Root.this:A")
    public void writes1stQualifiedThisSuperRegion() {}
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("writes Root.this:B")
    public void writes1stQualifiedThisSameRegion() {}
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("writes Root.this:C")
    public void writes1stQualifiedThisSubRegion() {}
    
    // BAD: wrong receiver
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
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:A")
      public void writes1stQualifiedThisSuperRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:B")
      public void writes1stQualifiedThisSameRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:C")
      public void writes1stQualifiedThisSubRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:X")
      public void writes1stQualifiedThisUnrelatedRegion() {}

    
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Root.this:A")
      public void writes2ndQualifiedThisSuperRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Root.this:B")
      public void writes2ndQualifiedThisSameRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("writes Root.this:C")
      public void writes2ndQualifiedThisSubRegion() {}
      
      // BAD: wrong receiver
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
    // BAD: wrong receiver
    @Override
    @RegionEffects("reads Root.this:A")
    public void writes1stQualifiedThisSuperRegion() {}
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("reads Root.this:B")
    public void writes1stQualifiedThisSameRegion() {}
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("reads Root.this:C")
    public void writes1stQualifiedThisSubRegion() {}
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("reads Root.this:X")
    public void writes1stQualifiedThisUnrelatedRegion() {}

    
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("reads Root.this:A")
    public void reads1stQualifiedThisSuperRegion() {}
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("reads Root.this:B")
    public void reads1stQualifiedThisSameRegion() {}
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("reads Root.this:C")
    public void reads1stQualifiedThisSubRegion() {}
    
    // BAD: wrong receiver
    @Override
    @RegionEffects("reads Root.this:X")
    public void reads1stQualifiedThisUnrelatedRegion() {}

  
  
    public class Reads2ndQualifiedThis extends Root {
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:A")
      public void writes1stQualifiedThisSuperRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:B")
      public void writes1stQualifiedThisSameRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:C")
      public void writes1stQualifiedThisSubRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:X")
      public void writes1stQualifiedThisUnrelatedRegion() {}

    
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Root.this:A")
      public void writes2ndQualifiedThisSuperRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Root.this:B")
      public void writes2ndQualifiedThisSameRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Root.this:C")
      public void writes2ndQualifiedThisSubRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Root.this:X")
      public void writes2ndQualifiedThisUnrelatedRegion() {}

    
    
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:A")
      public void reads1stQualifiedThisSuperRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:B")
      public void reads1stQualifiedThisSameRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:C")
      public void reads1stQualifiedThisSubRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:X")
      public void reads1stQualifiedThisUnrelatedRegion() {}

    
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Root.this:A")
      public void reads2ndQualifiedThisSuperRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Root.this:B")
      public void reads2ndQualifiedThisSameRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Root.this:C")
      public void reads2ndQualifiedThisSubRegion() {}
      
      // BAD: wrong receiver
      @Override
      @RegionEffects("reads Root.this:X")
      public void reads2ndQualifiedThisUnrelatedRegion() {}
    }
  }
}