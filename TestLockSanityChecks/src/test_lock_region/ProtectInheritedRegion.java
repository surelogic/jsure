package test_lock_region;

import com.surelogic.RegionLock;
import com.surelogic.RegionLocks;

@RegionLocks({
  @RegionLock("L1_good is this protects EmptyRegionFromGreatGrandparent" /* is CONSISTENT: Protecting empty region from great grandparent */),
  @RegionLock("L2_good is this protects EmptyRegionFromGrandparent" /* is CONSISTENT: Protecting empty region from grandparent */),
  @RegionLock("L3_good is this protects EmptyRegionFromParent" /* is CONSISTENT: Protecting empty region from parent */),
  @RegionLock("L10_bad is this protects RegionFromGGPFilledInGGP" /* is UNASSOCIATED: Protecting non-empty region inherited from GGP */),
  @RegionLock("L11_bad is this protects RegionFromGGPFilledInGP" /* is UNASSOCIATED: Protecting non-empty region inherited from GGP */),
  @RegionLock("L12_bad is this protects RegionFromGGPFilledInP" /* is UNASSOCIATED: Protecting non-empty region inherited from GGP */),
  @RegionLock("L20_bad is this protects RegionFromGPFilledInGP" /* is UNASSOCIATED: Protecting non-empty region inherited from GP */),
  @RegionLock("L21_bad is this protects RegionFromGPFilledInP" /* is UNASSOCIATED: Protecting non-empty region inherited from GP */),
  @RegionLock("L30_bad is this protects RegionFromPFilledInP" /* is UNASSOCIATED: Protecting non-empty region inherited from P */),
  @RegionLock("L40_good is this protects RegionFromGGPWithSubRegionFromGGP" /* is CONSISTENT: Protecting region with empty subregion from GGP */),
  @RegionLock("L41_good is this protects RegionFromGGPWithSubRegionFromGP" /* is CONSISTENT: Protecting region with empty subregion from GGP */),
  @RegionLock("L42_good is this protects RegionFromGGPWithSubRegionFromP" /* is CONSISTENT: Protecting region with empty subregion from GGP */),
  @RegionLock("L50_good is this protects RegionFromGPWithSubRegionFromGP" /* is CONSISTENT: Protecting region with empty subregion from GP */),
  @RegionLock("L51_good is this protects RegionFromGPWithSubRegionFromP" /* is CONSISTENT: Protecting region with empty subregion from GP */),
  @RegionLock("L60_good is this protects RegionFromPWithSubRegionFromP" /* is CONSISTENT: Protecting region with empty subregion from P */),
  @RegionLock("L70_bad is this protects RegionFromGGPWithNestedFieldFromP" /* is UNASSOCIATED: Protecting region with a non-empty subregion */),
})
public class ProtectInheritedRegion extends Parent {

}
