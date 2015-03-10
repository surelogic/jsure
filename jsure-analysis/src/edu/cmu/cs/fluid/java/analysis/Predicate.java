package edu.cmu.cs.fluid.java.analysis;

import java.util.Iterator;
import java.util.Hashtable;

import com.surelogic.analysis.effects.Effect;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.version.Version;

/** A predicate is a statement that is valid at some control point
 * in the program.  A somewhat out-of-date explanation is given
 * in <a href="/afs/cs/proj/fluid/www/work/fluid-properties.html">informal
 * documentation</a>.
 * <p> Predicates are cached and so must define <tt>equals</tt> and
 * <tt>hashCode</tt> methods.
 * @see EqPredicate
 * @see TrivialPredicate
 */
public abstract class Predicate {
  protected Predicate() {
  }

  /** Canonicalize the predicate.
   * For example, if trivially false, return TrivialPredicate.falsePredicate.
   */
  public Predicate canonicalize() {
    return this;
  }

  /** Return a comparison with another canonicalized predicate
   * (according to current version): <ul>
   * <li> <tt>Logic.EQUAL</tt> if the predicates are equal
   * <li> <tt>Logic.IMPLIES</tt> if this is stronger than its argument
   * <li> <tt>Logic.IMPLIED</tt> if this is weaker than its argument
   * <li> <tt>Logic.INVERSE</tt> if the two predicates are inverses
   * <li> <tt>Logic.CONFLICT</tt> if the two predicates cannot both be true
   * <li> <tt>Logic.COMPLEMENT</tt> if together they span truth
   * <li> <tt>Logic.ANDORABLE</tt> if both conjunction and disjunction
   *                               can be formed as Predicate objects
   * <li> <tt>Logic.ANDABLE</tt> if the conjunction can be formed
   * <li> <tt>Logic.ORABLE</tt> if the disjunction can be formed
   * <li> <tt>Logic.INCOMPARABLE</tt> if the predicates are incomparable
   * </ul>
   */
  public abstract int compare(Predicate other);

  /** Compare two predicates for a positive relation.
   * Do not use with <tt>Logic.INCOMPARABLE</tt>
   */
  public boolean compare(Predicate other, int mask) {
    return (compare(other)&mask)==mask;
  }
  
  /** Conjoin two ANDABLE predicates.
   */
  public Predicate and(Predicate other) {
    // default implementation
    switch (compare(other)) {
    case Logic.EQUAL:
    case Logic.IMPLIES:
      return this;
    case Logic.IMPLIED:
      return other;
    case Logic.CONFLICT:
    case Logic.INVERSE:
      return TrivialPredicate.falsePredicate;
    default:
      throw new FluidRuntimeException("cannot handle and");
    }
  }

  /** Disjoin two ORABLE predicates.
   */
  public Predicate or(Predicate other) {
    // default implementation
    switch (compare(other)) {
    case Logic.EQUAL:
    case Logic.IMPLIED:
      return this;
    case Logic.IMPLIES:
      return other;
    case Logic.COMPLEMENT:
    case Logic.INVERSE:
      return TrivialPredicate.truePredicate;
    default:
      throw new FluidRuntimeException("cannot handle or");
    }
  }

  /** Return any mutually implied predicates (other than trivial ones).
   */
  public Iterator generate(Predicate other) {
    // default implementation:
    return new EmptyIterator<Object>();
  }

  /** Return the effects of executing a predicate.
   * This set should only include read effects!
   * It is used to determine when a predicate is "killed"
   * by some effectful computation.
   */
  public abstract java.util.Set<Effect> effects();

  private static final Hashtable<Predicate,Predicate> ht = new Hashtable<Predicate,Predicate>();
  private static Version lastVersion = null;

  /** Generate a unique reference for a predicate. */
  public static Predicate cache(Predicate p) {
    /* The cache must be cleared everytime we have a new version,
     * because canonicalization may be affected.
     *?? Alternately, we could keep a cache of hashtables for
     *?? certain versions.
     */
    if (lastVersion != Version.getVersion()) {
      lastVersion = Version.getVersion();
      ht.clear();
    }
    Predicate cached = ht.get(p);
    if (cached == null) {
      cached = p.canonicalize();
      ht.put(p,cached);
    }
    return cached;
  }

  public static void clearCache() {
    ht.clear();
  }
}
  
