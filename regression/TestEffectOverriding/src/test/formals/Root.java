package test.formals;

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
  public void unannotated(Root p, Root q, Other o) {}

  
  
  // === Write effects
  
  // implicit receiver
  
  @RegionEffects("writes p:B")
  public void writesImplicitThisSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesImplicitThisSameRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesImplicitThisSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesImplicitThisNewSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesImplicitThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesImplicitThisNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // explicit receiver
  
  @RegionEffects("writes p:B")
  public void writesExplicitThisSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesExplicitThisSameRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesExplicitThisSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesExplicitThisNewSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesExplicitThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesExplicitThisNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // 0th-outer class receiver
  
  @RegionEffects("writes p:B")
  public void writes0thQualifiedThisSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes0thQualifiedThisSameRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes0thQualifiedThisSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes0thQualifiedThisNewSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes0thQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes0thQualifiedThisNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // implicit static class
  
  @RegionEffects("writes p:B")
  public void writesImplicitStaticSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesImplicitStaticUnrelatedRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesImplicitStaticNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // explicit static class
  
  @RegionEffects("writes p:B")
  public void writesExplicitStaticSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesExplicitStaticUnrelatedRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesExplicitStaticNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // formal argument
  
  @RegionEffects("writes p:B")
  public void writesSameFormalArgumentSuperRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesSameFormalArgumentSameRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesSameFormalArgumentSubRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesSameFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesRenamedFormalArgumentSuperRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesRenamedFormalArgumentSameRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesRenamedFormalArgumentSubRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writesRenamedFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes2ndFormalArgumentSuperRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes2ndFormalArgumentSameRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes2ndFormalArgumentSubRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes2ndFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}

  @RegionEffects("writes p:B")
  public void writes3rdFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}

  @RegionEffects("writes p:B")
  public void writes3rdFormalArgumentSameNamedRegion(final Root p, Root q, Other o) {}

  // Qualified receiver
  
  @RegionEffects("writes p:B")
  public void writes1stQualifiedThisSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes1stQualifiedThisSameRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes1stQualifiedThisSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes1stQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}


  
  @RegionEffects("writes p:B")
  public void writes2ndQualifiedThisSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes2ndQualifiedThisSameRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes2ndQualifiedThisSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("writes p:B")
  public void writes2ndQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}

  
  
  // === Read effects
  
  // implicit receiver
  
  @RegionEffects("reads p:B")
  public void readsImplicitThisSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsImplicitThisSameRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsImplicitThisSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsImplicitThisNewSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsImplicitThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsImplicitThisNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // explicit receiver
  
  @RegionEffects("reads p:B")
  public void readsExplicitThisSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsExplicitThisSameRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsExplicitThisSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsExplicitThisNewSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsExplicitThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsExplicitThisNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // 0th-outer class receiver
  
  @RegionEffects("reads p:B")
  public void reads0thQualifiedThisSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads0thQualifiedThisSameRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads0thQualifiedThisSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads0thQualifiedThisNewSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads0thQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads0thQualifiedThisNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // implicit static class
  
  @RegionEffects("reads p:B")
  public void readsImplicitStaticSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsImplicitStaticUnrelatedRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsImplicitStaticNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // explicit static class
  
  @RegionEffects("reads p:B")
  public void readsExplicitStaticSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsExplicitStaticUnrelatedRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsExplicitStaticNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // formal argument
  
  @RegionEffects("reads p:B")
  public void readsSameFormalArgumentSuperRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsSameFormalArgumentSameRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsSameFormalArgumentSubRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsSameFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsRenamedFormalArgumentSuperRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsRenamedFormalArgumentSameRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsRenamedFormalArgumentSubRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void readsRenamedFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads2ndFormalArgumentSuperRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads2ndFormalArgumentSameRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads2ndFormalArgumentSubRegion(final Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads2ndFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}

  @RegionEffects("reads p:B")
  public void reads3rdFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}

  @RegionEffects("reads p:B")
  public void reads3rdFormalArgumentSameNamedRegion(final Root p, Root q, Other o) {}

  // Qualified receiver
  
  @RegionEffects("reads p:B")
  public void reads1stQualifiedThisSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads1stQualifiedThisSameRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads1stQualifiedThisSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads1stQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}


  
  @RegionEffects("reads p:B")
  public void reads2ndQualifiedThisSuperRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads2ndQualifiedThisSameRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads2ndQualifiedThisSubRegion(Root p, Root q, Other o) {}
  
  @RegionEffects("reads p:B")
  public void reads2ndQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}


  
  // === Nested class
  
  public class Writes1stQualifiedThis extends Root {
    // BAD: wrong argument
    @Override
    @RegionEffects("writes Root.this:A")
    public void writes1stQualifiedThisSuperRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("writes Root.this:B")
    public void writes1stQualifiedThisSameRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("writes Root.this:C")
    public void writes1stQualifiedThisSubRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("writes Root.this:X")
    public void writes1stQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}

    
    
    // BAD: wrong argument
    @Override
    @RegionEffects("writes Root.this:A")
    public void reads1stQualifiedThisSuperRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("writes Root.this:B")
    public void reads1stQualifiedThisSameRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("writes Root.this:C")
    public void reads1stQualifiedThisSubRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("writes Root.this:X")
    public void reads1stQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}

  
  
    public class Writes2ndQualifiedThis extends Root {
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:A")
      public void writes1stQualifiedThisSuperRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:B")
      public void writes1stQualifiedThisSameRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:C")
      public void writes1stQualifiedThisSubRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:X")
      public void writes1stQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}

    
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Root.this:A")
      public void writes2ndQualifiedThisSuperRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Root.this:B")
      public void writes2ndQualifiedThisSameRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Root.this:C")
      public void writes2ndQualifiedThisSubRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Root.this:X")
      public void writes2ndQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}

    
    
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:A")
      public void reads1stQualifiedThisSuperRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:B")
      public void reads1stQualifiedThisSameRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:C")
      public void reads1stQualifiedThisSubRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes1stQualifiedThis.this:X")
      public void reads1stQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}

    
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Root.this:A")
      public void reads2ndQualifiedThisSuperRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Root.this:B")
      public void reads2ndQualifiedThisSameRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Root.this:C")
      public void reads2ndQualifiedThisSubRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Root.this:X")
      public void reads2ndQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}
    }
  }



  
  public class Reads1stQualifiedThis extends Root {
    // BAD: wrong argument
    @Override
    @RegionEffects("reads Root.this:A")
    public void writes1stQualifiedThisSuperRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("reads Root.this:B")
    public void writes1stQualifiedThisSameRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("reads Root.this:C")
    public void writes1stQualifiedThisSubRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("reads Root.this:X")
    public void writes1stQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}

    
    
    // BAD: wrong argument
    @Override
    @RegionEffects("reads Root.this:A")
    public void reads1stQualifiedThisSuperRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("reads Root.this:B")
    public void reads1stQualifiedThisSameRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("reads Root.this:C")
    public void reads1stQualifiedThisSubRegion(Root p, Root q, Other o) {}
    
    // BAD: wrong argument
    @Override
    @RegionEffects("reads Root.this:X")
    public void reads1stQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}

  
  
    public class Reads2ndQualifiedThis extends Root {
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:A")
      public void writes1stQualifiedThisSuperRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:B")
      public void writes1stQualifiedThisSameRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:C")
      public void writes1stQualifiedThisSubRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:X")
      public void writes1stQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}

    
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Root.this:A")
      public void writes2ndQualifiedThisSuperRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Root.this:B")
      public void writes2ndQualifiedThisSameRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
         @RegionEffects("reads Root.this:C")
      public void writes2ndQualifiedThisSubRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Root.this:X")
      public void writes2ndQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}

    
    
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:A")
      public void reads1stQualifiedThisSuperRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:B")
      public void reads1stQualifiedThisSameRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:C")
      public void reads1stQualifiedThisSubRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads1stQualifiedThis.this:X")
      public void reads1stQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}

    
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Root.this:A")
      public void reads2ndQualifiedThisSuperRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Root.this:B")
      public void reads2ndQualifiedThisSameRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Root.this:C")
      public void reads2ndQualifiedThisSubRegion(Root p, Root q, Other o) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Root.this:X")
      public void reads2ndQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}
    }
  }
}