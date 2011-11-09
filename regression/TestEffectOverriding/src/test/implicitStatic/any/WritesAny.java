package test.implicitStatic.any;

import com.surelogic.RegionEffects;

public class WritesAny extends Any {
  // === Write Effects
  
  // GOOD: More Specific
  @Override
  @RegionEffects("writes any(AnySuper):A")
  public void writesAnySuperClassSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySuper):X")
  public void writesAnySuperClassUnrelatedRegion() {}
  
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes any(Any):A")
  public void writesAnySameClassSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes any(Any):B")
  public void writesAnySameClassNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(Any):X")
  public void writesAnySameClassUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(Any):Y")
  public void writesAnySameClassNewUnrelatedRegion() {}
  
  
  // GOOD: MOre specific
  @Override
  @RegionEffects("writes any(AnySub):A")
  public void writesAnySubClassSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes any(AnySub):B")
  public void writesAnySubClassNewSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes any(AnySub):C")
  public void writesAnySubClassNewNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySub):X")
  public void writesAnySubClassUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySub):Y")
  public void writesAnySubClassNewUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySub):Z")
  public void writesAnySubClassNewNewUnrelatedRegion() {}
  
  
  // BAD:
  @Override
  @RegionEffects("writes any(Other):R")
  public void writesAnyUnrelatedClass() {}



  // === Read Effects
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySuper):A")
  public void readsAnySuperClassSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySuper):X")
  public void readsAnySuperClassUnrelatedRegion() {}
  
  
  // BAD
  @Override
  @RegionEffects("writes any(Any):A")
  public void readsAnySameClassSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(Any):B")
  public void readsAnySameClassNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(Any):X")
  public void readsAnySameClassUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(Any):Y")
  public void readsAnySameClassNewUnrelatedRegion() {}
  
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySub):A")
  public void readsAnySubClassSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySub):B")
  public void readsAnySubClassNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySub):C")
  public void readsAnySubClassNewNewSubRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySub):X")
  public void readsAnySubClassUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySub):Y")
  public void readsAnySubClassNewUnrelatedRegion() {}
  
  // BAD
  @Override
  @RegionEffects("writes any(AnySub):Z")
  public void readsAnySubClassNewNewUnrelatedRegion() {}
  
  
  // BAD:
  @Override
  @RegionEffects("writes any(Other):R")
  public void readsAnyUnrelatedClass() {}
}
