/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/InsertionPoint.java,v 1.8 2008/06/24 19:13:13 thallora Exp $ */
package edu.cmu.cs.fluid.ir;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

/** An insertion point is a point in a sequence.
 * It can be used to insert a new element into a sequence.
 */
public final class InsertionPoint {
  private static final Logger LOG = SLLogger.getLogger("FLUID.ir");
  private final IRLocation relative;
  private final boolean before;

  /** Create a new insertion point.
   * Example: <code>new InsertionPoint(null,true)</code>
   * The insertion point is at the end of the sequence.
   * @param r location relative to which the IP is defined.
   * If null, it is relative to the start or end of the sequence
   * @param b whether the insertion point is <em>before</em>
   * the location.  Otherwise it is <em>after</em>.
   * @see #createBefore
   * @see #createAfter
   */
  public InsertionPoint(IRLocation r, boolean b) {
    relative = r;
    before = b;
  }

  /** Create an insertion point before the given
   * location.  If the location is null, then create insertion
   * point at end of sequence.
   */
  public static InsertionPoint createBefore(IRLocation r) {
    return new InsertionPoint(r,true);
  }

  /** Create an insertion point after the given
   * location.  If the location is null, then create insertion
   * point at beginning of sequence.
   */
  public static InsertionPoint createAfter(IRLocation r) {
    return new InsertionPoint(r,false);
  }

  public IRLocation getRelativeTo() { return relative; }
  public boolean isBefore() { return before; }

  public static final InsertionPoint first = new InsertionPoint(null,false);
  public static final InsertionPoint last = new InsertionPoint(null,true);

  /** Return the location before this point in the given sequence. */
  public <T> IRLocation before(IRSequence<T> seq) {
    if (relative == null) {
      if (before) {
	return seq.lastLocation();
      } else {
	return null;
      }
    } else {
      if (before) {
	return seq.prevLocation(relative);
      } else {
	return relative;
      }
    }
  }

  /** Return the location just after this point in the given sequence. */
  public <T> IRLocation after(IRSequence<T> seq) {
    if (relative == null) {
      if (before) {
	return null;
      } else {
	return seq.firstLocation();
      }
    } else {
      if (before) {
	return relative;
      } else {
	return seq.nextLocation(relative);
      }
    }
  }

  public <T> IRLocation insert(IRSequence<T> seq, T v) {
    IRLocation result;
    if (relative == null) {
      if (before) {
	    result = seq.appendElement(v);
      } else {
        result = seq.insertElement(v);
      }
    } else {
      if (before) {
        result = seq.insertElementBefore(v,relative);
      } else {
        result = seq.insertElementAfter(v,relative);
      }
    }
    if (result == null && LOG.isLoggable(Level.WARNING)) {
      LOG.warning("shouldn't return null from insert in " + seq);
    }
    return result;
  }
}
