package test;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("public Public"),
  @Region("protected Protected extends Public"),
  @Region("Default extends Protected"),
  @Region("private Private extends Default")
})
public class SimpleTest {
  @InRegion("Public")
  public int publicField;
  
  @InRegion("Protected")
  protected int protectedField;
  
  @InRegion("Default")
  int defaultField;
  
  @SuppressWarnings("unused")
  @InRegion("Private")
  private int privateField;
  
  
  /* Needs @RegionEffects("writes this:Public") */
  public void publicMethod() {
    publicField = 1;    // Not promoted, but redundant with Public
    protectedField = 2; // Promoted to Public
    defaultField = 3;   // Promoted to Public
    privateField = 4;   // Promoted to Public
  }

  /* Needs @RegionEffects("writes this:Protected, this:publicField") */
  protected void protectedMethod() {
    publicField = 1;    // Not promoted
    protectedField = 2; // Not promoted, but redundant with Protected
    defaultField = 3;   // Promoted to Protected
    privateField = 4;   // Promoted to Protected
  }

  /* Needs @RegionEffects("writes this:Default, this:protectedField, this:publicField") */
  void defaultMethod() {
    publicField = 1;    // Not promoted
    protectedField = 2; // Not promoted
    defaultField = 3;   // Not promoted, but redundant with Default
    privateField = 4;   // Promoted to Default
  }

  /* Needs @RegionEffects("writes this:defaultField, this:privateField, this:protectedField, this:publicField") */
  @SuppressWarnings("unused")
  private void privateMethod() {
    publicField = 1;    // Not promoted
    protectedField = 2; // Not promoted
    defaultField = 3;   // Not promoted
    privateField = 4;   // Not promoted
  }
}