package test_lock_region;

import com.surelogic.Lock;
import com.surelogic.Locks;
import com.surelogic.Region;
import com.surelogic.Regions;

/**
 * Tests
 * (3) the associated region must exist
 * (4) instance field or this cannot be associated with a static region  
 * (4a) Static region must be from the same class as the declaration
 */
@Regions({ 
  @Region("static S1"),
  @Region("static S2"),
  @Region("static S3"),
  @Region("static S4"),
  @Region("static StaticRegionFromC1"),
  @Region("static StaticRegionFromC2"),
  @Region("static StaticRegionFromC3"),
  @Region("static StaticRegionFromC4"),
  @Region("static StaticRegionFromC5"),
})
@Locks({
  @Lock("L1 is this protects UnknownRegion" /* is UNBOUND: Region is undefined */),
  @Lock("L2 is this protects S1" /* is UNASSOCIATED: Cannot protect a static region with 'this' */),
  @Lock("L3 is nonStaticField protects S2" /* is UNASSOCIATED: Cannot protect a static region with a non-static field */),
  @Lock("L4 is class protects S3" /* is CONSISTENT: Static region protected by class */),
  @Lock("L5 is staticFieldFromC protects S4" /* is CONSISTENT: Static region protected by static field */),
  @Lock("L6 is staticFieldFromC protects StaticRegionFromC1" /* is CONSISTENT: Static region is from the protecting class; field is from the same */),
  @Lock("L7 is test_lock_region.B.staticFieldFromB protects StaticRegionFromC2" /* is CONSISTENT: Static region is from the protecting class; doesn't matter that field is from superclass (B) */),
  @Lock("L8 is test_lock_region.D.staticFieldFromD protects StaticRegionFromC3" /* is CONSISTENT: Static region is from the protecting class; doesn't matter that field is from subclass (D) */),
  @Lock("L9 is test_lock_region.Other.staticFieldFromOther protects StaticRegionFromC4" /* is CONSISTENT: Static region is from the protecting class; doesn't matter that field is from an unrelated class (Other) */),
  @Lock("L10 is staticFieldFromC protects test_lock_region.B:StaticRegionFromB" /* is UNASSOCIATED: static region is from superclass (B) */),
  @Lock("L11 is staticFieldFromC protects test_lock_region.D:StaticRegionFromD" /* is UNASSOCIATED: static region is from subclass (D) */),
  @Lock("L12 is staticFieldFromC protects test_lock_region.Other:StaticRegionFromOther" /* is UNASSOCIATED: static region is from unrelated class (Other) */),
})
public class C extends B {
  final Object nonStaticField = new Object();
  static final Object staticFieldFromC = new Object();
}
