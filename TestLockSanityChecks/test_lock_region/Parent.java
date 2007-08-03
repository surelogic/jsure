package test_lock_region;

/**
 * @Region EmptyRegionFromParent
 * 
 * @Region RegionFromPFilledInP
 * 
 * @Region RegionFromPWithSubRegionFromP
 *
 * @Region PSubRegion1 extends RegionFromGGPWithSubRegionFromP
 * @Region PSubRegion2 extends RegionFromGPWithSubRegionFromP
 * @Region PSubRegion3 extends RegionFromPWithSubRegionFromP
 * 
 * @Region PSubRegion4 extends GPSubRegion3
 */
public class Parent extends Grandparent {
  /** @MapInto RegionFromGGPFilledInP */
  @SuppressWarnings("unused")
  private int pf1 = 1;

  /** @MapInto RegionFromGPFilledInP */
  @SuppressWarnings("unused")
  private int pf2 = 1;

  /** @MapInto RegionFromPFilledInP */
  @SuppressWarnings("unused")
  private int pf3 = 1;
  
  /** @MapInto PSubRegion4 */
  @SuppressWarnings("unused")
  private int pf4 = 1;
}
