/*
 * Created on Nov 6, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import SableJBDD.bdd.JBDD;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.analysis.*;
import edu.cmu.cs.fluid.java.bind.ColorPromises;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author dfsuther
 */
@Deprecated
public class ColorCtxSummaryDrop extends IRReferenceDrop implements PleaseFolderize {

  // private static final String kind = "colorContext summary";

  private static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");

//  public boolean containsGrantOrRevoke = false;

//  private boolean localEmpty = true;

  private JBDD simpleExpr;

  private JBDD fullExpr;

//  private JBDD localSimpleExpr;
//
//  private JBDD localFullExpr;

  private ResultDrop resDrop;

  private String methodName;

//  private Set localCtxts = new HashSet(0);
  
  private Set<PromiseDrop> userDeponents = new HashSet<PromiseDrop>(1);

  public static ColorCtxSummaryDrop getSummaryFor(IRNode node) {
    ColorCtxSummaryDrop res = ColorPromises.getCtxSummDrop(node);

    if (res != null) {
      // already have a summary for this node. Return it.
      return res;
    }

    String msg;
    
    res = new ColorCtxSummaryDrop(node);

    final Operator op = JJNode.tree.getOperator(node);
    if ((MethodDeclaration.prototype.includes(op) || ConstructorDeclaration.prototype
        .includes(op))) {
      // summaries of calling context go on decls
      // these are always created "empty" and computed by inference.
      msg = "Calling color context for " + JJNode.getInfo(node);

      Collection<ColorContextDrop> ctxDrops = ColorPromises.getCtxDrops(node);
      if (!ctxDrops.isEmpty()) {
        res.fullExpr = ColorBDDPack.zero();
      }
      for (ColorContextDrop protoCtx : ctxDrops) {
//        ColorContextDrop protoCtx = (ColorContextDrop) ctxIter.next();

        LOG.fine("adding " + protoCtx.getRenamedExpr() + " to " + msg);

        final CExpr renamed = protoCtx.getRenamedExpr();
        JBDD tSimpleExpr = renamed.computeExpr(false);
        Set<String> refdNames = renamed.posReferencedColorNames();
        final IRNode cu = VisitUtil.computeOutermostEnclosingTypeOrCU(node);

        // use refdNames to get the proper conflicts needed to compute tFullExpr
        JBDD conflictExpr = res.namesToConflictExpr(refdNames, cu);
        JBDD tFullExpr = tSimpleExpr.and(conflictExpr);

        res.simpleExpr = res.simpleExpr.or(tSimpleExpr);
        res.fullExpr = res.fullExpr.or(tFullExpr);
        protoCtx.addDependent(res);
        res.userDeponents.add(protoCtx);
      }
//      ResultDrop rd = res.getResDrop();
//      rd.addCheckedPromise(res);
      //rd.addTrustedPromises(ctxDrops); // Wrong!

    } else {
      // an internal context. These are always created empty. It's the caller's
      // problem to fill them in with the correct values and dependencies.
      assert (BlockStatement.prototype.includes(op));
      msg = "Internal color context";
    }

    // ColorCtxSummaryDrops are ALWAYS virtual, because they summarize context changes
    // that come either from one or more callers, or from grants or revokes.  In
    // any of these cases, the context summary is a derived item.
//    res.setVirtual(true);
    res.setMessage(msg);
    return res;
  }

  //  public static ColorCtxSummaryDrop getSummaryFor(final ColorContextDrop
  // proto) {
  //    final IRNode node = proto.getNode();
  //    ColorCtxSummaryDrop res = ColorPromises.getCtxSummDrop(node);
  //    
  //    if (res == null) {
  //      res = new ColorCtxSummaryDrop(proto, ColorReqSummaryDrop.Inherited.NO);
  //    }
  //    
  //    return res;
  //  }

  /**
   * Build a ColorCtxSummary drop for a node that has no user-written
   * requiresColor annos attached to it. The resulting drop will have the node
   * as its location and will depend on the node's CU.
   * 
   * @param node
   *          The node to build the summary for.
   */
  private ColorCtxSummaryDrop(IRNode node) {
    simpleExpr = ColorBDDPack.zero();
    fullExpr = ColorBDDPack.zero();
//    localSimpleExpr = ColorBDDPack.zero();
//    localFullExpr = ColorBDDPack.zero();
    resDrop = null;

    methodName = JJNode.getInfo(node);

    setNodeAndCompilationUnitDependency(node);

    assert (ColorPromises.getCtxSummDrop(node) == null);
    ColorPromises.setCtxSummDrop(node, this);

    setCategory(ColorMessages.assuranceCategory);
  }

  //  private ColorCtxSummaryDrop(ColorContextDrop proto,
  // ColorReqSummaryDrop.Inherited isit) {
  //    CExpr rawExpr = proto.getRawExpr();
  //    final IRNode node = proto.getNode();
  //    methodName = JavaNode.getInfo(node);
  //    
  //    simpleExpr = rawExpr.computeExpr(false);
  //    JBDD conflictExpr = namesToConflictExpr(rawExpr.posReferencedColorNames());
  //    fullExpr = simpleExpr.and(conflictExpr);
  //    
  //    localSimpleExpr = simpleExpr.copy();
  //    localFullExpr = fullExpr.copy();
  //    
  //    resDrop = null;
  //
  //    proto.addDependent(this);
  //    if (isit == Inherited.NO) {
  //      assert (ColorPromises.getCtxSummDrop(node) == null);
  //      ColorPromises.setCtxSummDrop(node, this);
  //      setMessage("summary of colorContext for " + methodName);
  //    } else if (isit == Inherited.YES) {
  //      assert (ColorPromises.getInheritedCtxSummDrop(node) == null);
  //      ColorPromises.setInheritedCtxSummDrop(node, this);
  //      setMessage("summary of Inherited colorContext for " + methodName);
  //    }
  //    setCategory(ColorMessages.assuranceCategory);
  //    setFromSrc(proto.isFromSrc());
  //  }
  //  
  private JBDD namesToConflictExpr(Collection<String> names, final IRNode cu) {
    if ((names == null) || names.isEmpty()) return ColorBDDPack.one();

    JBDD res = ColorBDDPack.one();
//    Iterator<String> nameIter = names.iterator();
//    while (nameIter.hasNext()) {
//      String name = nameIter.next();
    for (String name : names) {
      ColorNameModel model = ColorNameModel.getInstance(name, cu);
//      TColor tc = model.getCanonicalTColor();
//      ColorNameModel canonModel = tc.getCanonicalNameModel();
      final ColorNameModel canonModel = model.getCanonicalNameModel();
      ColorIncSummaryDrop incSumm = canonModel.getIncompatibleSummary();

      JBDD tExpr = incSumm.getConflictExpr();
      incSumm.addDependent(this);
      res.andWith(tExpr);
    }
    return res;
  }

  /**
   * @return Returns the fullExpr.
   */
  public JBDD getFullExpr() {
    return fullExpr.copy();
  }

//  /**
//   * @return Returns the localFullExpr.
//   */
//  public JBDD getLocalFullExpr() {
//    return localFullExpr.copy();
//  }
//
//  /**
//   * @return Returns the localSimpleExpr.
//   */
//  public JBDD getLocalSimpleExpr() {
//    return localSimpleExpr.copy();
//  }

  /**
   * @return Returns the simpleExpr.
   */
  public JBDD getSimpleExpr() {
    return simpleExpr.copy();
  }

  /**
   * @param fullExpr
   *          The fullExpr to set.
   */
  public void setFullExpr(JBDD fullExpr) {
    this.fullExpr = fullExpr;
  }

//  /**
//   * @return Returns the resDrop. NEVER !isValid(), but may be null.
//   */
//  public ResultDrop getResDrop() {
//    if ((resDrop == null) || (!resDrop.isValid())) {
//      resDrop = ColorMessages.createResultDrop("Color Requirement summary for "
//          + methodName, getNode());
//      resDrop.addCheckedPromise(this);
//    }
//
//    Collection ctxDrops = Sea.filterDropsOfType(ColorContextDrop.class,
//                                                getDeponents());
//    resDrop.addTrustedPromises(ctxDrops);
//    return resDrop;
//  }

  //  /**
  //   * @param resDrop The resDrop to set.
  //   */
  //  public void setResDrop(ResultDrop resDrop) {
  //    this.resDrop = resDrop;
  //  }
//  /**
//   * @return Returns the localCtxts.
//   */
//  public Set getLocalCtxts() {
//    return localCtxts;
//  }

//  /**
//   * @return Returns the empty.
//   */
//  public boolean isLocalEmpty() {
//    return localEmpty;
//  }

  public boolean isEmpty() {
    if (fullExpr == null) return true;
    return fullExpr.isZero();
  }
  /**
   * @return Returns the userDeponents.
   */
  public Set<PromiseDrop> getUserDeponents() {
    return userDeponents;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
//    ColorFirstPass.trackCUchanges(this);
    // NO! ColorCtxSummaryDrops belong to colorSecondPass, and should not request
    // re-running CFP on the enclosing CU.
    super.deponentInvalidAction(invalidDeponent);
  }
}