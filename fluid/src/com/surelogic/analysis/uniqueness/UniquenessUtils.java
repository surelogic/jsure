package com.surelogic.analysis.uniqueness;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.effects.targets.NoEvidence;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.RegionRules;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.promises.ExplicitUniqueInRegionPromiseDrop;
import com.surelogic.dropsea.ir.drops.promises.IUniquePromise;
import com.surelogic.dropsea.ir.drops.promises.RegionAggregationDrop;
import com.surelogic.dropsea.ir.drops.promises.RegionModel;
import com.surelogic.dropsea.ir.drops.promises.SimpleUniqueInRegionPromiseDrop;
import com.surelogic.dropsea.ir.drops.promises.UniquePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * Contains methods for helping process aggregated regions.  These methods
 * used to be in EffectsAnalysis, but when the elaboration routines were moved
 * to EffectsVisitor, they needed to be separated out to avoid a circular
 * dependency between EffectsVisitor and EffectsAnalysis.
 */
public final class UniquenessUtils {
  private UniquenessUtils() {
    super();
  }
  
  

  /**
   * Given a field, parameter, or return declaration, return whether
   * it is unique.
   */
  public static boolean isUnique(final IRNode varDecl) {
    return UniquenessRules.getUnique(varDecl) != null || 
        RegionRules.getSimpleUniqueInRegion(varDecl) != null ||
        RegionRules.getExplicitUniqueInRegion(varDecl) != null;
  }
  
  /**
   * Given a VariableDeclarator (from a field declaration) return whether
   * the field is borrowed.
   */
  public static boolean isFieldBorrowed(final IRNode varDecl) {
    return UniquenessRules.getBorrowed(varDecl) != null || 
        RegionRules.getSimpleBorrowedInRegion(varDecl) != null ||
        RegionRules.getExplicitBorrowedInRegion(varDecl) != null;
  }
  
  /**
   * Return whether the given declaration is indeed unique-write.
   * TODO: Perhaps we want to add allowRead to UniqueFieldInRegion ?
   * @param decl declaration node (or null)
   * @return true if decl not null and is annotated Unique(allowRead).
   */
  public static boolean isUniqueWrite(final IRNode decl) {
	  if (decl == null) return false;
	  final IUniquePromise uPromise = getUnique(decl);
	  if (uPromise == null) return false;
	  return (uPromise.allowRead());
  }

  /**
   * Get the promise drop, if any, for the Unique or UniqueInRegion annotation
   * on a field, parameter, or return declaration.
   * 
   * @param varDecl
   *          The VariableDeclarator node to test
   * @return The promise drop (a {@link UniquePromiseDrop},
   *         {@link ExplicitUniqueInRegionPromiseDrop} or
   *         {@link SimpleUniqueInRegionPromiseDrop}, or <code>null</code> if
   *         the field is not annotated.
   */
  public static IUniquePromise getUnique(final IRNode varDecl) {
    IUniquePromise result = UniquenessRules.getUnique(varDecl);
    if (result == null) {
      result = RegionRules.getSimpleUniqueInRegion(varDecl);
    }
    if (result == null) {
      result = RegionRules.getExplicitUniqueInRegion(varDecl);
    }
    return result;
  }

  /**
   * Get the promise drop, if any, for the Borrowed or BorrowedInRegion annotation
   * on the field declaration.
   * 
   * @param varDecl
   *          The VariableDeclarator node to test
   * @return The promise drop (a {@link BorrowedPromiseDrop},
   *         {@link ExplicitBorrowedInRegionPromiseDrop} or
   *         {@link SimpleBorrowedInRegionPromiseDrop}, or <code>null</code> if
   *         the field is not annotated.
   */
  public static PromiseDrop<? extends IAASTRootNode> getFieldBorrowed(final IRNode varDecl) {
    PromiseDrop<? extends IAASTRootNode> result = 
      UniquenessRules.getBorrowed(varDecl);
    if (result == null) {
      result = RegionRules.getSimpleBorrowedInRegion(varDecl);
    }
    if (result == null) {
      result = RegionRules.getExplicitBorrowedInRegion(varDecl);
    }
    return result;
  }
  
  /**
   * Get the Unique, Borrowed, UniqueInRegion, or BorrowedInRegion annotation
   * for the given field declaration.
   * 
   * @param varDecl
   *          The VariableDeclarator node to test
   * @return The promise drop (a {@link UniquePromiseDrop},
   *         {@link ExplicitUniqueInRegionPromiseDrop},
   *         {@link SimpleUniqueInRegionPromiseDrop},
   *         {@link BorrowedPromiseDrop},
   *         {@link ExplicitBorrowedInRegionPromiseDrop}, or
   *         {@link SimpleBorrowedInRegionPromiseDrop}), or <code>null</code> if
   *         the field is not annotated.
   */  
  public static PromiseDrop<? extends IAASTRootNode> getFieldUniqueOrBorrowed(
      final IRNode varDecl) {
    PromiseDrop<? extends IAASTRootNode> result = UniquenessRules.getUnique(varDecl);
    if (result == null) {
      result = RegionRules.getSimpleUniqueInRegion(varDecl);
    }
    if (result == null) {
      result = RegionRules.getExplicitUniqueInRegion(varDecl);
    }
    if (result == null) {
      result = UniquenessRules.getBorrowed(varDecl);
    }
    if (result == null) {
      result = RegionRules.getSimpleBorrowedInRegion(varDecl);
    }
    if (result == null) {
      result = RegionRules.getExplicitBorrowedInRegion(varDecl);
    }
    return result;
  }
  
  /**
   * Compute the regions that a field reference maps into. This process is
   * recursive; that is, if the field reference is o.f1.f2.f3, and f3 maps into
   * region r of o.f1, and r maps into region q of o, then the regions
   * o.f1.f2.f3, o.f1.r, and o.q are returned, in that order. In general, the
   * results goes from most specific to least specific region. Or, put another
   * way, the results in the list move outwards through the levels of
   * aggregation.
   */
  public static List<Target> fieldRefAggregatesInto(
      final IBinder binder, final TargetFactory targetFactory,
      final IRNode expr, final IRegion region) {
    return Collections.unmodifiableList(
      fieldRefAggregatesInto(
          binder, targetFactory, expr, region, new LinkedList<Target>()));
  }

  
  
  /**
   * Compute the regions that a field reference maps into. This process is
   * recursive; that is, if the field reference is o.f1.f2.f3, and f3 maps into
   * region r of o.f1, and r maps into region q of o, then the regions
   * o.f1.f2.f3, o.f1.r, and o.q are returned, in that order. In general, the
   * results goes from most specific to least specific region. Or, put another
   * way, the results in the list move outwards through the levels of
   * aggregation.
   */
  private static List<Target> fieldRefAggregatesInto(
      final IBinder binder, final TargetFactory targetFactory, 
      final IRNode expr, final IRegion region, final List<Target> result) {
    /* Careful, region can be static!  No aggregation in the case of static
     * fields, just return the field itself.
     */
    if (region.isStatic()) {
      /* fieldRef = <Class> . <region> */
      result.add(targetFactory.createClassTarget(region, NoEvidence.INSTANCE));
    } else {
      /* fieldRef = <expr> . <region>
       * FieldRef always aggregates into itself
       */
      result.add(targetFactory.createInstanceTarget(expr, region, NoEvidence.INSTANCE));
  
      /* Field can only be aggregated if there is another level of indirection. */
      if (FieldRef.prototype.includes(JJNode.tree.getOperator(expr))) {
        /* expr = e.f1
         * fieldRef = <e.f1> . <region>
         */
        final IRNode fieldID = binder.getBinding(expr);
        /* The field is unique or borrowed, see if we can exploit aggregation. */
        final Map<IRegion, IRegion> aggregationMap = constructRegionMapping(fieldID);
        if (aggregationMap != null) {
          final IRNode newObject = FieldRef.getObject(expr);
          final IRegion newRegion = getMappedRegion(region.getModel(), aggregationMap);
          /* <expr> . <region> == <newObject.f1> . <region> aggregates into
           * <newObject> . <newRegion>
           * 
           * See what regions <newObject.newRegion> aggregate into. (The
           * recursive call will add <newObject> . <newRegion> to the results.)
           */
          fieldRefAggregatesInto(binder, targetFactory, newObject, newRegion, result);
        }
      }
    }
    return result;
  }

  
  
  /**
   * Build a map of the region mapping from a specific unique or borrowed field. Map is from
   * {@link RegionModel} to {@link Region}. Returns <code>null</code> if the
   * field is not unique or borrowed.
   * 
   * @param field
   *          A VariableDeclarator for a field declaration.
   */
  public static Map<IRegion, IRegion> constructRegionMapping(final IRNode field) {
    RegionAggregationDrop aggDrop;
    aggDrop = UniquenessRules.getUnique(field);
    if (aggDrop == null) aggDrop = RegionRules.getSimpleUniqueInRegion(field);
    if (aggDrop == null) aggDrop = RegionRules.getExplicitUniqueInRegion(field);
    if (aggDrop == null) aggDrop = UniquenessRules.getBorrowed(field);
    if (aggDrop == null) aggDrop = RegionRules.getSimpleBorrowedInRegion(field);
    if (aggDrop == null) aggDrop = RegionRules.getExplicitBorrowedInRegion(field);
    return (aggDrop == null) ? null : aggDrop.getAggregationMap(field);
  }

  
  
  /**
   * Get the minimum region that a region maps into. A region R may be mapped
   * into more than one region if R and an ancestor of R are mapped into
   * different regions. Returns <code>null</code> if the region is not
   * affected by the mapping.
   */
  public static IRegion getMappedRegion(
      final IRegion r, final Map<IRegion, IRegion> aggMapping) {
    /* Walk up the region hierarchy starting at r.  Because the mapping must
     * respect the region hierarchy, the first region we find that is mapped
     * is the answer.
     */
    IRegion currentRegion = r;
    while (currentRegion != null) {
      final IRegion to = aggMapping.get(currentRegion);
      if (to != null) {
        return to;
      }
      currentRegion = currentRegion.getParentRegion();
    }
    return null;
  }
}
