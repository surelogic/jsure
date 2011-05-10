package test.explicitStatic.any;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public B extends T"),
  
  @Region("public Y extends O")
})
public class Any extends AnySuper {
  // === Write Effects
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySuperClassSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySameClassSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySameClassNewSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySameClassUnrelatedRegion() {}
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySameClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySubClassSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySubClassNewSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySubClassNewNewSubRegion() {}
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySubClassNewUnrelatedRegion() {}
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnySubClassNewNewUnrelatedRegion() {}
  
  
  @RegionEffects("writes test.explicitStatic.any.AnySuper:T")
  public void writesAnyUnrelatedClass() {}



  // === Read Effects
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySuperClassSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySuperClassUnrelatedRegion() {}
  
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySameClassSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySameClassNewSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySameClassUnrelatedRegion() {}
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySameClassNewUnrelatedRegion() {}
  
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySubClassSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySubClassNewSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySubClassNewNewSubRegion() {}
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySubClassUnrelatedRegion() {}
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySubClassNewUnrelatedRegion() {}
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnySubClassNewNewUnrelatedRegion() {}
  
  
  @RegionEffects("reads test.explicitStatic.any.AnySuper:T")
  public void readsAnyUnrelatedClass() {}
}
