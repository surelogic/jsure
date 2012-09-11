/*
 * Created on Nov 6, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.threadroles;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import SableJBDD.bdd.JBDD;

import com.surelogic.RequiresLock;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.threadroles.TRoleBDDPack;
import com.surelogic.analysis.threadroles.TRoleMessages;
import com.surelogic.annotation.rules.ThreadRoleRules;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.BlockStatement;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author dfsuther
 */
public class TRoleCtxSummaryDrop extends IRReferenceDrop implements IThreadRoleDrop {

  // private static final String kind = "colorContext summary";

  private static final Logger LOG = SLLogger.getLogger("TRoleDropBuilding");

  // public boolean containsGrantOrRevoke = false;

  // private boolean localEmpty = true;

  private JBDD simpleExpr;

  private JBDD fullExpr;

  // private JBDD localSimpleExpr;
  //
  // private JBDD localFullExpr;

  private ResultDrop resDrop;

  private String methodName;

  // private Set localCtxts = new HashSet(0);

  private Set<PromiseDrop<? extends IAASTRootNode>> userDeponents = new HashSet<PromiseDrop<? extends IAASTRootNode>>(1);

  public static TRoleCtxSummaryDrop getSummaryFor(IRNode node) {
    TRoleCtxSummaryDrop res = ThreadRoleRules.getCtxSummDrop(node);

    if (res != null) {
      // already have a summary for this node. Return it.
      return res;
    }

    String msg;

    res = new TRoleCtxSummaryDrop(node);

    final Operator op = JJNode.tree.getOperator(node);
    if ((MethodDeclaration.prototype.includes(op) || ConstructorDeclaration.prototype.includes(op))) {
      // summaries of calling context go on decls
      // these are always created "empty" and computed by inference.
      msg = "Calling color context for " + JJNode.getInfo(node);

      // Collection<ColorContextDrop> ctxDrops = ColorRules.getCtxDrops(node);
      // if (!ctxDrops.isEmpty()) {
      res.fullExpr = TRoleBDDPack.zero();
      // }
      // for (ColorContextDrop protoCtx : ctxDrops) {
      // // ColorContextDrop protoCtx = (ColorContextDrop) ctxIter.next();
      //
      // LOG.fine("adding " + protoCtx.getRenamedExpr() + " to " + msg);
      //
      // final CExpr renamed = protoCtx.getRenamedExpr();
      // JBDD tSimpleExpr = renamed.computeExpr(false);
      // Set<String> refdNames = renamed.posReferencedColorNames();
      // final IRNode cu = VisitUtil.computeOutermostEnclosingTypeOrCU(node);
      //
      // // use refdNames to get the proper conflicts needed to compute
      // tFullExpr
      // JBDD conflictExpr = res.namesToConflictExpr(refdNames, cu);
      // JBDD tFullExpr = tSimpleExpr.and(conflictExpr);
      //
      // res.simpleExpr = res.simpleExpr.or(tSimpleExpr);
      // res.fullExpr = res.fullExpr.or(tFullExpr);
      // protoCtx.addDependent(res);
      // res.userDeponents.add(protoCtx);
      // }
      // // ResultDrop rd = res.getResDrop();
      // // rd.addCheckedPromise(res);
      // //rd.addTrustedPromises(ctxDrops); // Wrong!

    } else {
      // an internal context. These are always created empty. It's the caller's
      // problem to fill them in with the correct values and dependencies.
      assert (BlockStatement.prototype.includes(op));
      msg = "Internal color context";
    }

    // ColorCtxSummaryDrops are ALWAYS virtual, because they summarize context
    // changes
    // that come either from one or more callers, or from grants or revokes. In
    // any of these cases, the context summary is a derived item.
    // res.setVirtual(true);
    res.setMessage(msg);
    return res;
  }

  // public static ColorCtxSummaryDrop getSummaryFor(final ColorContextDrop
  // proto) {
  // final IRNode node = proto.getNode();
  // ColorCtxSummaryDrop res = ColorRules.getCtxSummDrop(node);
  //
  // if (res == null) {
  // res = new ColorCtxSummaryDrop(proto, TRoleReqSummaryDrop.Inherited.NO);
  // }
  //
  // return res;
  // }

  /**
   * Build a ColorCtxSummary drop for a node that has no user-written
   * requiresColor annos attached to it. The resulting drop will have the node
   * as its location and will depend on the node's CU.
   * 
   * @param node
   *          The node to build the summary for.
   */
  private TRoleCtxSummaryDrop(IRNode node) {
    simpleExpr = TRoleBDDPack.zero();
    fullExpr = TRoleBDDPack.zero();
    // localSimpleExpr = ColorBDDPack.zero();
    // localFullExpr = ColorBDDPack.zero();
    resDrop = null;

    methodName = JJNode.getInfoOrNull(node);
    if (methodName == null) {
      System.out.println("Got null methodName");
    }

    setNodeAndCompilationUnitDependency(node);

    assert (ThreadRoleRules.getCtxSummDrop(node) == null);
    ThreadRoleRules.setCtxSummDrop(node, this);

    setCategory(TRoleMessages.assuranceCategory);
  }

  // private ColorCtxSummaryDrop(ColorContextDrop proto,
  // TRoleReqSummaryDrop.Inherited isit) {
  // CExpr rawExpr = proto.getRawExpr();
  // final IRNode node = proto.getNode();
  // methodName = JavaNode.getInfo(node);
  //
  // simpleExpr = rawExpr.computeExpr(false);
  // JBDD conflictExpr = namesToConflictExpr(rawExpr.posReferencedColorNames());
  // fullExpr = simpleExpr.and(conflictExpr);
  //
  // localSimpleExpr = simpleExpr.copy();
  // localFullExpr = fullExpr.copy();
  //
  // resDrop = null;
  //
  // proto.addDependent(this);
  // if (isit == Inherited.NO) {
  // assert (ColorRules.getCtxSummDrop(node) == null);
  // ColorRules.setCtxSummDrop(node, this);
  // setMessage("summary of colorContext for " + methodName);
  // } else if (isit == Inherited.YES) {
  // assert (ColorRules.getInheritedCtxSummDrop(node) == null);
  // ColorRules.setInheritedCtxSummDrop(node, this);
  // setMessage("summary of Inherited colorContext for " + methodName);
  // }
  // setCategory(TRoleMessages.assuranceCategory);
  // setFromSrc(proto.isFromSrc());
  // }
  //
  private JBDD namesToConflictExpr(Collection<String> names, final IRNode cu) {
    if ((names == null) || names.isEmpty())
      return TRoleBDDPack.one();

    JBDD res = TRoleBDDPack.one();
    // Iterator<String> nameIter = names.iterator();
    // while (nameIter.hasNext()) {
    // String name = nameIter.next();
    for (String name : names) {
      TRoleNameModel model = TRoleNameModel.getInstance(name, cu);
      // TRoleName tc = model.getCanonicalTColor();
      // TRoleNameModel canonModel = tc.getCanonicalNameModel();
      final TRoleNameModel canonModel = model.getCanonicalNameModel();
      TRoleIncSummaryDrop incSumm = canonModel.getIncompatibleSummary();

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

  // /**
  // * @return Returns the localFullExpr.
  // */
  // public JBDD getLocalFullExpr() {
  // return localFullExpr.copy();
  // }
  //
  // /**
  // * @return Returns the localSimpleExpr.
  // */
  // public JBDD getLocalSimpleExpr() {
  // return localSimpleExpr.copy();
  // }

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

  // /**
  // * @return Returns the resDrop. NEVER !isValid(), but may be null.
  // */
  // public ResultDrop getResDrop() {
  // if ((resDrop == null) || (!resDrop.isValid())) {
  // resDrop = TRoleMessages.createResultDrop("Color Requirement summary for "
  // + methodName, getNode());
  // resDrop.addCheckedPromise(this);
  // }
  //
  // Collection ctxDrops = Sea.filterDropsOfType(ColorContextDrop.class,
  // getDeponents());
  // resDrop.addTrustedPromises(ctxDrops);
  // return resDrop;
  // }

  // /**
  // * @param resDrop The resDrop to set.
  // */
  // public void setResDrop(ResultDrop resDrop) {
  // this.resDrop = resDrop;
  // }
  // /**
  // * @return Returns the localCtxts.
  // */
  // public Set getLocalCtxts() {
  // return localCtxts;
  // }

  // /**
  // * @return Returns the empty.
  // */
  // public boolean isLocalEmpty() {
  // return localEmpty;
  // }

  public boolean isEmpty() {
    if (fullExpr == null)
      return true;
    return fullExpr.isZero();
  }

  /**
   * @return Returns the userDeponents.
   */
  public Set<PromiseDrop<? extends IAASTRootNode>> getUserDeponents() {
    return userDeponents;
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  @RequiresLock("SeaLock")
  protected void deponentInvalidAction(Drop invalidDeponent) {
    // TRolesFirstPass.trackCUchanges(this);
    // NO! ColorCtxSummaryDrops belong to colorSecondPass, and should not
    // request
    // re-running CFP on the enclosing CU.
    super.deponentInvalidAction(invalidDeponent);
  }
}