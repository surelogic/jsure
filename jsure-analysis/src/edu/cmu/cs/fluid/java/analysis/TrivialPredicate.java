package edu.cmu.cs.fluid.java.analysis;

import java.util.Collections;
import java.util.Set;

import com.surelogic.analysis.effects.Effect;

/** Predicates that are always true or always false.
 * Used as placeholders.
 */
public class TrivialPredicate extends Predicate {
  private final boolean truth;

  private TrivialPredicate(boolean t) { super(); truth = t; }

  public boolean isTrue() { return truth; }

  @Override
  public boolean equals(Object other) {
    return other instanceof TrivialPredicate &&
      ((TrivialPredicate)other).truth == truth;
  }

  @Override
  public int hashCode() {
    return truth ? 1 : 0;
  }

  @Override
  public String toString() {
    return truth ? "true" : "false";
  }
  
  public static final TrivialPredicate truePredicate =
      new TrivialPredicate(true);
  public static final TrivialPredicate falsePredicate =
      new TrivialPredicate(false);

  @Override
  public Predicate canonicalize() {
    return truth ? truePredicate : falsePredicate;
  }

  public static TrivialPredicate create(boolean truth) {
    return truth ? truePredicate : falsePredicate;
  }
  
  @Override
  public int compare(Predicate other) {
    if (other instanceof TrivialPredicate) {
      TrivialPredicate p = (TrivialPredicate)other;
      return (truth == p.truth) ? Logic.EQUAL : Logic.INVERSE;
    } else {
      return truth ? Logic.IMPLIED : Logic.IMPLIES;
    }
  }

  @Override
  public Set<Effect> effects() {
    return Collections.emptySet();
  }
}
