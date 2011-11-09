package test;

public class SamePackageSuper extends Super {
  /* Needs @RegionEffects("writes this:SuperPublic") */
  public void publicMethod() {
    publicField = 1;    // Redundant with SuperPublic
    protectedField = 2; // Promoted to SuperPublic
    defaultField = 3;   // Promoted to SuperPublic
  }

  /* Needs @RegionEffects("writes this:SuperProtected, this:publicField") */
  protected void protectedMethod() {
    publicField = 1;    // Not promoted
    protectedField = 2; // Redundant with SuperProtected
    defaultField = 3;   // Promoted to SuperProtected
  }

  /* Needs @RegionEffects("writes this:defaultField, this:protectedField, this:defaultField") */
  void defaultMethod() {
    publicField = 1;    // Not promoted
    protectedField = 2; // Not promoted
    defaultField = 3;   // Not promoted
    // Can never access private fields from super class
  }

  /* Needs @RegionEffects("writes this:defaultField, this:protectedField, this:defaultField") */
  @SuppressWarnings("unused")
  private void privateMethod() {
    publicField = 1;    // Not promoted
    protectedField = 2; // Not promoted
    defaultField = 3;   // Not promoted
    // Can never access private fields from super class
  }
}
