package test_lock_region;

/**
 * @region EmptyRegionFromGreatGrandparent
 * 
 * @region RegionFromGGPFilledInGGP
 * @region RegionFromGGPFilledInGP
 * @region RegionFromGGPFilledInP
 * 
 * @region RegionFromGGPWithSubRegionFromGGP
 * @region RegionFromGGPWithSubRegionFromGP
 * @region RegionFromGGPWithSubRegionFromP
 * 
 * @region RegionFromGGPWithNestedFieldFromP
 * 
 * @region GGPSubRegion1 extends RegionFromGGPWithSubRegionFromGGP
 * @region GGPSubRegion2 extends RegionFromGGPWithNestedFieldFromP
 */
public class GreatGrandparent {
  /** @mapInto RegionFromGGPFilledInGGP */
  @SuppressWarnings("unused")
private int gppf1 = 1;
}
