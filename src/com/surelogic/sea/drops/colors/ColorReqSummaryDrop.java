/*
 * Created on Nov 5, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.sea.drops.colors;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.colors.*;
import com.surelogic.annotation.rules.*;
import com.surelogic.common.logging.SLLogger;

import SableJBDD.bdd.JBDD;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author dfsuther
 * 
 * @-lock ColorReqSummaryLock is class protects nodeToDrop
 */
public class ColorReqSummaryDrop extends PromiseDrop implements PleaseFolderize {

  private static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");

  private static final String kind = "colorConstraint summary";
  
  private static final boolean tryingChainRules = true;

  private boolean localEmpty = true;

  // The transitively computed simple (e.g. w/o conflicts) and full (e.g. w/
  // conflicts)
  // requirement expressions for the attached method.
  private JBDD simpleExpr;

  private JBDD fullExpr;
  
  private String fullExprStr = null;

  //  // The simple and full requirement expressions for the attached method,
  // considering
  //  // ONLY explicit annos (local or inherited, depending on this drop).
  // Explicit
  //  // does include drops from annos in XML files. It does not include
  // transitive
  //  // information computed by analysis.
  //  private JBDD localSimpleExpr;
  //  private JBDD localFullExpr;

//  public boolean containsGrantOrRevoke = false;

  private String methodName;

  private ResultDrop resDrop;

  //  private final Set localReqs = new HashSet(0);

  private boolean inheritanceDone = false;

  private boolean reqsAreRelevant = true;

  private boolean cutPoint = false;

  private Set<PromiseDrop> userDeponents = new HashSet<PromiseDrop>(1);
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

  //  public static ColorReqSummaryDrop getSummaryFor(ColorRequireDrop proto) {
  //    final IRNode node = proto.getNode();
  //    ColorReqSummaryDrop res = ColorRules.getReqSummDrop(node);
  //    
  //    if (res == null) {
  //      res = new ColorReqSummaryDrop(proto, Inherited.NO);
  //      ColorRules.setReqSummDrop(node, res);
  //    }
  //    
  //    return res;
  //  }

  private static final String emptyRelevantMsg = " no user @colorConstraint for ";
  private static final String emptyNotRelevantMsg = "@transparent for ";
  
  public static ColorReqSummaryDrop getSummaryFor(IRNode node) {
    ColorReqSummaryDrop res = null;

    res = ColorRules.getReqSummDrop(node);

    if (res != null) {
      // already have a summary for this node. Return it.
      return res;
    }

    final Operator op = JJNode.tree.getOperator(node);
    if ((MethodDeclaration.prototype.includes(op) || ConstructorDeclaration.prototype
        .includes(op))) {
      // build a interface color requirements
      Collection<ColorRequireDrop> protos = null;
      final String mthName = JJNode.getInfo(node);
//      final String mthFullName = JavaNames.genQualifiedMethodConstructorName(node);
      final String msg = "Color Requirements for " + mthName;
      
      if (LOG.isLoggable(Level.FINER)) {
    	  LOG.finer("computing " + msg);
      }

      res = new ColorReqSummaryDrop(node);
      res.setNodeAndCompilationUnitDependency(node);
      ColorRules.setReqSummDrop(node, res);
      
      protos = ColorRules.getReqDrops(node);
      boolean haveCNR = !res.reqsAreRelevant;
      if (haveCNR && !protos.isEmpty()) {
        // problem: we have local @requiresColor annos AND an @transparent!
        // reject one or the other.
        ResultDrop rd = ColorMessages
            .createProblemDrop(
                               "Error: @colorConstraint and @transparent on same method (" + mthName +").",
                               "TODO: Fill Me In", node);
        rd.addCheckedPromise(res);
        //rd.addTrustedPromises(protos);
        rd.setInconsistent();
      }

      //      Collection iProtos = ColorRules.getInheritedRequireDrops(node);
      //      protos.addAll(iProtos);

      if (protos.isEmpty()) {
        // make an "empty" summary placed at this node. Constructor maintains
        // the nodeToDrop mapping for us.

        if (res.reqsAreRelevant) {
          res.setMessage(emptyRelevantMsg + mthName);
          res.setVirtual(true);
          res.setWhichStat(Status.INFERRED);
          if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(msg + ": empty.");
          }
        } else {
          res.setMessage(emptyNotRelevantMsg + mthName);
          res.setWhichStat(Status.USER);
          if (LOG.isLoggable(Level.FINER)) {
            LOG.finer(msg + ": transparent");
          }
          res.setVirtual(false);
        }

      } else {
        // Have at least one user-written requiresColor anno to summarize.
        
        res.localEmpty = false;
        res.setNodeAndCompilationUnitDependency(node);
        res.userDeponents.add(res);
        res.cutPoint = !protos.isEmpty();
        res.setWhichStat(Status.USER);
        boolean fromSrc = false;
//        Iterator<ColorRequireDrop> protoIter = protos.iterator();
//        while (protoIter.hasNext()) {
//          ColorRequireDrop proto = protoIter.next();
        for (ColorRequireDrop proto : protos) {
          if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("adding " + proto.getRenamedExpr() + " to " + msg);
          }
          final CExpr renamed = proto.getRenamedExpr();
          JBDD tSimpleExpr = renamed.computeExpr(false);
          Set<String> refdNames = renamed.posReferencedColorNames();

          // use refdNames to get the proper conflicts needed to compute
          // tFullExpr
          JBDD conflictExpr = res.namesToConflictExpr(refdNames, node);
          JBDD tFullExpr = tSimpleExpr.and(conflictExpr);

          res.simpleExpr = res.simpleExpr.and(tSimpleExpr);
          res.fullExpr = res.fullExpr.and(tFullExpr);
          //proto.addDependent(res);
          fromSrc |= proto.isFromSrc();
          res.referencePromiseAnnotation(proto.getNode(), "@colorConstraint " + renamed);
        }
        res.setFromSrc(fromSrc);
        //    res.localFullExpr = res.fullExpr.copy();
        //    res.localSimpleExpr = res.simpleExpr.copy();
//        String msg1;

        res.setMessage("@colorConstraint " + res.fullExpr + " for " + mthName);

      }
      if (LOG.isLoggable(Level.FINER)) {
        LOG.finer("built " + res.getMessage());
      }
    } else {
      // build an inferred color requirements
      //      res = new ColorReqSummaryDrop(node);
      //      res.setMessage("(partial) Inferred color requirement");
      res = null;
      LOG.severe("Internal requirements summaries ARE NOT SUPPORTED!");
    }

//    ColorInherit.doInherit(node);
    res.inheritanceDone = true;
    
    if (tryingChainRules) {
      //final List<ColorRenameDrop> chainRule = ColorRenameDrop.getChainRule();
      res.fullExpr = ColorRenameDrop.applyChainRule(res.fullExpr, res);
    }

    if (!res.getMessage().equals("(EMPTY)")) { return res; }

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
  private ColorReqSummaryDrop(IRNode node) {
    simpleExpr = ColorBDDPack.one();
    fullExpr = ColorBDDPack.one();
    //    localSimpleExpr = ColorBDDPack.one();
    //    localFullExpr = ColorBDDPack.one();
    setNodeAndCompilationUnitDependency(node);
//    resDrop = null;
    methodName = JJNode.getInfo(node);

    if (!ColorRules.isColorRelevant(node)) {
      reqsAreRelevant = false;
      cutPoint = true;
      setMessage("transparent for " + methodName);
    } else {
      setMessage("(partial) colorConstraint for " + methodName);
    }

    assert (ColorRules.getReqSummDrop(node) == null);
    ColorRules.setReqSummDrop(node, this);

    setCategory(ColorMessages.assuranceCategory);
    userDeponents.add(this);
    userDepsContainsThis = true;
  }

//  private ColorReqSummaryDrop(ColorRequireDrop proto) {
//    final IRNode node = proto.getNode();
//
//    methodName = JavaNode.getInfo(node);
//
//    CExpr rawExpr = proto.getRawExpr();
//
//    whichStat = Status.USER;
//
//    simpleExpr = rawExpr.computeExpr(false);
//    JBDD conflictExpr = namesToConflictExpr(rawExpr.posReferencedColorNames());
//    fullExpr = simpleExpr.and(conflictExpr);
//    //    localSimpleExpr = simpleExpr.copy();
//    //    localFullExpr = fullExpr.copy();
//
////    resDrop = null;
//
//    this.referencePromiseAnnotation(proto.getNode(), "@colorRequired "
//        + rawExpr);
//    //    proto.addDependent(this);
//    setNodeAndCompilationUnitDependency(node);
//    cutPoint = true;
//    if (!ColorRules.isColorRelevant(node)) {
//      ResultDrop pd = ColorMessages
//          .createProblemDrop(
//                             "@colorRequired and @transparent are incompatible.",
//                             node);
//      pd.addCheckedPromise(this);
//    }
//
//    ColorRules.setReqSummDrop(node, this);
//    setMessage("(partial) colorRequired for " + methodName);
//
//    setCategory(ColorMessages.assuranceCategory);
//    setFromSrc(proto.isFromSrc());
//    userDeponents.add(proto);
//    userDepsContainsThis = false;
//  }
  
  /**
   * Called when inheriting from a parent method for the very first time.  
   * Implication: whichStat MUST BE INFERRED!
   * 
   * @param parent The drop we're inheriting from.
   */
  public void inheritingFrom(ColorReqSummaryDrop parent) {
    assert(whichStat == Status.INFERRED);
    
    setWhichStat(ColorReqSummaryDrop.Status.INHERITED);
    
    cutPoint = true;
    setFullExpr(parent.getFullExpr());
    // don't do anything with inheritanceDone yet!
    assert(!parent.localEmpty);
    localEmpty = false; // because we're inheriting something that's not empty!
    // don't do anything with methodName...
    reqsAreRelevant = parent.reqsAreRelevant;
    if (!reqsAreRelevant) {
      cutPoint = true;
    }
    simpleExpr = parent.getSimpleExpr();
    addAllToUserDeponents(parent.getUserDeponents());
    
    parent.addDependent(this); 
    setMessage("@colorConstraint " + fullExpr + " for " + methodName);
  }

  private JBDD namesToConflictExpr(Collection<String> names, final IRNode where) {
    if ((names == null) || names.isEmpty()) return ColorBDDPack.one();

    JBDD res = ColorBDDPack.one();
//    Iterator<String> nameIter = names.iterator();
//    while (nameIter.hasNext()) {
//      String name = nameIter.next();
    for (String name : names) {
//      ColorNameModel model = ColorNameModel.getInstance(name);
//      TColor tc = model.getCanonicalTColor();
//      ColorNameModel canonModel = tc.getCanonicalNameModel();
      ColorNameModel canonModel = ColorNameModel.getCanonicalInstance(name, where);
      ColorIncSummaryDrop incSumm = canonModel.getIncompatibleSummary();

      JBDD tExpr = incSumm.getConflictExpr();
      incSumm.addDependent(this);
      res.andWith(tExpr);
    }
    return res;
  }

  
  //  /**
  //   * @return Returns the localFullExpr.
  //   */
  //  public JBDD getLocalFullExpr() {
  //    return localFullExpr.copy();
  //  }

  //  /**
  //   * @return Returns the localSimpleExpr.
  //   */
  //  public JBDD getLocalSimpleExpr() {
  //    return localSimpleExpr.copy();
  //  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    // don't track these in ColorFirstPass. They may be created there early, but
    // they are fundamentally creatures of ColorSecondPass.
//    ColorFirstPass.trackCUchanges(this);

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
      resDrop = 
        ColorMessages.createResultDrop("@colorConstraint "  + getReqString() + " for "  + methodName, 
                                       "TODO: fill me in", getNode());
      resDrop.addCheckedPromise(this);
    }
    
    @SuppressWarnings("unchecked") 
    Collection<? extends ColorRequireDrop> reqDrops = 
      Sea.filterDropsOfType(ColorRequireDrop.class, getDeponents());
    resDrop.addTrustedPromises(reqDrops);
    return resDrop;
  }

  //  /**
  //   * @param resDrop The resDrop to set.
  //   */
  //  public void setResDrop(ResultDrop resDrop) {
  //    this.resDrop = resDrop;
  //  }
  //  /**
  //   * @return Returns the localReqs.
  //   */
  //  public Set getLocalReqs() {
  //    return localReqs;
  //  }
  /**
   * @return Returns the empty.
   */
  public boolean isLocalEmpty() {
    return localEmpty;
  }

  public boolean isEmpty() {
    if (fullExpr == null) return true;
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
  public Set<PromiseDrop> getUserDeponents() {
    return userDeponents;
  }

  public void addToUserDeponents(PromiseDrop drop) {
    if (drop == null || drop == this) {
      return;
    }
    if (userDepsContainsThis) {
      userDeponents.remove(this);
      userDepsContainsThis = false;
    }
    
    userDeponents.add(drop);
  }
  
  public void addAllToUserDeponents(Collection<PromiseDrop> promiseDrops) {
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
    assert (cutPoint ? (whichStat == Status.USER)
        || (whichStat == Status.INHERITED) : whichStat == Status.INFERRED);
    return whichStat == Status.INFERRED;
  }

  public boolean isUser() {
    assert (cutPoint ? (whichStat == Status.USER)
        || (whichStat == Status.INHERITED) : whichStat == Status.INFERRED);
    return whichStat == Status.USER;
  }

  public boolean isInherited() {
    assert (cutPoint ? (whichStat == Status.USER)
        || (whichStat == Status.INHERITED) : whichStat == Status.INFERRED);
    return whichStat == Status.INHERITED;
  }
  
  public String getReqString() {
    return getReqString(false);
  }
  
  public String getReqString(boolean wantQualName) {
    if (reqsAreRelevant) {
      if (isEmpty()) {
        return "no known colorConstraint";
      } else {
        if (fullExprStr != null) {
          return fullExprStr;
        }
        
        String t = ColorRenamePerCU.jbddMessageName(fullExpr, wantQualName);
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