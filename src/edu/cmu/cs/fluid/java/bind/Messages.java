/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/Messages.java,v 1.9 2007/10/31 16:26:37 aarong Exp $*/
package edu.cmu.cs.fluid.java.bind;

public class Messages {
  public static int LockAnnotation_lockModel = 100;

  public static int LockAnnotation_policyLockModel = 101;

  public static String LockAnnotation_prescrubbedLock = "RegionLock {0} (pre-scrubbed, not complete)";

  public static String LockAnnotation_prescrubbedPolicyLock = "PolicyLock {0} (pre-scrubbed, not complete)";

  public static String LockAnnotation_prescrubbedRequiresLock = "RequiresLock  on  {0} (pre-scrubber, not complete)";

  public static String LockAnnotation_prescrubbedReturnsLock = "ReturnsLock (pre-scrubber, not complete)";

  public static int LockAnnotation_requiresLockDrop = 102;

  public static int LockAnnotation_returnsLockDrop = 111;

  public static int LockAnnotation_selfProtectedDrop = 112;
  
  public static int LockAnnotation_notThreadSafeDrop = 109;
  
  public static int LockAnnotation_immutableDrop = 106;

  public static int LockAnnotation_singleThreadedDrop = 113;

  public static String EffectsAnnotation_prescrubbedEffects = "method effects promise (pre-scrubbed, not complete) on {0}";

  public static int AssumeFinalAnnotation_finalFieldDrop = 104;

  public static String NotNullAnnotation_notNullDrop = "NotNull {0} on {1}";

  public static String UsedByAnnotation_usedByDrop = "UsedBy {0} on {1}";

  public static String RegionAnnotation_prescrubbedAggregate = "Aggregate promise (pre-scrubbed, not complete)";

  public static String RegionAnnotation_prescrubbedInRegion = "InRegion promise (pre-scrubbed, not complete)";

  public static int RegionAnnotation_regionDrop = 117;

  public static int RegionAnnotation_mapFieldsDrop = 108;

  public static String RegionAnnotation_parentRegionDrop = "Unknown parent region ' {0} ' for {1}";

  public static int RegionAnnotation_inRegionDrop = 107;

  public static String RegionAnnotation_aggregationAllowedDrop = "aggregation allowed because field is declared \"unique\"";

  public static String RegionAnnotation_aggregationDisallowedDrop = "aggregation disallowed because field is not declared \"unique\"";

  public static String RegionAnnotation_aggregateDrop = "Aggregate";
  
  public static int RegionAnnotation_aggregateInRegionDrop = 103;

  public static int StartsAnnotation_startNothingDrop = 114;

  public static String SubtypedByAnnotation_subtypedByDrop = "SubtypedBy({0})  on  {1}";

  public static String SubtypedByAnnotation_typeDrop = "Type {0} exists and is a subtype of {1}";

  public static int UniquenessAnnotation_uniqueDrop1 = 115;

  public static int UniquenessAnnotation_uniqueDrop2 = 116;

  public static int UniquenessAnnotation_borrowedDrop = 105;
  
  public static int UniquenessAnnotation_notUniqueDrop = 110;

  private Messages() {
    // Private constructor to prevent instantiation
  }
}
