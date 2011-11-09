package test.satisfiedByWritesAll;

import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public R"),
  @Region("public static S")
})
public class C {
  // Writes All
  
  public void unannotated() {}
  
  @RegionEffects("writes All")
  public void writesImplicitAll() {}
  
  @RegionEffects("writes java.lang.Object:All")
  public void writesQualifiedAll() {}
  
  
  
  // reads All
  
  @RegionEffects("reads All")
  public void readsImplicitAll() {}
  
  @RegionEffects("reads java.lang.Object:All")
  public void readsQualifiedAll() {}

  
  
  // Other Writes
  
  @RegionEffects("writes R")
  public void writesImplicitThis() {}
  
  @RegionEffects("writes S")
  public void writesImplicitStatic() {}
  
  @RegionEffects("writes any(C):R")
  public void writesAny() {}
  
  @RegionEffects("writes C.this:R")
  public void writesQualifiedThis() {}
  
  @RegionEffects("writes test.satisfiedByWritesAll.C:S")
  public void writesQualifiedStatic() {}
  
  @RegionEffects("writes this:R") 
  public void writesExplicitThis() {}
  
  @RegionEffects("writes x:R") 
  public void writesParam(final C x) {}

  
  
  // Other Reads
  
  @RegionEffects("reads R")
  public void readsImplicitThis() {}
  
  @RegionEffects("reads S")
  public void readsImplicitStatic() {}
  
  @RegionEffects("reads any(C):R")
  public void readsAny() {}
  
  @RegionEffects("reads C.this:R")
  public void readsQualifiedThis() {}
  
  @RegionEffects("reads test.satisfiedByWritesAll.C:S")
  public void readsQualifiedStatic() {}
  
  @RegionEffects("reads this:R") 
  public void readsExplicitThis() {}
  
  @RegionEffects("reads x:R") 
  public void readsParam(final C x) {}
}
