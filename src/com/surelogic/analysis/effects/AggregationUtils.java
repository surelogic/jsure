/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/effects/AggregationUtils.java,v 1.2 2008/01/18 23:52:03 aarong Exp $*/
package com.surelogic.analysis.effects;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.surelogic.aast.promise.RegionMappingNode;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.RegionRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.AggregatePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;

/**
 * Contains methods for helping process aggregated regions.  These methods
 * used to be in EffectsAnalysis, but when the elaboration routines were moved
 * to EffectsVisitor, they needed to be separated out to avoid a circular
 * dependency between EffectsVisitor and EffectsAnalysis.
 *
 * @author aarong
 */
public final class AggregationUtils {
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
      fieldRefAggregatesInto(binder, targetFactory, expr, region, new LinkedList<Target>()));
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
      result.add(targetFactory.createClassTarget(region));
    } else {
      /* fieldRef = <expr> . <region>
       * FieldRef always aggregates into itself
       */
      result.add(targetFactory.createInstanceTarget(expr, region));
  
      /* Field can only be aggregated if there is another level of indirection. */
      if (FieldRef.prototype.includes(JJNode.tree.getOperator(expr))) {
        /* expr = e.f1 fieldRef = <e.f1> . <region> */
        final IRNode fieldID = binder.getBinding(expr);
        final boolean isUnique = UniquenessRules.isUnique(fieldID);
  
        /* Field can only be aggregated if the indirect field is unique */
        if (isUnique) {
          /* The field is unique, see if we can exploit uniqueness aggregation. */
          final Map<RegionModel, IRegion> aggregationMap = constructRegionMapping(fieldID);
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
    }
    return result;
  }

  /**
   * Build a map of the region mapping, if possible, from the field dereferenced
   * by the given FieldRef expression. Returns <code>null</code> if the field
   * is not unique or if the field is unique, but does not declare any
   * region aggregations. Map is from {@link Region}to {@link Region}.
   * 
   * @param expr
   *          a FieldRef expression.
   */
  public static Map<RegionModel, IRegion> getRegionMappingFromFieldRef(
      final IBinder binder, final IRNode expr) {
    final IRNode fieldID = binder.getBinding(expr);
    if (UniquenessRules.isUnique(fieldID)) {
      return constructRegionMapping(fieldID);
    }
    return null;
  }

  /**
   * Build a map of the region mapping from a specific unique field. Map is
   * from {@link Region}to {@link Region}. Returns null if the statement
   * doesn't perform any mappings.
   */
  /* Should add a cache later? */
  protected static Map<RegionModel, IRegion> constructRegionMapping(final IRNode field) {
    /* Try to get the aggregation information. If no aggregation is defined,
     * then getFieldRegion will throw a SlotUndefinedException.
     */
    final AggregatePromiseDrop mrs = RegionRules.getAggregate(field);
    if (mrs != null) {
      final Map<RegionModel, IRegion> aggregationMap = new HashMap<RegionModel, IRegion>();
      for (final RegionMappingNode mapping : mrs.getAST().getSpec().getMappingList()) {
        aggregationMap.put(mapping.getFrom().resolveBinding().getModel(), 
                           mapping.getTo().resolveBinding().getRegion());
      }
      return Collections.unmodifiableMap(aggregationMap);
    } else {
      // No aggregation mapping, return null
      return null;
    }
  }

  /**
   * Get the minimum region that a region maps into. A region R may be mapped
   * into more than one region if R and an ancestor of R are mapped into
   * different regions. Returns <code>null</code> if the region is not
   * affected by the mapping.
   */
  public static IRegion getMappedRegion(
      final RegionModel r, final Map<RegionModel, IRegion> aggMapping) {
    RegionModel currentRegion = r;
    IRegion leastRegion = null;

    /* Walk up the region hierarchy starting at r. Test if each region is
     * mapped, and if so, add its destination region to the set.
     * 
     * (If we enforce well formed aggregation relationships (see p224 of ECOOP
     * paper), this is probably short-cutable.)
     */
    while (currentRegion != null) {
      final IRegion to = aggMapping.get(currentRegion);
      if (to != null) {
        if (leastRegion == null) {
          leastRegion = to;
        } else {
          if (leastRegion.ancestorOf(to)) {
            leastRegion = to;
          }
        }
      }
      currentRegion = currentRegion.getParentRegion();
    }

    return leastRegion;
  }
}
