package test;

import other.Super2;

public class DifferentPackageSuper extends Super2 {
  /* Needs @RegionEffects("writes this:SuperPublic") */
  public void publicMethod() {
    publicField = 1;    // Redundant with SuperPublic
    protectedField = 2; // Promoted to SuperPublic
    ppp = 3;            // Promoted to SuperPublic
  }

  /* Needs @RegionEffects("writes this:SuperProtected, this:publicField") */
  protected void protectedMethod() {
    publicField = 1;    // Not promoted
    protectedField = 2; // Redundant with SuperProtected
    ppp = 3;            // Promoted to SuperProtected 
  }

  /* Needs @RegionEffects("writes this:SuperProtected, this:publicField") */
  void defaultMethod() {
    publicField = 1;    // Not promoted
    protectedField = 2; // Redundant with SuperProtected
    ppp = 3;            // Promoted to SuperProtected because its declared in an ancestor class in another package
    // Can never access a private field of a superclass
  }

  /* Needs @RegionEffects("writes this:SuperProtected, this:publicField") */
  @SuppressWarnings("unused")
  private void privateMethod() {
    publicField = 1;    // Not promoted
    protectedField = 2; // Redundant with SuperProtected
    ppp = 3;            // Promoted to SuperProtected because its declared in an ancestor class in another package
    // Can never access a private field of a superclass
  }
}
