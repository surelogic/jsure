package test.anyInstance.any;

import com.surelogic.RegionEffects;

public class ReadsAny extends Any {
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
  
  // GOOD: Same
  @Override
  @RegionEffects("writes any(Any):B")
  public void writesAnySameClassSameRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes any(Any):C")
  public void writesAnySameClassSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes any(Any):D")
  public void writesAnySameClassNewSubRegion() {}
  
  // BAD: unrelated region
  @Override
  @RegionEffects("writes any(Any):X")
  public void writesAnySameClassUnrelatedRegion() {}
  
  
  // BAD: A is more general than B
  @Override
  @RegionEffects("writes any(AnySub):A")
  public void writesAnySubClassSuperRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes any(AnySub):B")
  public void writesAnySubClassSameRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes any(AnySub):C")
  public void writesAnySubClassSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("writes any(AnySub):E")
  public void writesAnySubClassNewSubRegion() {}
  
  // BAD: Unrelated region
  @Override
  @RegionEffects("writes any(AnySub):X")
  public void writesAnySubClassUnrelatedRegion() {}
  
  // BAD: Unrelated region
  @Override
  @RegionEffects("writes any(AnySub):Z")
  public void writesAnySubClassNewUnrelatedRegion() {}
  
  
  // BAD: Unrelated class, Unrelated region
  @Override
  @RegionEffects("writes any(Other):R")
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
  
  // GOOD: Same
  @Override
  @RegionEffects("reads any(Any):B")
  public void readsAnySameClassSameRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(Any):C")
  public void readsAnySameClassSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(Any):D")
  public void readsAnySameClassNewSubRegion() {}
  
  // BAD: unrelated region
  @Override
  @RegionEffects("reads any(Any):X")
  public void readsAnySameClassUnrelatedRegion() {}
  
  
  // BAD: A is more general than B
  @Override
  @RegionEffects("reads any(AnySub):A")
  public void readsAnySubClassSuperRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(AnySub):B")
  public void readsAnySubClassSameRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(AnySub):C")
  public void readsAnySubClassSubRegion() {}
  
  // GOOD: More specific
  @Override
  @RegionEffects("reads any(AnySub):E")
  public void readsAnySubClassNewSubRegion() {}
  
  // BAD: Unrelated region
  @Override
  @RegionEffects("reads any(AnySub):X")
  public void readsAnySubClassUnrelatedRegion() {}
  
  // BAD: Unrelated region
  @Override
  @RegionEffects("reads any(AnySub):Z")
  public void readsAnySubClassNewUnrelatedRegion() {}
  
  
  // BAD: Unrelated class, Unrelated region
  @Override
  @RegionEffects("reads any(Other):R")
  public void readsAnyUnrelatedClass() {}
}