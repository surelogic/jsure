package test_lock_region;

import com.surelogic.InRegion;
import com.surelogic.Region;
import com.surelogic.Regions;

@Regions({
  @Region("EmptyRegionFromGreatGrandparent"),
  @Region("RegionFromGGPFilledInGGP"),
  @Region("RegionFromGGPFilledInGP"),
  @Region("RegionFromGGPFilledInP"),
  @Region("RegionFromGGPWithSubRegionFromGGP"),
  @Region("RegionFromGGPWithSubRegionFromGP"),
  @Region("RegionFromGGPWithSubRegionFromP"),
  @Region("RegionFromGGPWithNestedFieldFromP"),
  @Region("GGPSubRegion1 extends RegionFromGGPWithSubRegionFromGGP"),
  @Region("GGPSubRegion2 extends RegionFromGGPWithNestedFieldFromP"),
})
public class GreatGrandparent {
  @InRegion("RegionFromGGPFilledInGGP")
  @SuppressWarnings("unused")
private int gppf1 = 1;
}
