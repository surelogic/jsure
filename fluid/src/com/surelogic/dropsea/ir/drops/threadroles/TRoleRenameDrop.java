/*
 * Created on Mar 7, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.threadroles;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.aast.promise.ThreadRoleRenameNode;
import com.surelogic.analysis.threadroles.*;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.PromiseDrop;

import SableJBDD.bdd.JBDD;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;

/**
 * @author dfsuther
 */
public class TRoleRenameDrop extends PromiseDrop<ThreadRoleRenameNode> 
implements IThreadRoleDrop{
  private final TRExpr rawExpr;

  public final String simpleName;
  public String qualName;
  public final String qual;

  private JBDD baseExpr = null;
  private JBDD fullExpr = null;

  private int loopCheck;

  public int chainLength = 0;
  private int preds = 0;

  private TRoleRenamePerCU myPerCU = null;
  
  private static final Map<String, Collection<TRoleRenameDrop>> qualNameToDrops = new HashMap<String, Collection<TRoleRenameDrop>>();

  static final Logger LOG = SLLogger.getLogger("TRoleDropBuilding");

  static Set<TRoleRenameDrop> allRenames = null;
  
  public static Collection<TRoleRenameDrop> getRenamesHere(IRNode where) {
    final String qual = JavaNames.computeQualForOutermostTypeOrCU(where);
    return getRenamesHere(qual);
  }
  
  public static synchronized Collection<TRoleRenameDrop> getRenamesHere(String qual) {
    Collection<TRoleRenameDrop> dropsHere = qualNameToDrops.get(qual);
    
    return dropsHere;
  }

  public static boolean globalHaveRenames() {
    if (allRenames == null || allRenames.size() == 0) {
      return false;
    } else {
      return true;
    }
  }
  public static boolean currCuHasSomeRenames() {
    if (!globalHaveRenames()) return false;

    if (TRoleRenamePerCU.currentPerCU == null
        || !TRoleRenamePerCU.currentPerCU.haveSomeRenames()) {
      return false;
    }

    if (!TRoleRenamePerCU.currentPerCU.chainsChecked) {
      sanityCheckRenames();
    }
    return true;
  }

  private TRoleRenameDrop(ThreadRoleRenameNode a) {
    super(a);
    final IRNode where = a.getPromisedFor();
    rawExpr = a.getTRExpr().getTheExpr().buildTRExpr(where);

    loopCheck = 0;
    simpleName = JavaNames.genSimpleName(a.getThreadRole().getId());
    qualName = simpleName;
    //setNodeAndCompilationUnitDependency(where);
    qual = JavaNames.computeQualForOutermostTypeOrCU(where);
   
  }

  public static TRoleRenameDrop buildTRoleRenameDrop(final ThreadRoleRenameNode a) {
    TRoleRenameDrop res = new TRoleRenameDrop(a);
    final IRNode where = a.getPromisedFor();
    res.myPerCU = TRoleRenamePerCU.getTRoleRenamePerCU(where);
    
    final String qual = JavaNames.computeQualForOutermostTypeOrCU(where);
    res.qualName = qual + "." + res.simpleName;

    synchronized(TRoleRenameDrop.class) {
      
      if (allRenames == null) {
        allRenames = new HashSet<TRoleRenameDrop>();
      }
      allRenames.add(res);
    
      Collection<TRoleRenameDrop> dropsHere = qualNameToDrops.get(qual);
      if (dropsHere == null) {
        dropsHere = new HashSet<TRoleRenameDrop>();
        qualNameToDrops.put(qual, dropsHere);
      }
      dropsHere.add(res);
    }
    
    res.myPerCU.addRename(res);
    res.myPerCU.chainsChecked = false;
    res.setMessage("ThreadRoleRename " +res.simpleName+ " for " +res.rawExpr);
    res.setCategory(TRoleMessages.assuranceCategory);
    return res;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(simpleName);
    sb.append(" renames ");
    sb.append(rawExpr.toString());
    return sb.toString();
  }
  
  public boolean isSimpleRename() {
    return rawExpr instanceof TRLeafExpr;
  }

  private static boolean emptyOrNull(String str) {
    return (str == null) || (str.equals(""));
  }

  static void sanityCheckRenames() {
    if (TRoleRenamePerCU.currentPerCU == null ||
        TRoleRenamePerCU.currentPerCU.chainsChecked || (allRenames == null))
      return;

    Set<TRoleRenameDrop> safeATRR = new HashSet<TRoleRenameDrop>(allRenames
        .size());
    safeATRR.addAll(allRenames);

    if (safeATRR.isEmpty()) {
      return;
    }

    for (TRoleRenameDrop trrDrop : safeATRR) {
      final int checkVal = TRoleRenamePerCU.lastLoopCheck + 1;
      Stack<TRoleRenameDrop> trail = new Stack<TRoleRenameDrop>();
      trail.push(trrDrop);
      trrDrop.sanityCheck(trail);

      TRoleRenamePerCU.lastLoopCheck = checkVal;
    }
    TRoleRenamePerCU.currentPerCU.chainsChecked = true;
  }

  private void sanityCheck(Stack<TRoleRenameDrop> trail) {
    final int checkVal = TRoleRenamePerCU.lastLoopCheck + 1;

    if (loopCheck == checkVal) {
      // we have a problem here!
      StringBuilder trace = new StringBuilder();
      for (TRoleRenameDrop d : trail) {
        StringBuilder dsb = new StringBuilder();
        dsb.append(d);
        dsb.append(";\n");
        trace = dsb.append(trace);
      }
      LOG.severe("Rename Loop discovered! traceback:\n" + trace.toString());
      return;
    } else {
      loopCheck = checkVal;
      if (trail.size() > chainLength) {
        chainLength = trail.size();
      }
      Set<TRoleRenameDrop> rhsDrops = rhsRenameDrops();
      for (TRoleRenameDrop d : rhsDrops) {
        trail.push(d);
        d.sanityCheck(trail);
        trail.pop();
      }
    }
  }

  public static boolean exprHasRenames(final TRExpr exprToCheck) {
    if (!currCuHasSomeRenames()) return false;

    Set<String> exprRenames = exprRenames(exprToCheck);

    return exprRenames.size() > 0;
  }

  private static Set<String> exprRenames(final TRExpr exprToCheck) {
    final Set<String> rhsNames = exprToCheck.referencedColorNames();

    rhsNames.retainAll(TRoleRenamePerCU.currentPerCU.getRFSKeySet());
    return rhsNames;
  }

  private Set<TRoleRenameDrop> rhsRenameDrops() {
    final Set<String> lclRenames = exprRenames(rawExpr);
    final Set<TRoleRenameDrop> res = new HashSet<TRoleRenameDrop>();

    for (String name : lclRenames) {
      TRoleRenameDrop trrd = TRoleRenamePerCU.currentPerCU.getThisRenameFromString(name);
      res.add(trrd);
    }

    return res;
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof TRoleSummaryDrop) {
      return;
    }

    
    synchronized(TRoleRenameDrop.class) {
      if (allRenames != null) allRenames.remove(this);
      Collection<TRoleRenameDrop> dropsHere = qualNameToDrops.get(qual);
      if (dropsHere != null) dropsHere.remove(this);
      
      myPerCU.removeRename(this);
    }
    TRolesFirstPass.trackCUchanges(this);
    TRoleImportDrop.reprocessImporters(getNode());

    super.deponentInvalidAction(invalidDeponent);
  }

  private static void buildChainRule() {
    if (!currCuHasSomeRenames()) {
      TRoleRenamePerCU.currentPerCU.chainRule = null;
      return;
    }

    for (TRoleRenameDrop trrn : TRoleRenamePerCU.currentPerCU.getCurrRenames()) {
      trrn.preds = 0;
    }
    
    for (TRoleRenameDrop trrn : TRoleRenamePerCU.currentPerCU.getCurrRenames()) {
      Set<TRoleRenameDrop> nexts = trrn.rhsRenameDrops();
      for (TRoleRenameDrop nxt : nexts) {
        nxt.preds += 1;
      }
    }
    
    List<TRoleRenameDrop> noPreds = new LinkedList<TRoleRenameDrop>();
    Collection<TRoleRenameDrop> preds = new HashSet<TRoleRenameDrop>();
    for (TRoleRenameDrop trrn : TRoleRenamePerCU.currentPerCU.getCurrRenames()) {
      if (trrn.preds == 0) {
        noPreds.add(trrn);
      } else {
        preds.add(trrn);
      }
    }
    
    List<TRoleRenameDrop> newChainRule = new ArrayList<TRoleRenameDrop>(
        TRoleRenamePerCU.currentPerCU.getCurrRenames().size());
    
    while (noPreds.size() > 0) {
      TRoleRenameDrop noPred = noPreds.remove(0);
      for (TRoleRenameDrop follow : noPred.rhsRenameDrops()) {
        follow.preds -= 1;
        if (follow.preds == 0) {
          preds.remove(follow);
          noPreds.add(follow);
        }
      }
      newChainRule.add(noPred);
    }
    
    if (preds.size() > 0) {
      LOG.severe("Found a chain rule loop!");
    }
  
    TRoleRenamePerCU.currentPerCU.chainRule = newChainRule;
  }

  public static JBDD applyARename(JBDD toExpr, TRoleRenameDrop theTRRD,
      final PromiseDrop exprsDrop) {
    if (toExpr.isOne() || toExpr.isZero()) return toExpr;
    final TRoleNameModel trnm = TRoleNameModel.getInstance(theTRRD.simpleName,
                                                          TRoleRenamePerCU.currentPerCU.cu);
    final TRoleNameModel canonModel = trnm.getCanonicalNameModel();
    JBDD res = canonModel.getSelfExpr().and(theTRRD.getBaseExpr());
    res = res.or(canonModel.getSelfExprNeg().and(theTRRD.getBaseExpr().not()));
    res = res.and(toExpr);
    res = res.exist(canonModel.getSelfExpr());
    if (!res.equals(toExpr)) {
      theTRRD.addDependent(exprsDrop);
      return res;
    } else {
      return toExpr;
    }
  }

  public static JBDD applyChainRule(final JBDD toExpr,
      final PromiseDrop exprsDrop) {
    if (!currCuHasSomeRenames()) return toExpr;

    JBDD res = toExpr.copy();
    for (TRoleRenameDrop trrd : getChainRule()) {
      res = applyARename(res, trrd, exprsDrop);
    }
    return res;
  }

  /**
   * @return Returns the Set of all Renames that apply in the current analysis
   */
  public static Set getAllRenames() {
    return allRenames;
  }

  /**
   * @return Returns the rawExpr.
   */
  public TRExpr getRawExpr() {
    return rawExpr;
  }

  /**
   * @return Returns the userName.
   */
  public String getUserName() {
    return simpleName;
  }

  public static List<TRoleRenameDrop> getChainRule() {
    if (!TRoleRenamePerCU.currentPerCU.chainsChecked) {
      sanityCheckRenames();
      buildChainRule();
    } else if (TRoleRenamePerCU.currentPerCU.chainRule == null) {
      buildChainRule();
    }
    return TRoleRenamePerCU.currentPerCU.chainRule;
  }

  /**
   * @return Returns the baseExpr.
   */
  public JBDD getBaseExpr() {
    if (baseExpr == null) {
      baseExpr = rawExpr.computeExpr(false);
    }
    return baseExpr;
  }

  
  /**
   * @return Returns the fullExpr.
   */
  public JBDD getFullExpr() {
    if (fullExpr == null) {
      JBDD t = rawExpr.computeExpr(true);
      fullExpr = applyChainRule(t, null);
    }
    return fullExpr;
  }

  
  /**
   * @return Returns the myPerCU.
   */
  public TRoleRenamePerCU getMyPerCU() {
    return myPerCU;
  }

}
