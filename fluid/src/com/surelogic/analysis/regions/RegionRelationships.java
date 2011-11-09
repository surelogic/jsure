package com.surelogic.analysis.regions;

/**
 * Enumeration for holding result "codes" describing the result of comparing to
 * {@link IRegion} objects.
 */
public enum RegionRelationships
{
	/**
   * Constant indicating two regions are incomparable: neither is an ancestor of
   * the other and they are not equal.
   */
	INCOMPARABLE("Incomparable"),
	
	/**
   * Constant indicating two regions are equal.
   */
	EQUAL("Equal"),

  /**
   * Constant indicating that two regions are not equal and region A is an
   * ancestor of region B. "Region A" and "Region B" are defined by the
   * operation that returns the result.
   */
  REGION_A_INCLUDES_REGION_B("Region A includes region B"),

  /**
   * Constant indicating that two regions are not equal and region B is an
   * ancestor of region A. "Region A" and "Region B" are defined by the
   * operation that returns the result.
   */
  REGION_B_INCLUDES_REGION_A("Region B includes region A");


	
	private final String id;
  
  private RegionRelationships(final String id) {
    this.id = id;
  }
  
  @Override
  public String toString() {
    return id;
  }  
}
