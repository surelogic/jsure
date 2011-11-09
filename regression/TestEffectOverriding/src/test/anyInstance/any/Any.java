package test.anyInstance.any;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public D extends B"),
  
  @Region("public Y")
})
public class Any extends AnySuper {
  // === Write Effects
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySuperClassSuperRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySuperClassSameRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySuperClassSubRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySameClassSuperRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySameClassSameRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySameClassSubRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySameClassNewSubRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySameClassUnrelatedRegion() {}
  
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySubClassSuperRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySubClassSameRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySubClassSubRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySubClassNewSubRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("writes any(Any):B")
  public void writesAnySubClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("writes any(Any):B")
  public void writesAnyUnrelatedClass() {}



  // === Read Effects
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySuperClassSuperRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySuperClassSameRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySuperClassSubRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySameClassSuperRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySameClassSameRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySameClassSubRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySameClassNewSubRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySameClassUnrelatedRegion() {}
  
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySubClassSuperRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySubClassSameRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySubClassSubRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySubClassNewSubRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("reads any(Any):B")
  public void readsAnySubClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("reads any(Any):B")
  public void readsAnyUnrelatedClass() {}
}
