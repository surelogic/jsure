package test.explicitStatic.any;

import com.surelogic.RegionEffects;

public class ReadsAny extends Any {
  // === Write Effects
  
  // GOOD: More Specific
  @Override
  @RegionEffects("reads any(AnySuper):A")
  public void writesAnySuperClassSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(AnySuper):X")
  public void writesAnySuperClassUnrelatedRegion() {}
  
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(Any):A")
  public void writesAnySameClassSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(Any):B")
  public void writesAnySameClassNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(Any):X")
  public void writesAnySameClassUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(Any):Y")
  public void writesAnySameClassNewUnrelatedRegion() {}
  
  
  // GOOD: MOre specific
  @Override
  @RegionEffects("reads any(AnySub):A")
  public void writesAnySubClassSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(AnySub):B")
  public void writesAnySubClassNewSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(AnySub):C")
  public void writesAnySubClassNewNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(AnySub):X")
  public void writesAnySubClassUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(AnySub):Y")
  public void writesAnySubClassNewUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(AnySub):Z")
  public void writesAnySubClassNewNewUnrelatedRegion() {}
  
  
  // BAD:
  @Override
  @RegionEffects("reads any(Other):R")
  public void writesAnyUnrelatedClass() {}



  // === Read Effects
  
  // GOOD: More Specific
  @Override
  @RegionEffects("reads any(AnySuper):A")
  public void readsAnySuperClassSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(AnySuper):X")
  public void readsAnySuperClassUnrelatedRegion() {}
  
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(Any):A")
  public void readsAnySameClassSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(Any):B")
  public void readsAnySameClassNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(Any):X")
  public void readsAnySameClassUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(Any):Y")
  public void readsAnySameClassNewUnrelatedRegion() {}
  
  
  // GOOD: MOre specific
  @Override
  @RegionEffects("reads any(AnySub):A")
  public void readsAnySubClassSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(AnySub):B")
  public void readsAnySubClassNewSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(AnySub):C")
  public void readsAnySubClassNewNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(AnySub):X")
  public void readsAnySubClassUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(AnySub):Y")
  public void readsAnySubClassNewUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("reads any(AnySub):Z")
  public void readsAnySubClassNewNewUnrelatedRegion() {}
  
  
  // BAD:
  @Override
  @RegionEffects("reads any(Other):R")
  public void readsAnyUnrelatedClass() {}
}
