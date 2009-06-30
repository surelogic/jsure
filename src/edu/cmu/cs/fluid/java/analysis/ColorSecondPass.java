/*
 * Created on Nov 10, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.java.analysis;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import SableJBDD.bdd.JBDD;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.BinaryCUDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author dfsuther
 * 
 */
@Deprecated
public class ColorSecondPass {

  private static final ColorSecondPass INSTANCE = new ColorSecondPass();

  public static ColorSecondPass getInstance() {
    return INSTANCE;
  }

  private ColorSecondPass() {
  }
  
  private static IBinder binder = null;

  private static final Logger LOG = SLLogger.getLogger("ColorSecondPass"); //$NON-NLS-1$

  private static final Logger LOG1 = SLLogger
      .getLogger("analysis.callgraph.stats"); //$NON-NLS-1$

  private Drop resultDependUpon = null;

  public static class WorkList {

    private List<IRNode> theWorkList = new ArrayList<IRNode>();

    private Set<IRNode> workListContents = new HashSet<IRNode>();

    public int currSize = 0;

    public int maxSize = 0;

    public int numAdds = 0;

    public int numGets = 0;

    final boolean DEBUG = true;

    public void addToWorkList(IRNode node) {
      if (workListContents.contains(node))
        return;

      theWorkList.add(node);
      workListContents.add(node);
      if (DEBUG) {
        numAdds += 1;
        currSize = theWorkList.size();
        if (currSize > maxSize)
          maxSize = currSize;
      }
    }

    public IRNode getFromWorkList() {
      if (!theWorkList.isEmpty()) {
        IRNode res = theWorkList.remove(theWorkList.size() - 1);
        workListContents.remove(res);
        if (DEBUG) {
          numGets += 1;
          currSize = theWorkList.size();
        }
        return res;
      }
      return null;
    }

    public boolean isEmpty() {
      return theWorkList.isEmpty();
    }

    public void clearCounts() {
      currSize = 0;
      maxSize = 0;
      numAdds = 0;
      numGets = 0;
    }
  }

  private static boolean hasUserWrittenRequireDrops(IRNode node) {
    ColorReqSummaryDrop reqSumm = ColorReqSummaryDrop.getSummaryFor(node);
    return !ColorPromises.getMutableRequiresColorSet(node).isEmpty();
  }

  // private static boolean hasUserWrittenContextDrops(IRNode node) {
  // // ColorCtxSummaryDrop ctxSumm = ColorCtxSummaryDrop.getSummaryFor(node);
  // return !ColorPromises.getMutableColorContextSet(node).isEmpty();
  // }

  // private static boolean hasInheritedContextDrops(IRNode node) {
  // return !ColorPromises.getMutableInheritedContextSet(node).isEmpty();
  // }

  private static boolean hasColorCutpoint(IRNode node) {
    final ColorReqSummaryDrop reqSum = ColorReqSummaryDrop.getSummaryFor(node);
    final boolean res = reqSum.isCutPoint();
    assert (res == !reqSum.isInferred());
    return res;
  }

  public void cspEnd(final Drop resultDependUpon, IBinder binder) {
      this.resultDependUpon = resultDependUpon;
    CSPStruct iWalk = CSPStruct.getInstance();
    iWalk.binder = binder;
    ColorSecondPass.binder = binder;

    inferColorContexts();
    // inferColorReqs();
    checkConsistencyReportErrors();
    final ColorStats cStats = ColorStats.getInstance();
    cStats.afterCsp = cStats.getColorStats("After ColorSecondPass:"); //$NON-NLS-1$

    // InfoDrop rd = ColorMessages.createInfoDrop(cStats.beforeCfp.toString(),
    // null);
    LOG.info(cStats.beforeCfp.toString());
    // rd = ColorMessages.createInfoDrop(cStats.afterCfp.toString(), null);
    LOG.info(cStats.afterCfp.toString());
    // rd = ColorMessages.createInfoDrop(cStats.afterCsp.toString(), null);
    LOG.info(cStats.afterCsp.toString());
  }

  // private void inferColorReqs() {
  //
  // }

  private boolean containsColoredDataRefs(IRNode mDecl) {
    return ColorizedRegionModel.haveColorizedRegions();
  }

  private void inferColorContexts() {
    // Add appropriate method/constructors to the worklist. The ones to choose
    // are
    // (A || B) && (C || D || E) where:
    // A: those that have user-written ColorRequire or ColorContext or
    // transparent
    // drops.
    // B: those that have inherited ColorRequire or ColorContext or
    // transparent
    // drops.
    // C: Those that have non-empty callee sets.
    // D: Those marked as containsGrantOrRevoke.
    // E: Those containing access to colored data regions
    SimpleCallGraphDrop[] allCGD = SimpleCallGraphDrop.getAllCGDrops();

    WorkList wList = new WorkList();
    StringBuilder debugMessage;

    for (int i = 0; i < allCGD.length; i++) {
      final SimpleCallGraphDrop aCGD = allCGD[i];
      final IRNode mDecl = aCGD.getNode();
      final String methodName = JJNode.getInfo(mDecl);
      
//      if ("windowClosing".equals(methodName)) {
//        LOG.fine("foundit");
//      }

      // final boolean cutPoint = hasColorCutpoint(mDecl);
      // final boolean userReqs = hasUserWrittenRequireDrops(mDecl);
      // final boolean userCtx = hasUserWrittenContextDrops(mDecl);
      // final boolean colorNR = !ColorPromises.isColorRelevant(mDecl);

      // final boolean inhCtx = hasInheritedContextDrops(mDecl);

      if ((!ColorReqSummaryDrop.getSummaryFor(mDecl).isInferred() || aCGD
          .colorsNeedBodyTraversal())
          && aCGD.foundABody()) {
        LOG.finer("Adding " + methodName + "to worklist 1st time"); //$NON-NLS-1$ //$NON-NLS-2$
        wList.addToWorkList(mDecl);
      }
    }

    // Iterate computing ColorCtxSummary/ColorReqSummary information, until the
    // worklist is empty.
    // REMEMBER: ColorReqSummaries that are not isInferred must be treated
    // as cutpoints
    // The scheme is this:
    // ColorCtxSummaries on methods ALWAYS represent the "union-of calling
    // contexts".
    // ColorReqSummaries on methods ALWAYS represent the color constraint of
    // the
    // method. This constraint was either a) written by the user (==>isUser),
    // b) inherited from one or more parents (==>isInherited), or c) inferred by
    // this analysis (==>isInferred).
    // 
    // IF !containsGrantOrRevoke THEN
    // OR the ColorContextSummary into the ColorContextSummaries of all methods
    // from callees. If a callee's ColorContextSummary changes, add him to the
    // worklist.
    // ELSE
    // since the method contains a grant or revoke, CtxInferWalk the body of the
    // method. CtxInferWalk will compute the current context, and push it onto
    // the
    // callee's context.
    // FI
    int numCallGraphOnly = 0;
    int numWalks = 0;

    while (!wList.isEmpty()) {
      if (IDE.getInstance().isCancelled()) {
        return;
      }
      final IRNode mDecl = wList.getFromWorkList();
      final String methodName = JJNode.getInfo(mDecl);
      final SimpleCallGraphDrop cgDrop = SimpleCallGraphDrop
          .getCGDropFor(mDecl);
      JBDD localCtx = null;
      LOG.finer("Got " + methodName + " from worklist."); //$NON-NLS-1$ //$NON-NLS-2$
      Collection<PromiseDrop> ctxDependsOn = new ArrayList<PromiseDrop>(2);

      localCtx = computeInitialCtx(mDecl, ctxDependsOn);

      if (!cgDrop.colorsNeedBodyTraversal()) {
        // simple pass-through of my current context to my callees (if any).
        numCallGraphOnly += 1;
        Collection<IRNode> callees = cgDrop.getCallees();
        Iterator<IRNode> calleeIter = callees.iterator();
        while (calleeIter.hasNext()) {
          IRNode callee = calleeIter.next();

          updateACallee(wList, localCtx, ctxDependsOn, callee);
        }
      } else {
        walkMethod(mDecl, localCtx, ctxDependsOn, wList, false);
        numWalks += 1;
      }
      LOG.finer("Done processing " + methodName + " from worklist."); //$NON-NLS-1$ //$NON-NLS-2$
    }

    if (wList.DEBUG && LOG.isLoggable(Level.FINE)) {
      debugMessage = new StringBuilder();
      debugMessage.append("Finished context inference.\n  Max work list size: " //$NON-NLS-1$
          + wList.maxSize);
      debugMessage.append("\n  Iterated " + wList.numGets + " times, of which " //$NON-NLS-1$ //$NON-NLS-2$
          + numCallGraphOnly + " were CallGraph only and "); //$NON-NLS-1$
      debugMessage.append(numWalks + " required tree-walks."); //$NON-NLS-1$
      LOG.fine(debugMessage.toString());
    }
    wList.clearCounts();

  }

  /**
   * Given an mDecl (and thus its color anno summaries), compute the initial
   * color context needed for context tree traversal. Also update the contents
   * of localSummaries with the appropriate annos to depend on w.r.t. the
   * context.
   * 
   * @param mDecl
   *          The method to process
   * @param ctxDependsOn
   *          A <code>Collection</code> that will, on exit, hold the annos to
   *          depend on.
   * @return The context expression.
   */
  private static JBDD computeInitialCtx(final IRNode mDecl,
      Collection<PromiseDrop> ctxDependsOn) {
    StringBuilder debugMessage = new StringBuilder();
    ColorReqSummaryDrop reqSum = ColorReqSummaryDrop.getSummaryFor(mDecl);
    if (reqSum.isInferred()) {
      // inferred constraints ARE the calling context.
      final ColorCtxSummaryDrop ctxSum = ColorCtxSummaryDrop
          .getSummaryFor(mDecl);
      // update the reqSum's constraint expression.
      reqSum.setFullExpr(ctxSum.getFullExpr());
      final Set userDeponents = reqSum.getUserDeponents();
      // Don't clear userDeponents if reqSum is an API method. API methods may
      // or
      // may not have calls to them, and so must have their own
      // requirementSummary
      // as a userDeponent just in case there is nothing else to use.
      if (!ModuleModel.isAPIinParentModule(mDecl)) {
        userDeponents.clear();
      }
      reqSum.addAllToUserDeponents(ctxSum.getUserDeponents());
    }
    JBDD localCtx = reqSum.getFullExpr();
    ctxDependsOn.addAll(reqSum.getUserDeponents());
    return localCtx;
  }

  static boolean contextImpliesReq(IRNode mDecl, boolean ctxIsEmpty,
      JBDD ctxFullExpr, ColorReqSummaryDrop req) {
    assert ((mDecl != null) && (ctxFullExpr != null) && (req != null));

    final String mName = JJNode.getInfo(mDecl);
    final boolean colorNR = !ColorPromises.isColorRelevant(mDecl);
    final boolean localEmpty = req.isLocalEmpty();
    final boolean reqEmpty = req.isEmpty();
    final JBDD reqFullExpr = req.getFullExpr();

    return coreContextImpliesReq(ctxIsEmpty, ctxFullExpr, colorNR, localEmpty,
        reqFullExpr);
  }

  /**
   * @param ctxIsEmpty
   * @param ctxFullExpr
   * @param colorNR
   * @param localEmpty
   * @param reqFullExpr
   * @return
   */
  private static boolean coreContextImpliesReq(boolean ctxIsEmpty,
      JBDD ctxFullExpr, final boolean colorNR, final boolean localEmpty,
      final JBDD reqFullExpr) {
    if (ctxIsEmpty) {
      // empty context is OK if transparent on mDecl
      if (colorNR) {
        return true;
      }
      // empty context is OK if constraint is empty too.
      if (localEmpty) {
        return true;
      } else if (!localEmpty) {
        // empty context cannot imply any non-empty requirement.
        return false;
      }
    } else if (localEmpty || colorNR) {
      // any context is OK for an empty requirement.
      return true;
    }

    // now the interesting case: neither ctx nor req is empty.
    // final JBDD implies = ctxFullExpr.imply(req.getLocalFullExpr());
    final JBDD implies = ctxFullExpr.imply(reqFullExpr); // change at JPL
    if (implies.isOne()) {
      return true;
    }

    return false;
  }

  private static class CGData {
    // a fanout number
    int fanout = 0;

    // number of callsites with this fanout
    int numCallSites = 0;

    // count of the subset of methods for which we KNOW they cannot be
    // over-ridden
    int numCantBeOverridden = 0;

    // count of the subset of methods for which we KNOW we have seen all
    // overrides
    int numSeenAll = 0;

    // count of the subset of methods for which our count of overrides may be
    // incomplete
    int numHaventSeenAll = 0;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof CGData))
        return false;
      if (obj == this)
        return true;
      final CGData o = (CGData) obj;

      return fanout == o.fanout;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return super.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      final StringBuilder buf = new StringBuilder();
      buf.append(fanout);
      buf.append("; "); //$NON-NLS-1$
      buf.append(numCallSites);
      buf.append("; "); //$NON-NLS-1$
      buf.append(numCantBeOverridden);
      buf.append("; "); //$NON-NLS-1$
      buf.append(numSeenAll);
      buf.append("; "); //$NON-NLS-1$
      buf.append(numHaventSeenAll);
      return buf.toString();
    }
  }

  private final ArrayList<CGData> statList = new ArrayList<CGData>();

  // addStat(aCGD, methodCanBeOveridden, !aCGD.partOfAPI && aCGD.foundABody());
  private void addStat(SimpleCallGraphDrop aCGD, boolean canOverride,
      boolean seenAll) {
    final int fanout = aCGD.numOverridingMethods + 1;
    CGData stat = null;
    if (statList.size() > fanout) {
      stat = statList.get(fanout);
    }
    if (stat == null) {
      stat = new CGData();
      stat.fanout = fanout;
      if (fanout > 0) {
        ensureStatListCapacity(false, stat.fanout);
        statList.set(stat.fanout, stat);
      }
    }

    final int sites = aCGD.numCallSitesSeen;
    stat.numCallSites += sites;
    if (!canOverride) {
      stat.numCantBeOverridden += sites;
    } else if (seenAll) {
      stat.numSeenAll += sites;
    } else {
      stat.numHaventSeenAll += sites;
    }
  }

  private void setResultDep(final IRReferenceDrop drop,
      final Drop resultDependUpon) {
    if (resultDependUpon != null && resultDependUpon.isValid()) {
      resultDependUpon.addDependent(drop);
    } else {
      LOG.log(Level.SEVERE,
          "setResultDep found invalid or null resultDependUpon drop"); //$NON-NLS-1$
    }
  }

  private InfoDrop makeWarningDrop(final Category category,
      final IRNode context, final Drop resultDependUpon,
      final String msgTemplate, final Object... msgArgs) {
    final String msg = MessageFormat.format(msgTemplate, msgArgs);
    final InfoDrop info = ColorMessages.createWarningDrop(msg, context);
    setResultDep(info, resultDependUpon);
    info.setCategory(category);
    return info;
  }

  private ResultDrop makeResultDrop(final IRNode context, final PromiseDrop p,
      final boolean isConsistent, final Drop resultDependUpon,
      final String msgTemplate, final Object... msgArgs) {
    final String msg = MessageFormat.format(msgTemplate, msgArgs);
    final ResultDrop result;
    if (isConsistent) {
      result = ColorMessages.createResultDrop(msg, context);
    } else {
      result = ColorMessages.createProblemDrop(msg, context);
    }
    setResultDep(result, resultDependUpon);
    result.addCheckedPromise(p);
    result.setConsistent(isConsistent);
    return result;
  }

  private void addSupportingInformation(final IRReferenceDrop drop,
      final IRNode link, final String msgTemplate, final Object... msgArgs) {
    final String msg = MessageFormat.format(msgTemplate, msgArgs);
    drop.addSupportingInformation(msg, link);
  }

  private static void computeCanonicalBddImages() {
    ColorBDDPack.resetCanonicalImages();
    for (ColorRequireDrop crd : Sea.getDefault().getDropsOfType(
        ColorRequireDrop.class)) {
      final CExpr rawExpr = crd.getRawExpr();
      final String image = rawExpr.toString();
      final JBDD rawBDD = crd.getRenamedExpr().computeExpr(true);
      ColorBDDPack.registerCanonicalImage(image, rawBDD);
    }

    for (ColorContextDrop ccd : Sea.getDefault().getDropsOfType(
        ColorContextDrop.class)) {
      final CExpr rawExpr = ccd.getRawExpr();
      final String image = rawExpr.toString();
      final JBDD rawBDD = ccd.getRenamedExpr().computeExpr(true);
      ColorBDDPack.registerCanonicalImage(image, rawBDD);
    }

    for (RegionColorDeclDrop rcd : Sea.getDefault().getDropsOfType(
        RegionColorDeclDrop.class)) {
      final CExpr rawExpr = rcd.getUserConstraint();
      final String image = rawExpr.toString();
      final JBDD rawBDD = rcd.getRenamedConstraint().computeExpr(true);
      ColorBDDPack.registerCanonicalImage(image, rawBDD);
    }

    for (ColorRenameDrop rd : Sea.getDefault().getDropsOfType(
        ColorRenameDrop.class)) {
      final Object cookie = ColorRenamePerCU.startACU(rd.getMyPerCU().getCu());
      final String image = rd.simpleName;
      final JBDD rawBDD = rd.getFullExpr();
      ColorBDDPack.registerCanonicalImage(image, rawBDD);
      ColorRenamePerCU.endACU(cookie);
    }
  }

  private boolean possibleMultipleThreads(final JBDD expr,
      final ColorCtxSummaryDrop ctxDrop) {
    final IRNode where = ctxDrop.getNode();

    final Set<ColorNameModel> colorNames = canonicalColornamesFromJBDD(expr,
        where);

    if (colorNames.size() > 1) {
      return true;
    }
    return false;
  }

  /**
   * @param expr
   *          The JBDD whose positive color names we want to find.
   * @param where
   *          the IRNode where expr applies, if any.
   * @return A HashSet of (canonicalized) ColorNameModels, one for each truly
   *         distinct variable referred to positively in expr.
   */
  private Set<ColorNameModel> canonicalColornamesFromJBDD(final JBDD expr,
      final IRNode where) {
    final Set<String> posColors = expr.supportingPosVars();

    final Set<ColorNameModel> colorNames = new HashSet<ColorNameModel>(
        posColors.size());
    for (String colorName : posColors) {
      colorNames.add(ColorNameModel.getCanonicalInstance(colorName, where));
    }
    return colorNames;
  }

  static final int initialStatListCapacity = 10000;

  /**
   * @param allCGD
   */
  private void checkConsistencyReportErrors() {
    if (IDE.getInstance().isCancelled()) {
      return;
    }
    computeCanonicalBddImages();
    SimpleCallGraphDrop[] allCGD = SimpleCallGraphDrop.getAllCGDrops();
    // When done, we must make a few checks.

    ensureStatListCapacity(true, initialStatListCapacity);

    Set<IRNode> mthWithBodiesToTraverse = new HashSet<IRNode>();

    ColorSummaryDrop usefulInferDependOn = new ColorSummaryDrop(
        "Inferred @colorConstraint"); //$NON-NLS-1$
    int usefulInferCount = 0;
    ColorSummaryDrop noInfoInferDependOn = new ColorSummaryDrop(
        "Thread Color model not inferrable"); //$NON-NLS-1$
    int noInfoInferCount = 0;

    ColorSummaryDrop cnrDependOn = new ColorSummaryDrop("@transparent methods"); //$NON-NLS-1$
    int cnrCount = 0;

    ColorSummaryDrop inheritDependOn = new ColorSummaryDrop(
        "Color Model via inheritance"); //$NON-NLS-1$
    int inheritCount = 0;

    ColorSummaryDrop colorizedDataDependOn = new ColorSummaryDrop(
        "Colorized Regions"); //$NON-NLS-1$
    int colorizedRegionCount = 0;

    ColorSummaryDrop colorConstrainedResultsDependOn = new ColorSummaryDrop(
        "Color Constrained Regions"); //$NON-NLS-1$
    int colorConstrainedRegionCount = 0;

    ColorSummaryDrop methodConstraintResultsDependOn = new ColorSummaryDrop(
    "colorConstraint methods"); //$NON-NLS-1$
    int methodConstraintCount = 0;
    resultDependUpon.addDependent(methodConstraintResultsDependOn);

    resultDependUpon.addDependent(noInfoInferDependOn);
    resultDependUpon.addDependent(cnrDependOn);
    resultDependUpon.addDependent(inheritDependOn);

    resultDependUpon.addDependent(colorizedDataDependOn);
    resultDependUpon.addDependent(colorConstrainedResultsDependOn);

    boolean log1HeaderPrinted = false;
    int numMethodsWithBodies = 0;
    for (int i = 0; i < allCGD.length; i++) {
      SimpleCallGraphDrop aCGD = allCGD[i];
      IRNode mDecl = aCGD.getNode();

      ColorRenamePerCU.startACU(aCGD.getOuterTypeOrCU());

      final ModuleModel mod = ModuleModel.getModuleDrop(aCGD.getNode());
      final boolean isAPI = mod.isAPI(aCGD.getNode());

      final ColorCtxSummaryDrop ctxSumm = ColorCtxSummaryDrop
          .getSummaryFor(mDecl);
      final ColorReqSummaryDrop reqSumm = ColorReqSummaryDrop
          .getSummaryFor(mDecl);
      final String expandedMName = JavaNames
          .genQualifiedMethodConstructorName(mDecl);
      
//      if (expandedMName.startsWith("jpl.gds.monitor.gui.EvrComposite.createControls")) {
//	      LOG.severe("found createControls");
//	    }
//      
      
      // final String mName =
      // JavaNames.genMethodConstructorName(reqSumm.getNode());

      if (LOG1.isLoggable(Level.INFO)) {
        if (!log1HeaderPrinted) {
          log1HeaderPrinted = true;
          LOG1
              .info("Method Name; (!IsAPI && foundABody); Total Number of Callees; Total Number of Concrete Callees; " + //$NON-NLS-1$
                  "Overridable; Possible methods invocable by a call to this method; Syntactic Call Sites; "); //$NON-NLS-1$
        }
        final String numCallees = Integer.toString(aCGD.getCallees().size());
        int numConcreteCallees = 0;

        Iterator<IRNode> calleeIter = aCGD.getCallees().iterator();
        while (calleeIter.hasNext()) {
          IRNode calleeDecl = calleeIter.next();
          if (ColorFirstPass.isConcrete(calleeDecl)) {
            numConcreteCallees += 1;
          }
        }
        final int mods = JavaNode.getModifiers(mDecl);
        final boolean isStatic = JavaNode.getModifier(mods, JavaNode.STATIC);
        final boolean isFinal = JavaNode.getModifier(mods, JavaNode.FINAL);
        final boolean isPrivate = JavaNode.getModifier(mods, JavaNode.PRIVATE);
        final boolean methodCanBeOveridden = !(isStatic || isFinal || isPrivate);

        LOG1.info(expandedMName + "; " + //$NON-NLS-1$
            Boolean.toString(!isAPI && aCGD.foundABody()) + "; " + //$NON-NLS-1$
            numCallees + "; " //$NON-NLS-1$
            + Integer.toString(numConcreteCallees) + "; " + //$NON-NLS-1$
            Boolean.toString(methodCanBeOveridden) + "; " + //$NON-NLS-1$
            Integer.toString((aCGD.numOverridingMethods + 1)) + "; " + //$NON-NLS-1$
            Integer.toString(aCGD.numCallSitesSeen));

        addStat(aCGD, methodCanBeOveridden, !isAPI && aCGD.foundABody());
        if (aCGD.foundABody()) {
          numMethodsWithBodies += 1;
        }
        /*
         * }
         * 
         * if (LOG1.isLoggable(Level.INFO)) {
         */

      }

      // final ResultDrop ctxResDrop = ctxSumm.getResDrop();
      // resultDependUpon.addDependent(ctxResDrop);
      final ResultDrop reqResDrop = reqSumm.getResDrop();
//      resultDependUpon.addDependent(reqResDrop);
      methodConstraintResultsDependOn.addDependent(reqSumm);
      reqResDrop.setCategory(ColorMessages.assuranceCategory);
      methodConstraintCount += 1;

      if (reqSumm.isInferred()) {

        if (reqSumm.isEmpty()) {
          if (aCGD.isPotentiallyCallable()) {
            InfoDrop wd = makeWarningDrop(null, reqSumm.getNode(),
        	noInfoInferDependOn,
        	"Thread Color model not inferrable for {0}", expandedMName); //$NON-NLS-1$
            // reqSumm.setMessage("Thread Color model not inferrable for " +
            // mName);
            // noInfoInferDependOn.addDependent(reqSumm);
            noInfoInferCount += 1;

            // ResultDrop rd = reqSumm.getResDrop();
            // rd.addCheckedPromise(reqSumm);
            // rd.setInconsistent();
            // rd.addSupportingInformation("Color requirement not inferrable for
            // this API method.", null);
          }
        } else {
          reqSumm.setMessage(Messages.ColorSecondPass_inferredColor, reqSumm
              .getReqString(), expandedMName); //$NON-NLS-1$
          if (reqSumm.isEmpty()) {
            usefulInferDependOn.addDependent(reqSumm);
          } else {
            usefulInferDependOn.addDependent(reqSumm);
          }
          usefulInferCount += 1;
        }
      } else if (reqSumm.isInherited()) {
        if (reqSumm.reqsAreRelevant()) {
          reqSumm.setMessage(Messages.ColorSecondPass_inheritedColor, reqSumm
              .getReqString(), expandedMName); //$NON-NLS-1$
        } else {
          reqSumm.setMessage(Messages.ColorSecondPass_inheritedTransparent,
              expandedMName); //$NON-NLS-1$
        }
        inheritDependOn.addDependent(reqSumm);
        inheritCount += 1;
      }

      if (!reqSumm.reqsAreRelevant()) {
        cnrDependOn.addDependent(reqSumm);
        cnrCount += 1;
      }
      // boolean reqConsistent = true;
      // boolean ctxConsistent = true;
      // First: find all API method/constructors from modules that we are
      // currently
      // processing. If any of them are do not have user-written (or inherited)
      // color
      // annos, mark their summaries as being inconsistent. This will poison the
      // consistency of everything that depends on them.
      if (ModuleModel.isAPIinParentModule(mDecl) && !hasColorCutpoint(mDecl)) {
        // Ask user for more info!

        // if (ColorPromises.isColorRelevant(mDecl)) {
        // LOG.fine("Color is relevant for " + methodName + ".");
        // }
        String msg;
        InfoDrop id;
        if (ctxSumm.isEmpty()) {
          msg = "Missing color model for unreferenced Visible method " //$NON-NLS-1$
              + expandedMName;
          id = ColorMessages.createInfoDrop(msg, aCGD.getNode());
        } else {
          msg = "Missing color model for Visible method " //$NON-NLS-1$
              + expandedMName;
          id = ColorMessages.createWarningDrop(msg, aCGD.getNode());
        }
        resultDependUpon.addDependent(id);
        if (ctxSumm.isEmpty()) {
          /*
           * id.addSupportingInformation("Union of calling contexts is EMPTY!",
           * null);
           */
        } else {
          id.addSupportingInformation("Union of calling contexts is " //$NON-NLS-1$
              + ColorBDDPack.userStr(ctxSumm.getFullExpr()), null);
        }

        // reqConsistent = false;
        // ctxConsistent = false;
      }

      // Second, visit all method/constructors that have user-written (or
      // inherited)
      // colorConstraint drops. Verify that the Context implies constraint for
      // each.
      // in any case where this is not true, mark the requirement as being
      // inconsistent.
      // if (hasColorCutpoint(mDecl)) {
      // // 2nd arg to contextImpliesReq is FALSE because we already know that
      // // the context CAN'T BE EMPTY. After all, we have a localColorCutpoint,
      // // so there MUST BE local contex!
      // if (!contextImpliesReq(mDecl, false, ctxSumm.getFullExpr(), reqSumm)
      // && !aCGD.getCallers().isEmpty()) {
      // // One or more of our callers contains a call-site with a colorContext
      // // that
      // // does not meet the requirements of mDecl. Add aCGD's callers to the
      // // list of methods we'll traverse later making specific call-site
      // // problem reports.
      // callSiteCtxProblems.addAll(aCGD.getCallers());
      //
      // ResultDrop rd = reqSumm.getResDrop();
      // rd.addCheckedPromise(reqSumm);
      // rd.addTrustedPromise(ctxSumm);
      // rd.setInconsistent();
      // rd.addSupportingInformation("calling context "
      // + ctxSumm.getFullExpr() + " does not imply requirement "
      // + reqSumm.getFullExpr(), null);
      // }
      // }

      if (aCGD.getCallers().isEmpty()) {
        if (aCGD.foundABody()) {
          if (!aCGD.isPotentiallyCallable()) {
            StringBuilder msg = new StringBuilder();
            msg.append("This method has no callers. "); //$NON-NLS-1$
            if (isAPI) {
              msg
              .append("That's OK, because it's part of the API for module " + mod.name + ", and may be called from other modules."); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
              msg.append("Suspicious, because we've seen all possible callers."); //$NON-NLS-1$
            }
            reqSumm.addSupportingInformation(msg.toString(), null);
          }
        }
      } else {
        if (ctxSumm.isEmpty()) {
          reqSumm.addSupportingInformation(
              "Union of calling contexts is EMPTY!", //$NON-NLS-1$
              null);
        } else {
          reqSumm.addSupportingInformation("Union of calling contexts is " //$NON-NLS-1$
              + ColorBDDPack.userStr(ctxSumm.getFullExpr()), null);
        }
      }
      // ctxResDrop.setConsistent(ctxConsistent);
      // ctxResDrop.addCheckedPromise(ctxSumm);
      //
      // reqResDrop.setConsistent(reqConsistent);

      // if (aCGD.foundABody()) {
      // ColorCtxSummaryDrop innerCtx = ColorCtxSummaryDrop.getSummaryFor(aCGD
      // .getTheBody());
      // ResultDrop iCtxRes = innerCtx.getResDrop();
      //
      // iCtxRes.addCheckedPromise(innerCtx);
      // if (iCtxRes.getMessage().equals("(EMPTY)")) {
      // iCtxRes.setMessage("Inner color context for " + methodName);
      // } else {
      // iCtxRes.setMessage("Inner color context for " + methodName);
      // }
      // iCtxRes.addSupportingInformation("Using " + innerCtx.getFullExpr()
      // + " as context", null);
      // if (hasColorCutpoint(mDecl)) {
      // iCtxRes.setConsistent();
      // } else {
      // iCtxRes.setConsistent(ctxConsistent);
      // }
      // }

      if (!ctxSumm.isEmpty()) {
        if (possibleMultipleThreads(ctxSumm.getFullExpr(), ctxSumm)) {
          StringBuilder sb = new StringBuilder();
          sb.append(expandedMName);
          sb.append(" may be invoked from more than one thread."); //$NON-NLS-1$
          InfoDrop id = ColorMessages.createInfoDrop(sb.toString(), ctxSumm
              .getNode());
          id.setCategory(ColorMessages.multiThreadedInfoCategory);
          sb.setLength(0);
          sb.append("Union of calling contexts is "); //$NON-NLS-1$
          sb.append(ColorBDDPack.userStr(ctxSumm.getFullExpr()));
          id.addSupportingInformation(sb.toString(), null);
          resultDependUpon.addDependent(id);
        }
      }

      if (aCGD.foundABody()) {
        mthWithBodiesToTraverse.add(mDecl);
      }
      ColorRenamePerCU.endACU(null);
    }
    /*
     * noInfoInferDependOn.setMessage(noInfoInferDependOn.getMessage() + " (" +
     * Integer.toString(noInfoInferCount) + " issues)");
     * usefulInferDependOn.setMessage(usefulInferDependOn.getMessage() + " (" +
     * Integer.toString(usefulInferCount) + " issues)");
     * cnrDependOn.setMessage(cnrDependOn.getMessage() + " (" +
     * Integer.toString(cnrCount) + " issues)");
     * inheritDependOn.setMessage(inheritDependOn.getMessage() + " (" +
     * Integer.toString(inheritCount) + " issues)");
     */
    noInfoInferDependOn.setCount(noInfoInferCount);
    usefulInferDependOn.setCount(usefulInferCount);
    cnrDependOn.setCount(cnrCount);
    inheritDependOn.setCount(inheritCount);
    methodConstraintResultsDependOn.setCount(methodConstraintCount);

    checkCallSites(mthWithBodiesToTraverse);

    Collection<ColorizedRegionModel> crms = ColorizedRegionModel
        .getAllValidCRMs();
    for (ColorizedRegionModel crm : crms) {
      // StringBuilder sb = new StringBuilder();
      final JBDD userConstraint = crm.getAndOfUserConstraints();
      if (userConstraint == null || userConstraint.isOne()) {
        crm.setMessage(Messages.ColorSecondPass_colorContextDrop, crm
            .getMessage(), ColorRenamePerCU.jbddMessageName(crm
            .getComputedContext()));
        // just report the computed constraint
        final String colorizedRegion_OK = "{0} is accessed from color context {1}."; 
        ResultDrop rd = makeResultDrop(null, crm, true, colorizedDataDependOn,
            colorizedRegion_OK, crm.getMessage(), ColorRenamePerCU
                .jbddMessageName(crm.getComputedContext()));
        colorizedDataDependOn.addDependent(crm);
        colorizedRegionCount += 1;
        rd.addCheckedPromises(crm.getUserDeponents());
        rd.addTrustedPromise(crm);
        rd.setConsistent();
        rd.setCategory(JavaGlobals.COLORIZED_REGION_CAT);
      } else {
        // Check whether the computed context satisfies the user constraint
        final JBDD computedContext = crm.getComputedContext();
        final boolean compCtxIsEmpty = (computedContext == null)
            || computedContext.isOne();
        ResultDrop rd;
        if (coreContextImpliesReq(compCtxIsEmpty, computedContext, false,
            userConstraint.isOne(), userConstraint)) {
          // all is well. Report this.
          final String ok_CCR = "All accesses to " + crm.getMessage() + " are consistent with constraint " + //$NON-NLS-1$ //$NON-NLS-2$
              ColorRenamePerCU.jbddMessageName(crm.getComputedContext());
          rd = makeResultDrop(null, crm, true, colorConstrainedResultsDependOn,
              ok_CCR, crm.getMessage());
        } else {
          // context does not satisfy constraint!
          final String bad_CCR = "Color Constrained Region \"{0}\" has inconsistent accesses."; //$NON-NLS-1$
          final String contextFormat = "Context ({0}) does not imply requirements ({1})."; //$NON-NLS-1$
          rd = makeResultDrop(null, crm, false,
              colorConstrainedResultsDependOn, bad_CCR,
              crm.getMasterRegion().regionName);
          
          String contextStr = 
            ColorRenamePerCU.jbddMessageName(crm.getComputedContext());

          String constraintStr = 
            ColorRenamePerCU.jbddMessageName(crm.getAndOfUserConstraints());
          
          if (contextStr.equals(constraintStr)) {
            contextStr = 
              ColorRenamePerCU.jbddMessageName(crm.getComputedContext(), true);
            constraintStr = 
              ColorRenamePerCU.jbddMessageName(crm.getAndOfUserConstraints(), true);
          }
          
	  addSupportingInformation(rd, null, contextFormat, contextStr, constraintStr);
        }
        colorConstrainedResultsDependOn.addDependent(crm);
        colorConstrainedRegionCount += 1;
        rd.addCheckedPromises(crm.getUserDeponents());
        rd.addTrustedPromise(crm);
        rd.setConsistent();
        rd.setCategory(JavaGlobals.COLOR_CONSTRAINED_REGION_CAT);

      }
    }
    /*
     * colorizedDataDependOn.setMessage(colorizedDataDependOn.getMessage() + " (" +
     * Integer.toString(colorizedRegionCount) + " issues)");
     * colorConstrainedResultsDependOn.setMessage(colorConstrainedResultsDependOn.getMessage() + " (" +
     * Integer.toString(colorConstrainedRegionCount) + " issues)");
     */
    colorizedDataDependOn.setCount(colorizedRegionCount);
    colorConstrainedResultsDependOn.setCount(colorConstrainedRegionCount);

    // make a top-level result drop for each color name.
    Collection<ColorNameModel> allCNameModels = ColorNameModel
        .getAllValidColorNameModels();
    for (final ColorNameModel aCNM : allCNameModels) {
      // if (!aCNM.isDeclared()) {
      // ResultDrop pd = ColorMessages.createProblemDrop(aCNM.getMessage()
      // + " is invalid because it has no declaration.", aCNM.getNode());
      // pd.addCheckedPromise(aCNM);
      // resultDependUpon.addDependent(pd);
      // }
      ColorIncSummaryDrop incSumm = aCNM.getIncompatibleSummary();
      if (!incSumm.getConflictExpr().isOne()) {
        aCNM.addSupportingInformation(incSumm.getMessage(), null);
      }
    }

    if (LOG1.isLoggable(Level.INFO)) {
      LOG1
          .info("There are " + Integer.toString(numMethodsWithBodies) + " methods with bodies."); //$NON-NLS-1$ //$NON-NLS-2$
      for (Iterator<CGData> statIter = statList.iterator(); statIter.hasNext();) {
        CGData stat = statIter.next();
        if (stat != null) {
          LOG1.info(stat.toString());
        }
      }
    }

    final Sea sea = Sea.getDefault();
    folderizeDrops(
        "@color annotations", sea.getDropsOfExactType(ColorDeclareDrop.class), true); //$NON-NLS-1$
    folderizeDrops(
        "@grant annotations", sea.getDropsOfExactType(ColorGrantDrop.class), false); //$NON-NLS-1$
    folderizeDrops(
        "@colorConstraint annotations", sea.getDropsOfExactType(ColorRequireDrop.class), false); //$NON-NLS-1$
    folderizeDrops(
        "@colorRename annotations", sea.getDropsOfExactType(ColorRenameDrop.class), true); //$NON-NLS-1$
    // folderizeDrops("@colorizedRegion annotations",
    // sea.getDropsOfExactType(ColorizedRegionDeclDrop.class), false);
    // ColorBDDPack.resetCanonicalImages();
  }

  private <T extends Drop> void folderizeDrops(String name, Set<T> drops,
      boolean wantBinary) {
    if (drops.isEmpty()) {
      return;
    }
    ColorSummaryDrop dependOn = new ColorSummaryDrop(name);
    resultDependUpon.addDependent(dependOn);

    int count = 0;
    final Sea sea = Sea.getDefault();
    for (Drop d : drops) {
      boolean fromBinaryCU = false;
      if (!wantBinary) {
        Set<BinaryCUDrop> bCUs = Sea.filterDropsOfExactType(BinaryCUDrop.class,
            d.getDeponents());
        fromBinaryCU = !bCUs.isEmpty();
      }
      if (wantBinary || !fromBinaryCU) {
        count++;
        dependOn.addDependent(d);
      }
    }
    dependOn.setCount(count);
  }

  /**
   * 
   */
  @SuppressWarnings("unchecked")
  private void ensureStatListCapacity(boolean startOver, int minCapacity) {
    if (LOG1.isLoggable(Level.INFO)) {
      if (startOver || minCapacity >= statList.size()) {
        final int startIndex;
        if (startOver) {
          statList.clear();
          startIndex = 1;
        } else {
          startIndex = statList.size();
        }
        ((ArrayList) statList).ensureCapacity(minCapacity + 1);
        for (int i = startIndex; i <= minCapacity; i++)
          statList.add(null);
      }
    }
  }

  /**
   * Walk each method in methodsToCheck looking for call sites where the context
   * does not, in fact, imply the requirement on the callee. These call sites
   * are the location of problems, and should be flagged as such. The only
   * difference between setting up for this traversal and setting up for a
   * context traversal is that we set the checkingErrors flag for this one, and
   * not for the initial context computation.
   * 
   * @param methodsToCheck
   *          a <code>Set</code> holding the methodDecls that are possible
   *          candidates for having errors. Not all such methods actually do
   *          have erroneous call sites, so we'll have to traverse each method
   *          individually to check.
   */
  private void checkCallSites(Set<IRNode> methodsToCheck) {
    // Iterator<IRNode> mTCIter = methodsToCheck.iterator();
    // while (mTCIter.hasNext()) {
    // final IRNode mth = mTCIter.next();
    for (IRNode mth : methodsToCheck) {

      SimpleCallGraphDrop aCGD = SimpleCallGraphDrop.getCGDropFor(mth);
      ColorRenamePerCU.startACU(aCGD.getOuterTypeOrCU());

      Collection<PromiseDrop> ctxDependsOn = new ArrayList<PromiseDrop>(2);
      JBDD localCtx = computeInitialCtx(mth, ctxDependsOn);

      walkMethod(mth, localCtx, ctxDependsOn, null, true);

      ColorRenamePerCU.endACU(null);
    }

  }

  private void walkMethod(IRNode mth, JBDD localCtx,
      Collection<PromiseDrop> userDeponents, WorkList wList,
      boolean checkingCallsites) {
    assert (checkingCallsites ? (wList == null) : (wList != null));

    final CSPStruct walker = CSPStruct.getInstance();
    walker.startedHere = mth;
    walker.currCtx = localCtx.copy();
    walker.userDeponents = new HashSet<PromiseDrop>(userDeponents.size());
    walker.userDeponents.addAll(userDeponents);
    walker.wList = wList;
    walker.checkingErrors = checkingCallsites;
    walker.cu = VisitUtil.computeOutermostEnclosingTypeOrCU(mth);
    final ColorCtxSummaryDrop mthCtxSumm = ColorCtxSummaryDrop
        .getSummaryFor(mth);
    walker.ctxIsEmpty = mthCtxSumm.isEmpty() & !hasColorCutpoint(mth);

    ColorStaticMeth cMeth = ColorStaticMeth.getStaticMeth(mth);

    if (checkingCallsites && LOG.isLoggable(Level.FINE)) {
      StringBuilder msg = new StringBuilder();
      msg.append("Starting error check tree-walk on " + JJNode.getInfo(mth) //$NON-NLS-1$
          + " with:\n"); //$NON-NLS-1$
      msg.append("   context==" + localCtx + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
      msg.append("   ctxIsEmpty==" + Boolean.toString(walker.ctxIsEmpty)); //$NON-NLS-1$
      LOG.fine(msg.toString());
    }

    try {
      walker.doAccept(cMeth);
    } finally {
      walker.userDeponents = null;
      walker.currCtx = null;
      walker.startedHere = null;
      walker.checkingErrors = false;
      walker.ctxIsEmpty = false;
    }

  }

  /**
   * @param wList
   * @param localCtx
   * @param userDeponents
   * @param callee
   */
  static void updateACallee(WorkList wList, JBDD localCtx,
      Collection<PromiseDrop> userDeponents, IRNode callee) {
    ColorCtxSummaryDrop calleeCtxSumm = ColorCtxSummaryDrop
        .getSummaryFor(callee);

    JBDD newCalleeCtx = calleeCtxSumm.getFullExpr();
    newCalleeCtx = newCalleeCtx.or(localCtx);
    if (!newCalleeCtx.equals(calleeCtxSumm.getFullExpr())) {
      calleeCtxSumm.setFullExpr(newCalleeCtx);
      wList.addToWorkList(callee);
    }
    calleeCtxSumm.getUserDeponents().addAll(userDeponents);
    calleeCtxSumm.addDeponents(userDeponents);
  }

  static void updateCallees(final WorkList wList, final JBDD localCtx,
      final Collection<PromiseDrop> userDeponents, final IRNode callee,
      final IRNode receiver) {

    final boolean methodCanBeOveridden = methodCanBeOverridden(callee);
    if (methodCanBeOveridden) {
      Iterator<IRNode> overrides = binder.findOverridingMethodsFromType(callee,
          receiver);
      while (overrides.hasNext()) {
        IRNode oCallee = overrides.next();
        updateACallee(wList, localCtx, userDeponents, oCallee);
      }
    }
    // don't forget to update the exact callee
    updateACallee(wList, localCtx, userDeponents, callee);
  }

  /**
   * @param mDecl
   * @return
   */
  static private boolean methodCanBeOverridden(final IRNode mDecl) {
    final boolean isStatic = JavaNode.getModifier(mDecl, JavaNode.STATIC);
    final boolean isFinal = JavaNode.getModifier(mDecl, JavaNode.FINAL);
    final boolean isPrivate = JavaNode.getModifier(mDecl, JavaNode.PRIVATE);
    final boolean methodCanBeOveridden = !(isStatic || isFinal || isPrivate);
    return methodCanBeOveridden;
  }

  static void updateSomeCRMs(List<ColorizedRegionModel> crms, JBDD localCtx,
      Collection<PromiseDrop> userDeponents) {
    if (crms == null) {
      return;
    }
    for (ColorizedRegionModel crm : crms) {
      JBDD constraint = crm.getComputedContext();
      if ((constraint == null) || constraint.isOne()) {
        constraint = localCtx.copy();
      } else {
        constraint = constraint.or(localCtx);
      }
      crm.setComputedContext(constraint);
      crm.getUserDeponents().addAll(userDeponents);
      crm.addDeponents(userDeponents);
    }
  }

  static void checkSomeCRMs(List<ColorizedRegionModel> crms, IRNode locInIR,
      JBDD localCtx, final boolean LocalCtxIsEmpty,
      final Set<PromiseDrop> userDeponents) {
    if (crms == null) {
      return;
    }
    for (ColorizedRegionModel crm : crms) {
      final JBDD constraint = crm.getAndOfUserConstraints();
      final boolean reqIsEmpty = (constraint == null);
      final boolean constraintMet = coreContextImpliesReq(LocalCtxIsEmpty,
          localCtx, false, reqIsEmpty, constraint);
      final RegionModel rmod = crm.getMasterRegion();

      if (!constraintMet) {
        // report an error
        // this call is one of the error sites we're looking for.
        // produce a problem report.
        StringBuilder msg = new StringBuilder();

        msg.append("Color model not consistent with code at reference to " //$NON-NLS-1$
            + rmod.regionName);
        msg.append('.');
        ResultDrop prd = ColorMessages.createProblemDrop(msg.toString(),
            locInIR);
        prd.addSupportingInformation("Local color context is " + //$NON-NLS-1$
            ColorRenamePerCU.jbddMessageName(localCtx) + "; constraint is " + //$NON-NLS-1$
            ColorRenamePerCU.jbddMessageName(constraint) + '.', null);
        prd.addCheckedPromises(userDeponents);

        prd.addTrustedPromise(crm);
      } else if (!reqIsEmpty) {
        // report an OK reference
        StringBuilder msg = new StringBuilder();
        msg.append("Color context OK for reference to " + rmod.regionName); //$NON-NLS-1$
        msg.append('.');
        ResultDrop rd = ColorMessages.createResultDrop(msg.toString(), locInIR);
        rd.addSupportingInformation("Local color context is " + //$NON-NLS-1$
            ColorRenamePerCU.jbddMessageName(localCtx) + "; constraint is " + //$NON-NLS-1$
            ColorRenamePerCU.jbddMessageName(constraint) + '.', null);
        rd.addCheckedPromises(userDeponents);
        rd.addTrustedPromise(crm);
        rd.setConsistent();
      }
    }

  }
  @Deprecated
  private static class CSPStruct extends ColorStructVisitor {
    private IBinder binder;

    private static final CSPStruct INSTANCE = new CSPStruct();

    private CSPStruct() {
    }

    JBDD currCtx = null;

    ColorCtxSummaryDrop currCtxSumm = null;

    IRNode startedHere = null;

    Set<PromiseDrop> userDeponents = null;

    WorkList wList = null;

    boolean ctxIsEmpty = false;

    boolean checkingErrors = false;

    // InstanceInitVisitor<Void> initHelper = null;

    IRNode cu = null;

    private JBDD revokeForInfer(JBDD ctx,
        final Collection<ColorRevokeDrop> revokes) {
      if ((revokes == null) || revokes.isEmpty())
        return ctx;
      if ((ctx == null) || ctx.isOne() || ctx.isZero())
        return ctx;

      JBDD tCtx = ctx.copy();
      JBDD tCtxSave = tCtx.copy();

      Iterator<ColorRevokeDrop> revIter = revokes.iterator();
      while (revIter.hasNext()) {
        ColorRevokeDrop aRevoke = revIter.next();
        Collection<String> rNames = aRevoke.getRevokedNames();
        Collection<ColorNameModel> rModels = ColorNameModel
            .getColorNameModelInstances(rNames, cu);

        for (Iterator<ColorNameModel> rModelIter = rModels.iterator(); rModelIter
            .hasNext();) {
          TColor canonTC = rModelIter.next().getCanonicalTColor();
          tCtx = tCtx.exist(canonTC.getSelfExpr());
          tCtx.andWith(canonTC.getSelfExprNeg());

          ctxIsEmpty = tCtx.isZero();

        }

        if (checkingErrors) {
          final IRNode node = aRevoke.getNode();
          StringBuilder msg = new StringBuilder();
          msg.append(aRevoke.getMessage());
          msg.append(" successful."); //$NON-NLS-1$
          ResultDrop rd = ColorMessages.createResultDrop(msg.toString(), node);
          rd.addTrustedPromise(aRevoke);
          rd.addCheckedPromises(userDeponents);
          msg = new StringBuilder();
          msg.append("Context before revoke was "); //$NON-NLS-1$
          msg.append(tCtxSave);
          msg.append("; context after revoke is "); //$NON-NLS-1$
          if (ctxIsEmpty) {
            msg.append("empty"); //$NON-NLS-1$
          } else {
            msg.append(tCtx);
          }
          rd.addSupportingInformation(msg.toString(), null);
        }
      }

      // For ideal dependency tracking, should remove from deponents any "grant
      // <names>"
      // that just had all their <names> revoked. Skipping this step for now.

      userDeponents.addAll(revokes);
      return tCtx;
    }

    private JBDD grantForInfer(JBDD ctx, final Collection<ColorGrantDrop> grants) {
      if ((grants == null) || grants.isEmpty())
        return ctx;
      JBDD tCtx;
      if ((ctx == null) || ctx.isZero()) {
        // AND with Zero will always yield Zero.
        // But we want the result of @grant Color
        // in an empty context to be
        // <Color:1>, so we'll flick the expr over to <1>
        tCtx = ColorBDDPack.one();
      } else if (ctx.isOne()) {
        tCtx = ColorBDDPack.one();
      } else {
        tCtx = ctx.copy();
      }

      Iterator<ColorGrantDrop> grantIter = grants.iterator();
      while (grantIter.hasNext()) {
        ColorGrantDrop aGrant = grantIter.next();
        Collection<String> gNames = aGrant.getGrantedNames();
        Collection<ColorNameModel> gModels = ColorNameModel
            .getColorNameModelInstances(gNames, cu);
        final IRNode node = aGrant.getNode();

        for (Iterator<ColorNameModel> rModelIter = gModels.iterator(); rModelIter
            .hasNext();) {
          TColor canonTC = rModelIter.next().getCanonicalTColor();
          final ColorNameModel nmCanon = canonTC.getCanonicalNameModel();
          tCtx = tCtx.exist(canonTC.getSelfExpr());

          JBDD tcFullExpr = canonTC.getSelfExpr();
          JBDD tCtxSave = tCtx.copy();
          tcFullExpr.andWith(canonTC.getConflictExpr());
          tCtx.andWith(tcFullExpr);
          if (checkingErrors) {
            // don't need to add deponents. Just want to be sure that the
            // context is satisfiable.
            StringBuilder msg = new StringBuilder();
            if (tCtx.isZero()) {
              // context switched from not-zero to zero. The thing we just
              // granted must have made us go unsatisfiable. Produce an error!

              msg.append("Error: Granting color " + nmCanon.getColorName()); //$NON-NLS-1$
              msg.append(" yields unsatisfiable context."); //$NON-NLS-1$
              ResultDrop prd = ColorMessages.createProblemDrop(msg.toString(),
                  node);
              prd.addTrustedPromise(aGrant);
              prd.addCheckedPromises(userDeponents);
              prd.setInconsistent();
              msg = new StringBuilder();
              msg.append("Context before grant was "); //$NON-NLS-1$
              msg.append(tCtxSave);
              msg.append("; context effect of grant is " + tcFullExpr); //$NON-NLS-1$
              prd.addSupportingInformation(msg.toString(), null);

              // The context is empty now, whether it was before or not!
              ctxIsEmpty = true;
            } else if (ctxIsEmpty
                && !canonTC.getSelfExpr().equals(canonTC.getConflictExpr())) {
              // Can't grant a color in an empty context if the color has
              // conflicts.
              // This is because we can't justify the & !foo part of the
              // expression.
              // N.B.: a TCName whose SelfExpr and ConflictExpr are equal must
              // not have
              // any conflicts. If it did, the two expressions would not be
              // equal.

              msg.append("Error: Empty color context can not justify @grant " //$NON-NLS-1$
                  + nmCanon.getColorName());
              msg.append(", because " + nmCanon.getColorName() //$NON-NLS-1$
                  + " has conflicts that are not satisfied."); //$NON-NLS-1$
              ResultDrop prd = ColorMessages.createProblemDrop(msg.toString(),
                  node);
              prd.addTrustedPromise(aGrant);
              prd.addCheckedPromises(userDeponents);
              prd.setInconsistent();
              msg = new StringBuilder();
              msg.append("Context before grant was empty"); //$NON-NLS-1$
              msg.append("; context effect of grant would be " + tcFullExpr); //$NON-NLS-1$
              prd.addSupportingInformation(msg.toString(), null);
              ctxIsEmpty = (tCtx.isOne() || tCtx.isZero());

            } else {

              msg.append("@grant of " + nmCanon.getColorName()); //$NON-NLS-1$
              msg.append(" successful."); //$NON-NLS-1$
              ResultDrop rd = ColorMessages.createResultDrop(msg.toString(),
                  node);
              rd.addTrustedPromise(aGrant);
              rd.addCheckedPromises(userDeponents);
              msg = new StringBuilder();
              msg.append("Context before grant was "); //$NON-NLS-1$
              msg.append(tCtxSave);
              msg.append("; context after grant is " + tCtx); //$NON-NLS-1$
              rd.addSupportingInformation(msg.toString(), null);

              // a grant just succeeded, so we MUST have a non-empty context!
              ctxIsEmpty = false;
            }
          }

          // For ideal dependency tracking, should remove from deponents any
          // "grant <names>"
          // that just had all their <names> revoked. Skipping this step for
          // now.

          // now that we've changed the context, we must add aGrant to the
          // current
          // deponents for others to depend on.
          userDeponents.add(aGrant);

        }

      }
      return tCtx;
    }

    /**
     * Visit all the interesting References inside the blockish structure.
     * Process the CRMs therein as appropriate.
     * 
     * @param blockish
     *          A blockish structure that may have some interesting references
     *          to process.
     */
    private void visitChildReferences(ColorStaticBlockish blockish) {
      for (ColorStaticRef aRef : blockish.interestingRefs) {
        if (checkingErrors) {
          checkSomeCRMs(aRef.colorTargetsHere, aRef.getNode(), currCtx,
              ctxIsEmpty, userDeponents);
        } else {
          updateSomeCRMs(aRef.colorTargetsHere, currCtx, userDeponents);
        }
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visit(edu.cmu.cs.fluid.java.analysis.ColorStaticStructure)
     */
    @Override
    public void visit(ColorStaticStructure node) {
      if (node instanceof ColorStaticWithChildren) {
        doAcceptForChildren((ColorStaticWithChildren) node);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitBlock(edu.cmu.cs.fluid.java.analysis.ColorStaticBlock)
     */
    @Override
    public void visitBlock(ColorStaticBlock node) {
      final IRNode root = node.getNode();

      final Collection<ColorGrantDrop> grants = node.grants;
      final Collection<ColorRevokeDrop> revokes = node.revokes;

      if (!grants.isEmpty() || !revokes.isEmpty()) {
        // save currCtx, deponents and ctxIsEmpty. We'll restore them after the
        // block is done with.
        JBDD currCtxSAVE = currCtx;
        ColorCtxSummaryDrop ctxSummSAVE = currCtxSumm;
        currCtx = currCtxSAVE.copy();
        Set<PromiseDrop> deponentsSAVE = new HashSet<PromiseDrop>(userDeponents
            .size());
        deponentsSAVE.addAll(userDeponents);
        boolean ctxIsEmptySAVE = ctxIsEmpty;

        if (!checkingErrors) {
          currCtxSumm = ColorCtxSummaryDrop.getSummaryFor(root);
          currCtxSumm.setFullExpr(currCtx);
          if (ctxSummSAVE != null) {
            ctxSummSAVE.addDependent(currCtxSumm);
            currCtxSumm.getUserDeponents().addAll(
                ctxSummSAVE.getUserDeponents());
          }
        }

        try {
          // process revokes first.
          currCtx = revokeForInfer(currCtx, revokes);

          // process grants second.
          currCtx = grantForInfer(currCtx, grants);

          if (!checkingErrors) {
            currCtxSumm.addDeponents(userDeponents);
            // ResultDrop rd = currCtxSumm.getResDrop();
            // ResultDrop rd = currCtxSumm.getResDrop();
            // rd.addTrustedPromise(currCtxSumm);
            // rd.addCheckedPromises(userDeponents);
          }

          visitChildReferences(node);

          // traverse block
          super.visitBlock(node);

        } finally {
          currCtx = currCtxSAVE;
          userDeponents = deponentsSAVE;
          ctxIsEmpty = ctxIsEmptySAVE;
          currCtxSumm = ctxSummSAVE;
        }

      } else {
        super.visitBlock(node);
      }

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitCall(edu.cmu.cs.fluid.java.analysis.ColorStaticCall)
     */
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitCall(edu.cmu.cs.fluid.java.analysis.ColorStaticCall)
     */
    @Override
    public void visitCall(ColorStaticCall node) {
      final IRNode root = node.getNode();
      final IRNode mDecl = binder.getBinding(root);
      final String mName = JavaNames.genQualifiedMethodConstructorName(mDecl);

      if (checkingErrors) {
        // check to see whether the context at this particular constructorCall
        // always implies the requirement (if any) of the called constructor.
        ColorReqSummaryDrop reqSumm = ColorReqSummaryDrop.getSummaryFor(mDecl);
        if (!contextImpliesReq(mDecl, ctxIsEmpty, currCtx, reqSumm)) {
          // this call is one of the error sites we're looking for.
          // produce a problem report.
          StringBuilder msg = new StringBuilder();
          msg.append("Color model not consistent with code at call to " //$NON-NLS-1$
              + mName);
          msg.append('.');
          ResultDrop prd = 
            ColorMessages.createProblemDrop(msg.toString(), root);
          String ctxStr = ColorRenamePerCU.jbddMessageName(currCtx, true);
          
          
          final CUDrop cud = ColorFirstPass.getCUDropOf(mDecl);
          final IRNode theCUsRoot = cud.cu;
          //final ColorRenamePerCU theCUsCRpCU = ColorRenamePerCU.getColorRenamePerCU(theCUsRoot);
          final Object saveCookie = ColorRenamePerCU.startACU(theCUsRoot);
          
          String reqStr;
          try {
           reqStr = reqSumm.getReqString(true);
          } finally {
            ColorRenamePerCU.endACU(saveCookie);
          }
          if (ctxStr.equals(reqStr)) {
            LOG.severe("matching exprs: " + ctxStr);
//            if (contextImpliesReq(mDecl, ctxIsEmpty, currCtx, reqSumm)) {
//                LOG.severe("should never happen, because we already know the test above failed");
//            }
          }
          prd
              .addSupportingInformation(
                  "Local color context is " + ColorRenamePerCU.jbddMessageName(currCtx, true) //$NON-NLS-1$
                      + "; constraint is " + reqStr + '.', null); //$NON-NLS-1$
          prd.addCheckedPromises(userDeponents);
          final ColorReqSummaryDrop mReqSumm = ColorReqSummaryDrop
              .getSummaryFor(mDecl);
          prd.addTrustedPromise(mReqSumm);
        } else {
          // this call is NOT one of the error sites we're looking for.
          StringBuilder msg = new StringBuilder();
          msg.append("Color context OK for call to " + mName); //$NON-NLS-1$
          msg.append('.');
          ResultDrop rd = ColorMessages.createResultDrop(msg.toString(), root);
          rd
              .addSupportingInformation(
                  "Local color context is " + ColorRenamePerCU.jbddMessageName(currCtx) //$NON-NLS-1$
                      + "; constraint is " + reqSumm.getReqString() + '.', null); //$NON-NLS-1$
          rd.addCheckedPromises(userDeponents);
          rd.addTrustedPromise(ColorReqSummaryDrop.getSummaryFor(mDecl));
          rd.setConsistent();
        }
      } else {
        // not error checking. Just update the callee.

        final IRNode receiverNode = getAppropriateType(root);
        updateCallees(wList, currCtx, userDeponents, mDecl, receiverNode);
      }
      // Don't mess with CRMs for the call itself, because we really don't
      // want to be doing global effects (3/22/07)
//      if (ColorizedRegionModel.haveColorizedRegions()) {
//        // List<ColorizedRegionModel> crms = node.colorTargetsHere;
//
//        if (checkingErrors) {
//          checkSomeCRMs(node.colorCRMsHere, root, currCtx, ctxIsEmpty,
//              userDeponents);
//        } else {
//          updateSomeCRMs(node.colorCRMsHere, currCtx, userDeponents);
//        }
//      }
      super.visitCall(node);
    }

    private IRNode getAppropriateType(final IRNode node) {
      final Operator op = JJNode.tree.getOperator(node);
      final IJavaType receiverType;

      if (MethodCall.prototype.includes(op)) {
        MethodCall call = (MethodCall) op;
        final IRNode object = call.get_Object(node);
        receiverType = binder.getJavaType(object);
      } else if (ConstructorCall.prototype.includes(op)) {
        final IRNode object = ConstructorCall.getObject(node);
        receiverType = binder.getJavaType(object);
      } else if (NewExpression.prototype.includes(op)) {
        receiverType = binder.getJavaType(node);
      } else {
        return null;
      }
      final IRNode receiverNode = ((IJavaSourceRefType) receiverType)
          .getDeclaration();
      return receiverNode;
    }

    // /* (non-Javadoc)
    // * @see
    // edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitClass(edu.cmu.cs.fluid.java.analysis.ColorStaticClass)
    // */
    // @Override
    // public void visitClass(ColorStaticClass node) {
    // // LOG.severe("Unexpected visit to ColorStaticClass:" + node);
    // super.visitClass(node);
    // }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitCU(edu.cmu.cs.fluid.java.analysis.ColorStaticCU)
     */
    @Override
    public void visitCU(ColorStaticCU node) {
      LOG.severe("Unexpected visit to ColorStaticCU:" + node); //$NON-NLS-1$
      super.visitCU(node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitMeth(edu.cmu.cs.fluid.java.analysis.ColorStaticMeth)
     */
    @Override
    public void visitMeth(ColorStaticMeth node) {
      // don't traverse nested method decls.
      if (!node.getNode().equals(startedHere))
        return;
      final CUDrop cud = ColorFirstPass.getCUDropOf(node.getNode());
      final IRNode theCUsRoot = cud.cu;
      //final ColorRenamePerCU theCUsCRpCU = ColorRenamePerCU.getColorRenamePerCU(theCUsRoot);
      final Object saveCookie = ColorRenamePerCU.startACU(theCUsRoot);
      
      try {	
	visitChildReferences(node);
	super.visitMeth(node);
      } finally {
	ColorRenamePerCU.endACU(saveCookie);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitReference(edu.cmu.cs.fluid.java.analysis.ColorStaticRef)
     */
    @Override
    public void visitReference(ColorStaticRef node) {
      // LOG.severe("Visiting a ColorStaticRef" + node);
      super.visitReference(node);
    }

    /**
     * @return Returns the iNSTANCE.
     */
    public static CSPStruct getInstance() {
      return INSTANCE;
    }

  }

  /**
   * @return Returns the binder.
   */
  public IBinder getBinder() {
    return binder;
  }
}