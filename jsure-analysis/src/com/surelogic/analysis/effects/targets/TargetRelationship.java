package com.surelogic.analysis.effects.targets;

import com.surelogic.analysis.regions.RegionRelationships;

/**
 * Encapsulation of the result of comparing two targets for overlap. Includes
 * rationale for why they overlap (if they do).
 */
public final class TargetRelationship {
  /**
   * When the relationship does not need a region relationship as part of its
   * reasoning, the region relationship attribute of the object will have this
   * value.
   */
  public static final RegionRelationships NOT_APPLICABLE = null;

  /**
   * Private prototype value for the unrelated relationship.
   */
  private static final TargetRelationship UNRELATED =
    new TargetRelationship(0, TargetRelationships.UNRELATED);

  /**
   * Private prototype value for the same variable relationship.
   */
  private static final TargetRelationship SAME_VARIABLE =
    new TargetRelationship(1, TargetRelationships.SAME_VARIABLE);

  /**
   * Private prototypes for the aliased relationship; note that we do not have
   * an element for {@link RegionRelationships#INCOMPARABLE} because that would
   * result in a two unrelated targets.  We have a null first element instead,
   * to avoid off-by-one ArrayIndexOutOfBoundsExceptions.
   */
  private static final TargetRelationship[] ALIASED = new TargetRelationship[] {
      null,
      new TargetRelationship(2, TargetRelationships.POSSIBLE_ALIASING, RegionRelationships.EQUAL),
      new TargetRelationship(3, TargetRelationships.POSSIBLE_ALIASING, RegionRelationships.REGION_A_INCLUDES_REGION_B),
      new TargetRelationship(4, TargetRelationships.POSSIBLE_ALIASING, RegionRelationships.REGION_B_INCLUDES_REGION_A) };

  /**
   * Private prototypes for the a-is-larger relationship; note that we do not have
   * an element for {@link RegionRelationships#INCOMPARABLE} because that would
   * result in a two unrelated targets.  We have a null first element instead,
   * to avoid off-by-one ArrayIndexOutOfBoundsExceptions.
   */
  private static final TargetRelationship[] A_IS_LARGER = new TargetRelationship[] {
      null,
      new TargetRelationship(5, TargetRelationships.TARGET_A_IS_LARGER, RegionRelationships.EQUAL),
      new TargetRelationship(6, TargetRelationships.TARGET_A_IS_LARGER, RegionRelationships.REGION_A_INCLUDES_REGION_B),
      new TargetRelationship(7, TargetRelationships.TARGET_A_IS_LARGER, RegionRelationships.REGION_B_INCLUDES_REGION_A) };

  /**
   * Private prototypes for the a-is-larger relationship; note that we do not have
   * an element for {@link RegionRelationships#INCOMPARABLE} because that would
   * result in a two unrelated targets.  We have a null first element instead,
   * to avoid off-by-one ArrayIndexOutOfBoundsExceptions.
   */
  private static final TargetRelationship[] B_IS_LARGER = new TargetRelationship[] {
      null,
      new TargetRelationship(8, TargetRelationships.TARGET_B_IS_LARGER, RegionRelationships.EQUAL),
      new TargetRelationship(9, TargetRelationships.TARGET_B_IS_LARGER, RegionRelationships.REGION_A_INCLUDES_REGION_B),
      new TargetRelationship(10, TargetRelationships.TARGET_B_IS_LARGER, RegionRelationships.REGION_B_INCLUDES_REGION_A) };

  /**
   * List of all TargetRelationship objects in order. Used only by
   * {@link EffectRelationship} to index private arrays so that it can index
   * arrays of preconstructed effect relationship objects.
   */
  public static final TargetRelationship[] ALL = { UNRELATED, SAME_VARIABLE,
      ALIASED[0], ALIASED[1], ALIASED[2], A_IS_LARGER[0], A_IS_LARGER[1],
      A_IS_LARGER[2], B_IS_LARGER[0], B_IS_LARGER[1], B_IS_LARGER[2] };

  /**
   * Order in the list of target relationships. Used only by
   * {@link EffectRelationship} to index private arrays so that it can index
   * arrays of preconstructed effect relationship objects.
   */
  public final int index;
  
  /** Encapsulated target relationship value. */
  private final TargetRelationships targetResult;

  /**
   * Region relationship value describing the relationship between the regions
   * of the two targets that serves as justification for the value of
   * {@link #targetResult}.
   */
  private final RegionRelationships regionResult;

  
  
  /**
   * Create a new relationship object. This constructor is private to force the
   * use of the static object factory methods.
   */
  private TargetRelationship(final int idx,
      final TargetRelationships tr, final RegionRelationships rr) {
    index = idx;
    targetResult = tr;
    regionResult = rr;
  }

  /**
   * Create a new relationship object that does not require the region
   * relationship attribute.
   */
  private TargetRelationship(final int idx, final TargetRelationships tr) {
    this(idx, tr, NOT_APPLICABLE);
  }

  
  
  /**
   * Get a target relationship object that describes unrelated targets.
   */
  public static TargetRelationship unrelated() {
    return UNRELATED;
  }

  /**
   * Get a target relationship object that describes local targets referring to
   * the same variable.
   */
  public static TargetRelationship sameVariable() {
    return SAME_VARIABLE;
  }

  /**
   * Get a target relationship object that describes possibly aliased targets
   * that have the given region relationship.
   */
  public static TargetRelationship aliased(final RegionRelationships rr) {
    return ALIASED[rr.ordinal()];
  }

  /**
   * Get a target relationship object that describes targets such that the first
   * refers to state that includes the state of the second target and the
   * regions have the given relationship.
   */
  public static TargetRelationship aIsLarger(final RegionRelationships rr) {
    return A_IS_LARGER[rr.ordinal()];
  }

  /**
   * Get a target relationship object that describes targets such that the
   * second refers to state that includes the state of the first target and the
   * regions have the given relationship.
   */
  public static TargetRelationship bIsLarger(final RegionRelationships rr) {
    return B_IS_LARGER[rr.ordinal()];
  }

  
  
  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof TargetRelationship) {
      final TargetRelationship tr = (TargetRelationship) other;
      // NB. Can do this because the fields refer to enumerations
      return regionResult == tr.regionResult &&
          targetResult == tr.targetResult;
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    int hash = 17;
    hash += 31 * hash + ((regionResult == null) ? 0 : regionResult.hashCode());
    hash += 31 * hash + ((targetResult == null) ? 0 : targetResult.hashCode());
    return hash;
  }
  
  
  
  @Override
  public String toString() {
    if (regionResult != NOT_APPLICABLE) {
      return targetResult + " and " + regionResult;
    } else {
      return targetResult.toString();
    }
  }

  /** Get the target relationship value. */
  public TargetRelationships getTargetRelationship() {
    return targetResult;
  }

  /** Get the associated region relationship value. */
  public RegionRelationships getRegionRelationship() {
    return regionResult;
  }
}
