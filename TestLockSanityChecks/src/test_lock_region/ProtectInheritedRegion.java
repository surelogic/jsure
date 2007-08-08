package test_lock_region;

import com.surelogic.Lock;
import com.surelogic.Locks;

@Locks({
  @Lock("L1_good is this protects EmptyRegionFromGreatGrandparent" /* is CONSISTENT: Protecting empty region from great grandparent */),
  @Lock("L2_good is this protects EmptyRegionFromGrandparent" /* is CONSISTENT: Protecting empty region from grandparent */),
  @Lock("L3_good is this protects EmptyRegionFromParent" /* is CONSISTENT: Protecting empty region from parent */),
  @Lock("L10_bad is this protects RegionFromGGPFilledInGGP" /* is UNASSOCIATED: Protecting non-empty region inherited from GGP */),
  @Lock("L11_bad is this protects RegionFromGGPFilledInGP" /* is UNASSOCIATED: Protecting non-empty region inherited from GGP */),
  @Lock("L12_bad is this protects RegionFromGGPFilledInP" /* is UNASSOCIATED: Protecting non-empty region inherited from GGP */),
  @Lock("L20_bad is this protects RegionFromGPFilledInGP" /* is UNASSOCIATED: Protecting non-empty region inherited from GP */),
  @Lock("L21_bad is this protects RegionFromGPFilledInP" /* is UNASSOCIATED: Protecting non-empty region inherited from GP */),
  @Lock("L30_bad is this protects RegionFromPFilledInP" /* is UNASSOCIATED: Protecting non-empty region inherited from P */),
  @Lock("L40_good is this protects RegionFromGGPWithSubRegionFromGGP" /* is CONSISTENT: Protecting region with empty subregion from GGP */),
  @Lock("L41_good is this protects RegionFromGGPWithSubRegionFromGP" /* is CONSISTENT: Protecting region with empty subregion from GGP */),
  @Lock("L42_good is this protects RegionFromGGPWithSubRegionFromP" /* is CONSISTENT: Protecting region with empty subregion from GGP */),
  @Lock("L50_good is this protects RegionFromGPWithSubRegionFromGP" /* is CONSISTENT: Protecting region with empty subregion from GP */),
  @Lock("L51_good is this protects RegionFromGPWithSubRegionFromP" /* is CONSISTENT: Protecting region with empty subregion from GP */),
  @Lock("L60_good is this protects RegionFromPWithSubRegionFromP" /* is CONSISTENT: Protecting region with empty subregion from P */),
  @Lock("L70_bad is this protects RegionFromGGPWithNestedFieldFromP" /* is UNASSOCIATED: Protecting region with a non-empty subregion */),
})
public class ProtectInheritedRegion extends Parent {

}
