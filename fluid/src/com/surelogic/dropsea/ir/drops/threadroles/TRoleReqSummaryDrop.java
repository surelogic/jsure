/*
 * Created on Nov 5, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.threadroles;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import SableJBDD.bdd.JBDD;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.threadroles.TRExpr;
import com.surelogic.analysis.threadroles.TRoleBDDPack;
import com.surelogic.analysis.threadroles.TRoleInherit;
import com.surelogic.analysis.threadroles.TRoleMessages;
import com.surelogic.annotation.rules.ThreadRoleRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.Sea;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author dfsuther
 * 
 * @-lock ColorReqSummaryLock is class protects nodeToDrop
 */
public class TRoleReqSummaryDrop extends PromiseDrop implements IThreadRoleDrop {

  private static final Logger LOG = SLLogger.getLogger("TRoleDropBuilding");

  private static final String kind = "ThreadRoleConstraint summary";

  private static final boolean tryingChainRules = true;

  private boolean localEmpty = true;

  // The transitively computed simple (e.g. w/o conflicts) and full (e.g. w/
  // conflicts)
  // requirement expressions for the attached method.
  private JBDD simpleExpr;

  private JBDD fullExpr;

  private String fullExprStr = null;

  // // The simple and full requirement expressions for the attached method,
  // considering
  // // ONLY explicit annos (local or inherited, depending on this drop).
  // Explicit
  // // does include drops from annos in XML files. It does not include
  // transitive
  // // information computed by analysis.
  // private JBDD localSimpleExpr;
  // private JBDD localFullExpr;

  // public boolean containsGrantOrRevoke = false;

  private String methodName;

  private ResultDrop resDrop;

  // private final Set localReqs = new HashSet(0);

  private boolean inheritanceDone = false;

  private boolean reqsAreRelevant = true;

  private boolean cutPoint = false;

  private Set<PromiseDrop<? extends IAASTRootNode>> userDeponents = new HashSet<PromiseDrop<? extends IAASTRootNode>>(1);
  private boolean userDepsContainsThis;

  private Status whichStat = Status.INFERRED;

  public static class Status {

    private final String kind;

    private Status(String stat) {
      kind = stat.intern();
    }

    public static final Status USER = new Status("user-written");

    public static final Status INHERITED = new Status("inherited");

    public static final Status INFERRED = new Status("inferred");
  }

  // public static TRoleReqSummaryDrop getSummaryFor(TRoleRequireDrop proto) {
  // final IRNode node = proto.getNode();
  // TRoleReqSummaryDrop res = ColorRules.getReqSummDrop(node);
  //
  // if (res == null) {
  // res = new TRoleReqSummaryDrop(proto, Inherited.NO);
  // ColorRules.setReqSummDrop(node, res);
  // }
  //
  // return res;
  // }

  private static final String emptyRelevantMsg = " no user @colorConstraint for ";
  private static final String emptyNotRelevantMsg = "@transparent for ";

  public static TRoleReqSummaryDrop getSummaryFor(IRNode node) {
    TRoleReqSummaryDrop res = null;

    res = ThreadRoleRules.getReqSummDrop(node);

    if (res != null) {
      // already have a summary for this node. Return it.
      return res;
    }

    final Operator op = JJNode.tree.getOperator(node);
    if ((MethodDeclaration.prototype.includes(op) || ConstructorDeclaration.prototype.includes(op))) {
      // build a interface color requirements

      final String mthName = JJNode.getInfo(node);
      // final String mthFullName =
      // JavaNames.genQualifiedMethodConstructorName(node);
      final String msg = "Color Requirements for " + mthName;

      if (LOG.isLoggable(Level.FINER)) {
        LOG.finer("computing " + msg);
      }

      res = new TRoleReqSummaryDrop(node);
      //res.setNodeAndCompilationUnitDependency(node);
      ThreadRoleRules.setReqSummDrop(node, res);

      final TRoleRequireDrop proto = ThreadRoleRules.getReqDrop(node);
      boolean haveCNR = !res.reqsAreRelevant;
      if (haveCNR && (proto != null)) {
        // problem: we have local @requiresColor annos AND an @transparent!
        // reject one or the other.
        ResultDrop rd = TRoleMessages.createProblemDrop("Error: @colorConstraint and @transparent on same method (" + mthName
            + ").", "TODO: Fill Me In", node);
        rd.addCheckedPromise(res);
        rd.setInconsistent();
      }

      // Collection iProtos = ColorRules.getInheritedRequireDrops(node);
      // protos.addAll(iProtos);

      if (proto == null) {
        // make an "empty" summary placed at this node. Constructor maintains
        // the nodeToDrop mapping for us.

        if (res.reqsAreRelevant) {
          res.setMessage(12,emptyRelevantMsg + mthName);
          res.setVirtual(true);
          res.setWhichStat(Status.INFERRED);
          if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(msg + ": empty.");
          }
        } else {
          res.setMessage(12,emptyNotRelevantMsg + mthName);
          res.setWhichStat(Status.USER);
          if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(msg + ": transparent");
          }
          res.setVirtual(false);
        }

      } else {
        // Have a user-written requiresColor anno to summarize.

        res.localEmpty = false;
        // res.setNodeAndCompilationUnitDependency(node);
        res.userDeponents.add(res);
        res.cutPoint = true;
        res.setWhichStat(Status.USER);
        boolean fromSrc = false;
        if (LOG.isLoggable(Level.FINER)) {
          LOG.finer("adding " + proto.getRenamedExpr() + " to " + msg);
        }
        final TRExpr renamed = proto.getRenamedExpr();
        JBDD tSimpleExpr = renamed.computeExpr(false);
        Set<String> refdNames = renamed.posReferencedColorNames();

        // use refdNames to get the proper conflicts needed to compute
        // tFullExpr
        JBDD conflictExpr = res.namesToConflictExpr(refdNames, node);
        JBDD tFullExpr = tSimpleExpr.and(conflictExpr);

        res.simpleExpr = res.simpleExpr.and(tSimpleExpr);
        res.fullExpr = res.fullExpr.and(tFullExpr);
        // proto.addDependent(res);
        fromSrc |= proto.isFromSrc();
        res.referencePromiseAnnotation(proto.getNode(), "@colorConstraint " + renamed);
        res.setFromSrc(fromSrc);
        // res.localFullExpr = res.fullExpr.copy();
        // res.localSimpleExpr = res.simpleExpr.copy();
        // String msg1;

        res.setMessage(12,"@colorConstraint " + res.fullExpr + " for " + mthName);

      }
      if (LOG.isLoggable(Level.FINER)) {
        LOG.finer("built " + res.getMessage());
      }
    } else {
      // build an inferred color requirements
      // res = new TRoleReqSummaryDrop(node);
      // res.setMessage("(partial) Inferred color requirement");
      res = null;
      LOG.severe("Internal requirements summaries ARE NOT SUPPORTED!");
      return null;
    }

    TRoleInherit.doInherit(node);
    res.inheritanceDone = true;

    if (tryingChainRules) {
      // final List<TRoleRenameDrop> chainRule = TRoleRenameDrop.getChainRule();
      res.fullExpr = TRoleRenameDrop.applyChainRule(res.fullExpr, res);
    }

    if (!res.getMessage().equals("(EMPTY)")) {
      return res;
    }

    return res;
  }

  /**
   * Build a ColorReqSummary drop for a node that has no user-written
   * requiresColor annos attached to it. The resulting drop will have the node
   * as its location and will depend on the node's CU.
   * 
   * @param node
   *          The node to build the summary for.
   */
  private TRoleReqSummaryDrop(IRNode node) {
	super(null);
    simpleExpr = TRoleBDDPack.one();
    fullExpr = TRoleBDDPack.one();
    // localSimpleExpr = ColorBDDPack.one();
    // localFullExpr = ColorBDDPack.one();
  //  setNodeAndCompilationUnitDependency(node);
    // resDrop = null;
    methodName = JJNode.getInfo(node);

    if (!ThreadRoleRules.isTRoleRelevant(node)) {
      reqsAreRelevant = false;
      cutPoint = true;
      setMessage(12,"transparent for " + methodName);
    } else {
      setMessage(12,"(partial) colorConstraint for " + methodName);
    }

    assert (ThreadRoleRules.getReqSummDrop(node) == null);
    ThreadRoleRules.setReqSummDrop(node, this);

    setCategory(TRoleMessages.assuranceCategory);
    userDeponents.add(this);
    userDepsContainsThis = true;
  }

  // private TRoleReqSummaryDrop(TRoleRequireDrop proto) {
  // final IRNode node = proto.getNode();
  //
  // methodName = JavaNode.getInfo(node);
  //
  // CExpr rawExpr = proto.getRawExpr();
  //
  // whichStat = Status.USER;
  //
  // simpleExpr = rawExpr.computeExpr(false);
  // JBDD conflictExpr = namesToConflictExpr(rawExpr.posReferencedColorNames());
  // fullExpr = simpleExpr.and(conflictExpr);
  // // localSimpleExpr = simpleExpr.copy();
  // // localFullExpr = fullExpr.copy();
  //
  // // resDrop = null;
  //
  // this.referencePromiseAnnotation(proto.getNode(), "@colorRequired "
  // + rawExpr);
  // // proto.addDependent(this);
  // setNodeAndCompilationUnitDependency(node);
  // cutPoint = true;
  // if (!ColorRules.isColorRelevant(node)) {
  // ResultDrop pd = TRoleMessages
  // .createProblemDrop(
  // "@colorRequired and @transparent are incompatible.",
  // node);
  // pd.addCheckedPromise(this);
  // }
  //
  // ColorRules.setReqSummDrop(node, this);
  // setMessage("(partial) colorRequired for " + methodName);
  //
  // setCategory(TRoleMessages.assuranceCategory);
  // setFromSrc(proto.isFromSrc());
  // userDeponents.add(proto);
  // userDepsContainsThis = false;
  // }

  /**
   * Called when inheriting from a parent method for the very first time.
   * Implication: whichStat MUST BE INFERRED!
   * 
   * @param parent
   *          The drop we're inheriting from.
   */
  public void inheritingFrom(TRoleReqSummaryDrop parent) {
    assert (whichStat == Status.INFERRED);

    setWhichStat(TRoleReqSummaryDrop.Status.INHERITED);

    cutPoint = true;
    setFullExpr(parent.getFullExpr());
    // don't do anything with inheritanceDone yet!
    assert (!parent.localEmpty);
    localEmpty = false; // because we're inheriting something that's not empty!
    // don't do anything with methodName...
    reqsAreRelevant = parent.reqsAreRelevant;
    if (!reqsAreRelevant) {
      cutPoint = true;
    }
    simpleExpr = parent.getSimpleExpr();
    addAllToUserDeponents(parent.getUserDeponents());

    parent.addDependent(this);
    setMessage(12,"@ThreadRole " + fullExpr + " for " + methodName);
  }

  private JBDD namesToConflictExpr(Collection<String> names, final IRNode where) {
    if ((names == null) || names.isEmpty())
      return TRoleBDDPack.one();

    JBDD res = TRoleBDDPack.one();
    for (String name : names) {
      TRoleNameModel canonModel = TRoleNameModel.getCanonicalInstance(name, where);
      TRoleIncSummaryDrop incSumm = canonModel.getIncompatibleSummary();

      JBDD tExpr = incSumm.getConflictExpr();
      incSumm.addDependent(this);
      res.andWith(tExpr);
    }
    return res;
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    // don't track these in TRoleFirstPass. They may be created there early, but
    // they are fundamentally creatures of TRoleSecondPass.
    // TRoleFirstPass.trackCUchanges(this);

    super.deponentInvalidAction(invalidDeponent);
  }

  /**
   * @return Returns the fullExpr.
   */
  public JBDD getFullExpr() {
    return fullExpr.copy();
  }

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

  /**
   * @return Returns the resDrop. Never null or !isValid()
   */
  public ResultDrop getResDrop() {
    if ((resDrop == null) || (!resDrop.isValid())) {
      resDrop = TRoleMessages.createResultDrop("@ThreadRole " + getReqString() + " for " + methodName, "TODO: fill me in",
          getNode());
      resDrop.addCheckedPromise(this);
    }

    @SuppressWarnings("unchecked")
    Collection<? extends TRoleRequireDrop> reqDrops = Sea.filterDropsOfType(TRoleRequireDrop.class, getDeponents());
    resDrop.addTrustedPromises(reqDrops);
    return resDrop;
  }

  /**
   * @return <code>true</code> when local is empty.
   */
  public boolean isLocalEmpty() {
    return localEmpty;
  }

  public boolean isEmpty() {
    if (fullExpr == null)
      return true;
    return fullExpr.isZero() || fullExpr.isOne();
  }

  /**
   * @return Returns the inheritanceDone.
   */
  public boolean isInheritanceDone() {
    return inheritanceDone;
  }

  /**
   * @param inheritanceDone
   *          The inheritanceDone to set.
   */
  public void setInheritanceDone() {
    inheritanceDone = true;
  }

  /**
   * @return Returns the cutPoint.
   */
  public boolean isCutPoint() {
    return cutPoint;
  }

  /**
   * @return Returns the reqsAreRelevant.
   */
  public boolean reqsAreRelevant() {
    return reqsAreRelevant;
  }

  /**
   * @param reqsAreRelevant
   *          The reqsAreRelevant to set.
   */
  public void setReqsAreRelevant(boolean reqsAreRelevant) {
    this.reqsAreRelevant = reqsAreRelevant;
    if (!this.reqsAreRelevant) {
      cutPoint = true;
    }
  }

  /**
   * @return Returns the userDeponents.
   */
  public Set<PromiseDrop<? extends IAASTRootNode>> getUserDeponents() {
    return userDeponents;
  }

  public void addToUserDeponents(PromiseDrop<? extends IAASTRootNode> drop) {
    if (drop == null || drop == this) {
      return;
    }
    if (userDepsContainsThis) {
      userDeponents.remove(this);
      userDepsContainsThis = false;
    }

    userDeponents.add(drop);
  }

  public void addAllToUserDeponents(Collection<PromiseDrop<? extends IAASTRootNode>> promiseDrops) {
    if (promiseDrops == null || promiseDrops.isEmpty()) {
      return;
    }
    if (userDepsContainsThis) {
      userDeponents.remove(this);
      userDepsContainsThis = false;
    }
    userDeponents.addAll(promiseDrops);
  }

  /**
   * @return Returns the whichStat.
   */
  public Status getWhichStat() {
    return whichStat;
  }

  /**
   * @param whichStat
   *          The whichStat to set.
   */
  public void setWhichStat(Status whichStat) {
    this.whichStat = whichStat;
  }

  public boolean isInferred() {
    // cutPoint should be true when USER or INHERITED, cutpoint should be false
    // when
    // INFERRED.
    assert (cutPoint ? (whichStat == Status.USER) || (whichStat == Status.INHERITED) : whichStat == Status.INFERRED);
    return whichStat == Status.INFERRED;
  }

  public boolean isUser() {
    assert (cutPoint ? (whichStat == Status.USER) || (whichStat == Status.INHERITED) : whichStat == Status.INFERRED);
    return whichStat == Status.USER;
  }

  public boolean isInherited() {
    assert (cutPoint ? (whichStat == Status.USER) || (whichStat == Status.INHERITED) : whichStat == Status.INFERRED);
    return whichStat == Status.INHERITED;
  }

  public String getReqString() {
    return getReqString(false);
  }

  public String getReqString(boolean wantQualName) {
    if (reqsAreRelevant) {
      if (isEmpty()) {
        return "no known ThreadRole";
      } else {
        if (fullExprStr != null) {
          return fullExprStr;
        }

        String t = TRoleRenamePerCU.jbddMessageName(fullExpr, wantQualName);
        if (!t.equals(fullExpr.toString())) {
          fullExprStr = t;
        }
        return t;
      }
    } else {
      return "@transparent";
    }
  }
}