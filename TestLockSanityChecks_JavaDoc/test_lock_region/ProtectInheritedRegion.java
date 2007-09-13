package test_lock_region;

/**
 * @TestResult is CONSISTENT: Protecting empty region from great grandparent
 * @RegionLock L1_good is this protects EmptyRegionFromGreatGrandparent
 * @TestResult is CONSISTENT: Protecting empty region from grandparent
 * @RegionLock L2_good is this protects EmptyRegionFromGrandparent
 * @TestResult is CONSISTENT: Protecting empty region from parent
 * @RegionLock L3_good is this protects EmptyRegionFromParent
 * 
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from GGP
 * @RegionLock L10_bad is this protects RegionFromGGPFilledInGGP
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from GGP
 * @RegionLock L11_bad is this protects RegionFromGGPFilledInGP
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from GGP
 * @RegionLock L12_bad is this protects RegionFromGGPFilledInP
 * 
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from GP
 * @RegionLock L20_bad is this protects RegionFromGPFilledInGP
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from GP
 * @RegionLock L21_bad is this protects RegionFromGPFilledInP
 *
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from P
 * @RegionLock L30_bad is this protects RegionFromPFilledInP
 * 
 * @TestResult is CONSISTENT: Protecting region with empty subregion from GGP
 * @RegionLock L40_good is this protects RegionFromGGPWithSubRegionFromGGP
 * @TestResult is CONSISTENT: Protecting region with empty subregion from GGP
 * @RegionLock L41_good is this protects RegionFromGGPWithSubRegionFromGP
 * @TestResult is CONSISTENT: Protecting region with empty subregion from GGP
 * @RegionLock L42_good is this protects RegionFromGGPWithSubRegionFromP
 * 
 * @TestResult is CONSISTENT: Protecting region with empty subregion from GP
 * @RegionLock L50_good is this protects RegionFromGPWithSubRegionFromGP
 * @TestResult is CONSISTENT: Protecting region with empty subregion from GP
 * @RegionLock L51_good is this protects RegionFromGPWithSubRegionFromP
 * 
 * @TestResult is CONSISTENT: Protecting region with empty subregion from P
 * @RegionLock L60_good is this protects RegionFromPWithSubRegionFromP
 * 
 * @TestResult is UNASSOCIATED: Protecting region with a non-empty subregion
 * @RegionLock L70_bad is this protects RegionFromGGPWithNestedFieldFromP
 */
public class ProtectInheritedRegion extends Parent {

}
