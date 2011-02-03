package test_lock_region;

/**
 * @Region EmptyRegionFromGrandparent
 * 
 * @Region RegionFromGPFilledInGP
 * @Region RegionFromGPFilledInP
 * 
 * @Region RegionFromGPWithSubRegionFromGP
 * @Region RegionFromGPWithSubRegionFromP
 * 
 * @Region GPSubRegion1 extends RegionFromGGPWithSubRegionFromGP
 * @Region GPSubRegion2 extends RegionFromGPWithSubRegionFromGP
 * 
 * @Region GPSubRegion3 extends GGPSubRegion2
 */
public class Grandparent extends GreatGrandparent {
  /** @InRegion RegionFromGGPFilledInGP */
  @SuppressWarnings("unused")
  private int gpf1 = 1;

  /** @InRegion RegionFromGPFilledInGP */
  @SuppressWarnings("unused")
  private int gpf2 = 1;
}
