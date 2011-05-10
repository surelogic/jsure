package test.qualifiedThis.any;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public D extends B"),
  
  @Region("public Y")
})
public class Any extends AnySuper {
  // === Write Effects
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySuperClassSuperRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySuperClassSameRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySuperClassSubRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySameClassSuperRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySameClassSameRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySameClassSubRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySameClassNewSubRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySameClassUnrelatedRegion() {}
  
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySubClassSuperRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySubClassSameRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySubClassSubRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySubClassNewSubRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("writes Any.this:B")
  public void writesAnySubClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("writes Any.this:B")
  public void writesAnyUnrelatedClass() {}



  // === Read Effects
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySuperClassSuperRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySuperClassSameRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySuperClassSubRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySameClassSuperRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySameClassSameRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySameClassSubRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySameClassNewSubRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySameClassUnrelatedRegion() {}
  
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySubClassSuperRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySubClassSameRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySubClassSubRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySubClassNewSubRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("reads Any.this:B")
  public void readsAnySubClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("reads Any.this:B")
  public void readsAnyUnrelatedClass() {}
}
