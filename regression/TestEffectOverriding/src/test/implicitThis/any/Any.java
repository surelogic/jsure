package test.implicitThis.any;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public D extends B"),
  
  @Region("public Y")
})
public class Any extends AnySuper {
  // === Write Effects
  
  @RegionEffects("writes B")
  public void writesAnySuperClassSuperRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySuperClassSameRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySuperClassSubRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("writes B")
  public void writesAnySameClassSuperRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySameClassSameRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySameClassSubRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySameClassNewSubRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySameClassUnrelatedRegion() {}
  
  
  @RegionEffects("writes B")
  public void writesAnySubClassSuperRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySubClassSameRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySubClassSubRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySubClassNewSubRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("writes B")
  public void writesAnySubClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("writes B")
  public void writesAnyUnrelatedClass() {}



  // === Read Effects
  
  @RegionEffects("reads B")
  public void readsAnySuperClassSuperRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySuperClassSameRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySuperClassSubRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("reads B")
  public void readsAnySameClassSuperRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySameClassSameRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySameClassSubRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySameClassNewSubRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySameClassUnrelatedRegion() {}
  
  
  @RegionEffects("reads B")
  public void readsAnySubClassSuperRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySubClassSameRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySubClassSubRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySubClassNewSubRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("reads B")
  public void readsAnySubClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("reads B")
  public void readsAnyUnrelatedClass() {}
}
