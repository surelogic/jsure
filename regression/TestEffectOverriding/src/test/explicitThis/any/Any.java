package test.explicitThis.any;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public D extends B"),
  
  @Region("public Y")
})
public class Any extends AnySuper {
  // === Write Effects
  
  @RegionEffects("writes this:B")
  public void writesAnySuperClassSuperRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySuperClassSameRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySuperClassSubRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("writes this:B")
  public void writesAnySameClassSuperRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySameClassSameRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySameClassSubRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySameClassNewSubRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySameClassUnrelatedRegion() {}
  
  
  @RegionEffects("writes this:B")
  public void writesAnySubClassSuperRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySubClassSameRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySubClassSubRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySubClassNewSubRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("writes this:B")
  public void writesAnySubClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("writes this:B")
  public void writesAnyUnrelatedClass() {}



  // === Read Effects
  
  @RegionEffects("reads this:B")
  public void readsAnySuperClassSuperRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySuperClassSameRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySuperClassSubRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("reads this:B")
  public void readsAnySameClassSuperRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySameClassSameRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySameClassSubRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySameClassNewSubRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySameClassUnrelatedRegion() {}
  
  
  @RegionEffects("reads this:B")
  public void readsAnySubClassSuperRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySubClassSameRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySubClassSubRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySubClassNewSubRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("reads this:B")
  public void readsAnySubClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("reads this:B")
  public void readsAnyUnrelatedClass() {}
}
