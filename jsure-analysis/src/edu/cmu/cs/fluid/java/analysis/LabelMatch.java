/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/LabelMatch.java,v 1.7 2007/07/05 18:15:14 aarong Exp $ */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.control.TrackLabel;
import edu.cmu.cs.fluid.control.UnknownLabel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.control.*;
import edu.cmu.cs.fluid.java.operator.LabeledBreakStatement;
import edu.cmu.cs.fluid.java.operator.LabeledContinueStatement;
import edu.cmu.cs.fluid.java.operator.LabeledStatement;

/** This class contains matching of generated labels
 * to accepted labels.  It is used for both forward and backward
 * analysis of Java programs.
 * @see JavaForwardTransfer
 * @see JavaBackwardTransfer
 */
public class LabelMatch {
  public static final LabelMatch equality = new LabelMatch(7);
  public static final LabelMatch includes = new LabelMatch(5);
  public static final LabelMatch included = new LabelMatch(3);
  public static final LabelMatch overlaps = new LabelMatch(1);
  public static final LabelMatch disjoint = new LabelMatch(0);

  private final int mask;
  private LabelMatch(int m) { mask = m; }
  @Override
  public String toString() {
    switch (mask) {
    case 0: return "disjoint";
    case 1: return "overlaps";
    case 3: return "included";
    case 5: return "includes";
    case 7: return "equality";
    default: return "??" + mask;
    }
  }

  /** Return true if this is at least as close to equality as its argument.
   * For example <ul>
   * <li> <tt>includes.atLeast(equality)</tt> => false
   * <li> <tt>includes.atLeast(overlaps)</tt> => true
   * <li> <tt>includes.atLeast(included)</tt> => false
   * </ul>
   */
  public boolean atLeast(LabelMatch other) {
    return (mask | other.mask) == mask;
  }

  /** Return true is the argument is between it and equality.
   * For example <ul>
   * <li> <tt>includes.atMost(equality)</tt> => true
   * <li> <tt>includes.atMost(overlaps)</tt> => false
   * <li> <tt>includes.atMost(included)</tt> => false
   * </ul>
   */
  public boolean atMost(LabelMatch other) {
    return (mask & other.mask) == mask;
  }

  /** Check if the label generated at one point of the
   * program could match the label accepted at a later point.
   */
  public static LabelMatch compareLabels(ControlLabel generated,
					 ControlLabel accepted) {
    if (accepted instanceof TrackLabel) {
      if (generated == accepted) {
	return equality;
      } else {
	return disjoint;
      }
    } else if (generated instanceof TrackLabel) {
      return disjoint;
    } else if (accepted instanceof UnknownLabel) {
      return included;
    } else if (accepted instanceof AnchoredBreakLabel) {
      if (generated instanceof NamedBreakLabel) {
	IRNode node = ((AnchoredBreakLabel)accepted).stmtNode;
	IRNode breakNode = ((NamedBreakLabel)generated).breakNode;
	String name = LabeledBreakStatement.getId(breakNode);
	if (name.equals(LabeledStatement.getStatementLabel(node))) {
	  // Assume no duplicate names.
	  return equality;
	} else {
	  return disjoint;
	}
      } else if (generated instanceof BreakLabel) {
	// there may be intervening nodes,
	// but we can't tell
	return included;
      } else {
	// no match!
	return disjoint;
      }
    } else if (accepted instanceof NamedBreakLabel) {
      // for labeled statements only
      if (generated instanceof NamedBreakLabel) {
	IRNode node = ((NamedBreakLabel)accepted).breakNode;
	IRNode breakNode = ((NamedBreakLabel)generated).breakNode;
	String name = LabeledBreakStatement.getId(breakNode);
	if (name.equals(LabeledStatement.getLabel(node))) {
	  // Assume no duplicate names.
	  return equality;
	} else {
	  return disjoint;
	}
      } else {
	return disjoint;
      }
    } else if (accepted instanceof AnchoredContinueLabel) {
      if (generated instanceof NamedContinueLabel) {
	IRNode node = ((AnchoredContinueLabel)accepted).stmtNode;
	IRNode continueNode = ((NamedContinueLabel)generated).continueNode;
	String name = LabeledContinueStatement.getId(continueNode);
	if (name.equals(LabeledStatement.getStatementLabel(node))) {
	  // Assume no duplicate names.
	  return equality;
	} else {
	  return disjoint;
	}
      } else if (generated instanceof ContinueLabel) {
	// there may be intervening nodes,
	// but we can't tell
	// (COULD BE BETTER)
	return included;
      } else {
	// no match!
	return disjoint;
      }
    } else if (accepted instanceof ReturnLabel) {
      if (generated instanceof ReturnLabel) {
	return equality;
      } else {
	return disjoint;
      }
    } else if (accepted instanceof CaughtExceptionLabel) {
      if (generated instanceof ExceptionLabel) {
	// be stupid for now:
	return overlaps;
      } else {
	return disjoint;
      }
    } else {
      // unknown:
      return overlaps;
    }
  }
}
