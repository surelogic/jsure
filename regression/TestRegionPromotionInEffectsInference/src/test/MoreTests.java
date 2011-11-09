package test;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.RegionEffects;
import com.surelogic.Regions;

@Regions({
  @Region("public static Static"),
  
  @Region("public Public extends Static"),
  @Region("protected Protected extends Public"),
  @Region("Default extends Protected"),
  @Region("private Private extends Default"),
})
@SuppressWarnings("unused")
public class MoreTests {
  @InRegion("Public")
  public int publicField;
  
  @InRegion("Protected")
  protected int protectedField;
  
  @InRegion("Default")
  int defaultField;
  
  @InRegion("Private")
  private int privateField;
  
  
  
  @RegionEffects("none")
  public MoreTests() {
    super();
  }
  
  
  /* Missing some effects.  Masks others because it's a constructor */
  /* Check that we ignore masked effects, and properly integrate missing
   * effects into the declared effects.  Should propose
   *   @RegionEffects("reads other:defaultField, other:privateField; writes other:protectedField, other:publicField")
   */
  @RegionEffects("writes other:publicField; reads other:defaultField")
  private MoreTests(final MoreTests other) {
    // masked
    publicField = privateField;
    protectedField = defaultField;

    // not masked
    other.publicField = other.privateField;
    other.protectedField = other.defaultField;
  }
  
  /* Test that missing effects are integrated with existing declared effects.
   * Annotation should be
   *   @RegionEffects("reads this:defaultField, this:privateField; writes this:protectedField, this:publicField")
   */
  @RegionEffects("writes publicField; reads defaultField")
  private void someButNotAll() {
    publicField = privateField;
    protectedField = defaultField;
  }
  
  /* Test that we don't infer a read and write effect for the same region; 
   * just keep the write effect.  Should propose
   *   @RegionEffect("writes this:Protected")
   */
  private void t1() {
    // read and write to the same region: just want the write effect
    writeProtected();
    readProtected();
  }
  
  /* A read from a super region should not make a write to a subregion
   * redundant.  We want to suggest
   *   @RegionEffects("reads this:Protected; writes this:Default")
   */
  private void t2() {
    // read from a super region, write to a sub region: keep both
    readProtected();
    writeDefault();
  }
  
  /* A write to a super region does make a read from a subregion redundant.
   * Suggest
   *   @RegionEffects("writes this:Protected")
   */
  private void t3() {
    // write to a super region, read from a subregion: only keep write
    writeProtected();
    readDefault();
  }

  /* Test that any-instance effects on region R make instance effects on region
   * R redundant.  Should propose
   *   @RegionEffects("reads any(test.MoreTests):Public")
   */
  private void t4(MoreTests o) {
    // any instance includes instance
    readAnyInstance();
    readFrom(this);
    readFrom(o);
  }

  /* Test that an effect on a static region makes any any-instance and instance
   * effects on subregions redundant.  Should propose:
   *   @RegionEffects("writes test.MoreTests:Static") 
   */
  private void t5(MoreTests o) {
    // class effect includes any instance, includes instance
    writeStatic();
    readAnyInstance();
    readFrom(this);
    readFrom(o);
  }


  
  @RegionEffects("writes Protected")
  private void writeProtected() {}
  
  @RegionEffects("reads Protected")
  private void readProtected() {}
  
  @RegionEffects("writes Default")
  private void writeDefault() {}
  
  @RegionEffects("reads Default")
  private void readDefault() {}
  
  @RegionEffects("writes Static")
  private void writeStatic() {}
  
  @RegionEffects("reads any(MoreTests):Public")
  private void readAnyInstance() {}
  
  @RegionEffects("reads p:Default")
  private void readFrom(MoreTests p) {}
}
