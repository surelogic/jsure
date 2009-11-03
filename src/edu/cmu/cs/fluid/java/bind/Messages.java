/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/Messages.java,v 1.9 2007/10/31 16:26:37 aarong Exp $*/
package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.util.AbstractMessages;

public class Messages extends AbstractMessages {
  
  private static final String BUNDLE_NAME = "edu.cmu.cs.fluid.java.bind.messages"; //$NON-NLS-1$

  public static String LockAnnotation_lockModel = "lock {0} is {1} protects {2}  on  {3}";

  public static String LockAnnotation_policyLockModel = "policy lock {0} is {1}  on  {2}";

  public static String LockAnnotation_prescrubbedLock = "lock {0} (pre-scrubbed, not complete)";

  public static String LockAnnotation_prescrubbedPolicyLock = "policyLock {0} (pre-scrubbed, not complete)";

  public static String LockAnnotation_prescrubbedRequiresLock = "requiresLock  on  {0} (pre-scrubber, not complete)";

  public static String LockAnnotation_prescrubbedReturnsLock = "returnsLock (pre-scrubber, not complete)";

  public static String LockAnnotation_requiresLockDrop = "requiresLock {0}  on  {1}";

  public static String LockAnnotation_returnsLockDrop = "returnsLock {0}  on  {1}";

  public static String LockAnnotation_selfProtectedDrop = "selfProtected  on  {0}";

  public static String LockAnnotation_singleThreadedDrop = "SingleThreaded  on  {0}";

  public static String EffectsAnnotation_prescrubbedEffects = "method effects promise (pre-scrubbed, not complete) on {0}";

  public static String AssumeFinalAnnotation_finalFieldDrop = "assumedFinal on {0}";

  public static String NotNullAnnotation_notNullDrop = "notNull {0} on {1}";

  public static String UsedByAnnotation_usedByDrop = "usedBy {0} on {1}";

  public static String RegionAnnotation_prescrubbedAggregate = "aggregate promise (pre-scrubbed, not complete)";

  public static String RegionAnnotation_prescrubbedInRegion = "inRegion promise (pre-scrubbed, not complete)";

  public static String RegionAnnotation_regionDrop = "region {0}{1} {2}";

  public static String RegionAnnotation_mapFieldsDrop = "InRegion({0} into {1})";

  public static String RegionAnnotation_parentRegionDrop = "Unknown parent region ' {0} ' for {1}";

  public static String RegionAnnotation_inRegionDrop = "InRegion({0}) on {1}";

  public static String RegionAnnotation_aggregationAllowedDrop = "aggregation allowed because field is declared \"unique\"";

  public static String RegionAnnotation_aggregationDisallowedDrop = "aggregation disallowed because field is not declared \"unique\"";

  public static String RegionAnnotation_aggregateDrop = "aggregate";
  
  public static String RegionAnnotation_aggregateInRegionDrop = "AggregateInRegion({0}) on {1}";

  public static String StartsAnnotation_startNothingDrop = "starts(nothing)  on  {0}";

  public static String SubtypedByAnnotation_subtypedByDrop = "subtypedBy({0})  on  {1}";

  public static String SubtypedByAnnotation_typeDrop = "Type {0} exists and is a subtype of {1}";

  public static String UniquenessAnnotation_uniqueDrop1 = "unique  on  {0}";

  public static String UniquenessAnnotation_uniqueDrop2 = "unique({0})  on  {1}";

  public static String UniquenessAnnotation_borrowedDrop = "borrowed({0})  on  {1}";
  
  public static String UniquenessAnnotation_notUniqueDrop = "notunique  on  {0}";

  static {
    // initialize resource bundle
    load(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
    // Private constructor to prevent instantiation
  }
}
