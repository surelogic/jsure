/*
 * Created on Oct 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.threadroles;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.analysis.DefaultThisExpressionBinder;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.*;
import com.surelogic.analysis.effects.targets.NoEvidence;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.effects.targets.TargetRelationship;
import com.surelogic.analysis.effects.targets.TargetRelationships;
import com.surelogic.analysis.effects.targets.ThisBindingTargetFactory;
import com.surelogic.analysis.regions.*;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.WholeModuleFXDrop;
//import com.surelogic.dropsea.ir.drops.promises.ModuleModel;
import com.surelogic.dropsea.ir.drops.threadroles.RegionTRoleModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;


public class TRoleTargets {

  private static IBinder binder = null;
  private static TargetFactory targetFactory = null;
  private static BindingContextAnalysis bindingContextAnalysis = null;
  private static IMayAlias mayAlias = null;
  

  private static final Logger LOG = SLLogger.getLogger("ColorSupport");
  
  /**
   * Region to use for array elements.
   */
  private static RegionModel ELEMENT = null;

  /** Reference to the Instance region */
  private static RegionModel INSTANCE = null;

  /**
   * Construct a new Lock Visitor.
   * 
   * @param b
   *          The Binder to use to look up names.
   */
  public static void initRegionTRoleTargets(final IBinder b) {
    binder = b;
    targetFactory = new ThisBindingTargetFactory(new DefaultThisExpressionBinder(b));

    mayAlias = new TypeBasedMayAlias(b);
    bindingContextAnalysis = new BindingContextAnalysis(b, false, true);
   
    if (INSTANCE == null) {
      // Get the Instance region
      INSTANCE = RegionModel.getInstanceRegion(null); // TODO
    }

    if (ELEMENT == null) {
      // Get the region for array elements
      ELEMENT = RegionModel.getInstanceRegion(null); // TODO
    }
  }

  //----------------------------------------------------------------------
  // Helper methods
  //----------------------------------------------------------------------
  
  public static IRNode getBinding(final IRNode node) {
    return binder.getBinding(node);
  }
  public static IRNode getParent(final IRNode node) {
    return JJNode.tree.getParentOrNull(node);
  }

  public static Operator getOperator(final IRNode node) {
    return JJNode.tree.getOperator(node);
  }
  
  public static RegionTRoleModel getRegTRoleModFromTarget(final Target tgt) {
    final IRegion reg = tgt.getRegion();
    if (reg == null) {
      LOG.severe("null Region for target " + tgt);
      return null;
    }
    final RegionModel rm = reg.getModel();
    RegionTRoleModel rTRoleMod = null;
    if (rm == null) {
      LOG.severe("null RegionModel for target " + tgt);
    } else {
      rTRoleMod = null; //(RegionTRoleModel) rm.getColorInfo();
      if (rTRoleMod == null) {
        LOG.info("null RegTRoleModel for target " + tgt);
      }
    }
    return rTRoleMod;
  }
  
  public static List<RegionTRoleModel> getRegTRoleModsFromTargets(final Set<Target> tgts) {
    List<RegionTRoleModel> regTRoleMods = new LinkedList<RegionTRoleModel>();
    
    
    for (Target tgt: tgts) {
      RegionTRoleModel rtrm = getRegTRoleModFromTarget(tgt);
      if (rtrm != null) {
        regTRoleMods.add(rtrm);
      }
    }
    return regTRoleMods;
  }
  

  /**
   * Get the class declaration node for the type of the
   * given expression node.  Returns <code>null</code> if the
   * type is an array.
   */
  public static IRNode getTypeDeclaration(final IRNode n) {
    final IJavaType jt = getJavaType(n);
    if (jt instanceof IJavaPrimitiveType) {
      return null;      
    } else if (jt instanceof IJavaArrayType || jt instanceof IJavaVoidType) {
      return null;
    } else if (jt instanceof IJavaTypeFormal) {
      return ((IJavaTypeFormal) jt).getDeclaration();
    } else {
      return ((IJavaDeclaredType) jt).getDeclaration();
    }
  }
  
  public static IJavaType getJavaType(final IRNode n) {
//    if (ReceiverDeclaration.prototype.includes(getOperator(n))) {
//      // XXX: Good for now, but not good for Java 5 code.
//      return JavaTypeFactory.getDeclaredType(
//          ReceiverDeclaration.getType(n), null, null);
//    } else {
      return binder.getJavaType(n);
//    }
  }
  
  /**
   * Get the colored region (if any) that contains the given region in the
   * given class.
   * 
   * @param cdecl
   *          ClassDeclaration node.
   * @param fieldAsRegion
   *          A region of the given class.  It is known to be static!
   * @return The colored region that contains the given region,
   *         which may in fact be assocatiated with a super region.
   *         <code>null</code> if neither the region nor its parents are colorized.
   */
  public static IRegion getTRoleConstrainedRegion(
      final IRNode cdecl, final IRegion fieldAsRegion) {
    IRegion reg = fieldAsRegion;
    IRegion res = null;
    while (res == null && reg != null) {
      if (isTRoleConstrained(reg)) {
        res = reg;
      } else {
        reg = reg.getParentRegion();
      }
    }
    return res;
  }
  
  public static List<RegionTRoleModel> getRegTRoleModsForArrayRef(final IRNode arrayRef) {
    Set<Target> tgts = filterTRConstrainedTargets(arrayRef, getTargetsForArrayRef(arrayRef));
    List<RegionTRoleModel> regTroleMods = getRegTRoleModsFromTargets(tgts);
    return regTroleMods;
  }
  
  public static List<RegionTRoleModel> getRegTRoleModsForFieldRef(final IRNode fieldRef) {
    Set<Target> tgts = filterTRConstrainedTargets(fieldRef, getTargetsForFieldRef(fieldRef));
    List<RegionTRoleModel> regTroleMods = getRegTRoleModsFromTargets(tgts);
    return regTroleMods;
  }
  
  public static List<RegionTRoleModel> getRegTRoleModsForRef(final IRNode ref) {
    final Operator op = JJNode.tree.getOperator(ref);
    if (FieldRef.prototype.includes(op)) {
      return getRegTRoleModsForFieldRef(ref);
    } else if (ArrayRefExpression.prototype.includes(op)) {
      return getRegTRoleModsForArrayRef(ref);
    } else {
      throw new UnsupportedOperationException();
    }
  }
  
  public static List<RegionTRoleModel> getRegTRoleModsForMethodCall(final IRNode mCall) {
    Set<Target> tgts = filterTRConstrainedTargets(mCall, getTargetsForMethodCall(mCall, getBinding(mCall)));
    tgts.addAll(getTargetsForMethodAsRegionRef(mCall));
    List<RegionTRoleModel> regTroleMods = getRegTRoleModsFromTargets(tgts);
    return regTroleMods;
  }
  
  public static Set<Target> getTargetsForFieldRef(final IRNode fieldRef) {
    final IRNode obj = FieldRef.getObject(fieldRef);
    final IRNode field = getBinding(fieldRef);
    
    
    final IRegion fieldAsRegion = RegionModel.getInstance(field);
    
    /* NOTE: Static regions cannot be aggregated into other regions, so we
     * don't have to do anything fancy here. Instance regions can be
     * aggregated into other regions, so we pass the buck to the
     * getTargetsForInstance() method which chases the aggregation chain to
     * see if a field access references a colorized region because the object it
     * belongs to has been aggregated into a colorized region.
     */
    if (fieldAsRegion.isStatic()) {
      final IRNode cdecl = getTypeDeclaration(obj);
      final IRegion creg =
        (cdecl == null) ? null : getTRoleConstrainedRegion(cdecl, fieldAsRegion);
      
      // If we found a suitable region, generate the result
      if (creg != null) {
        Target tgt = targetFactory.createClassTarget(creg, NoEvidence.INSTANCE);
        Set<Target> res = new HashSet<Target>();
        res.add(tgt);
        return res;
      } else {
        return Collections.emptySet();
      }
    } else {
      return getTargetsForInstanceRegion(fieldRef, obj, fieldAsRegion);
    }
  }
  
  
  public static Set<Target> getTargetsForArrayRef(final IRNode arrayRef) {
    final IRNode obj = ArrayRefExpression.getArray(arrayRef);
    final Set<Target> tgts = getTargetsForInstanceRegion(arrayRef, obj, ELEMENT);
    //final IRNode field = getBinding(arrayRef);
    
    
    //final Region fieldAsRegion = new Region(field);
    
//    /* NOTE: Static regions cannot be aggregated into other regions, so we
//     * don't have to do anything fancy here. Instance regions can be
//     * aggregated into other regions, so we pass the buck to the
//     * getTargetsForInstance() method which chases the aggregation chain to
//     * see if a field access references a colorized region because the object it
//     * belongs to has been aggregated into a colorized region.
//     */
//    if (fieldAsRegion.isStatic()) {
//      final IRNode cdecl = getTypeDeclaration(obj);
//      final Region creg =
//        (cdecl == null) ? null : getColorizedRegion(cdecl, fieldAsRegion);
//      
//      // If we found a suitable region, generate the result
//      if (creg != null) {
//        Target tgt = new ClassTarget(creg);
//        Set<Target> res = new HashSet<Target>();
//        res.add(tgt);
//        return res;
//      } else {
//        return Collections.emptySet();
//      }
//    } else {
//      return getTargetsForInstanceRegion(arrayRef, obj, fieldAsRegion);
//    }
    return tgts;
  }
  
  
  
  /**
   * Given a reference to an instance region, find the Targets that region
   * aggregates into, using aggregation information.
   * 
   * @param src
   *          The reference to the region, either a FieldRef,
   *          ArrayRefExpression, or VariableDeclarator. Used for chain of
   *          evidence reports.
   * @param obj
   *          Node for the expression naming the object whose region is being
   *          reference.
   * @param fieldAsRegion
   *          The region being referenced.
   * @return The Set of Regions that are relevant.
   */
  public static Set<Target> getTargetsForInstanceRegion(final IRNode src,
      final IRNode obj, final IRegion fieldAsRegion) {
    final Set<Target> result = new HashSet<Target>();
    getTargetsFromAggregation(src, obj, fieldAsRegion, false, result);
    return result;
  }
  
  
  /**
   * Given a reference to an instance region, find the locks that protect that
   * region, using aggregation information.
   * 
   * @param src
   *          The reference to the region, either a FieldRef,
   *          ArrayRefExpression, or VariableDeclarator. Used for chain of
   *          evidence reports.
   * @param obj
   *          Node for the expression naming the object whose region is being
   *          reference.
   * @param fieldAsRegion
   *          The region being referenced.
   * @param skipSelf
   *          Whether the initial field access should be included in the
   *          accesses that need locks or not.
   * @param result
   *          The Set to which the needed locks will be added.
   */
  public static final void getTargetsFromAggregation(
      final IRNode src, final IRNode obj, final IRegion fieldAsRegion, 
      final boolean skipSelf, final Set<Target> result) {
    /*
     * Field may belong to other targets because of (uniqueness) aggregation.
     * Find those targets and see if any of them are protected.
     * 
     * Get all the regions that the region aggregates into. Each level of
     * aggregation may be colorized
     * 
     */
    
    // XXX: This should be stored in a more global place, so as not to be rebuilt each time
    result.addAll(UniquenessUtils.fieldRefAggregatesInto(binder, targetFactory, obj, fieldAsRegion));

  }
  
  
  /**
   * A method call may require locks because the regions it affects are 
   * aggregated into regions that are protected in the referring object.
   * Specifically, if an actual parameter (including the receiver) is
   * a field reference to an unique field, then the effects the method has
   * on regions of that field must be reinterpreted based on the field's 
   * aggregation mapping, and the associated locks looked up.  This method 
   * returns those locks that need to be held because of this.
   * 
   * @param mcall A MethodCall, ConstructorCall,
   *   NewExpression, or AnonClassExpression
   */
  // TODO: Fix the call sites
  private static Set<Target> getTargetsForMethodAsRegionRef(final IRNode mcall) {
    final Set<Target> result = new HashSet<Target>();
    // This is no worse than before changing MethodCallUtils.constructFormalToActualMap() to take the enclosing decl, but it is probably not correct: you need to make sure this works properly with initialization traversals.
    final IRNode enclosingMethod = PromiseUtil.getEnclosingMethod(mcall);

    Effects eff = null;
    final Set<Effect> methodFx = eff.getMethodCallEffects(null,
        bindingContextAnalysis.getExpressionObjectsQuery(enclosingMethod),
        targetFactory, binder,
        JavaPromise.getReturnNodeOrNull(enclosingMethod),
        Effects.ElaborationErrorCallback.NullCallback.INSTANCE, mcall,
        enclosingMethod);
    final Operator callOp = getOperator(mcall);
    
    // Process all the actual parameters
    Iterator<IRNode> actualsEnum;
    try {
      actualsEnum = Arguments.getArgIterator(((CallInterface) callOp).get_Args(mcall));
    } catch (final CallInterface.NoArgs e) {
      actualsEnum = new EmptyIterator<IRNode>();
    }
    while (actualsEnum.hasNext()) {
      final IRNode actual = actualsEnum.next();
      getTargetsForMethodAsRegionRef_forParam(mcall, actual, methodFx, result);
    }
    
    // Process receiver of non-static method calls
    if (MethodCall.prototype.includes(callOp)) {
      MethodCall call = (MethodCall) callOp;
      if (!TypeUtil.isStatic(getBinding(mcall))) { // add receiver of non-static methods
        getTargetsForMethodAsRegionRef_forParam(mcall, call.get_Object(mcall), methodFx, result);
      }
    }

    return Collections.unmodifiableSet(result);
  }

  private static void getTargetsForMethodAsRegionRef_forParam(
      final IRNode mcall, final IRNode actual, final Set<Effect> methodFx, 
      final Set<Target> outTargets) {
    // Is the actual parameter of the form 'e.f'
    final Operator objExprOp = getOperator(actual);
    if (FieldRef.prototype.includes(objExprOp)) {
      /* Get the region mapping for the field 'f'. Mapping is 'null' if the
       * field is not unique or does not have a declared mapping.
       */
      final Map<IRegion, IRegion> regionMap =
        UniquenessUtils.constructRegionMapping(binder.getBinding(actual));
      if (regionMap != null) {
        /* For each region in 'e.f' that may be affected that is mapped into a
         * region of 'e', we need to find what locks it may have associated
         * with it.
         */
        for (IRegion mappedRegion : regionMap.keySet()) {
          /* See if <e.f> . mappedRegion may be affected by the method call.
           * We do this by checking whether "writes <e.f>.mappedRegion"
           * conflicts with the method's effects.
           */
          final Target t = targetFactory.createInstanceTarget(actual, mappedRegion, NoEvidence.INSTANCE);
          final Effect e = Effect.newWrite(mcall, t); // bogus src expression
          final Set<Effect> eAsSet = Collections.singleton(e);
          /* XXX: This is just as broken as it was before we cared about the
           * constructor context.  To fix this, the caller of this method needs
           * to track the current flow unit of interest.
           */
          if (new ConflictChecker(binder, mayAlias).mayConflict(eAsSet, methodFx)) {
// was:          if (conflicter.mayConflict(eAsSet, methodFx, mcall)) {
            getTargetsFromAggregation(mcall, actual, mappedRegion, true, outTargets);            
          }
        }
      }
    }
  }
  
  /* Was in Effects.  Got tired of it being there.  Moved here on 
   * 2011-03-09.
   */
  /** Get the declared effects for a method invoked from a particular call-site.
   * @param mCall The IRNode for the call site
   * @param mDecl The IRNode that is the MethodDecl
   * @return Declared effects for cross-module or TheWorld calls, or null for
   * same-non-world-module calls.
   */
  public static List<Effect> getDeclaredEffectsWM(
      final IRNode mCall, final IRNode mDecl) {
    //if call-site and callee are in different modules, or if either is part of
    // TheWorld we can only depend on the declared effects!
    
    if (false){//!ModuleModel.sameNonWorldModule(mCall, mDecl)) {
      // it's declared effects, or WritesAll!
    	Effects e = null;
      return e.getMethodEffects(mDecl, mCall);
    } else {
      // this module does not apply!
      return null;
    }
  }
  
  /** Get the targets that are affected by this call. Uses wholeModuleFX, and is thus
   * valid only AFTER ModuleEffectsAnalysis has been run.
   * 
   * @param mCall IR for the call site
   * @param mDecl IR for the method/constructor declaration.
   * @return
   */
  public static Set<Target> getTargetsForMethodCall(final IRNode mCall, final IRNode mDecl) {
    List<Effect> callFX = getDeclaredEffectsWM(mCall, mDecl);
    // callFX is now null only when mCall and mDecl are in the same module, AND that
    // module is not TheWorld. Require same module, because can only use declared 
    // effects across modules. Require not TheWorld, because we can't count on TheWorld
    // being a sealed module.
    if (callFX == null) {
      WholeModuleFXDrop callFXDrop = WholeModuleFXDrop.getMethodFX(mDecl);
      callFX = callFXDrop.getFixedPointFX();
    }
    
    Set<Target> res = new HashSet<Target>(callFX.size());
    for (Effect e : callFX) {
      res.add(e.getTarget());
    }
    return res;
  }

  public static boolean isTRoleConstrained(IRegion reg) {
    RegionModel regMod = reg.getModel();
    return false; // (regMod.getColorInfo() != null);
  }
  
  /** Given a set of targets and a place in the code, compare the targets to the
   * group of "interesting region writes" and return (anyinstance targets for) 
   * as many of the interesting regions as potentially overlap with any of the given
   * targets. 
   * @param before Where are we in the AST?
   * @param tgts Some targets that are touched as a result of executing this place in
   * the ast.
   * @return Canonicalized targets for the "interesting regions" that may be affected.
   * Note that we are <b>not</b> returning any of the original targets! We're actually
   * returning targets from the saved "interesting regions" originally defined for
   * WholeModuleEffectsAnalysis.  These will certainly include any colorized regions
   * as all such are "interesting."
   */
  public static Set<Target> filterTRConstrainedTargets(final IRNode before, Set<Target> tgts) {
    final Set<Effect> intRegionWrites = WholeModuleFXDrop.getInterestingRegionWriteFX();
    final Set<Target> res = new HashSet<Target>(1);
    for (Target tgt : tgts) {
      for (Effect eff : intRegionWrites) {
        /* XXX This should produce the same wrong behavior as before we cared
         * about the constructor context.  To fix this we need to have the 
         * clients of this method track the current flow unit.
         */ 
        final TargetRelationship trel =
          tgt.overlapsWith(mayAlias, binder, eff.getTarget());
        if (trel.getTargetRelationship() != TargetRelationships.UNRELATED ) {
          res.add(eff.getTarget());
          
        }
      }
    }
    return res; 
  }
}
