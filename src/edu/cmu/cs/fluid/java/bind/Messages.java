/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/Messages.java,v 1.9 2007/10/31 16:26:37 aarong Exp $*/
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.util.AbstractMessages;

public class Messages extends AbstractMessages {
  
  private static final String BUNDLE_NAME = "edu.cmu.cs.fluid.java.bind.messages"; //$NON-NLS-1$

  public static String LockAnnotation_lockModel = "RegionLock {0} is {1} protects {2}  on  {3}";

  public static String LockAnnotation_policyLockModel = "PolicyLock {0} is {1}  on  {2}";

  public static String LockAnnotation_prescrubbedLock = "RegionLock {0} (pre-scrubbed, not complete)";

  public static String LockAnnotation_prescrubbedPolicyLock = "PolicyLock {0} (pre-scrubbed, not complete)";

  public static String LockAnnotation_prescrubbedRequiresLock = "RequiresLock  on  {0} (pre-scrubber, not complete)";

  public static String LockAnnotation_prescrubbedReturnsLock = "ReturnsLock (pre-scrubber, not complete)";

  public static String LockAnnotation_requiresLockDrop = "{0}  on  {1}";

  public static String LockAnnotation_returnsLockDrop = "ReturnsLock {0}  on  {1}";

  public static String LockAnnotation_selfProtectedDrop = "ThreadSafe  on  {0}";
  
  public static String LockAnnotation_notThreadSafeDrop = "NotThreadSafe  on  {0}";
  
  public static String LockAnnotation_immutableDrop = "Immutable  on  {0}";

  public static String LockAnnotation_singleThreadedDrop = "SingleThreaded  on  {0}";

  public static String EffectsAnnotation_prescrubbedEffects = "method effects promise (pre-scrubbed, not complete) on {0}";

  public static String AssumeFinalAnnotation_finalFieldDrop = "AssumedFinal on {0}";

  public static String NotNullAnnotation_notNullDrop = "NotNull {0} on {1}";

  public static String UsedByAnnotation_usedByDrop = "UsedBy {0} on {1}";

  public static String RegionAnnotation_prescrubbedAggregate = "Aggregate promise (pre-scrubbed, not complete)";

  public static String RegionAnnotation_prescrubbedInRegion = "InRegion promise (pre-scrubbed, not complete)";

  public static String RegionAnnotation_regionDrop = "Region {0}{1} {2}";

  public static String RegionAnnotation_mapFieldsDrop = "InRegion({0} into {1})";

  public static String RegionAnnotation_parentRegionDrop = "Unknown parent region ' {0} ' for {1}";

  public static String RegionAnnotation_inRegionDrop = "InRegion({0}) on {1}";

  public static String RegionAnnotation_aggregationAllowedDrop = "aggregation allowed because field is declared \"unique\"";

  public static String RegionAnnotation_aggregationDisallowedDrop = "aggregation disallowed because field is not declared \"unique\"";

  public static String RegionAnnotation_aggregateDrop = "Aggregate";
  
  public static String RegionAnnotation_aggregateInRegionDrop = "AggregateInRegion({0}) on {1}";

  public static String StartsAnnotation_startNothingDrop = "Starts(nothing)  on  {0}";

  public static String SubtypedByAnnotation_subtypedByDrop = "SubtypedBy({0})  on  {1}";

  public static String SubtypedByAnnotation_typeDrop = "Type {0} exists and is a subtype of {1}";

  public static String UniquenessAnnotation_uniqueDrop1 = "Unique  on  {0}";

  public static String UniquenessAnnotation_uniqueDrop2 = "Unique({0})  on  {1}";

  public static String UniquenessAnnotation_borrowedDrop = "Borrowed({0})  on  {1}";
  
  public static String UniquenessAnnotation_notUniqueDrop = "NotUnique  on  {0}";

  static {
    // initialize resource bundle
    load(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
    // Private constructor to prevent instantiation
  }
}
