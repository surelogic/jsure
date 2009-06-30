package com.surelogic.analysis.effects;

import com.surelogic.analysis.effects.targets.TargetRelationship;

/**
 * Encapsulation of the result of comparing two effects for conflict. Includes
 * rationale for why they conflict (if they do).
 */
public final class EffectRelationship {
  /**
   * Used as the value of the target relationship for effects relationships that
   * do not require further explanation.
   */
  public static final TargetRelationship NOT_APPLICABLE = null;

  /**
   * Prototype value for the no conflict relationship.
   */
  private static final EffectRelationship NO_CONFLICT =
    new EffectRelationship(EffectRelationships.NO_CONFLICT);

  /**
   * Private prototypes for the write-includes-write relationship; note that we
   * do not have an element for unrelated target relationships because that
   * would result in a two unrelated effects targets. We have a null first
   * element instead, to avoid off-by-one ArrayIndexOutOfBoundsExceptions.
   */
  private static final EffectRelationship[] WRITES = new EffectRelationship[] {
      null,
      new EffectRelationship(EffectRelationships.WRITE_INCLUDES_WRITE, TargetRelationship.ALL[1]), 
      new EffectRelationship(EffectRelationships.WRITE_INCLUDES_WRITE, TargetRelationship.ALL[2]), 
      new EffectRelationship(EffectRelationships.WRITE_INCLUDES_WRITE, TargetRelationship.ALL[3]), 
      new EffectRelationship(EffectRelationships.WRITE_INCLUDES_WRITE, TargetRelationship.ALL[4]), 
      new EffectRelationship(EffectRelationships.WRITE_INCLUDES_WRITE, TargetRelationship.ALL[5]), 
      new EffectRelationship(EffectRelationships.WRITE_INCLUDES_WRITE, TargetRelationship.ALL[6]), 
      new EffectRelationship(EffectRelationships.WRITE_INCLUDES_WRITE, TargetRelationship.ALL[7]), 
      new EffectRelationship(EffectRelationships.WRITE_INCLUDES_WRITE, TargetRelationship.ALL[8]), 
      new EffectRelationship(EffectRelationships.WRITE_INCLUDES_WRITE, TargetRelationship.ALL[9]), 
      new EffectRelationship(EffectRelationships.WRITE_INCLUDES_WRITE, TargetRelationship.ALL[10])
  };

  /**
   * Private prototypes for the reads-a-writes-b relationship; note that we do
   * not have an element for unrelated target relationships because that would
   * result in a two unrelated effects targets. We have a null first element
   * instead, to avoid off-by-one ArrayIndexOutOfBoundsExceptions.
   */
  private static final EffectRelationship[] READS_A = new EffectRelationship[] {
      null,
      new EffectRelationship(EffectRelationships.WRITE_B_INCLUDES_READ_A, TargetRelationship.ALL[1]), 
      new EffectRelationship(EffectRelationships.WRITE_B_INCLUDES_READ_A, TargetRelationship.ALL[2]), 
      new EffectRelationship(EffectRelationships.WRITE_B_INCLUDES_READ_A, TargetRelationship.ALL[3]), 
      new EffectRelationship(EffectRelationships.WRITE_B_INCLUDES_READ_A, TargetRelationship.ALL[4]), 
      new EffectRelationship(EffectRelationships.WRITE_B_INCLUDES_READ_A, TargetRelationship.ALL[5]), 
      new EffectRelationship(EffectRelationships.WRITE_B_INCLUDES_READ_A, TargetRelationship.ALL[6]), 
      new EffectRelationship(EffectRelationships.WRITE_B_INCLUDES_READ_A, TargetRelationship.ALL[7]), 
      new EffectRelationship(EffectRelationships.WRITE_B_INCLUDES_READ_A, TargetRelationship.ALL[8]), 
      new EffectRelationship(EffectRelationships.WRITE_B_INCLUDES_READ_A, TargetRelationship.ALL[9]), 
      new EffectRelationship(EffectRelationships.WRITE_B_INCLUDES_READ_A, TargetRelationship.ALL[10])
  };

  /**
   * Private prototypes for the writes-a-reads-b relationship; note that we do
   * not have an element for unrelated target relationships because that would
   * result in a two unrelated effects targets. We have a null first element
   * instead, to avoid off-by-one ArrayIndexOutOfBoundsExceptions.
   */
  private static final EffectRelationship[] READS_B = new EffectRelationship[] {
      null,
      new EffectRelationship(EffectRelationships.WRITE_A_INCLUDES_READ_B, TargetRelationship.ALL[1]), 
      new EffectRelationship(EffectRelationships.WRITE_A_INCLUDES_READ_B, TargetRelationship.ALL[2]), 
      new EffectRelationship(EffectRelationships.WRITE_A_INCLUDES_READ_B, TargetRelationship.ALL[3]), 
      new EffectRelationship(EffectRelationships.WRITE_A_INCLUDES_READ_B, TargetRelationship.ALL[4]), 
      new EffectRelationship(EffectRelationships.WRITE_A_INCLUDES_READ_B, TargetRelationship.ALL[5]), 
      new EffectRelationship(EffectRelationships.WRITE_A_INCLUDES_READ_B, TargetRelationship.ALL[6]), 
      new EffectRelationship(EffectRelationships.WRITE_A_INCLUDES_READ_B, TargetRelationship.ALL[7]), 
      new EffectRelationship(EffectRelationships.WRITE_A_INCLUDES_READ_B, TargetRelationship.ALL[8]), 
      new EffectRelationship(EffectRelationships.WRITE_A_INCLUDES_READ_B, TargetRelationship.ALL[9]), 
      new EffectRelationship(EffectRelationships.WRITE_A_INCLUDES_READ_B, TargetRelationship.ALL[10])
  };
  
  
  
  /**
   * The value that describes the relationship between the targets of the two
   * effects.
   */
  private final TargetRelationship overlap;

  /**
   * Encapsulated relationship between the two effects.
   */
  private final EffectRelationships conflict;

  
  
  /**
   * Create a new effect relationship object. Private to force the use of the
   * static factory methods.
   */
  private EffectRelationship(
      final EffectRelationships er, final TargetRelationship tr) {
    conflict = er;
    overlap = tr;
  }

  /**
   * Create a new effect relationship object that does not require additional
   * explanation.
   */
  private EffectRelationship(final EffectRelationships er) {
    this(er, NOT_APPLICABLE);
  }

  
  
  /**
   * Get a relationship value that describes no conflict.
   */
  public static EffectRelationship newNoConflict() {
    return NO_CONFLICT;
  }

  /**
   * Get a relationship value that describes a conflict between two write
   * effects that have targets related as given.
   */
  public static EffectRelationship newWritesConflict(final TargetRelationship tr) {
    return WRITES[tr.index];
  }

  /**
   * Get a relationship value that describes a conflict between a read and a
   * write effect, where the first effect is a read and the targets have the
   * given relationship.
   */
  public static EffectRelationship newReadAWriteB(final TargetRelationship tr) {
    return READS_A[tr.index];
  }

  /**
   * Get a relationship value that describes a conflict between a read and a
   * write effect, where the second effect is a read and the targets have the
   * given relationship.
   */
  public static EffectRelationship newWriteAReadB(final TargetRelationship tr) {
    return READS_B[tr.index];
  }

  
  
  /** Get the effect relationship value. */
  public EffectRelationships getEffectRelationship() {
    return conflict;
  }

  /** Get the target relationship. */
  public TargetRelationship getTargetRelationship() {
    return overlap;
  }

  /** Query if the relationship is a conflict. */
  public boolean isConflict() {
    return conflict != EffectRelationships.NO_CONFLICT;
  }

  @Override
  public String toString() {
    if (overlap != NOT_APPLICABLE) {
      return conflict + " and " + overlap;
    } else {
      return conflict.toString();
    }
  }
}
