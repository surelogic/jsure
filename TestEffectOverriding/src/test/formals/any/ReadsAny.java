package test.formals.any;

import com.surelogic.RegionEffects;

public class ReadsAny extends Any {
  // === Write Effects
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySuper):A")
  public void writesAnySuperClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySuper):B")
  public void writesAnySuperClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySuper):C")
  public void writesAnySuperClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySuper):X")
  public void writesAnySuperClassUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Any):A")
  public void writesAnySameClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Any):B")
  public void writesAnySameClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Any):C")
  public void writesAnySameClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Any):D")
  public void writesAnySameClassNewSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Any):X")
  public void writesAnySameClassUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):A")
  public void writesAnySubClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):B")
  public void writesAnySubClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):C")
  public void writesAnySubClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):E")
  public void writesAnySubClassNewSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):X")
  public void writesAnySubClassUnrelatedRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):Z")
  public void writesAnySubClassNewUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Other):R")
  public void writesAnyUnrelatedClass(Any p) {}



  // === Read Effects
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySuper):A")
  public void readsAnySuperClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySuper):B")
  public void readsAnySuperClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySuper):C")
  public void readsAnySuperClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySuper):X")
  public void readsAnySuperClassUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Any):A")
  public void readsAnySameClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Any):B")
  public void readsAnySameClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Any):C")
  public void readsAnySameClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Any):D")
  public void readsAnySameClassNewSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Any):X")
  public void readsAnySameClassUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):A")
  public void readsAnySubClassSuperRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):B")
  public void readsAnySubClassSameRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):C")
  public void readsAnySubClassSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):E")
  public void readsAnySubClassNewSubRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):X")
  public void readsAnySubClassUnrelatedRegion(Any p) {}
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(AnySub):Z")
  public void readsAnySubClassNewUnrelatedRegion(Any p) {}
  
  
  // BAD: too general
  @Override
  @RegionEffects("reads any(Other):R")
  public void readsAnyUnrelatedClass(Any p) {}}
