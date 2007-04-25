package test_lock_region;

/**
 * @region EmptyRegionFromParent
 * 
 * @region RegionFromPFilledInP
 * 
 * @region RegionFromPWithSubRegionFromP
 *
 * @region PSubRegion1 extends RegionFromGGPWithSubRegionFromP
 * @region PSubRegion2 extends RegionFromGPWithSubRegionFromP
 * @region PSubRegion3 extends RegionFromPWithSubRegionFromP
 * 
 * @region PSubRegion4 extends GPSubRegion3
 */
public class Parent extends Grandparent {
  /** @mapInto RegionFromGGPFilledInP */
  @SuppressWarnings("unused")
  private int pf1 = 1;

  /** @mapInto RegionFromGPFilledInP */
  @SuppressWarnings("unused")
  private int pf2 = 1;

  /** @mapInto RegionFromPFilledInP */
  @SuppressWarnings("unused")
  private int pf3 = 1;
  
  /** @mapInto PSubRegion4 */
  @SuppressWarnings("unused")
  private int pf4 = 1;
}
