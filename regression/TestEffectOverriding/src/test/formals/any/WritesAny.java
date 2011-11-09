package test.formals.any;

import com.surelogic.RegionEffects;

public class WritesAny extends Any {
  // === Write Effects
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySuper):A")
  public void writesAnySuperClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySuper):B")
  public void writesAnySuperClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySuper):C")
  public void writesAnySuperClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySuper):X")
  public void writesAnySuperClassUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Any):A")
  public void writesAnySameClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Any):B")
  public void writesAnySameClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Any):C")
  public void writesAnySameClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Any):D")
  public void writesAnySameClassNewSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Any):X")
  public void writesAnySameClassUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):A")
  public void writesAnySubClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):B")
  public void writesAnySubClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):C")
  public void writesAnySubClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):E")
  public void writesAnySubClassNewSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):X")
  public void writesAnySubClassUnrelatedRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):Z")
  public void writesAnySubClassNewUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Other):R")
  public void writesAnyUnrelatedClass(Any p) {}



  // === Read Effects
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySuper):A")
  public void readsAnySuperClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySuper):B")
  public void readsAnySuperClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySuper):C")
  public void readsAnySuperClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySuper):X")
  public void readsAnySuperClassUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Any):A")
  public void readsAnySameClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Any):B")
  public void readsAnySameClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Any):C")
  public void readsAnySameClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Any):D")
  public void readsAnySameClassNewSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Any):X")
  public void readsAnySameClassUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):A")
  public void readsAnySubClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):B")
  public void readsAnySubClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):C")
  public void readsAnySubClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):E")
  public void readsAnySubClassNewSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):X")
  public void readsAnySubClassUnrelatedRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(AnySub):Z")
  public void readsAnySubClassNewUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("writes any(Other):R")
  public void readsAnyUnrelatedClass(Any p) {}}
