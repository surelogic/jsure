/*
 * Created on Apr 17, 2003
 * 
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.surelogic.analysis.threadroles;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.annotation.rules.ThreadRoleRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.WarningDrop;
import com.surelogic.dropsea.ir.drops.SimpleCallGraphDrop;
import com.surelogic.dropsea.ir.drops.modules.ModuleModel;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleReqSummaryDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author dfsuther
 * 
 * Propagate ThreadRole annotations down the inheritance hierarchy. (Whether we
 * need/want to propagate upwards is to be determined.)
 * 
 * Possible problem: This pass might not do right by multiple levels of
 * inheritance in JAR files. For example, (i) if A extends B extends C where A
 * is user code and B and C are in (possibly different) JAR files, (ii) C
 * provides a promise on method m, (iii) B overrides C.m, (iv) and A overrides
 * B.m, then we may not propagate the promise from A to C because we do not have
 * a method body to process. If this problem does exist, then a solution is to
 * propagate one level at a time for user code (as is now done for all code) and
 * to handle all levels at once for stuff in JAR files.
 * 
 * TODO: Determine if this problem exists in practice and fix it.
 * 
 * Result of running doInherit for a method "mth" is as follows:
 * The mutableInheritedRequiresSet will contain one Drop for each inherited TRoleRequire
 * drop.  It thus may be an empty set.  Each drop in the set will depend on the parent
 * drop it is inherited from, along with the CU in which mth lives.
 * 
 * Context drops are handled exactly analogously to Require drops.
 * 
 */
final public class TRoleInherit {

  private static TRoleInherit INSTANCE = new TRoleInherit();

  private static final Logger LOG = SLLogger.getLogger("analysis.threadroles.inherit");

  private static IBinder binder;

  // Singleton pattern!
  private TRoleInherit() {}
  
  public static final void startACu(IBinder eBinder) {
    binder = eBinder;
  }
  
  public static final void endACu() {
	  // DO NOTHING!
  }
  

  /**
   * @param mth
   *          the IRNode for a method declaration. This method may override one
   *          from a superclass or implement one from an interface.
   */
  /**
   * @param mth
   */
  public static final void doInherit(IRNode mth) {
    final boolean finerIsLoggable = LOG.isLoggable(Level.FINER);    
    final Operator op = JJNode.tree.getOperator(mth);
    if (!((MethodDeclaration.prototype.includes(op) || 
        ConstructorDeclaration.prototype.includes(op)))) {
      // not a method or constructor, so nothing to do!
      return;
    }
    final String idStr = finerIsLoggable ? JJNode.getInfo(mth) : null;
    if (finerIsLoggable) {
      LOG.finer(">>Starting doInherit on " + idStr);
    }
    
    final TRoleReqSummaryDrop mthReqSumm = ThreadRoleRules.getReqSummDrop(mth);
    if ((mthReqSumm != null) && mthReqSumm.isInheritanceDone()) {
      if (finerIsLoggable) {
        LOG.finer("<<doInherit for " + idStr + " is redundant.");
      }
      return;
    }
    
    if (mthReqSumm != null) {
      mthReqSumm.setInheritanceDone();
    }
    
    SimpleCallGraphDrop aCGD = SimpleCallGraphDrop.getCGDropFor(mth);
    if (ModuleModel.isAPIinParentModule(mth)) {
      aCGD.setPotentiallyCallable(true);
    }
    
    Iterator<IBinding> parentIter = binder.findOverriddenParentMethods(mth);
    if (!parentIter.hasNext()) {
      // nothing to inherit from, so we're done!
      if (finerIsLoggable) {
        LOG.finer("<<no Parent for " + idStr + ".");
      }
      return;
    }
    
    // note that get... here forces a purge of invalid drops!
//    Collection inheritedContextDrops = ColorRules.getInheritedContextDrops(mth);
//    Collection inheritedRequireDrops = ColorRules.getInheritedRequireDrops(mth);
    
    
    boolean icrHere = ThreadRoleRules.isTRoleRelevant(mth);
    
//    final boolean haveReqsHere = !reqsHere.isEmpty();
//    final boolean haveCtxHere = !ctxHere.isEmpty();
    
        
    while (parentIter.hasNext()) {
      IRNode mDecl = parentIter.next().getNode();
      
      if (!aCGD.isPotentiallyCallable()) {
        if (ModuleModel.isAPIinParentModule(mDecl)) {
          aCGD.setPotentiallyCallable(true);
        } else {
          final SimpleCallGraphDrop parentCGD = SimpleCallGraphDrop.getCGDropFor(mDecl);
          if (parentCGD.isPotentiallyCallable()) {
            aCGD.setPotentiallyCallable(true);
          }
        }
      }

      // getSummaryFor forces a doInherit on the parent
      TRoleReqSummaryDrop parentReqSum = TRoleReqSummaryDrop.getSummaryFor(mDecl);
      
//      Collection reqsFromParent = TRoleRules.getReqDrops(mDecl);
//      if (reqsFromParent.isEmpty()) {
//        reqsFromParent = TRoleRules.getInheritedRequireDrops(mDecl);
//      }
      
//      Collection ctxFromParent = TRoleRules.getCtxDrops(mDecl);
//      if (ctxFromParent.isEmpty()) {
//        ctxFromParent = ColorRules.getInheritedContextDrops(mDecl);
//      }
      
      boolean itrrFromParent = ThreadRoleRules.isTRoleRelevant(mDecl);
      final String parentName = JJNode.getInfo(mDecl);
      
      boolean somethingToInherit = !parentReqSum.isInferred();
      
      if (!somethingToInherit) {
        // there was nothing to inherit here, so just...
        if (finerIsLoggable) {
          LOG.finer("nothing to inherit from parent " + parentName);
        }
        continue;
      }
      
      TRoleReqSummaryDrop reqSumHere = TRoleReqSummaryDrop.getSummaryFor(mth);
      parentReqSum = TRoleReqSummaryDrop.getSummaryFor(mDecl);
      if (reqSumHere.isUser()) {
        checkOverriddenInheritance(parentReqSum, reqSumHere);
      } else if (reqSumHere.isInferred()) {
        // inheriting from parent to a ColorReqSummary that has no information of
        // its own.  Just clone the information down.
        reqSumHere.inheritingFrom(parentReqSum);
      } else {
        assert(reqSumHere.isInherited());
        // inheriting from parent to a ColorReqSummary that has already inherited from
        // another parent.  We'd like to see a consistent world.
        checkOverriddenInheritance(parentReqSum, reqSumHere);
        // in any case, we need to get the userDeponents updated.
        reqSumHere.addAllToUserDeponents(parentReqSum.getUserDeponents());
      }
    }
    if (finerIsLoggable) {
      LOG.finer("<<Exiting" + idStr);
    }
  }

  
  /**
   * @param mth
   * @param idStr
   * @param parentReqSum
   * @param icrFromParent
   * @param parentName
   * @param reqSumHere
   */
  private static void checkOverriddenInheritance(TRoleReqSummaryDrop parentReqSum, TRoleReqSummaryDrop reqSumHere) {
    boolean wantWarning = false;
    final IRNode here = reqSumHere.getNode();
    final String idStr = JJNode.getInfo(here);
    final IRNode parent = parentReqSum.getNode();
    final String parentName = JJNode.getInfo(parent);
    final boolean icrFromParent = parentReqSum.reqsAreRelevant();
    final StringBuilder warnMsg = new StringBuilder();
    
    if (reqSumHere.reqsAreRelevant()) {
      if (!icrFromParent) {
      // local method has requirements that over-ride a @transparent
      // from the parent.  Warn, but accept.
      warnMsg.append("@colorConstraint " + reqSumHere.getFullExpr() + " on " + idStr);
      warnMsg.append(" overrides @transparent from parent" + parentName);
      wantWarning = true;
      } else {
        // local method has requirements, parent has other requirements.  Check-em.
//          Child requirements that imply the parent are OK, others get a warning.
        if (!reqSumHere.getFullExpr().imply(parentReqSum.getFullExpr()).isOne()) {
          warnMsg.append("@colorConstraint " + reqSumHere.getFullExpr());
          warnMsg.append(" on " + idStr + "overrides requirement inherited from ");
          warnMsg.append(" parent " + parentName);
          wantWarning = true;
        }
      }
    } else {
      // must have a transparent here.
      if (icrFromParent) {
        // parent has requirements!
        warnMsg.append("@transparent on " + idStr + " overrides ");
        warnMsg.append("@colorConstraint " + parentReqSum.getFullExpr());
        warnMsg.append(" inherited from parent " + parentName);
        wantWarning = true;
      } else {
        // parent and child both have @transparent.
        wantWarning = false;
      }
     
      if (wantWarning) {
        WarningDrop wd = 
          TRoleMessages.createWarningDrop(warnMsg.toString(), here);
      }
//            wd.addSupportingInformation(reqSumHere.getFullExpr().toString() +
//                                        " does not imply " + parentReqSum.getFullExpr(), null);
    }
  }

  public static TRoleInherit getInstance() {
    return INSTANCE;
  }

 
}