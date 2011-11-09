package test.implicitStatic;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public B extends T"),
  @Region("public static V extends T"),
  
  @Region("public Y extends O"),
  
  @Region("public static P")
})
public class Reads extends Root {
  // GOOD
  @Override
  @RegionEffects("reads T")
  public void unannotated() {}

  
  
  // === Original Write effects
  
  // implicit receiver
  
  // GOOD: A is more specific than T
  @Override
  @RegionEffects("reads A")
  public void writesImplicitThisSubRegion() {}
  
  // GOOD: B is more specific than T
  @Override
  @RegionEffects("reads B")
  public void writesImplicitThisNewSubRegion() {}
  
  // BAD: X is unrelated to T
  @Override
  @RegionEffects("reads X")
  public void writesImplicitThisUnrelatedRegion() {}
  
  // BAD: X is unrelated to T
  @Override
  @RegionEffects("reads Y")
  public void writesImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  // GOOD: A is more specific than T
  @Override
  @RegionEffects("reads this:A")
  public void writesExplicitThisSubRegion() {}
  
  // GOOD: B is more specific than T
  @Override
  @RegionEffects("reads this:B")
  public void writesExplicitThisNewSubRegion() {}
  
  // BAD: X is unrelated to T
  @Override
  @RegionEffects("reads this:X")
  public void writesExplicitThisUnrelatedRegion() {}
  
  // BAD: Y is unrelated to T
  @Override
  @RegionEffects("reads this:Y")
  public void writesExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver
  
  // GOOD: A is more specific than T
  @Override
  @RegionEffects("reads Reads.this:A")
  public void writes0thQualifiedThisSubRegion() {}
  
  // GOOD: B is more specific than T
  @Override
  @RegionEffects("reads Reads.this:B")
  public void writes0thQualifiedThisNewSubRegion() {}
  
  // BAD: X is unrelated to B
  @Override
  @RegionEffects("reads Reads.this:X")
  public void writes0thQualifiedThisUnrelatedRegion() {}
  
  // BAD: Y is unrelated to B
  @Override
  @RegionEffects("reads Reads.this:Y")
  public void writes0thQualifiedThisNewUnrelatedRegion() {}

  // implicit static class
  
  // BAD: S is more general than T
  @Override
  @RegionEffects("reads S")
  public void writesImplicitStaticSuperRegion() {}

  // GOOD: T is the same as T
  @Override
  @RegionEffects("reads T")
  public void writesImplicitStaticSameRegion() {}

  // GOOD: U is more specific than T
  @Override
  @RegionEffects("reads U")
  public void writesImplicitStaticSubRegion() {}

  // GOOD: V is more specific than T
  @Override
  @RegionEffects("reads V")
  public void writesImplicitStaticNewRegion() {}

  // BAD
  @Override
  @RegionEffects("reads O")
  public void writesImplicitStaticUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads P")
  public void writesImplicitStaticNewUnrelatedRegion() {}

  // explicit static class
  
  // BAD: S is more general than T
  @Override
  @RegionEffects("reads test.implicitStatic.Root:S")
  public void writesExplicitStaticSuperRegion() {}

  // GOOD: T is the same as T
  @Override
  @RegionEffects("reads test.implicitStatic.Root:T")
  public void writesExplicitStaticSameRegion() {}

  // GOOD: U is more specific than T
  @Override
  @RegionEffects("reads test.implicitStatic.Root:U")
  public void writesExplicitStaticSubRegion() {}

  // GOOD: V is more specific than T
  @Override
  @RegionEffects("reads test.implicitStatic.Reads:V")
  public void writesExplicitStaticNewSubRegion() {}

  // BAD
  @Override
  @RegionEffects("reads test.implicitStatic.Root:O")
  public void writesExplicitStaticUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads test.implicitStatic.Reads:P")
  public void writesExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads p:A")
  public void writesFormalArgumentSubRegion(final Root p) {}
  
  // BAD
  @Override
  @RegionEffects("reads p:X")
  public void writesFormalArgumentUnrelatedRegion(final Root p) {}

  
  
  // === Original read effects
  
  // implicit receiver
  
  // GOOD: A is more specific
  @Override
  @RegionEffects("reads A")
  public void readsImplicitThisSubRegion() {}
  
  // GOOD: B is more specific
  @Override
  @RegionEffects("reads B")
  public void readsImplicitThisNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads X")
  public void readsImplicitThisUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads Y")
  public void readsImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  // GOOD: A is more specific
  @Override
  @RegionEffects("reads this:A")
  public void readsExplicitThisSubRegion() {}
  
  // GOOD: B is more specific
  @Override
  @RegionEffects("reads this:B")
  public void readsExplicitThisNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads this:X")
  public void readsExplicitThisUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads this:Y")
  public void readsExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver
  
  // GOOD: A is more specific
  @Override
  @RegionEffects("reads Reads.this:A")
  public void reads0thQualifiedThisSubRegion() {}
  
  // GOOD: B is more specific
  @Override
  @RegionEffects("reads Reads.this:B")
  public void reads0thQualifiedThisNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads Reads.this:X")
  public void reads0thQualifiedThisUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads Reads.this:Y")
  public void reads0thQualifiedThisNewUnrelatedRegion() {}

  // implicit static class
  
  // BAD
  @Override
  @RegionEffects("reads S")
  public void readsImplicitStaticSuperRegion() {}

  // GOOD: T is the same as T
  @Override
  @RegionEffects("reads T")
  public void readsImplicitStaticSameRegion() {}

  // GOOD: U is more specific than T
  @Override
  @RegionEffects("reads U")
  public void readsImplicitStaticSubRegion() {}

  // GOOD: V is more specific than T
  @Override
  @RegionEffects("reads V")
  public void readsImplicitStaticNewRegion() {}

  // BAD
  @Override
  @RegionEffects("reads O")
  public void readsImplicitStaticUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads P")
  public void readsImplicitStaticNewUnrelatedRegion() {}

  // explicit static class
  
  // BAD
  @Override
  @RegionEffects("reads test.implicitStatic.Root:S")
  public void readsExplicitStaticSuperRegion() {}

  // GOOD: T is the same as T
  @Override
  @RegionEffects("reads test.implicitStatic.Root:T")
  public void readsExplicitStaticSameRegion() {}

  // GOOD: U is more specific than T
  @Override
  @RegionEffects("reads test.implicitStatic.Root:U")
  public void readsExplicitStaticSubRegion() {}

  // GOOD: V is more specific than T
  @Override
  @RegionEffects("reads test.implicitStatic.Reads:V")
  public void readsExplicitStaticNewSubRegion() {}

  // BAD
  @Override
  @RegionEffects("reads test.implicitStatic.Root:O")
  public void readsExplicitStaticUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads test.implicitStatic.Reads:P")
  public void readsExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
  
  // GOOD: A is more specific
  @Override
  @RegionEffects("reads p:A")
  public void readsFormalArgumentSubRegion(final Root p) {}
  
  // BAD
  @Override
  @RegionEffects("reads p:X")
  public void readsFormalArgumentUnrelatedRegion(final Root p) {}
}
