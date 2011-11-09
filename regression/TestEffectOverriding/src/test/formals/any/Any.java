package test.formals.any;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public D extends B"),
  
  @Region("public Y")
})
public class Any extends AnySuper {
  // === Write Effects
  
  @RegionEffects("writes p:B")
  public void writesAnySuperClassSuperRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySuperClassSameRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySuperClassSubRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySuperClassUnrelatedRegion(Any p) {}
  
  
  @RegionEffects("writes p:B")
  public void writesAnySameClassSuperRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySameClassSameRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySameClassSubRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySameClassNewSubRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySameClassUnrelatedRegion(Any p) {}
  
  
  @RegionEffects("writes p:B")
  public void writesAnySubClassSuperRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySubClassSameRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySubClassSubRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySubClassNewSubRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySubClassUnrelatedRegion(Any p) {}
  
  @RegionEffects("writes p:B")
  public void writesAnySubClassNewUnrelatedRegion(Any p) {}
  
  
  @RegionEffects("writes p:B")
  public void writesAnyUnrelatedClass(Any p) {}



  // === Read Effects
  
  @RegionEffects("reads p:B")
  public void readsAnySuperClassSuperRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySuperClassSameRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySuperClassSubRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySuperClassUnrelatedRegion(Any p) {}
  
  
  @RegionEffects("reads p:B")
  public void readsAnySameClassSuperRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySameClassSameRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySameClassSubRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySameClassNewSubRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySameClassUnrelatedRegion(Any p) {}
  
  
  @RegionEffects("reads p:B")
  public void readsAnySubClassSuperRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySubClassSameRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySubClassSubRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySubClassNewSubRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySubClassUnrelatedRegion(Any p) {}
  
  @RegionEffects("reads p:B")
  public void readsAnySubClassNewUnrelatedRegion(Any p) {}
  
  
  @RegionEffects("reads p:B")
  public void readsAnyUnrelatedClass(Any p) {}
}
