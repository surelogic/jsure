package test_lock_region;

/**
 * Tests
 * (3) the associated region must exist
 * (4) instance field or this cannot be associated with a static region  
 * (4a) Static region must be from the same class as the declaration
 * 
 * @Region static S1
 * @Region static S2
 * @Region static S3
 * @Region static S4
 * 
 * @Region static StaticRegionFromC1
 * @Region static StaticRegionFromC2
 * @Region static StaticRegionFromC3
 * @Region static StaticRegionFromC4
 * @Region static StaticRegionFromC5
 * 
 * @TestResult is UNBOUND: Region is undefined
 * @RegionLock L1 is this protects UnknownRegion
 * 
 * @TestResult is UNASSOCIATED: Cannot protect a static region with 'this'
 * @RegionLock L2 is this protects S1
 * @TestResult is UNASSOCIATED: Cannot protect a static region with a non-static field
 * @RegionLock L3 is nonStaticField protects S2
 * @TestResult is CONSISTENT: Static region protected by class
 * @RegionLock L4 is class protects S3
 * @TestResult is CONSISTENT: Static region protected by static field
 * @RegionLock L5 is staticFieldFromC protects S4
 * 
 * @TestResult is CONSISTENT: Static region is from the protecting class; field is from the same
 * @RegionLock L6 is staticFieldFromC protects StaticRegionFromC1
 * @TestResult is CONSISTENT: Static region is from the protecting class; doesn't matter that field is from superclass (B)
 * @RegionLock L7 is test_lock_region.B.staticFieldFromB protects StaticRegionFromC2
 * @TestResult is CONSISTENT: Static region is from the protecting class; doesn't matter that field is from subclass (D)
 * @RegionLock L8 is test_lock_region.D.staticFieldFromD protects StaticRegionFromC3
 * @TestResult is CONSISTENT: Static region is from the protecting class; doesn't matter that field is from an unrelated class (Other)
 * @RegionLock L9 is test_lock_region.Other.staticFieldFromOther protects StaticRegionFromC4
 * 
 * @TestResult is UNASSOCIATED: static region is from superclass (B)
 * @RegionLock L10 is staticFieldFromC protects test_lock_region.B:StaticRegionFromB
 * @TestResult is UNASSOCIATED: static region is from subclass (D)
 * @RegionLock L11 is staticFieldFromC protects test_lock_region.D:StaticRegionFromD
 * @TestResult is UNASSOCIATED: static region is from unrelated class (Other)
 * @RegionLock L12 is staticFieldFromC protects test_lock_region.Other:StaticRegionFromOther
 */
public class C extends B {
  final Object nonStaticField = new Object();
  static final Object staticFieldFromC = new Object();
}
