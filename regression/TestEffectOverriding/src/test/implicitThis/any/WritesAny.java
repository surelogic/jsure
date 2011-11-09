package test.implicitThis.any;

import com.surelogic.RegionEffects;

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
  public void readsAnyUnrelatedClass() {}}
