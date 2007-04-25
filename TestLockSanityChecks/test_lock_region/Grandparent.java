package test_lock_region;

/**
 * @region EmptyRegionFromGrandparent
 * 
 * @region RegionFromGPFilledInGP
 * @region RegionFromGPFilledInP
 * 
 * @region RegionFromGPWithSubRegionFromGP
 * @region RegionFromGPWithSubRegionFromP
 * 
 * @region GPSubRegion1 extends RegionFromGGPWithSubRegionFromGP
 * @region GPSubRegion2 extends RegionFromGPWithSubRegionFromGP
 * 
 * @region GPSubRegion3 extends GGPSubRegion2
 */
public class Grandparent extends GreatGrandparent {
  /** @mapInto RegionFromGGPFilledInGP */
  @SuppressWarnings("unused")
  private int gpf1 = 1;

  /** @mapInto RegionFromGPFilledInGP */
  @SuppressWarnings("unused")
  private int gpf2 = 1;
}
