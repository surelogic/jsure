package com.surelogic.analysis.effects.targets;

/**
 * Enumeration holding values for the results of testing for
 * target overlap.
 */
public enum TargetRelationships
{
	/**
	 * Element indicating that one target does not include another.
	 */
	UNRELATED("Unrelated targets"),
	
	/**
	 * Element indicating that one target includes another because they 
	 * lexically name the same local variable.
	 */
	SAME_VARIABLE("Same variable"),
	  
	/**
	 * Element indicating that one target includes another because they
	 * may refer to state of the same object.
	 */
	POSSIBLE_ALIASING("Possible aliasing"),
	  
	/**
   * Element indicating that one target includes another because the targets are
   * of different kinds, and one completely denotes the state identified by the
   * other. In this case the first target is larger than the second. The roles
   * of first and second target are identified by the particular operation that
   * returns the result.
   */
	TARGET_A_IS_LARGER("Target A is larger"),
	  
	/**
   * Element indicating that one target includes another because the targets are
   * of different kinds, and one completely denotes the state identified by the
   * other. In this case the second target is larger than the first. The roles
   * of first and second target are identified by the particular operation that
   * returns the result.
   */
	TARGET_B_IS_LARGER("Target B is larger");


	
  private final String id;
  
  private TargetRelationships(final String id) {
    this.id = id;
  }
  
  @Override
  public String toString() {
    return id;
  }
}
