package test.nestedClass;

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
public class Outer1 {
  @Regions({
    @Region("public DD extends B"),
    @Region("public YY extends T")
  })
  public class Outer2 extends Outer1 {
    @Regions({
      @Region("public D extends B"),
      @Region("public Y extends T")
    })
    public class Root extends Outer1 {
      // Same as @RegionEffects("writes All")
      public void unannotated() {}

      
      
      // === Write effects
      
      // implicit receiver
      
      @RegionEffects("writes Outer1.this:B")
      public void writesImplicitThisSuperRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesImplicitThisSameRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesImplicitThisSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesImplicitThisNewSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesImplicitThisUnrelatedRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesImplicitThisNewUnrelatedRegion() {}
      
      // explicit receiver
      
      @RegionEffects("writes Outer1.this:B")
      public void writesExplicitThisSuperRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesExplicitThisSameRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesExplicitThisSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesExplicitThisNewSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesExplicitThisUnrelatedRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesExplicitThisNewUnrelatedRegion() {}
      
      // 0th-outer class receiver
      
      @RegionEffects("writes Outer1.this:B")
      public void writes0thQualifiedThisSuperRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writes0thQualifiedThisSameRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writes0thQualifiedThisSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writes0thQualifiedThisNewSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writes0thQualifiedThisUnrelatedRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writes0thQualifiedThisNewUnrelatedRegion() {}
      
      // implicit static class
      
      @RegionEffects("writes Outer1.this:B")
      public void writesImplicitStaticSuperRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesImplicitStaticUnrelatedRegion() {}
      
      // explicit static class
      
      @RegionEffects("writes Outer1.this:B")
      public void writesExplicitStaticSuperRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesExplicitStaticUnrelatedRegion() {}
      
      // formal argument
      
      @RegionEffects("writes Outer1.this:B")
      public void writesFormalArgumentSuperRegion(final Root p) {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesFormalArgumentSameRegion(final Root p) {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesFormalArgumentSubRegion(final Root p) {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesFormalArgumentUnrelatedRegion(final Root p) {}
      
      // Qualified receiver
      
      @RegionEffects("writes Outer1.this:B")
      public void writes1stQualifiedThisSuperRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writes1stQualifiedThisSameRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writes1stQualifiedThisSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writes1stQualifiedThisUnrelatedRegion() {}


      
      @RegionEffects("writes Outer1.this:B")
      public void writes2ndQualifiedThisSuperRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writes2ndQualifiedThisSameRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writes2ndQualifiedThisSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writes2ndQualifiedThisUnrelatedRegion() {}

      
      
      // === Read effects
      
      // implicit receiver
      
      @RegionEffects("reads Outer1.this:B")
      public void readsImplicitThisSuperRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsImplicitThisSameRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsImplicitThisSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsImplicitThisNewSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsImplicitThisUnrelatedRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsImplicitThisNewUnrelatedRegion() {}
      
      // explicit receiver
      
      @RegionEffects("reads Outer1.this:B")
      public void readsExplicitThisSuperRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsExplicitThisSameRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsExplicitThisSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsExplicitThisNewSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsExplicitThisUnrelatedRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsExplicitThisNewUnrelatedRegion() {}
      
      // 0th-outer class receiver
      
      @RegionEffects("reads Outer1.this:B")
      public void reads0thQualifiedThisSuperRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void reads0thQualifiedThisSameRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void reads0thQualifiedThisSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void reads0thQualifiedThisNewSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void reads0thQualifiedThisUnrelatedRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void reads0thQualifiedThisNewUnrelatedRegion() {}
      
      // implicit static class
      
      @RegionEffects("reads Outer1.this:B")
      public void readsImplicitStaticSuperRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsImplicitStaticUnrelatedRegion() {}
      
      // explicit static class
      
      @RegionEffects("reads Outer1.this:B")
      public void readsExplicitStaticSuperRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsExplicitStaticUnrelatedRegion() {}
      
      // formal argument
      
      @RegionEffects("reads Outer1.this:B")
      public void readsFormalArgumentSuperRegion(final Root p) {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsFormalArgumentSameRegion(final Root p) {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsFormalArgumentSubRegion(final Root p) {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsFormalArgumentUnrelatedRegion(final Root p) {}
      
      // Qualified receiver
      
      @RegionEffects("reads Outer1.this:B")
      public void reads1stQualifiedThisSuperRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void reads1stQualifiedThisSameRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void reads1stQualifiedThisSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void reads1stQualifiedThisUnrelatedRegion() {}


      
      @RegionEffects("reads Outer1.this:B")
      public void reads2ndQualifiedThisSuperRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void reads2ndQualifiedThisSameRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void reads2ndQualifiedThisSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void reads2ndQualifiedThisUnrelatedRegion() {}
    }

  
  
    public class Writes extends Root {
      @Override
      @RegionEffects("writes Outer1.this:B")
      public void unannotated() {}

      
      
      // === Write effects
      
      // implicit receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes A")
      public void writesImplicitThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes B")
      public void writesImplicitThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes C")
      public void writesImplicitThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes D")
      public void writesImplicitThisNewSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes X")
      public void writesImplicitThisUnrelatedRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Y")
      public void writesImplicitThisNewUnrelatedRegion() {}
      
      // explicit receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes this:A")
      public void writesExplicitThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes this:B")
      public void writesExplicitThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes this:C")
      public void writesExplicitThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes this:D")
      public void writesExplicitThisNewSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes this:X")
      public void writesExplicitThisUnrelatedRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes this:Y")
      public void writesExplicitThisNewUnrelatedRegion() {}
      
      // 0th-outer class receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes.this:A")
      public void writes0thQualifiedThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes.this:B")
      public void writes0thQualifiedThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes.this:C")
      public void writes0thQualifiedThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes.this:D")
      public void writes0thQualifiedThisNewSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes.this:X")
      public void writes0thQualifiedThisUnrelatedRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Writes.this:Y")
      public void writes0thQualifiedThisNewUnrelatedRegion() {}
      
      // implicit static class

      // BAD: Too general
      @Override
      @RegionEffects("writes S")
      public void writesImplicitStaticSuperRegion() {}
      
      // BAD: Unrelated region
      @Override
      @RegionEffects("writes T")
      public void writesImplicitStaticUnrelatedRegion() {}
      
      // explicit static class
      
      // BAD: Too general
      @Override
      @RegionEffects("writes test.nestedClass.Outer1:S")
      public void writesExplicitStaticSuperRegion() {}
      
      // BAD: Unrelated region
      @Override
      @RegionEffects("writes test.nestedClass.Outer1:T")
      public void writesExplicitStaticUnrelatedRegion() {}
      
      // formal argument
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes p:A")
      public void writesFormalArgumentSuperRegion(final Root p) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes p:B")
      public void writesFormalArgumentSameRegion(final Root p) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes p:C")
      public void writesFormalArgumentSubRegion(final Root p) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes p:X")
      public void writesFormalArgumentUnrelatedRegion(final Root p) {}
      
      // Qualified receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Outer2.this:A")
      public void writes1stQualifiedThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Outer2.this:B")
      public void writes1stQualifiedThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Outer2.this:C")
      public void writes1stQualifiedThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("writes Outer2.this:X")
      public void writes1stQualifiedThisUnrelatedRegion() {}


      
      // BAD: Too general
      @Override
      @RegionEffects("writes Outer1.this:A")
      public void writes2ndQualifiedThisSuperRegion() {}
      
      // GOOD: Same argument, same region
      @Override
      @RegionEffects("writes Outer1.this:B")
      public void writes2ndQualifiedThisSameRegion() {}
      
      // GOOD: Same argument, more specific region
      @Override
      @RegionEffects("writes Outer1.this:C")
      public void writes2ndQualifiedThisSubRegion() {}
      
      // BAD: Unrelated region
      @Override
      @RegionEffects("writes Outer1.this:X")
      public void writes2ndQualifiedThisUnrelatedRegion() {}

      
      
      // === Read effects
      
      // implicit receiver
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes A")
      public void readsImplicitThisSuperRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes B")
      public void readsImplicitThisSameRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes C")
      public void readsImplicitThisSubRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes D")
      public void readsImplicitThisNewSubRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes X")
      public void readsImplicitThisUnrelatedRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Y")
      public void readsImplicitThisNewUnrelatedRegion() {}
      
      // explicit receiver
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes this:A")
      public void readsExplicitThisSuperRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes this:B")
      public void readsExplicitThisSameRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes this:C")
      public void readsExplicitThisSubRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes this:D")
      public void readsExplicitThisNewSubRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes this:X")
      public void readsExplicitThisUnrelatedRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes this:Y")
      public void readsExplicitThisNewUnrelatedRegion() {}
      
      // 0th-outer class receiver
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Writes.this:A")
      public void reads0thQualifiedThisSuperRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Writes.this:B")
      public void reads0thQualifiedThisSameRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Writes.this:C")
      public void reads0thQualifiedThisSubRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Writes.this:D")
      public void reads0thQualifiedThisNewSubRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Writes.this:X")
      public void reads0thQualifiedThisUnrelatedRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Writes.this:Y")
      public void reads0thQualifiedThisNewUnrelatedRegion() {}
      
      // implicit static class

      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes S")
      public void readsImplicitStaticSuperRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes T")
      public void readsImplicitStaticUnrelatedRegion() {}
      
      // explicit static class
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes test.nestedClass.Outer1:S")
      public void readsExplicitStaticSuperRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes test.nestedClass.Outer1:T")
      public void readsExplicitStaticUnrelatedRegion() {}
      
      // formal argument
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes p:A")
      public void readsFormalArgumentSuperRegion(final Root p) {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes p:B")
      public void readsFormalArgumentSameRegion(final Root p) {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes p:C")
      public void readsFormalArgumentSubRegion(final Root p) {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes p:X")
      public void readsFormalArgumentUnrelatedRegion(final Root p) {}
      
      // Qualified receiver
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Outer2.this:A")
      public void reads1stQualifiedThisSuperRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Outer2.this:B")
      public void reads1stQualifiedThisSameRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Outer2.this:C")
      public void reads1stQualifiedThisSubRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Outer2.this:X")
      public void reads1stQualifiedThisUnrelatedRegion() {}


      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Outer1.this:A")
      public void reads2ndQualifiedThisSuperRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Outer1.this:B")
      public void reads2ndQualifiedThisSameRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Outer1.this:C")
      public void reads2ndQualifiedThisSubRegion() {}
      
      // BAD: cannot go from a reads to a writes
      @Override
      @RegionEffects("writes Outer1.this:X")
      public void reads2ndQualifiedThisUnrelatedRegion() {}
    }



    public class Reads extends Root {
      @Override
      @RegionEffects("reads Outer1.this:B")
      public void unannotated() {}
    
      
      
      // === Write effects
      
      // implicit receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads A")
      public void writesImplicitThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads B")
      public void writesImplicitThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads C")
      public void writesImplicitThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads D")
      public void writesImplicitThisNewSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads X")
      public void writesImplicitThisUnrelatedRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Y")
      public void writesImplicitThisNewUnrelatedRegion() {}
      
      // explicit receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:A")
      public void writesExplicitThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:B")
      public void writesExplicitThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:C")
      public void writesExplicitThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:D")
      public void writesExplicitThisNewSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:X")
      public void writesExplicitThisUnrelatedRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:Y")
      public void writesExplicitThisNewUnrelatedRegion() {}
      
      // 0th-outer class receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:A")
      public void writes0thQualifiedThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:B")
      public void writes0thQualifiedThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:C")
      public void writes0thQualifiedThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:D")
      public void writes0thQualifiedThisNewSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:X")
      public void writes0thQualifiedThisUnrelatedRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:Y")
      public void writes0thQualifiedThisNewUnrelatedRegion() {}
      
      // implicit static class
    
      // BAD: Too general
      @Override
      @RegionEffects("reads S")
      public void writesImplicitStaticSuperRegion() {}
      
      // BAD: Unrelated region
      @Override
      @RegionEffects("reads T")
      public void writesImplicitStaticUnrelatedRegion() {}
      
      // explicit static class
      
      // BAD: Too general
      @Override
      @RegionEffects("reads test.nestedClass.Outer1:S")
      public void writesExplicitStaticSuperRegion() {}
      
      // BAD: Unrelated region
      @Override
      @RegionEffects("reads test.nestedClass.Outer1:T")
      public void writesExplicitStaticUnrelatedRegion() {}
      
      // formal argument
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads p:A")
      public void writesFormalArgumentSuperRegion(final Root p) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads p:B")
      public void writesFormalArgumentSameRegion(final Root p) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads p:C")
      public void writesFormalArgumentSubRegion(final Root p) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads p:X")
      public void writesFormalArgumentUnrelatedRegion(final Root p) {}
      
      // Qualified receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Outer2.this:A")
      public void writes1stQualifiedThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Outer2.this:B")
      public void writes1stQualifiedThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Outer2.this:C")
      public void writes1stQualifiedThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Outer2.this:X")
      public void writes1stQualifiedThisUnrelatedRegion() {}
    
    
      
      // BAD: Too general
      @Override
      @RegionEffects("reads Outer1.this:A")
      public void writes2ndQualifiedThisSuperRegion() {}
      
      // GOOD: Same argument, same region
      @Override
      @RegionEffects("reads Outer1.this:B")
      public void writes2ndQualifiedThisSameRegion() {}
      
      // GOOD: Same argument, more specific region
      @Override
      @RegionEffects("reads Outer1.this:C")
      public void writes2ndQualifiedThisSubRegion() {}
      
      // BAD: Unrelated region
      @Override
      @RegionEffects("reads Outer1.this:X")
      public void writes2ndQualifiedThisUnrelatedRegion() {}
    
      
      
      // === Read effects
      
      // implicit receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads A")
      public void readsImplicitThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads B")
      public void readsImplicitThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads C")
      public void readsImplicitThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads D")
      public void readsImplicitThisNewSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads X")
      public void readsImplicitThisUnrelatedRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Y")
      public void readsImplicitThisNewUnrelatedRegion() {}
      
      // explicit receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:A")
      public void readsExplicitThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:B")
      public void readsExplicitThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:C")
      public void readsExplicitThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:D")
      public void readsExplicitThisNewSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:X")
      public void readsExplicitThisUnrelatedRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads this:Y")
      public void readsExplicitThisNewUnrelatedRegion() {}
      
      // 0th-outer class receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:A")
      public void reads0thQualifiedThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:B")
      public void reads0thQualifiedThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:C")
      public void reads0thQualifiedThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:D")
      public void reads0thQualifiedThisNewSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:X")
      public void reads0thQualifiedThisUnrelatedRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Reads.this:Y")
      public void reads0thQualifiedThisNewUnrelatedRegion() {}
      
      // implicit static class
    
      // BAD: Too general
      @Override
      @RegionEffects("reads S")
      public void readsImplicitStaticSuperRegion() {}
      
      // BAD: Unrelated region
      @Override
      @RegionEffects("reads T")
      public void readsImplicitStaticUnrelatedRegion() {}
      
      // explicit static class
      
      // BAD: Too general
      @Override
      @RegionEffects("reads test.nestedClass.Outer1:S")
      public void readsExplicitStaticSuperRegion() {}
      
      // BAD: Unrelated region
      @Override
      @RegionEffects("reads test.nestedClass.Outer1:T")
      public void readsExplicitStaticUnrelatedRegion() {}
      
      // formal argument
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads p:A")
      public void readsFormalArgumentSuperRegion(final Root p) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads p:B")
      public void readsFormalArgumentSameRegion(final Root p) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads p:C")
      public void readsFormalArgumentSubRegion(final Root p) {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads p:X")
      public void readsFormalArgumentUnrelatedRegion(final Root p) {}
      
      // Qualified receiver
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Outer2.this:A")
      public void reads1stQualifiedThisSuperRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Outer2.this:B")
      public void reads1stQualifiedThisSameRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Outer2.this:C")
      public void reads1stQualifiedThisSubRegion() {}
      
      // BAD: wrong argument
      @Override
      @RegionEffects("reads Outer2.this:X")
      public void reads1stQualifiedThisUnrelatedRegion() {}
    
    
      
      // BAD: Too general
      @Override
      @RegionEffects("reads Outer1.this:A")
      public void reads2ndQualifiedThisSuperRegion() {}
      
      // GOOD: Same argument, same region
      @Override
      @RegionEffects("reads Outer1.this:B")
      public void reads2ndQualifiedThisSameRegion() {}
      
      // GOOD: Same argument, more specific region
      @Override
      @RegionEffects("reads Outer1.this:C")
      public void reads2ndQualifiedThisSubRegion() {}
      
      // BAD: Unrelated region
      @Override
      @RegionEffects("reads Outer1.this:X")
      public void reads2ndQualifiedThisUnrelatedRegion() {}
    }



    @Regions({
      @Region("public B"),
      @Region("public W")
    })
    public class UnrelatedRoot {
      @RegionEffects("writes Outer1.this:B")
      public void writes0thQualifiedThisSameRegion() {} // same name, but not the same region

      @RegionEffects("writes Outer1.this:B")
      public void writes0thQualifiedThisUnrelatedRegion() {}
    
      
            
      @RegionEffects("reads Outer1.this:B")
      public void reads0thQualifiedThisSameRegion() {} // same name, but not the same region
      
      @RegionEffects("reads Outer1.this:B")
      public void reads0thQualifiedThisUnrelatedRegion() {}
    }
    
    
    
    public class WritesUnrelatedRoot extends UnrelatedRoot {
      // BAD: wrong argument, wrong region
      @Override
      @RegionEffects("writes WritesUnrelatedRoot.this:B")
      public void writes0thQualifiedThisSameRegion() {} // same name, but not the same region

      // BAD: wrong argument, wrong region
      @Override
      @RegionEffects("writes WritesUnrelatedRoot.this:W")
      public void writes0thQualifiedThisUnrelatedRegion() {}
    
      
            
      // BAD: wrong argument, wrong region
      @Override
      @RegionEffects("writes WritesUnrelatedRoot.this:B")
      public void reads0thQualifiedThisSameRegion() {} // same name, but not the same region
      
      // BAD: wrong argument, wrong region
      @Override
      @RegionEffects("writes WritesUnrelatedRoot.this:W")
      public void reads0thQualifiedThisUnrelatedRegion() {}
    }
    
    
    
    public class ReadsUnrelatedRoot extends UnrelatedRoot {
      // BAD: wrong argument, wrong region
      @Override
      @RegionEffects("reads ReadsUnrelatedRoot.this:B")
      public void writes0thQualifiedThisSameRegion() {} // same name, but not the same region

      // BAD: wrong argument, wrong region
      @Override
      @RegionEffects("reads ReadsUnrelatedRoot.this:W")
      public void writes0thQualifiedThisUnrelatedRegion() {}
    
      
            
      // BAD: wrong argument, wrong region
      @Override
      @RegionEffects("reads ReadsUnrelatedRoot.this:B")
      public void reads0thQualifiedThisSameRegion() {} // same name, but not the same region
      
      // BAD: wrong argument, wrong region
      @Override
      @RegionEffects("reads ReadsUnrelatedRoot.this:W")
      public void reads0thQualifiedThisUnrelatedRegion() {}
    }
    
    
    
    @Regions({
      @Region("public A"),
      @Region("public B extends A"),
      @Region("public C extends B"),
      
      @Region("public X")
    })
    public class AnySuper {

    }

    @Regions({
      @Region("public D extends B"),
      
      @Region("public Y")
    })
    public class Any extends AnySuper {
      // === Write Effects
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySuperClassSuperRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySuperClassSameRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySuperClassSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySuperClassUnrelatedRegion() {}
      
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySameClassSuperRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySameClassSameRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySameClassSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySameClassNewSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySameClassUnrelatedRegion() {}
      
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySubClassSuperRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySubClassSameRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySubClassSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySubClassNewSubRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySubClassUnrelatedRegion() {}
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnySubClassNewUnrelatedRegion() {}
      
      
      @RegionEffects("writes Outer1.this:B")
      public void writesAnyUnrelatedClass() {}



      // === Read Effects
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySuperClassSuperRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySuperClassSameRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySuperClassSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySuperClassUnrelatedRegion() {}
      
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySameClassSuperRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySameClassSameRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySameClassSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySameClassNewSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySameClassUnrelatedRegion() {}
      
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySubClassSuperRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySubClassSameRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySubClassSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySubClassNewSubRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySubClassUnrelatedRegion() {}
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnySubClassNewUnrelatedRegion() {}
      
      
      @RegionEffects("reads Outer1.this:B")
      public void readsAnyUnrelatedClass() {}
    }
  
    @Regions({
      @Region("public E extends B"),
      
      @Region("public Z")
    })
    public class AnySub extends Any {

    }
  
    public class WritesAny extends Any {
      // === Write Effects
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySuper):A")
      public void writesAnySuperClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySuper):B")
      public void writesAnySuperClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySuper):C")
      public void writesAnySuperClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySuper):X")
      public void writesAnySuperClassUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Any):A")
      public void writesAnySameClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Any):B")
      public void writesAnySameClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Any):C")
      public void writesAnySameClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Any):D")
      public void writesAnySameClassNewSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Any):X")
      public void writesAnySameClassUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):A")
      public void writesAnySubClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):B")
      public void writesAnySubClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):C")
      public void writesAnySubClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):E")
      public void writesAnySubClassNewSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):X")
      public void writesAnySubClassUnrelatedRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):Z")
      public void writesAnySubClassNewUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Other):R")
      public void writesAnyUnrelatedClass() {}



      // === Read Effects
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySuper):A")
      public void readsAnySuperClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySuper):B")
      public void readsAnySuperClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySuper):C")
      public void readsAnySuperClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySuper):X")
      public void readsAnySuperClassUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Any):A")
      public void readsAnySameClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Any):B")
      public void readsAnySameClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Any):C")
      public void readsAnySameClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Any):D")
      public void readsAnySameClassNewSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Any):X")
      public void readsAnySameClassUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):A")
      public void readsAnySubClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):B")
      public void readsAnySubClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):C")
      public void readsAnySubClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):E")
      public void readsAnySubClassNewSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):X")
      public void readsAnySubClassUnrelatedRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(AnySub):Z")
      public void readsAnySubClassNewUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("writes any(Other):R")
      public void readsAnyUnrelatedClass() {}
    }

    public class ReadsAny extends Any {
      // === Write Effects
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySuper):A")
      public void writesAnySuperClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySuper):B")
      public void writesAnySuperClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySuper):C")
      public void writesAnySuperClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySuper):X")
      public void writesAnySuperClassUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Any):A")
      public void writesAnySameClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Any):B")
      public void writesAnySameClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Any):C")
      public void writesAnySameClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Any):D")
      public void writesAnySameClassNewSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Any):X")
      public void writesAnySameClassUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):A")
      public void writesAnySubClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):B")
      public void writesAnySubClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):C")
      public void writesAnySubClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):E")
      public void writesAnySubClassNewSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):X")
      public void writesAnySubClassUnrelatedRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):Z")
      public void writesAnySubClassNewUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Other):R")
      public void writesAnyUnrelatedClass() {}



      // === Read Effects
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySuper):A")
      public void readsAnySuperClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySuper):B")
      public void readsAnySuperClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySuper):C")
      public void readsAnySuperClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySuper):X")
      public void readsAnySuperClassUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Any):A")
      public void readsAnySameClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Any):B")
      public void readsAnySameClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Any):C")
      public void readsAnySameClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Any):D")
      public void readsAnySameClassNewSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Any):X")
      public void readsAnySameClassUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):A")
      public void readsAnySubClassSuperRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):B")
      public void readsAnySubClassSameRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):C")
      public void readsAnySubClassSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):E")
      public void readsAnySubClassNewSubRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):X")
      public void readsAnySubClassUnrelatedRegion() {}
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(AnySub):Z")
      public void readsAnySubClassNewUnrelatedRegion() {}
      
      
      // BAD: too general
      @Override
      @RegionEffects("reads any(Other):R")
      public void readsAnyUnrelatedClass() {}
    }

    @Region("public R")
    public class Other {

    }
  }
}
