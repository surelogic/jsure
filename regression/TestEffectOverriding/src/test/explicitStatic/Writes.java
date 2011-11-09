package test.explicitStatic;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public B extends T"),
  @Region("public static V extends T"),
  
  @Region("public Y extends O"),
  
  @Region("public static P")
})
public class Writes extends Root {
  // GOOD
  @Override
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void unannotated() {}

  
  
  // === Original Write effects
  
  // implicit receiver
  
  // GOOD: A is more specific than T
  @Override
  @RegionEffects("writes A")
  public void writesImplicitThisSubRegion() {}
  
  // GOOD: B is more specific than T
  @Override
  @RegionEffects("writes B")
  public void writesImplicitThisNewSubRegion() {}
  
  // BAD: X is unrelated to T
  @Override
  @RegionEffects("writes X")
  public void writesImplicitThisUnrelatedRegion() {}
  
  // BAD: X is unrelated to T
  @Override
  @RegionEffects("writes Y")
  public void writesImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  // GOOD: A is more specific than T
  @Override
  @RegionEffects("writes this:A")
  public void writesExplicitThisSubRegion() {}
  
  // GOOD: B is more specific than T
  @Override
  @RegionEffects("writes this:B")
  public void writesExplicitThisNewSubRegion() {}
  
  // BAD: X is unrelated to T
  @Override
  @RegionEffects("writes this:X")
  public void writesExplicitThisUnrelatedRegion() {}
  
  // BAD: Y is unrelated to T
  @Override
  @RegionEffects("writes this:Y")
  public void writesExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver
  
  // GOOD: A is more specific than T
  @Override
  @RegionEffects("writes Writes.this:A")
  public void writes0thQualifiedThisSubRegion() {}
  
  // GOOD: B is more specific than T
  @Override
  @RegionEffects("writes Writes.this:B")
  public void writes0thQualifiedThisNewSubRegion() {}
  
  // BAD: X is unrelated to B
  @Override
  @RegionEffects("writes Writes.this:X")
  public void writes0thQualifiedThisUnrelatedRegion() {}
  
  // BAD: Y is unrelated to B
  @Override
  @RegionEffects("writes Writes.this:Y")
  public void writes0thQualifiedThisNewUnrelatedRegion() {}

  // implicit static class
  
  // BAD: S is more general than T
  @Override
  @RegionEffects("writes S")
  public void writesImplicitStaticSuperRegion() {}

  // GOOD: T is the same as T
  @Override
  @RegionEffects("writes T")
  public void writesImplicitStaticSameRegion() {}

  // GOOD: U is more specific than T
  @Override
  @RegionEffects("writes U")
  public void writesImplicitStaticSubRegion() {}

  // GOOD: V is more specific than T
  @Override
  @RegionEffects("writes V")
  public void writesImplicitStaticNewRegion() {}

  // BAD
  @Override
  @RegionEffects("writes O")
  public void writesImplicitStaticUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes P")
  public void writesImplicitStaticNewUnrelatedRegion() {}

  // explicit static class
  
  // BAD: S is more general than T
  @Override
  @RegionEffects("writes test.explicitStatic.Root:S")
  public void writesExplicitStaticSuperRegion() {}

  // GOOD: T is the same as T
  @Override
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void writesExplicitStaticSameRegion() {}

  // GOOD: U is more specific than T
  @Override
  @RegionEffects("writes test.explicitStatic.Root:U")
  public void writesExplicitStaticSubRegion() {}

  // GOOD: V is more specific than T
  @Override
  @RegionEffects("writes test.explicitStatic.Writes:V")
  public void writesExplicitStaticNewSubRegion() {}

  // BAD
  @Override
  @RegionEffects("writes test.explicitStatic.Root:O")
  public void writesExplicitStaticUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes test.explicitStatic.Writes:P")
  public void writesExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes p:A")
  public void writesFormalArgumentSubRegion(final Root p) {}
  
  // BAD
  @Override
  @RegionEffects("writes p:X")
  public void writesFormalArgumentUnrelatedRegion(final Root p) {}

  
  
  // === Original read effects
  
  // implicit receiver
  
  // BAD
  @Override
  @RegionEffects("writes A")
  public void readsImplicitThisSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes B")
  public void readsImplicitThisNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes X")
  public void readsImplicitThisUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes Y")
  public void readsImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  // BAD
  @Override
  @RegionEffects("writes this:A")
  public void readsExplicitThisSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes this:B")
  public void readsExplicitThisNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes this:X")
  public void readsExplicitThisUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes this:Y")
  public void readsExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver
  
  // BAD
  @Override
  @RegionEffects("writes Writes.this:A")
  public void reads0thQualifiedThisSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes Writes.this:B")
  public void reads0thQualifiedThisNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes Writes.this:X")
  public void reads0thQualifiedThisUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes Writes.this:Y")
  public void reads0thQualifiedThisNewUnrelatedRegion() {}

  // implicit static class
  
  // BAD
  @Override
  @RegionEffects("writes S")
  public void readsImplicitStaticSuperRegion() {}

  // BAD
  @Override
  @RegionEffects("writes T")
  public void readsImplicitStaticSameRegion() {}

  // BAD
  @Override
  @RegionEffects("writes U")
  public void readsImplicitStaticSubRegion() {}

  // BAD
  @Override
  @RegionEffects("writes V")
  public void readsImplicitStaticNewRegion() {}

  // BAD
  @Override
  @RegionEffects("writes O")
  public void readsImplicitStaticUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes P")
  public void readsImplicitStaticNewUnrelatedRegion() {}

  // explicit static class
  
  // BAD
  @Override
  @RegionEffects("writes test.explicitStatic.Root:S")
  public void readsExplicitStaticSuperRegion() {}

  // BAD
  @Override
  @RegionEffects("writes test.explicitStatic.Root:T")
  public void readsExplicitStaticSameRegion() {}

  // BAD
  @Override
  @RegionEffects("writes test.explicitStatic.Root:U")
  public void readsExplicitStaticSubRegion() {}

  // BAD
  @Override
  @RegionEffects("writes test.explicitStatic.Writes:V")
  public void readsExplicitStaticNewSubRegion() {}

  // BAD
  @Override
  @RegionEffects("writes test.explicitStatic.Root:O")
  public void readsExplicitStaticUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes test.explicitStatic.Writes:P")
  public void readsExplicitStaticNewUnrelatedRegion() {}
  
  // formal argument
  
  // BAD
  @Override
  @RegionEffects("writes p:A")
  public void readsFormalArgumentSubRegion(final Root p) {}
  
  // BAD
  @Override
  @RegionEffects("writes p:X")
  public void readsFormalArgumentUnrelatedRegion(final Root p) {}
}
