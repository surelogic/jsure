package test_lock_region;

/**
 * Tests
 * (3) the associated region must exist
 * (4) instance field or this cannot be associated with a static region  
 * (4a) Static region must be from the same class as the declaration
 * 
 * L1 is BAD: region is undefined
 * 
 * L2 is BAD: this is not static
 * L3 is BAD: field is not static
 * L4 is GOOD: class is static
 * L5 is GOOD: field is static
 * 
 * L6 is GOOD: static region is from this class (C)
 * L7 is GOOD: static region is from this class (C); doesn't matter that field is from superclass (B)
 * L8 is GOOD: static region is from this class (C); doesn't matter that field is from subclass (D)
 * L9 is GOOD: static region is from this class (C); doesn't matter that field is from an unrelated class (Other)
 * Point is that region must be from the same class as the declaration
 * 
 * L10 is BAD: static region is from superclass (B)
 * L11 is BAD: static region is from subclass (D)
 * L12 is BAD: static region is from unrelated class (Other)
 * Point is that region must be from the same class as the declaration
 * 
 * @region static S1
 * @region static S2
 * @region static S3
 * @region static S4
 * 
 * @region static StaticRegionFromC1
 * @region static StaticRegionFromC2
 * @region static StaticRegionFromC3
 * @region static StaticRegionFromC4
 * @region static StaticRegionFromC5
 * 
 * @lock L1 is this protects UnknownRegion
 * 
 * @lock L2 is this protects S1
 * @lock L3 is nonStaticField protects S2
 * @lock L4 is class protects S3
 * @lock L5 is staticFieldFromC protects S4
 * 
 * @lock L6 is staticFieldFromC protects StaticRegionFromC1
 * @lock L7 is test_lock_region.B:staticFieldFromB protects StaticRegionFromC2
 * @lock L8 is test_lock_region.D:staticFieldFromD protects StaticRegionFromC3
 * @lock L9 is test_lock_region.Other:staticFieldFromOther protects StaticRegionFromC4
 * 
 * @lock L10 is staticFieldFromC protects test_lock_region.B:StaticRegionFromB
 * @lock L11 is staticFieldFromC protects test_lock_region.D:StaticRegionFromD
 * @lock L12 is staticFieldFromC protects test_lock_region.Other:StaticRegionFromOther
 */
public class C extends B {
  final Object nonStaticField = new Object();
  static final Object staticFieldFromC = new Object();
}
