package test.implicitStatic.any;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public B extends T"),
  
  @Region("public Y extends O")
})
public class Any extends AnySuper {
  // === Write Effects
  
  @RegionEffects("writes T")
  public void writesAnySuperClassSubRegion() {}
  
  @RegionEffects("writes T")
  public void writesAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("writes T")
  public void writesAnySameClassSubRegion() {}
  
  @RegionEffects("writes T")
  public void writesAnySameClassNewSubRegion() {}
  
  @RegionEffects("writes T")
  public void writesAnySameClassUnrelatedRegion() {}
  
  @RegionEffects("writes T")
  public void writesAnySameClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("writes T")
  public void writesAnySubClassSubRegion() {}
  
  @RegionEffects("writes T")
  public void writesAnySubClassNewSubRegion() {}
  
  @RegionEffects("writes T")
  public void writesAnySubClassNewNewSubRegion() {}
  
  @RegionEffects("writes T")
  public void writesAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("writes T")
  public void writesAnySubClassNewUnrelatedRegion() {}
  
  @RegionEffects("writes T")
  public void writesAnySubClassNewNewUnrelatedRegion() {}
  
  
  @RegionEffects("writes T")
  public void writesAnyUnrelatedClass() {}



  // === Read Effects
  
  @RegionEffects("reads T")
  public void readsAnySuperClassSubRegion() {}
  
  @RegionEffects("reads T")
  public void readsAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("reads T")
  public void readsAnySameClassSubRegion() {}
  
  @RegionEffects("reads T")
  public void readsAnySameClassNewSubRegion() {}
  
  @RegionEffects("reads T")
  public void readsAnySameClassUnrelatedRegion() {}
  
  @RegionEffects("reads T")
  public void readsAnySameClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("reads T")
  public void readsAnySubClassSubRegion() {}
  
  @RegionEffects("reads T")
  public void readsAnySubClassNewSubRegion() {}
  
  @RegionEffects("reads T")
  public void readsAnySubClassNewNewSubRegion() {}
  
  @RegionEffects("reads T")
  public void readsAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("reads T")
  public void readsAnySubClassNewUnrelatedRegion() {}
  
  @RegionEffects("reads T")
  public void readsAnySubClassNewNewUnrelatedRegion() {}
  
  
  @RegionEffects("reads T")
  public void readsAnyUnrelatedClass() {}
}
