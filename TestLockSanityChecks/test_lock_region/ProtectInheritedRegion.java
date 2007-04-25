package test_lock_region;

/**
 * @lock L1_good is this protects EmptyRegionFromGreatGrandparent
 * @lock L2_good is this protects EmptyRegionFromGrandparent
 * @lock L3_good is this protects EmptyRegionFromParent
 * 
 * @lock L10_bad is this protects RegionFromGGPFilledInGGP
 * @lock L11_bad is this protects RegionFromGGPFilledInGP
 * @lock L12_bad is this protects RegionFromGGPFilledInP
 * 
 * @lock L20_bad is this protects RegionFromGPFilledInGP
 * @lock L21_bad is this protects RegionFromGPFilledInP
 *
 * @lock L30_bad is this protects RegionFromPFilledInP
 * 
 * @lock L40_good is this protects RegionFromGGPWithSubRegionFromGGP
 * @lock L41_good is this protects RegionFromGGPWithSubRegionFromGP
 * @lock L42_good is this protects RegionFromGGPWithSubRegionFromP
 * 
 * @lock L50_good is this protects RegionFromGPWithSubRegionFromGP
 * @lock L51_good is this protects RegionFromGPWithSubRegionFromP
 * 
 * @lock L60_good is this protects RegionFromPWithSubRegionFromP
 * 
 * @lock L70_bad is this protects RegionFromGGPWithNestedFieldFromP
 * 
 * @author aarong
 *
 */
public class ProtectInheritedRegion extends Parent {

}
