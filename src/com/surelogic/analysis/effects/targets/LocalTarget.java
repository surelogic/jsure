package com.surelogic.analysis.effects.targets;

import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;


/*
 * 99 Feb 23 Remove iwAnything() because I removed the AnythingTarget class.
 * Added implementation of equals() and hashCode()
 */

/*
 * 98 Sept 11 Removed iwArrayElt because I removed the ArrayEltTarget class
 */

/**
 * Representation of a use of local variable or a formal parameter. Can only
 * intersect with another LocalTarget that represents an alias of this Target.
 *
 * <em>These Target classes are a disaster.  They need to be redone in a more
 * understandable way.  I've spent the last 8 years trying to make them 
 * make sense, I don't really feel like I've succeeded.  Someone who is smarter 
 * than I am needs to fix this.  The problem is that Java has very bad
 * support for union types.</em> &mdash; Aaron Greenhouse, 18 Oct 2006.
 *
 * @author Aaron Greenhouse
 */
public final class LocalTarget extends AbstractTarget {
  private final IRNode var;

  /**
	 * Create a new local target.
	 * 
	 * @param v
	 *          IRNode of the declaration of the local or parameter. For the
	 *          receiver it should be the special ReceiverDeclaration node.
	 */
  // Force use of the target factories
  LocalTarget(final IRNode v) {
    super();
    if (v == null) {
      LOG.log(
        Level.SEVERE,
        "Got a null variable for local target",
        new Throwable("For stack trace"));
    }
    var = v;
  }
  
  public boolean isMaskable(final IBinder binder) {
    // Local targets are always maskable
    return true;
  }

  public boolean overlapsReceiver(final IRNode rcvrNode) {
    return false;
  }

  public boolean checkTgt(final IBinder b, final Target declaredTarget) {
    return ((AbstractTarget) declaredTarget).checkTargetAgainstLocal(b, this);
  }

  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstLocal(
      final IBinder b, final LocalTarget actualTarget) {
    /* Doesn't make sense because we should not have a local target in the 
     * declared effects.
     */
    throw new UnsupportedOperationException(
        "Doesn't make sense to use this method on a local target");
  }
  
  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstAnyInstance(
      final IBinder b, final AnyInstanceTarget actualTarget) {
    /* Doesn't make sense because we should not have a local target in the 
     * declared effects.
     */
    throw new UnsupportedOperationException(
        "Doesn't make sense to use this method on a local target");
  }
  
  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstClass(
      final IBinder b, final ClassTarget actualTarget) {
    /* Doesn't make sense because we should not have a local target in the 
     * declared effects.
     */
    throw new UnsupportedOperationException(
        "Doesn't make sense to use this method on a local target");
  }
  
  // Receiver is the target from the declared effect
  @Override
  boolean checkTargetAgainstInstance(
      final IBinder b, final InstanceTarget actualTarget) {
    /* Doesn't make sense because we should not have a local target in the 
     * declared effects.
     */
    throw new UnsupportedOperationException(
        "Doesn't make sense to use this method on a local target");
  }

  public TargetRelationship overlapsWith(
      final IAliasAnalysis.Method am, final IBinder binder, final Target t) {
    return ((AbstractTarget) t).overlapsWithLocal(am, binder, this);
  }

  // t is the receiver, and thus TARGET A in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithLocal(
      final IAliasAnalysis.Method am, final IBinder binder, final LocalTarget t) {
    if (var.equals(t.var)) {
      return TargetRelationship.newSameVariable();
    } else {
      return TargetRelationship.newUnrelated();
    }
  }

  // t is the receiver, and thus TARGET A in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithAnyInstance(
      final IAliasAnalysis.Method am, final IBinder binder, final AnyInstanceTarget t) {
    return TargetRelationship.newUnrelated();
  }

  // t is the receiver, and thus TARGET A in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithClass(
      final IAliasAnalysis.Method am, final IBinder binder, final ClassTarget t) {
    return TargetRelationship.newUnrelated();
  }

  // t is the receiver, and thus TARGET A in the original overlapsWith() call!
  @Override
  TargetRelationship overlapsWithInstance(
      final IAliasAnalysis.Method am, final IBinder binder, final InstanceTarget t) {
    return TargetRelationship.newUnrelated();
  }

  /**
	 * Get a String representation of the Target.
	 * 
	 * @return A String of the form "<TT>&lt;Local</tt><i>V</i><tt>(
	 *         </tt><i>H</i><tt>)</tt>, where <i>V</i> is a String
	 *         represenation of the variable use and <i>H</I> is the hashcode
	 *         of the IRNode representing the variable use
	 */
  @Override
  public StringBuilder toString(final StringBuilder sb) {
    final Operator op = JJNode.tree.getOperator(var);
    if (ParameterDeclaration.prototype.includes(op)) {
      sb.append(ParameterDeclaration.getId(var));
    } else if (ReceiverDeclaration.prototype.includes(op)) {
      sb.append("this");
    } else if (QualifiedReceiverDeclaration.prototype.includes(op)) {
      sb.append(JavaNames.unparseType(QualifiedReceiverDeclaration.getBase(var)));
      sb.append(".this");
    }
    return sb;
  }
  
  /**
	 * Compare two local targets. Two local targets are equal if the refer to the
	 * same region.
	 */
  @Override
  public boolean equals(final Object o) {
    if (o instanceof LocalTarget) {
      final LocalTarget t = (LocalTarget) o;
      return var.equals(t.var);
    }
    return false;
  }

  /**
	 * Get the hashcode of a local target.
	 * 
	 * @return The hashcode, which is the hashcode of the region.
	 */
  @Override
  public int hashCode() {
    return var.hashCode();
  }
}
