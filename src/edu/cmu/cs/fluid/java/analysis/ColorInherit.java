/*
 * Created on Apr 17, 2003
 * 
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ColorPromises;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author dfsuther
 * 
 * Propogate color annotations down the inheritance hierarchy. (Whether we
 * need/want to propogate upwards is to be determined.)
 * 
 * Possible problem: This pass might not do right by multiple levels of
 * inheritance in JAR files. For example, (i) if A extends B extends C where A
 * is user code and B and C are in (possibly different) JAR files, (ii) C
 * provides a promise on method m, (iii) B overrides C.m, (iv) and A overides
 * B.m, then we may not propogate the promise from A to C because we do not have
 * a method body to process. If this problem does exist, then a solution is to
 * propogate one level at a time for user code (as is now done for all code) and
 * to handle all levels at once for stuff in JAR files.
 * 
 * TODO: Determine if this problem exists in practice and fix it.
 * 
 * Result of running doInherit for a method "mth" is as follows:
 * The mutableInheritedRequiresSet will contain one Drop for each inherited ColorRequire
 * drop.  It thus may be an empty set.  Each drop in the set will depend on the parent
 * drop it is inherited from, along with the CU in which mth lives.
 * 
 * Context drops are handled exactly analogously to Require drops.
 * 
 */
@Deprecated
final public class ColorInherit {

  private static ColorInherit INSTANCE = new ColorInherit();

  private static final Logger LOG = SLLogger.getLogger("analysis.colors.inherit");

  private static IBinder binder;

  // Singleton pattern!
  private ColorInherit() {}
  
  public static final void startACu(IBinder eBinder) {
    binder = eBinder;
  }
  
  public static final void endACu() {
    
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
    final boolean finer = LOG.isLoggable(Level.FINER);    
    final Operator op = JJNode.tree.getOperator(mth);
    if (!((MethodDeclaration.prototype.includes(op) || 
        ConstructorDeclaration.prototype.includes(op)))) {
      // not a method or constructor, so nothing to do!
      return;
    }
    final String idStr = finer ? JJNode.getInfo(mth) : null;
    if (finer) {
      LOG.finer(">>Starting doInherit on " + idStr);
    }
    
    final ColorReqSummaryDrop mthReqSumm = ColorPromises.getReqSummDrop(mth);
    if ((mthReqSumm != null) && mthReqSumm.isInheritanceDone()) {
      if (finer) {
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
    
    Iterator<IRNode> parentIter = binder.findOverriddenParentMethods(mth);
    if (!parentIter.hasNext()) {
      // nothing to inherit from, so we're done!
      if (finer) {
        LOG.finer("<<no Parent for " + idStr + ".");
      }
      return;
    }
    
    // note that get... here forces a purge of invalid drops!
//    Collection inheritedContextDrops = ColorPromises.getInheritedContextDrops(mth);
//    Collection inheritedRequireDrops = ColorPromises.getInheritedRequireDrops(mth);
    
    
//    Collection reqsHere = ColorPromises.getReqDrops(mth);
//    Collection ctxHere = ColorPromises.getCtxDrops(mth);
    boolean icrHere = ColorPromises.isColorRelevant(mth);
    
//    final boolean haveReqsHere = !reqsHere.isEmpty();
//    final boolean haveCtxHere = !ctxHere.isEmpty();
    
    
//    Collection oldInheritedReqDrops = ColorPromises.getInheritedRequireDrops(mth);
//    Collection oldInheritedCtxDrops = ColorPromises.getInheritedContextDrops(mth);
    
    while (parentIter.hasNext()) {
      IRNode mDecl = parentIter.next();
      
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
      ColorReqSummaryDrop parentReqSum = ColorReqSummaryDrop.getSummaryFor(mDecl);
      //doInherit(mDecl);
      
//      Collection reqsFromParent = ColorPromises.getReqDrops(mDecl);
//      if (reqsFromParent.isEmpty()) {
//        reqsFromParent = ColorPromises.getInheritedRequireDrops(mDecl);
//      }
      
//      Collection ctxFromParent = ColorPromises.getCtxDrops(mDecl);
//      if (ctxFromParent.isEmpty()) {
//        ctxFromParent = ColorPromises.getInheritedContextDrops(mDecl);
//      }
      
      boolean icrFromParent = ColorPromises.isColorRelevant(mDecl);
      final String parentName = JJNode.getInfo(mDecl);
      
      boolean somethingToInherit = !parentReqSum.isInferred();
      
      if (!somethingToInherit) {
        // there was nothing to inherit here, so just...
        if (finer) {
          LOG.finer("nothing to inherit from parent " + parentName);
        }
        continue;
      }
      
      ColorReqSummaryDrop reqSumHere = ColorReqSummaryDrop.getSummaryFor(mth);
      parentReqSum = ColorReqSummaryDrop.getSummaryFor(mDecl);
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
    if (finer) {
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
  private static void checkOverriddenInheritance(ColorReqSummaryDrop parentReqSum, ColorReqSummaryDrop reqSumHere) {
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
      warnMsg.append("@colorConstraint " + reqSumHere.getFullExpr() + "  on  " + idStr);
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
          ColorMessages.createWarningDrop(warnMsg.toString(), here);
      }
//            wd.addSupportingInformation(reqSumHere.getFullExpr().toString() +
//                                        " does not imply " + parentReqSum.getFullExpr(), null);
    }
  }

  public static ColorInherit getInstance() {
    return INSTANCE;
  }

 
}