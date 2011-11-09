package test.implicitStatic;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public static S"),
  @Region("public static T extends S"),
  @Region("public A extends T"),
  @Region("public static U extends T"),
  
  @Region("public static O"),
  @Region("public X extends O")
})
public class Root {
  // Same as @RegionEffects("writes All")
  public void unannotated() {}

  
  
  // === Write effects
  
  // implicit receiver
  @RegionEffects("writes T")
  public void writesImplicitThisSubRegion() {}
  
  @RegionEffects("writes T")
  public void writesImplicitThisNewSubRegion() {}
  
  @RegionEffects("writes T")
  public void writesImplicitThisUnrelatedRegion() {}
  
  @RegionEffects("writes T")
  public void writesImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  @RegionEffects("writes T")
  public void writesExplicitThisSubRegion() {}
  
  @RegionEffects("writes T")
  public void writesExplicitThisNewSubRegion() {}
  
  @RegionEffects("writes T")
  public void writesExplicitThisUnrelatedRegion() {}
  
  @RegionEffects("writes T")
  public void writesExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver

  @RegionEffects("writes T")
  public void writes0thQualifiedThisSubRegion() {}
  
  @RegionEffects("writes T")
  public void writes0thQualifiedThisNewSubRegion() {}
  
  @RegionEffects("writes T")
  public void writes0thQualifiedThisUnrelatedRegion() {}
  
  @RegionEffects("writes T")
  public void writes0thQualifiedThisNewUnrelatedRegion() {}
  
  // implicit static class
  
  @RegionEffects("writes T")
  public void writesImplicitStaticSuperRegion() {}

  @RegionEffects("writes T")
  public void writesImplicitStaticSameRegion() {}

  @RegionEffects("writes T")
  public void writesImplicitStaticSubRegion() {}

  @RegionEffects("writes T")
  public void writesImplicitStaticNewRegion() {}

  @RegionEffects("writes T")
  public void writesImplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("writes T")
  public void writesImplicitStaticNewUnrelatedRegion() {}
  
  // explicit static class
  
  @RegionEffects("writes T")
  public void writesExplicitStaticSuperRegion() {}

  @RegionEffects("writes T")
  public void writesExplicitStaticSameRegion() {}

  @RegionEffects("writes T")
  public void writesExplicitStaticSubRegion() {}

  @RegionEffects("writes T")
  public void writesExplicitStaticNewSubRegion() {}

  @RegionEffects("writes T")
  public void writesExplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("writes T")
  public void writesExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
   
  @RegionEffects("writes T")
  public void writesFormalArgumentSubRegion(final Root p) {}
  
  @RegionEffects("writes T")
  public void writesFormalArgumentUnrelatedRegion(final Root p) {}
  
  // Qualified receiver
  
  @RegionEffects("writes T")
  public void writes1stQualifiedThisSubRegion() {}
  
  @RegionEffects("writes T")
  public void writes1stQualifiedThisUnrelatedRegion() {}


  
  @RegionEffects("writes T")
  public void writes2ndQualifiedThisSubRegion() {}
  
  @RegionEffects("writes T")
  public void writes2ndQualifiedThisUnrelatedRegion() {}

  
  
  // === Read effects

  // implicit receiver
  @RegionEffects("reads T")
  public void readsImplicitThisSubRegion() {}
  
  @RegionEffects("reads T")
  public void readsImplicitThisNewSubRegion() {}
  
  @RegionEffects("reads T")
  public void readsImplicitThisUnrelatedRegion() {}
  
  @RegionEffects("reads T")
  public void readsImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  @RegionEffects("reads T")
  public void readsExplicitThisSubRegion() {}
  
  @RegionEffects("reads T")
  public void readsExplicitThisNewSubRegion() {}
  
  @RegionEffects("reads T")
  public void readsExplicitThisUnrelatedRegion() {}
  
  @RegionEffects("reads T")
  public void readsExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver

  @RegionEffects("reads T")
  public void reads0thQualifiedThisSubRegion() {}
  
  @RegionEffects("reads T")
  public void reads0thQualifiedThisNewSubRegion() {}
  
  @RegionEffects("reads T")
  public void reads0thQualifiedThisUnrelatedRegion() {}
  
  @RegionEffects("reads T")
  public void reads0thQualifiedThisNewUnrelatedRegion() {}
  
  // implicit static class
  
  @RegionEffects("reads T")
  public void readsImplicitStaticSuperRegion() {}

  @RegionEffects("reads T")
  public void readsImplicitStaticSameRegion() {}

  @RegionEffects("reads T")
  public void readsImplicitStaticSubRegion() {}

  @RegionEffects("reads T")
  public void readsImplicitStaticNewRegion() {}

  @RegionEffects("reads T")
  public void readsImplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("reads T")
  public void readsImplicitStaticNewUnrelatedRegion() {}
  
  // explicit static class
  
  @RegionEffects("reads T")
  public void readsExplicitStaticSuperRegion() {}

  @RegionEffects("reads T")
  public void readsExplicitStaticSameRegion() {}

  @RegionEffects("reads T")
  public void readsExplicitStaticSubRegion() {}

  @RegionEffects("reads T")
  public void readsExplicitStaticNewSubRegion() {}

  @RegionEffects("reads T")
  public void readsExplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("reads T")
  public void readsExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
   
  @RegionEffects("reads T")
  public void readsFormalArgumentSubRegion(final Root p) {}
  
  @RegionEffects("reads T")
  public void readsFormalArgumentUnrelatedRegion(final Root p) {}
  
  // Qualified receiver
  
  @RegionEffects("reads T")
  public void reads1stQualifiedThisSubRegion() {}
  
  @RegionEffects("reads T")
  public void reads1stQualifiedThisUnrelatedRegion() {}


  
  @RegionEffects("reads T")
  public void reads2ndQualifiedThisSubRegion() {}
  
  @RegionEffects("reads T")
  public void reads2ndQualifiedThisUnrelatedRegion() {}

  
  
  // === Nested class
  
  public class Writes1stQualifiedThis extends Root {
    // GOOD: more specific
    @Override
    @RegionEffects("writes Root.this:A")
    public void writes1stQualifiedThisSubRegion() {}
    
    // BAD
    @Override
    @RegionEffects("writes Root.this:X")
    public void writes1stQualifiedThisUnrelatedRegion() {}

    
    
    // BAD
    @Override
    @RegionEffects("writes Root.this:A")
    public void reads1stQualifiedThisSubRegion() {}
    
    // BAD
    @Override
    @RegionEffects("writes Root.this:X")
    public void reads1stQualifiedThisUnrelatedRegion() {}

  
  
    public class Writes2ndQualifiedThis extends Root {
      // GOOD: more specific
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:A")
      public void writes1stQualifiedThisSubRegion() {}
      
      // BAD
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:X")
      public void writes1stQualifiedThisUnrelatedRegion() {}

    
      // GOOD: more specific
      @Override
      @RegionEffects("writes Root.this:A")
      public void writes2ndQualifiedThisSubRegion() {}
      
      // BAD
      @Override
      @RegionEffects("writes Root.this:X")
      public void writes2ndQualifiedThisUnrelatedRegion() {}

    
    
      // BAD
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:A")
      public void reads1stQualifiedThisSubRegion() {}
      
      // BAD
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:X")
      public void reads1stQualifiedThisUnrelatedRegion() {}

    
      // BAD
      @Override
      @RegionEffects("writes Root.this:A")
      public void reads2ndQualifiedThisSubRegion() {}
      
      // BAD
      @Override
      @RegionEffects("writes Root.this:X")
      public void reads2ndQualifiedThisUnrelatedRegion() {}
    }
  }



  
  public class Reads1stQualifiedThis extends Root {
    // GOOD: More specific
    @Override
    @RegionEffects("reads Root.this:A")
    public void writes1stQualifiedThisSubRegion() {}
    
    // BAD
    @Override
    @RegionEffects("reads Root.this:X")
    public void writes1stQualifiedThisUnrelatedRegion() {}

    
    
    // GOOD: more specific
    @Override
    @RegionEffects("reads Root.this:A")
    public void reads1stQualifiedThisSubRegion() {}
    
    // BAD
    @Override
    @RegionEffects("reads Root.this:X")
    public void reads1stQualifiedThisUnrelatedRegion() {}

  
  
    public class Reads2ndQualifiedThis extends Root {
      // GOOD: more specific
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:A")
      public void writes1stQualifiedThisSubRegion() {}
      
      // BAD
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:X")
      public void writes1stQualifiedThisUnrelatedRegion() {}

    
      // GOOD: more specific
      @Override
      @RegionEffects("reads Root.this:A")
      public void writes2ndQualifiedThisSubRegion() {}
      
      // BAD
      @Override
      @RegionEffects("reads Root.this:X")
      public void writes2ndQualifiedThisUnrelatedRegion() {}

    
    
      // GOOD: more specific
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:A")
      public void reads1stQualifiedThisSubRegion() {}
      
      // BAD
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:X")
      public void reads1stQualifiedThisUnrelatedRegion() {}

    
      // GOOD: more specific
      @Override
      @RegionEffects("reads Root.this:A")
      public void reads2ndQualifiedThisSubRegion() {}
      
      // BAD
      @Override
      @RegionEffects("reads Root.this:X")
      public void reads2ndQualifiedThisUnrelatedRegion() {}
    }
  }
}