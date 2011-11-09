package test.explicitStatic;

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
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesImplicitThisSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesImplicitThisNewSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesImplicitThisUnrelatedRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesExplicitThisSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesExplicitThisNewSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesExplicitThisUnrelatedRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver

  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writes0thQualifiedThisSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writes0thQualifiedThisNewSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writes0thQualifiedThisUnrelatedRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writes0thQualifiedThisNewUnrelatedRegion() {}
  
  // implicit static class
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesImplicitStaticSuperRegion() {}

  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesImplicitStaticSameRegion() {}

  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesImplicitStaticSubRegion() {}

  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesImplicitStaticNewRegion() {}

  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesImplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesImplicitStaticNewUnrelatedRegion() {}
  
  // explicit static class
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesExplicitStaticSuperRegion() {}

  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesExplicitStaticSameRegion() {}

  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesExplicitStaticSubRegion() {}

  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesExplicitStaticNewSubRegion() {}

  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesExplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
   
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesFormalArgumentSubRegion(final Root p) {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesFormalArgumentUnrelatedRegion(final Root p) {}
  
  // Qualified receiver
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writes1stQualifiedThisSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writes1stQualifiedThisUnrelatedRegion() {}


  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writes2ndQualifiedThisSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writes2ndQualifiedThisUnrelatedRegion() {}

  
  
  // === Read effects

  // implicit receiver
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsImplicitThisSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsImplicitThisNewSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsImplicitThisUnrelatedRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsExplicitThisSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsExplicitThisNewSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsExplicitThisUnrelatedRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver

  @RegionEffects("reads test.explicitStatic.Root:T")
  public void reads0thQualifiedThisSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void reads0thQualifiedThisNewSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void reads0thQualifiedThisUnrelatedRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void reads0thQualifiedThisNewUnrelatedRegion() {}
  
  // implicit static class
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsImplicitStaticSuperRegion() {}

  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsImplicitStaticSameRegion() {}

  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsImplicitStaticSubRegion() {}

  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsImplicitStaticNewRegion() {}

  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsImplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsImplicitStaticNewUnrelatedRegion() {}
  
  // explicit static class
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsExplicitStaticSuperRegion() {}

  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsExplicitStaticSameRegion() {}

  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsExplicitStaticSubRegion() {}

  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsExplicitStaticNewSubRegion() {}

  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsExplicitStaticUnrelatedRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
   
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsFormalArgumentSubRegion(final Root p) {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void readsFormalArgumentUnrelatedRegion(final Root p) {}
  
  // Qualified receiver
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void reads1stQualifiedThisSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void reads1stQualifiedThisUnrelatedRegion() {}


  
  @RegionEffects("reads test.explicitStatic.Root:T")
  public void reads2ndQualifiedThisSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.Root:T")
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