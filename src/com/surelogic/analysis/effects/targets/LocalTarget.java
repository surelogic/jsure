package com.surelogic.analysis.effects.targets;

import java.util.logging.Level;

import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.NameType;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.java.operator.TypeRef;
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

  @Override
  public Kind getKind() {
    return Target.Kind.LOCAL_TARGET;
  }
  
  public boolean isMaskable(final IBinder binder) {
    // Local targets are always maskable
    return true;
  }

  public boolean overlapsReceiver(final IRNode rcvrNode) {
    return false;
  }

  /**
   * Illegal operation on local targets.
   */
  public boolean checkTgt(final IBinder b, final Target t) {
    throw new UnsupportedOperationException("Doesn't make sense to use this method on a local target");
  }

  @Override
  TargetRelationship owLocal(LocalTarget t) {
    if (var.equals(t.var)) {
      return TargetRelationship.newSameVariable();
    } else {
      return TargetRelationship.newUnrelated();
    }
  }

  @Override
  TargetRelationship owAnyInstance(
    final IBinder b, final IJavaType c, final IRegion reg) {
    return TargetRelationship.newUnrelated();
  }

  @Override
  TargetRelationship owClass(final IBinder binder, final IRegion reg) {
    return TargetRelationship.newUnrelated();
  }

  @Override
  TargetRelationship owInstance(
    final IAliasAnalysis.Method am, final IBinder binder, final IRNode ref, final IRegion reg) {
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
