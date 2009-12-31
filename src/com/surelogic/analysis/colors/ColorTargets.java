/*
 * Created on Oct 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.analysis.colors;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.analysis.DefaultThisExpressionBinder;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.*;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.effects.targets.TargetRelationship;
import com.surelogic.analysis.effects.targets.TargetRelationships;
import com.surelogic.analysis.effects.targets.ThisBindingTargetFactory;
import com.surelogic.analysis.regions.*;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.IAliasAnalysis;
import edu.cmu.cs.fluid.java.analysis.TypeBasedAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.colors.ColorizedRegionModel;
import edu.cmu.cs.fluid.sea.drops.effects.WholeModuleFXDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.tree.Operator;


public class ColorTargets {

  private static IBinder binder = null;
  private static TargetFactory targetFactory = null;
  private static BindingContextAnalysis bindingContextAnalysis = null;
  private static ConflictChecker conflicter = null;
  private static IAliasAnalysis tbAlias = null;
  

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
  public static void initColorTargets(final IBinder b) {
    binder = b;
    targetFactory = new ThisBindingTargetFactory(new DefaultThisExpressionBinder(b));

    tbAlias = new TypeBasedAliasAnalysis(b);
    conflicter = new ConflictChecker(b, tbAlias);
    bindingContextAnalysis = new BindingContextAnalysis(b);
   
    if (INSTANCE == null) {
      // Get the Instance region
      INSTANCE = RegionModel.getInstance(RegionModel.INSTANCE);
    }

    if (ELEMENT == null) {
      // Get the region for array elements
      ELEMENT = RegionModel.getInstance(PromiseConstants.REGION_ELEMENT_NAME);
      ELEMENT.setNode(IOldTypeEnvironment.arrayType);
//      ELEMENT = 
//        RegionModel.getInstance(binder.findRegionInType(IOldTypeEnvironment.arrayType,
//                                           PromiseConstants.REGION_ELEMENT_NAME));
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
  
  public static ColorizedRegionModel getCRMfromTarget(final Target tgt) {
    final IRegion reg = tgt.getRegion();
    if (reg == null) {
      LOG.severe("null Region for target " + tgt);
      return null;
    }
    final RegionModel rm = reg.getModel();
    ColorizedRegionModel crm = null;
    if (rm == null) {
      LOG.severe("null RegionModel for target " + tgt);
    } else {
      crm = (ColorizedRegionModel) rm.getColorInfo();
      if (crm == null) {
        LOG.info("null ColorizedRegionModel for target " + tgt);
      }
    }
    return crm;
  }
  
  public static List<ColorizedRegionModel> getCRMsFromTargets(final Set<Target> tgts) {
    List<ColorizedRegionModel> crms = new LinkedList<ColorizedRegionModel>();
    
    
    for (Target tgt: tgts) {
      ColorizedRegionModel crm = getCRMfromTarget(tgt);
      if (crm != null) {
        crms.add(crm);
      }
    }
    return crms;
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
  public static IRegion getColorizedRegion(
      final IRNode cdecl, final IRegion fieldAsRegion) {
    IRegion reg = fieldAsRegion;
    IRegion res = null;
    while (res == null && reg != null) {
      if (isColorized(reg)) {
        res = reg;
      } else {
        reg = reg.getParentRegion();
      }
    }
    return res;
  }
  
  public static List<ColorizedRegionModel> getCRMsForArrayRef(final IRNode arrayRef) {
    Set<Target> tgts = filterColorizedTargets(arrayRef, getTargetsForArrayRef(arrayRef));
    List<ColorizedRegionModel> crms = getCRMsFromTargets(tgts);
    return crms;
  }
  
  public static List<ColorizedRegionModel> getCRMsForFieldRef(final IRNode fieldRef) {
    Set<Target> tgts = filterColorizedTargets(fieldRef, getTargetsForFieldRef(fieldRef));
    List<ColorizedRegionModel> crms = getCRMsFromTargets(tgts);
    return crms;
  }
  
  public static List<ColorizedRegionModel> getCRMsForRef(final IRNode ref) {
    final Operator op = JJNode.tree.getOperator(ref);
    if (FieldRef.prototype.includes(op)) {
      return getCRMsForFieldRef(ref);
    } else if (ArrayRefExpression.prototype.includes(op)) {
      return getCRMsForArrayRef(ref);
    } else {
      throw new UnsupportedOperationException();
    }
  }
  
  public static List<ColorizedRegionModel> getCRMsForMethodCall(final IRNode mCall) {
    Set<Target> tgts = filterColorizedTargets(mCall, getTargetsForMethodCall(mCall, getBinding(mCall)));
    tgts.addAll(getTargetsForMethodAsRegionRef(mCall));
    List<ColorizedRegionModel> crms = getCRMsFromTargets(tgts);
    return crms;
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
        (cdecl == null) ? null : getColorizedRegion(cdecl, fieldAsRegion);
      
      // If we found a suitable region, generate the result
      if (creg != null) {
        Target tgt = targetFactory.createClassTarget(creg);
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
    result.addAll(AggregationUtils.fieldRefAggregatesInto(binder, targetFactory, obj, fieldAsRegion));

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
    final Set<Effect> methodFx = Effects.getMethodCallEffects(bindingContextAnalysis, targetFactory, binder, mcall,
        // This is no worse than before changing MethodCallUtils.constructFormalToActualMap() to take the enclosing decl, but it is probably not correct: you need to make sure this works properly with initialization traversals.
        PromiseUtil.getEnclosingMethod(mcall));
    final Operator callOp = getOperator(mcall);
    
    // Process all the actual parameters
    final Iterator<IRNode> actualsEnum = 
      Arguments.getArgIterator(((CallInterface) callOp).get_Args(mcall));
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
      final Map<RegionModel, IRegion> regionMap =
        AggregationUtils.getRegionMappingFromFieldRef(binder, actual);
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
          final Target t = targetFactory.createInstanceTarget(actual, mappedRegion);
          final Effect e = Effect.newWrite(mcall, t); // bogus src expression
          final Set<Effect> eAsSet = Collections.singleton(e);
          if (conflicter.mayConflict(eAsSet, methodFx, mcall)) {
            getTargetsFromAggregation(mcall, actual, mappedRegion, true, outTargets);            
          }
        }
      }
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
    Set<Effect> callFX = Effects.getDeclaredEffectsWM(mCall, mDecl);
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

  public static boolean isColorized(IRegion reg) {
    RegionModel regMod = reg.getModel();
    return (regMod.getColorInfo() != null);
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
  public static Set<Target> filterColorizedTargets(final IRNode before, Set<Target> tgts) {
    final Set<Effect> intRegionWrites = WholeModuleFXDrop.getInterestingRegionWriteFX();
    final Set<Target> res = new HashSet<Target>(1);
    for (Target tgt : tgts) {
      for (Effect eff : intRegionWrites) {
        final TargetRelationship trel = 
          tgt.overlapsWith(tbAlias.getMayAliasMethod(before), binder, eff.getTarget());
        if (trel.getTargetRelationship() != TargetRelationships.UNRELATED ) {
          res.add(eff.getTarget());
          
        }
      }
    }
    return res; 
  }
}
