/*
 * Created on Mar 7, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.colors;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.aast.promise.ColorRenameNode;
import com.surelogic.analysis.colors.*;
import com.surelogic.common.logging.SLLogger;

import SableJBDD.bdd.JBDD;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * @author dfsuther
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ColorRenameDrop extends PromiseDrop<ColorRenameNode> {
  private final CExpr rawExpr;

  public final String simpleName;
  public String qualName;
  public final String qual;

  private JBDD baseExpr = null;
  private JBDD fullExpr = null;

  private int loopCheck;

  public int chainLength = 0;
  private int preds = 0;

  private ColorRenamePerCU myPerCU = null;
  
  private static final Map<String, Collection<ColorRenameDrop>> qualNameToDrops = new HashMap<String, Collection<ColorRenameDrop>>();

  static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");

  static Set<ColorRenameDrop> allRenames = null;
  
  public static Collection<ColorRenameDrop> getRenamesHere(IRNode where) {
    final String qual = JavaNames.computeQualForOutermostTypeOrCU(where);
    return getRenamesHere(qual);
  }
  
  public static synchronized Collection<ColorRenameDrop> getRenamesHere(String qual) {
    Collection<ColorRenameDrop> dropsHere = qualNameToDrops.get(qual);
    
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

    if (ColorRenamePerCU.currentPerCU == null
        || !ColorRenamePerCU.currentPerCU.haveSomeRenames()) {
      return false;
    }

    if (!ColorRenamePerCU.currentPerCU.chainsChecked) {
      sanityCheckRenames();
    }
    return true;
  }

  private ColorRenameDrop(ColorRenameNode a) {
    super(a);
    final IRNode where = a.getPromisedFor();
    rawExpr = a.getCExpr().getTheExpr().buildCExpr(where);

    loopCheck = 0;
    simpleName = JavaNames.genSimpleName(a.getColor().getId());
    qualName = simpleName;
    setNodeAndCompilationUnitDependency(where);
    qual = JavaNames.computeQualForOutermostTypeOrCU(where);
   
  }

  public static ColorRenameDrop buildColorRenameDrop(final ColorRenameNode a) {
    ColorRenameDrop res = new ColorRenameDrop(a);
    final IRNode where = a.getPromisedFor();
    res.myPerCU = ColorRenamePerCU.getColorRenamePerCU(where);
    
    final String qual = JavaNames.computeQualForOutermostTypeOrCU(where);
    res.qualName = qual + "." + res.simpleName;

    synchronized(ColorRenameDrop.class) {
      
      if (allRenames == null) {
        allRenames = new HashSet<ColorRenameDrop>();
      }
      allRenames.add(res);
    
      Collection<ColorRenameDrop> dropsHere = qualNameToDrops.get(qual);
      if (dropsHere == null) {
        dropsHere = new HashSet<ColorRenameDrop>();
        qualNameToDrops.put(qual, dropsHere);
      }
      dropsHere.add(res);
    }
    
    res.myPerCU.addRename(res);
    res.myPerCU.chainsChecked = false;
    res.setMessage("colorRename " +res.simpleName+ " for " +res.rawExpr);
    res.setCategory(JavaGlobals.THREAD_COLORING_CAT);
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
    return rawExpr instanceof CLeafExpr;
  }

  private static boolean emptyOrNull(String str) {
    return (str == null) || (str.equals(""));
  }

  static void sanityCheckRenames() {
    if (ColorRenamePerCU.currentPerCU == null ||
        ColorRenamePerCU.currentPerCU.chainsChecked || (allRenames == null))
      return;

    Set<ColorRenameDrop> safeACR = new HashSet<ColorRenameDrop>(allRenames
        .size());
    safeACR.addAll(allRenames);

    if (safeACR.isEmpty()) {
      return;
    }

    for (ColorRenameDrop crDrop : safeACR) {
      final int checkVal = ColorRenamePerCU.lastLoopCheck + 1;
      Stack<ColorRenameDrop> trail = new Stack<ColorRenameDrop>();
      trail.push(crDrop);
      crDrop.sanityCheck(trail);

      ColorRenamePerCU.lastLoopCheck = checkVal;
    }
    ColorRenamePerCU.currentPerCU.chainsChecked = true;
  }

  private void sanityCheck(Stack<ColorRenameDrop> trail) {
    final int checkVal = ColorRenamePerCU.lastLoopCheck + 1;

    if (loopCheck == checkVal) {
      // we have a problem here!
      StringBuilder trace = new StringBuilder();
      for (ColorRenameDrop d : trail) {
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
      Set<ColorRenameDrop> rhsDrops = rhsRenameDrops();
      for (ColorRenameDrop d : rhsDrops) {
        trail.push(d);
        d.sanityCheck(trail);
        trail.pop();
      }
    }
  }

  public static boolean exprHasRenames(final CExpr exprToCheck) {
    if (!currCuHasSomeRenames()) return false;

    Set<String> exprRenames = exprRenames(exprToCheck);

    return exprRenames.size() > 0;
  }

  private static Set<String> exprRenames(final CExpr exprToCheck) {
    final Set<String> rhsNames = exprToCheck.referencedColorNames();

    rhsNames.retainAll(ColorRenamePerCU.currentPerCU.getRFSKeySet());
    return rhsNames;
  }

  private Set<ColorRenameDrop> rhsRenameDrops() {
    final Set<String> lclRenames = exprRenames(rawExpr);
    final Set<ColorRenameDrop> res = new HashSet<ColorRenameDrop>();

    for (String name : lclRenames) {
      ColorRenameDrop crd = ColorRenamePerCU.currentPerCU.getThisRenameFromString(name);
      res.add(crd);
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
    if (invalidDeponent instanceof ColorSummaryDrop) {
      return;
    }

    
    synchronized(ColorRenameDrop.class) {
      if (allRenames != null) allRenames.remove(this);
      Collection<ColorRenameDrop> dropsHere = qualNameToDrops.get(qual);
      if (dropsHere != null) dropsHere.remove(this);
      
      myPerCU.removeRename(this);
    }
    ColorFirstPass.trackCUchanges(this);
    ColorImportDrop.reprocessImporters(getNode());

    super.deponentInvalidAction(invalidDeponent);
  }

  private static void buildChainRule() {
    if (!currCuHasSomeRenames()) {
      ColorRenamePerCU.currentPerCU.chainRule = null;
      return;
    }

    for (ColorRenameDrop crn : ColorRenamePerCU.currentPerCU.getCurrRenames()) {
      crn.preds = 0;
    }
    
    for (ColorRenameDrop crn : ColorRenamePerCU.currentPerCU.getCurrRenames()) {
      Set<ColorRenameDrop> nexts = crn.rhsRenameDrops();
      for (ColorRenameDrop nxt : nexts) {
        nxt.preds += 1;
      }
    }
    
    List<ColorRenameDrop> noPreds = new LinkedList<ColorRenameDrop>();
    Collection<ColorRenameDrop> preds = new HashSet<ColorRenameDrop>();
    for (ColorRenameDrop crn : ColorRenamePerCU.currentPerCU.getCurrRenames()) {
      if (crn.preds == 0) {
        noPreds.add(crn);
      } else {
        preds.add(crn);
      }
    }
    
    List<ColorRenameDrop> newChainRule = new ArrayList<ColorRenameDrop>(
        ColorRenamePerCU.currentPerCU.getCurrRenames().size());
    
    while (noPreds.size() > 0) {
      ColorRenameDrop noPred = noPreds.remove(0);
      for (ColorRenameDrop follow : noPred.rhsRenameDrops()) {
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
  
    ColorRenamePerCU.currentPerCU.chainRule = newChainRule;
  }

  public static JBDD applyARename(JBDD toExpr, ColorRenameDrop theCRD,
      final PromiseDrop exprsDrop) {
    if (toExpr.isOne() || toExpr.isZero()) return toExpr;
    final ColorNameModel cnm = ColorNameModel.getInstance(theCRD.simpleName,
                                                          ColorRenamePerCU.currentPerCU.cu);
    final TColor canonColor = cnm.getCanonicalTColor();
    JBDD res = canonColor.getSelfExpr().and(theCRD.getBaseExpr());
    res = res.or(canonColor.getSelfExprNeg().and(theCRD.getBaseExpr().not()));
    res = res.and(toExpr);
    res = res.exist(canonColor.getSelfExpr());
    if (!res.equals(toExpr)) {
      theCRD.addDependent(exprsDrop);
      return res;
    } else {
      return toExpr;
    }
  }

  public static JBDD applyChainRule(final JBDD toExpr,
      final PromiseDrop exprsDrop) {
    if (!currCuHasSomeRenames()) return toExpr;

    JBDD res = toExpr.copy();
    for (ColorRenameDrop crd : getChainRule()) {
      res = applyARename(res, crd, exprsDrop);
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
  public CExpr getRawExpr() {
    return rawExpr;
  }

  /**
   * @return Returns the userName.
   */
  public String getUserName() {
    return simpleName;
  }

  public static List<ColorRenameDrop> getChainRule() {
    if (!ColorRenamePerCU.currentPerCU.chainsChecked) {
      sanityCheckRenames();
      buildChainRule();
    } else if (ColorRenamePerCU.currentPerCU.chainRule == null) {
      buildChainRule();
    }
    return ColorRenamePerCU.currentPerCU.chainRule;
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
  public ColorRenamePerCU getMyPerCU() {
    return myPerCU;
  }

//  private static class CrnComparator implements Comparator<ColorRenameDrop> {
//
//    private static Collator myCollator = null;
//
//    private static CrnComparator INSTANCE = new CrnComparator();
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see java.util.Comparator#compare(T, T)
//     */
//    public int compare(ColorRenameDrop o1, ColorRenameDrop o2) {
//      int res = o1.chainLength - o2.chainLength;
//
//      if (res != 0) return res;
//
//      if (myCollator == null) myCollator = Collator.getInstance();
//
//      return myCollator.compare(o1.toString(), o2.toString());
//    }
//
//    private CrnComparator() {
//    }
//
//    public static CrnComparator getInstance() {
//      return INSTANCE;
//    }
//
//  }

}
