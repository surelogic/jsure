/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/Messages.java,v 1.9 2007/10/31 16:26:37 aarong Exp $*/
package edu.cmu.cs.fluid.java.bind;

public final class Messages {
  public static final int LockAnnotation_lockModel = 100;
  public static final int LockAnnotation_policyLockModel = 101;
  public static final int LockAnnotation_requiresLockDrop = 102;
  public static final int RegionAnnotation_aggregateInRegionDrop = 103;
  public static final int AssumeFinalAnnotation_finalFieldDrop = 104;
  public static final int UniquenessAnnotation_borrowedDrop = 105;
  public static final int LockAnnotation_immutableDrop = 106;
  public static final int RegionAnnotation_inRegionDrop = 107;
  public static final int RegionAnnotation_mapFieldsDrop = 108;
  public static final int LockAnnotation_notThreadSafeDrop = 109;
  public static final int UniquenessAnnotation_notUniqueDrop = 110;
  public static final int LockAnnotation_returnsLockDrop = 111;
  public static final int LockAnnotation_selfProtectedDrop = 112;
  public static final int LockAnnotation_singleThreadedDrop = 113;
  public static final int StartsAnnotation_startNothingDrop = 114;
  public static final int UniquenessAnnotation_uniqueDrop1 = 115;
  public static final int UniquenessAnnotation_uniqueDrop2 = 116;
  public static final int RegionAnnotation_regionDrop = 117;
  public static final int LockAnnotation_containableDrop = 118;

  
  /* 2011-01-18: Edwin says these are referenced from code that will become
   * obsolete and deleted.
   */
  public static final String LockAnnotation_prescrubbedLock = "RegionLock {0} (pre-scrubbed, not complete)";
  public static final String LockAnnotation_prescrubbedPolicyLock = "PolicyLock {0} (pre-scrubbed, not complete)";
  public static final String LockAnnotation_prescrubbedRequiresLock = "RequiresLock on {0} (pre-scrubber, not complete)";
  public static final String LockAnnotation_prescrubbedReturnsLock = "ReturnsLock (pre-scrubber, not complete)";
  public static final String EffectsAnnotation_prescrubbedEffects = "method effects promise (pre-scrubbed, not complete) on {0}";
  public static final String NotNullAnnotation_notNullDrop = "NotNull {0} on {1}";
  public static final String UsedByAnnotation_usedByDrop = "UsedBy {0} on {1}";
  public static final String RegionAnnotation_prescrubbedAggregate = "Aggregate promise (pre-scrubbed, not complete)";
  public static final String RegionAnnotation_prescrubbedInRegion = "InRegion promise (pre-scrubbed, not complete)";
  public static final String RegionAnnotation_parentRegionDrop = "Unknown parent region ' {0} ' for {1}";
  public static final String RegionAnnotation_aggregationAllowedDrop = "aggregation allowed because field is declared \"unique\"";
  public static final String RegionAnnotation_aggregationDisallowedDrop = "aggregation disallowed because field is not declared \"unique\"";
  public static final String RegionAnnotation_aggregateDrop = "Aggregate";
  public static final String SubtypedByAnnotation_subtypedByDrop = "SubtypedBy({0}) on {1}";
  public static final String SubtypedByAnnotation_typeDrop = "Type {0} exists and is a subtype of {1}";

  
  
  private Messages() {
    // Private constructor to prevent instantiation
  }
}
