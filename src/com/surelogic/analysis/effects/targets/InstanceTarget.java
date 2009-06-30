package com.surelogic.analysis.effects.targets;

import java.util.logging.Level;

import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.regions.RegionRelationships;
import com.surelogic.annotation.rules.RegionRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.AllocationExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.OuterObjectSpecifier;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.tree.Operator;

/*
 * 99 Feb 23 Remove iwAnything() because I removed the AnythingTarget class.
 * Added implementation of equals() and hashCode()
 */

/*
 * 98 Sept 11 Removed iwArrayElt because I removed the ArrayEltTarget class
 */

/**
 * <em>These Target classes are a disaster.  They need to be redone in a more
 * understandable way.  I've spent the last 8 years trying to make them 
 * make sense, I don't really feel like I've succeeded.  Someone who is smarter 
 * than I am needs to fix this.  The problem is that Java has very bad
 * support for union types.</em> &mdash; Aaron Greenhouse, 18 Oct 2006.
 */
/* I only want this class to be usable by the TargetFactory implementations */
public final class InstanceTarget extends AbstractTarget {
  private IRNode reference;

  // Force use of the target factories
  InstanceTarget(final IRNode object, final IRegion field) {
    super(field);
    
    /* I've had this error too many times. */
    final Operator op = JJNode.tree.getOperator(object);
    if (ThisExpression.prototype.includes(op) || SuperExpression.prototype.includes(op)) {
      throw new IllegalArgumentException("The object expression cannot be a ThisExpression or SuperExpression: use ReceiverDeclaration instead");
    } else if (QualifiedThisExpression.prototype.includes(op)) {
      throw new IllegalArgumentException("The object expression cannot be a QualifiedThisExpression: use QualifiedReceiverDeclaration instead");
    }
    
    reference = object;
  }

  @Override
  public Kind getKind() {
    return Target.Kind.INSTANCE_TARGET;
  }
  
  public boolean isMaskable(final IBinder binder) {
    IRNode expr = reference;
    Operator exprOp = JJNode.tree.getOperator(expr);
    if (Initialization.prototype.includes(exprOp)) {
      expr = Initialization.getValue(expr);
      exprOp = JJNode.tree.getOperator(expr);
    }
    
    /* Expression is "unique" if it is a FieldRef (e.f), f is a unique field,
     * and region mappings exist for the field f.
     */
    final boolean isUnique;
    if (FieldRef.prototype.includes(exprOp)) {
      final IRNode fieldID = binder.getBinding(expr);
      if (UniquenessRules.isUnique(fieldID)) {
        isUnique = (RegionRules.getAggregate(fieldID) != null);
      } else {
        isUnique = false;
      }
    } else {
      isUnique = false;
    }

    if (isUnique) {
      // Filter out unique field accesses (handle by elaboration)
      return true;
    } else if (VariableUseExpression.prototype.includes(exprOp)) {
      // Throw out; handled by elaboration
      return true;
    } else if (AllocationExpression.prototype.includes(exprOp)) {
      /* Filter out instance targets with allocation expressions:
       * The newly allocated state is unknown in the calling context.
       */
      return true;
    } else if (OuterObjectSpecifier.prototype.includes(exprOp)) {
      /* The expression must be of the form "o. new C(...)", which is an
       * allocation expression, so we can ignore it.  (OuterObjectSpecifier 
       * could also be "o. super(...)", but that is impossible in this context
       * because it wouldn't ever be returned by BCA.)
       */
      return true;
    }
                        
    /* We leave QualifiedReceiverDeclarations because they do refer to
     * objects visible outside the context.
     */
    return false;
  }

  public boolean overlapsReceiver(final IRNode rcvrNode) {
    /* XXX: Are we guaranteed that the instance region is always represented by
     * the same object for the life of JSure?  If so, we can cache the instance
     * region.
     */
    return reference.equals(rcvrNode) && 
      RegionModel.getInstance(RegionModel.INSTANCE).ancestorOf(region);
  }

  @Override
  public IRNode getReference() {
    return reference;
  }

  /**
	 * TODO: Say something intelligent here!
	 */
  public boolean checkTgt(final IBinder b, final Target t) {
    if (t.getKind() == Target.Kind.INSTANCE_TARGET) {
      /*
       * t must be of the form p.rgn, where p is a parameter declaration or
       * ReceiverDeclaration. We will only check if we are also of the form
       * q.rgn', where p == q, rgn' is a descendant of rgn.
       */
      final Operator op = JJNode.tree.getOperator(reference);
      if (ParameterDeclaration.prototype.includes(op)
        || QualifiedReceiverDeclaration.prototype.includes(op) 
        || ReceiverDeclaration.prototype.includes(op)) {
        if (reference.equals(t.getReference())
          && t.getRegion().ancestorOf(region)) {
          return true;
        }
      }
    } else if (t.getKind() == Target.Kind.ANY_INSTANCE_TARGET) {
      final IJavaType clazz = b.getJavaType(reference);
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("type of reference in " + this + " is " + clazz.getName());
      }
      if (areDirectlyRelated(b, clazz, ((AnyInstanceTarget) t).clazz)
        && t.getRegion().ancestorOf(region)) {
        return true;
      }
    } else if (t.getKind() == Target.Kind.CLASS_TARGET) {
      if (t.getRegion().ancestorOf(region)) {
        return true;
      }
    }

    return false;
  }

  @Override
  TargetRelationship owLocal(final LocalTarget t) {
    return TargetRelationship.newUnrelated();
  }

  @Override
  TargetRelationship owInstance(
    final IAliasAnalysis.Method am, final IBinder binder, final IRNode ref, final IRegion reg) {
    if (am.aliases(reference, ref)) {
      if (region.equals(reg)) {
        return TargetRelationship.newAliased(RegionRelationships.EQUAL);
      } else if (region.ancestorOf(reg)) {
        return TargetRelationship.newAliased(
          RegionRelationships.REGION_A_INCLUDES_REGION_B);
      } else if (reg.ancestorOf(region)) {
        return TargetRelationship.newAliased(
          RegionRelationships.REGION_B_INCLUDES_REGION_A);
      }
    }
    return TargetRelationship.newUnrelated();
  }

  @Override
  TargetRelationship owAnyInstance(
    final IBinder binder, final IJavaType clazz, final IRegion reg) {
    /* NB. page 229 of ECOOP paper says we should check that Instance target
     * is shared (!unique). I think this because we want to make sure that
     * overlap is based on the aggregated region hierarchy. We don't have to
     * check this here because we are assuming that effects have already
     * been elaborated and masked, and thus aggregation relationships have
     * already been resolved.
     */
    if (areDirectlyRelated(binder, binder.getJavaType(reference), clazz)) {
      if (region.equals(reg)) {
        return TargetRelationship.newAliased(RegionRelationships.EQUAL);
      } else if (region.ancestorOf(reg)) {
        return TargetRelationship.newAliased(
          RegionRelationships.REGION_A_INCLUDES_REGION_B);
      } else if (reg.ancestorOf(region)) {
        return TargetRelationship.newAliased(
          RegionRelationships.REGION_B_INCLUDES_REGION_A);
      }
    }
    return TargetRelationship.newUnrelated();
  }

  @Override
  TargetRelationship owClass(final IBinder binder, final IRegion reg) {
    /* NB. page 229 of ECOOP paper says we should check that Instance target 
     * is shared (!unique). I think this because we want to make sure that
     * overlap is based on the aggregated region hierarchy. We don't have to
     * check this here because we are assuming that effects have already
     * been elaborated and masked, and thus aggregation relationships have
     * already been resolved.
     */
    if (region.equals(reg)) {
      return TargetRelationship.newBIsLarger(RegionRelationships.EQUAL);
    } else if (region.ancestorOf(reg)) {
      return TargetRelationship.newBIsLarger(
        RegionRelationships.REGION_A_INCLUDES_REGION_B);
    } else if (reg.ancestorOf(region)) {
      return TargetRelationship.newBIsLarger(
        RegionRelationships.REGION_B_INCLUDES_REGION_A);
    } else {
      return TargetRelationship.newUnrelated();
    }
  }

  @Override
  public StringBuilder toString(final StringBuilder sb) {
    sb.append('<');
    /* Because of BCA, uses of parameters are represented as
     * ParameterDeclarations. These unparse as the paramter declaration in the
     * method header. This is not appropriate for the use here, where we just
     * want the name of the parameter.
     */
    if (ParameterDeclaration.prototype.includes(reference)) {
      sb.append(ParameterDeclaration.getId(reference));
    } else {
      sb.append(DebugUnparser.toString(reference));
    }
    sb.append(">:");
    sb.append(region.getName()); // XXX: Doesn't work well with shadowed regions
    return sb;
  }

  /**
	 * Compare two instance targets. Two local targets are equal if the refer to
	 * the same expression and same region.
	 */
  @Override
  public boolean equals(final Object o) {
    if (o instanceof InstanceTarget) {
      final InstanceTarget t = (InstanceTarget) o;
      return region.equals(t.region) && reference.equals(t.reference);
    }
    return false;
  }

  /**
	 * Get the hashcode of an instance target.
	 * 
	 * @return The hashcode, which is the sum of the hashcode of the reference
	 *         and the hashcode of the region.
	 */
  @Override
  public int hashCode() {
    final int hc1 = (reference == null) ? 0 : reference.hashCode();
    final int hc2 = (region == null) ? 0 : region.hashCode();
    return hc1 + hc2;
  }
}
