package test.formals;

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
  @RegionEffects("reads p:B")
  public void unannotated(Root p, Root q, Other o) {}

  
  
  // === Original Write effects
  
  // implicit receiver
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads A")
  public void writesImplicitThisSuperRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads B")
  public void writesImplicitThisSameRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads C")
  public void writesImplicitThisSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads D")
  public void writesImplicitThisNewSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads X")
  public void writesImplicitThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Y")
  public void writesImplicitThisNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // explicit receiver
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:A")
  public void writesExplicitThisSuperRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:B")
  public void writesExplicitThisSameRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:C")
  public void writesExplicitThisSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:D")
  public void writesExplicitThisNewSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:X")
  public void writesExplicitThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:Y")
  public void writesExplicitThisNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // 0th-outer class receiver
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:A")
  public void writes0thQualifiedThisSuperRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:B")
  public void writes0thQualifiedThisSameRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:C")
  public void writes0thQualifiedThisSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:D")
  public void writes0thQualifiedThisNewSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:X")
  public void writes0thQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:Y")
  public void writes0thQualifiedThisNewUnrelatedRegion(Root p, Root q, Other o) {}

  // implicit static class
  
  // BAD: More general than B
  @Override
  @RegionEffects("reads S")
  public void writesImplicitStaticSuperRegion(Root p, Root q, Other o) {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads T")
  public void writesImplicitStaticUnrelatedRegion(Root p, Root q, Other o) {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads TT")
  public void writesImplicitStaticNewUnrelatedRegion(Root p, Root q, Other o) {}

  // explicit static class
  
  // BAD: More general than B
  @Override
  @RegionEffects("reads test.formals.Root:S")
  public void writesExplicitStaticSuperRegion(Root p, Root q, Other o) {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads test.formals.Root:T")
  public void writesExplicitStaticUnrelatedRegion(Root p, Root q, Other o) {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads test.formals.Reads:TT")
  public void writesExplicitStaticNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // formal argument
  
  // BAD: Too general
  @Override
  @RegionEffects("writes p:A")
  public void writesSameFormalArgumentSuperRegion(final Root p, Root q, Other o) {}
  
  // GOOD: Same region
  @Override
  @RegionEffects("writes p:B")
  public void writesSameFormalArgumentSameRegion(final Root p, Root q, Other o) {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes p:C")
  public void writesSameFormalArgumentSubRegion(final Root p, Root q, Other o) {}
  
  // BAD: Unrelated
  @Override
  @RegionEffects("writes p:X")
  public void writesSameFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}
  
  // BAD: More general
  @Override
  @RegionEffects("writes w:A")
  public void writesRenamedFormalArgumentSuperRegion(final Root w, Root q, Other o) {}
  
  // GOOD: Same region
  @Override
  @RegionEffects("writes w:B")
  public void writesRenamedFormalArgumentSameRegion(final Root w, Root q, Other o) {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes w:C")
  public void writesRenamedFormalArgumentSubRegion(final Root w, Root q, Other o) {}
  
  // BAD: Unrelated
  @Override
  @RegionEffects("writes w:X")
  public void writesRenamedFormalArgumentUnrelatedRegion(final Root w, Root q, Other o) {}
  
  // BAD: wrong formal
  @Override
  @RegionEffects("writes q:A")
  public void writes2ndFormalArgumentSuperRegion(final Root p, Root q, Other o) {}
  
  // BAD: wrong formal
  @Override
  @RegionEffects("writes q:B")
  public void writes2ndFormalArgumentSameRegion(final Root p, Root q, Other o) {}
  
  // BAD: wrong formal
  @Override
  @RegionEffects("writes q:C")
  public void writes2ndFormalArgumentSubRegion(final Root p, Root q, Other o) {}
  
  // BAD: wrong formal
  @Override
  @RegionEffects("writes q:X")
  public void writes2ndFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}

  // BAD: wrong formal -- wrong region
  @Override
  @RegionEffects("writes o:Other")
  public void writes3rdFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}

  // BAD: wrong formal -- wrong region (but with the same name)
  @Override
  @RegionEffects("writes o:B")
  public void writes3rdFormalArgumentSameNamedRegion(final Root p, Root q, Other o) {}

  
  
  // === Original read effects
  
  // implicit receiver
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads A")
  public void readsImplicitThisSuperRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads B")
  public void readsImplicitThisSameRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads C")
  public void readsImplicitThisSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads D")
  public void readsImplicitThisNewSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads X")
  public void readsImplicitThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Y")
  public void readsImplicitThisNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // explicit receiver
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:A")
  public void readsExplicitThisSuperRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:B")
  public void readsExplicitThisSameRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:C")
  public void readsExplicitThisSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:D")
  public void readsExplicitThisNewSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:X")
  public void readsExplicitThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads this:Y")
  public void readsExplicitThisNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // 0th-outer class receiver
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:A")
  public void reads0thQualifiedThisSuperRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:B")
  public void reads0thQualifiedThisSameRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:C")
  public void reads0thQualifiedThisSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:D")
  public void reads0thQualifiedThisNewSubRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:X")
  public void reads0thQualifiedThisUnrelatedRegion(Root p, Root q, Other o) {}
  
  // BAD: wrong argument
  @Override
  @RegionEffects("reads Reads.this:Y")
  public void reads0thQualifiedThisNewUnrelatedRegion(Root p, Root q, Other o) {}

  // implicit static class
  
  // BAD: More general than B
  @Override
  @RegionEffects("reads S")
  public void readsImplicitStaticSuperRegion(Root p, Root q, Other o) {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads T")
  public void readsImplicitStaticUnrelatedRegion(Root p, Root q, Other o) {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads TT")
  public void readsImplicitStaticNewUnrelatedRegion(Root p, Root q, Other o) {}

  // explicit static class
  
  // BAD: More general than B
  @Override
  @RegionEffects("reads test.formals.Root:S")
  public void readsExplicitStaticSuperRegion(Root p, Root q, Other o) {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads test.formals.Root:T")
  public void readsExplicitStaticUnrelatedRegion(Root p, Root q, Other o) {}
  
  // BAD: More unrelated to B
  @Override
  @RegionEffects("reads test.formals.Reads:TT")
  public void readsExplicitStaticNewUnrelatedRegion(Root p, Root q, Other o) {}
  
  // formal argument
  
  // BAD: Too general
  @Override
  @RegionEffects("reads p:A")
  public void readsSameFormalArgumentSuperRegion(final Root p, Root q, Other o) {}
  
  // GOOD: Same region
  @Override
  @RegionEffects("reads p:B")
  public void readsSameFormalArgumentSameRegion(final Root p, Root q, Other o) {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads p:C")
  public void readsSameFormalArgumentSubRegion(final Root p, Root q, Other o) {}
  
  // BAD: Unrelated
  @Override
  @RegionEffects("reads p:X")
  public void readsSameFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}
  
  // BAD: More general
  @Override
  @RegionEffects("reads w:A")
  public void readsRenamedFormalArgumentSuperRegion(final Root w, Root q, Other o) {}
  
  // GOOD: Same region
  @Override
  @RegionEffects("reads w:B")
  public void readsRenamedFormalArgumentSameRegion(final Root w, Root q, Other o) {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads w:C")
  public void readsRenamedFormalArgumentSubRegion(final Root w, Root q, Other o) {}
  
  // BAD: Unrelated
  @Override
  @RegionEffects("reads w:X")
  public void readsRenamedFormalArgumentUnrelatedRegion(final Root w, Root q, Other o) {}
  
  // BAD: wrong formal
  @Override
  @RegionEffects("reads q:A")
  public void reads2ndFormalArgumentSuperRegion(final Root p, Root q, Other o) {}
  
  // BAD: wrong formal
  @Override
  @RegionEffects("reads q:B")
  public void reads2ndFormalArgumentSameRegion(final Root p, Root q, Other o) {}
  
  // BAD: wrong formal
  @Override
  @RegionEffects("reads q:C")
  public void reads2ndFormalArgumentSubRegion(final Root p, Root q, Other o) {}
  
  // BAD: wrong formal
  @Override
  @RegionEffects("reads q:X")
  public void reads2ndFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}

  // BAD: wrong formal -- wrong region
  @Override
  @RegionEffects("reads o:Other")
  public void reads3rdFormalArgumentUnrelatedRegion(final Root p, Root q, Other o) {}

  // BAD: wrong formal -- wrong region (but with the same name)
  @Override
  @RegionEffects("reads o:B")
  public void reads3rdFormalArgumentSameNamedRegion(final Root p, Root q, Other o) {}
}
