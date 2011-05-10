package test.implicitThis;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public D extends B"),
  
  @Region("public Y extends T"),
  
  @Region("public static TT")
})
public class Reads extends Root {
  // GOOD
  @Override
  @RegionEffects("reads B")
  public void unannotated() {}

  
  
  // === Original Write effects
  
  // implicit receiver
  
  // BAD: A is more general than B
  @Override
  @RegionEffects("reads A")
  public void writesImplicitThisSuperRegion() {}
  
  // GOOD: B is the same as B
  @Override
  @RegionEffects("reads B")
  public void writesImplicitThisSameRegion() {}
  
  // GOOD: C is more specific than B
  @Override
  @RegionEffects("reads C")
  public void writesImplicitThisSubRegion() {}
  
  // GOOD: D is more specific than B
  @Override
  @RegionEffects("reads D")
  public void writesImplicitThisNewSubRegion() {}
  
  // BAD: X is unrelated to B
  @Override
  @RegionEffects("reads X")
  public void writesImplicitThisUnrelatedRegion() {}
  
  // BAD: X is unrelated to Y
  @Override
  @RegionEffects("reads Y")
  public void writesImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  // BAD: A is more general than B
  @Override
  @RegionEffects("reads this:A")
  public void writesExplicitThisSuperRegion() {}
  
  // GOOD: B is the same as B
  @Override
  @RegionEffects("reads this:B")
  public void writesExplicitThisSameRegion() {}
  
  // GOOD: C is more specific than B
  @Override
  @RegionEffects("reads this:C")
  public void writesExplicitThisSubRegion() {}
  
  // GOOD: D is more specific than B
  @Override
  @RegionEffects("reads this:D")
  public void writesExplicitThisNewSubRegion() {}
  
  // BAD: X is unrelated to B
  @Override
  @RegionEffects("reads this:X")
  public void writesExplicitThisUnrelatedRegion() {}
  
  // BAD: Y is unrelated to B
  @Override
  @RegionEffects("reads this:Y")
  public void writesExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver
  
  // BAD: A is more general than B
  @Override
  @RegionEffects("reads Reads.this:A")
  public void writes0thQualifiedThisSuperRegion() {}
  
  // GOOD: B is the same as B
  @Override
  @RegionEffects("reads Reads.this:B")
  public void writes0thQualifiedThisSameRegion() {}
  
  // GOOD: C is more specific than B
  @Override
  @RegionEffects("reads Reads.this:C")
  public void writes0thQualifiedThisSubRegion() {}
  
  // GOOD: D is more specific than B
  @Override
  @RegionEffects("reads Reads.this:D")
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
  
  // BAD: More general than B
  @Override
  @RegionEffects("reads S")
  public void writesImplicitStaticSuperRegion() {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads T")
  public void writesImplicitStaticUnrelatedRegion() {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads TT")
  public void writesImplicitStaticNewUnrelatedRegion() {}

  // explicit static class
  
  // BAD: More general than B
  @Override
  @RegionEffects("reads test.implicitThis.Root:S")
  public void writesExplicitStaticSuperRegion() {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads test.implicitThis.Root:T")
  public void writesExplicitStaticUnrelatedRegion() {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads test.implicitThis.Reads:TT")
  public void writesExplicitStaticNewUnrelatedRegion() {}
  
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

  
  
  // === Original read effects
  
  // implicit receiver
  
  // BAD: A is more general than B
  @Override
  @RegionEffects("reads A")
  public void readsImplicitThisSuperRegion() {}
  
  // GOOD: B is the same as B
  @Override
  @RegionEffects("reads B")
  public void readsImplicitThisSameRegion() {}
  
  // GOOD: C is more specific than B
  @Override
  @RegionEffects("reads C")
  public void readsImplicitThisSubRegion() {}
  
  // GOOD: D is more specific than B
  @Override
  @RegionEffects("reads D")
  public void readsImplicitThisNewSubRegion() {}
  
  // BAD: X is unrelated to B
  @Override
  @RegionEffects("reads X")
  public void readsImplicitThisUnrelatedRegion() {}
  
  // BAD: X is unrelated to Y
  @Override
  @RegionEffects("reads Y")
  public void readsImplicitThisNewUnrelatedRegion() {}
  
  // explicit receiver
  
  // BAD: writes is more general than reads even though A is more general than B
  @Override
  @RegionEffects("reads this:A")
  public void readsExplicitThisSuperRegion() {}
  
  // GOOD: B is the same as B
  @Override
  @RegionEffects("reads this:B")
  public void readsExplicitThisSameRegion() {}
  
  // GOOD: C is more specific than B
  @Override
  @RegionEffects("reads this:C")
  public void readsExplicitThisSubRegion() {}
  
  // GOOD: D is more specific than B
  @Override
  @RegionEffects("reads this:D")
  public void readsExplicitThisNewSubRegion() {}
  
  // BAD: X is unrelated to B
  @Override
  @RegionEffects("reads this:X")
  public void readsExplicitThisUnrelatedRegion() {}
  
  // BAD: Y is unrelated to B
  @Override
  @RegionEffects("reads this:Y")
  public void readsExplicitThisNewUnrelatedRegion() {}
  
  // 0th-outer class receiver
  
  // BAD: A is more general than B
  @Override
  @RegionEffects("reads Reads.this:A")
  public void reads0thQualifiedThisSuperRegion() {}
  
  // GOOD: B is the same as B
  @Override
  @RegionEffects("reads Reads.this:B")
  public void reads0thQualifiedThisSameRegion() {}
  
  // GOOD: C is more specific than B
  @Override
  @RegionEffects("reads Reads.this:C")
  public void reads0thQualifiedThisSubRegion() {}
  
  // GOOD: D is more specific than B
  @Override
  @RegionEffects("reads Reads.this:D")
  public void reads0thQualifiedThisNewSubRegion() {}
  
  // BAD: X is unrelated to B
  @Override
  @RegionEffects("reads Reads.this:X")
  public void reads0thQualifiedThisUnrelatedRegion() {}
  
  // BAD: Y is unrelated to B
  @Override
  @RegionEffects("reads Reads.this:Y")
  public void reads0thQualifiedThisNewUnrelatedRegion() {}

  // implicit static class
  
  // BAD: More general than B
  @Override
  @RegionEffects("reads S")
  public void readsImplicitStaticSuperRegion() {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads T")
  public void readsImplicitStaticUnrelatedRegion() {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads TT")
  public void readsImplicitStaticNewUnrelatedRegion() {}

  // explicit static class
  
  // BAD: More general than B
  @Override
  @RegionEffects("reads test.implicitThis.Root:S")
  public void readsExplicitStaticSuperRegion() {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads test.implicitThis.Root:T")
  public void readsExplicitStaticUnrelatedRegion() {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads test.implicitThis.Reads:TT")
  public void readsExplicitStaticNewUnrelatedRegion() {}
  
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
}
