package test_lock_region;

/**
 * @TestResult is CONSISTENT: Protecting empty region from great grandparent
 * @Lock L1_good is this protects EmptyRegionFromGreatGrandparent
 * @TestResult is CONSISTENT: Protecting empty region from grandparent
 * @Lock L2_good is this protects EmptyRegionFromGrandparent
 * @TestResult is CONSISTENT: Protecting empty region from parent
 * @Lock L3_good is this protects EmptyRegionFromParent
 * 
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from GGP
 * @Lock L10_bad is this protects RegionFromGGPFilledInGGP
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from GGP
 * @Lock L11_bad is this protects RegionFromGGPFilledInGP
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from GGP
 * @Lock L12_bad is this protects RegionFromGGPFilledInP
 * 
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from GP
 * @Lock L20_bad is this protects RegionFromGPFilledInGP
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from GP
 * @Lock L21_bad is this protects RegionFromGPFilledInP
 *
 * @TestResult is UNASSOCIATED: Protecting non-empty region inherited from P
 * @Lock L30_bad is this protects RegionFromPFilledInP
 * 
 * @TestResult is CONSISTENT: Protecting region with empty subregion from GGP
 * @Lock L40_good is this protects RegionFromGGPWithSubRegionFromGGP
 * @TestResult is CONSISTENT: Protecting region with empty subregion from GGP
 * @Lock L41_good is this protects RegionFromGGPWithSubRegionFromGP
 * @TestResult is CONSISTENT: Protecting region with empty subregion from GGP
 * @Lock L42_good is this protects RegionFromGGPWithSubRegionFromP
 * 
 * @TestResult is CONSISTENT: Protecting region with empty subregion from GP
 * @Lock L50_good is this protects RegionFromGPWithSubRegionFromGP
 * @TestResult is CONSISTENT: Protecting region with empty subregion from GP
 * @Lock L51_good is this protects RegionFromGPWithSubRegionFromP
 * 
 * @TestResult is CONSISTENT: Protecting region with empty subregion from P
 * @Lock L60_good is this protects RegionFromPWithSubRegionFromP
 * 
 * @TestResult is UNASSOCIATED: Protecting region with a non-empty subregion
 * @Lock L70_bad is this protects RegionFromGGPWithNestedFieldFromP
 */
public class ProtectInheritedRegion extends Parent {

}
