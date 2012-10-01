/*
 * Created on Nov 2, 2004
 *
 */
package com.surelogic.dropsea.ir.drops.threadroles;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import SableJBDD.bdd.JBDD;
import SableJBDD.bdd.JBddVariable;

import com.surelogic.analysis.threadroles.TRoleBDDPack;
import com.surelogic.analysis.threadroles.TRoleMessages;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.DropPredicate;
import com.surelogic.dropsea.ir.DropPredicateFactory;
import com.surelogic.dropsea.ir.IRReferenceDrop;
import com.surelogic.dropsea.ir.Sea;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * @author dfsuther
 * 
 * Promise drop for "ThreadRole Name" models. We have one of these drops for each
 * unique spelling of "thread role name" (in the sense of "the string a user
 * wrote in an annotation where a thread role name should appear").
 * 
 * 
 * 
 * @lock ColorNameModelLock is class protects globalNameToDrop
 */
public class TRoleNameModel extends IRReferenceDrop implements IThreadRoleDrop, Comparable<TRoleNameModel> {

  Logger LOG = SLLogger.getLogger("TRoleDropBuilding");

  /**
   * Map from thread role names to drop instances. globalNameToDrop maps fully qualified
   * thread role names to ColorNameModels.
   * simpleNameToLocalDrop maps canonical CU's (according to ColorDeclareDrop) to
   * a map from the simpleName of a role to the drop in that CU.
   */
  private static Map<String, TRoleNameModel> globalNameToDrop = 
    new HashMap<String, TRoleNameModel>();
  private static Map<IRNode, Map<String, TRoleNameModel>> simpleNameToLocalDrop =
    new HashMap<IRNode, Map<String, TRoleNameModel>>();
  private static Map<JBddVariable,TRoleNameModel> bddVarToDrop =
    new HashMap<JBddVariable,TRoleNameModel>();
  
  private TRoleNameModel canonicalTRNM = null;

  private JBddVariable theBddVar = null;
  private JBDD selfExpr = null;
  
  private TRoleIncSummaryDrop incompatibleSummary = null;

  public static synchronized TRoleNameModel getInstance(String tRoleName,
      IRNode where) {
    // purgeUnusedColorNames(); // cleanup the colors

    final String simpleName = JavaNames.genSimpleName(tRoleName);
    boolean simpleNameOnly = simpleName.equals(tRoleName);
    final String qualName;
    if (where != null) {
      if (simpleNameOnly) {
        qualName = JavaNames.computeQualForOutermostTypeOrCU(where) + '.' + simpleName;
      } else {
        qualName = tRoleName;
      }
    } else {
      if (simpleNameOnly) {
        qualName = null;
      } else {
        qualName = tRoleName;
      }
    }
    
    String key = qualName;
    final IRNode canonCU = VisitUtil.computeOutermostEnclosingTypeOrCU(where);

    
    TRoleNameModel result = null;
    if (!simpleNameOnly) {
     result = globalNameToDrop.get(key);
      if (result == null) {
        result = new TRoleNameModel(key, null);
        globalNameToDrop.put(key, result);
      }
    }
    
    key = simpleName;
    Map<String, TRoleNameModel> simpleMap = simpleNameToLocalDrop.get(canonCU);
    if (simpleMap == null) {
      simpleMap = new HashMap<String, TRoleNameModel>(1);
      simpleNameToLocalDrop.put(canonCU, simpleMap);
    }
    TRoleNameModel simpleRes = simpleMap.get(key);
    if (simpleRes == null)  {
      simpleRes = new TRoleNameModel(key, where);
      simpleMap.put(key, simpleRes);
    }
    
    if (result != null) {
      simpleRes.canonicalTRNM = result;
      return result;
    } else {
      return simpleRes;
    }
  }

  public static synchronized TRoleNameModel getCanonicalInstance(
      final String colorName, IRNode locInIR) {
    final TRoleNameModel model = TRoleNameModel.getInstance(colorName, locInIR);
    final TRoleNameModel canonModel = model.getCanonicalNameModel();
    return canonModel;
  }

	/*
	 * We need to be able to find any TRoleIncompatibleDrops by filtering
	 * through the dependents of the canonical TRNM for the colors they name.
	 * But those TRoleIncompatibleDrops are probably only in the dependent set
	 * of the local short-names for the roles they name. So we grovel through
	 * the dependents of all the local short-names to find the
	 * TRoleIncompatibleDrops that we are looking for and install them as direct
	 * dependents of the canonical TRNMs for the colors they name.
	 */
  public static synchronized void promoteIncDepsToCanonName(IRNode forThisPackage) {
	  final IRNode canonCU = VisitUtil.computeOutermostEnclosingTypeOrCU(forThisPackage);
	  final Map<String, TRoleNameModel> simpleMap = simpleNameToLocalDrop.get(canonCU);
	  
	  if (simpleMap == null) {
		  // nothing to do right now
		  return;
	  }
	  
	  for (TRoleNameModel locTRNM : simpleMap.values()) {
		  final Collection<? extends TRoleIncompatibleDrop> incompatibles =
			  Sea.filterDropsOfType(TRoleIncompatibleDrop.class, locTRNM.getDependents());
		  final TRoleNameModel canonTRNM = locTRNM.getCanonicalNameModel();
		  canonTRNM.addDependents(incompatibles);
	  }
  }
  
  /**
   * The global thread role name this drop represents the declaration for.
   */
  private final String tRoleName;

  /**
   * private constructor invoked by {@link #getInstance(String)}.
   * 
   * @param name
   *          the lock name
   */
  private TRoleNameModel(String name, final IRNode locInIR) {
    super(locInIR); // may blow up!! can't be null
    tRoleName = name;
    this.setMessage("ThreadRole " + name);
    setCategorizingMessage(TRoleMessages.assuranceCategory);
//    if (locInIR != null) {
//      setNodeAndCompilationUnitDependency(locInIR);
//    }
  }

  private static DropPredicate definingDropPred = new DropPredicate() {

    public boolean match(IDrop d) {
      return d.instanceOfIRDropSea(TRoleDeclareDrop.class) || d.instanceOfIRDropSea(TRoleRevokeDrop.class) || d.instanceOfIRDropSea(TRoleGrantDrop.class)
          || d.instanceOfIRDropSea(TRoleIncompatibleDrop.class) || d.instanceOfIRDropSea(TRoleRenameDrop.class)
          || d.instanceOfIRDropSea(TRoleImportDrop.class);
    }
  };

  /**
   * Removes color names that are no longer defined by any promise definitions.
   */
  public static synchronized void purgeUnusedTRoleNames() {
    Map<String, TRoleNameModel> newMap = new HashMap<String, TRoleNameModel>();
    Set<String> keySet = globalNameToDrop.keySet();
    for (Iterator<String> i = keySet.iterator(); i.hasNext();) {
      String key = i.next();
      TRoleNameModel drop = globalNameToDrop.get(key);

      boolean colorDefinedInCode = drop.hasMatchingDependents(definingDropPred);
      if (colorDefinedInCode) {
        newMap.put(key, drop);
      } else {
        drop.invalidate();
      }
    }
    // swap out the static map to locks
    globalNameToDrop = newMap;
  }

  public static Collection<TRoleNameModel> getAllValidTRoleNameModels() {
    int workingSize = 0;
    Set<TRoleNameModel> safeCopy = null;
    synchronized (TRoleNameModel.class) {
      workingSize = globalNameToDrop.size();
      safeCopy = new HashSet<TRoleNameModel>(workingSize);
      safeCopy.addAll(globalNameToDrop.values());
    }
    Set<TRoleNameModel> res = new HashSet<TRoleNameModel>(workingSize);
    // Iterator<TRoleNameModel> scIter = safeCopy.iterator();
    // while (scIter.hasNext()) {
    // TRoleNameModel cnModel = scIter.next();
    for (TRoleNameModel cnModel : safeCopy) {
      if (cnModel.isValid()) {
        res.add(cnModel);
      }
    }
    return res;
  }

  public boolean isDeclared() {
    return hasMatchingDependents(DropPredicateFactory.matchType(TRoleDeclareDrop.class));
  }

  public boolean isReallyARename() {
    return hasMatchingDependents(DropPredicateFactory.matchType(TRoleRenameDrop.class));
  }

  public static synchronized void makeTRoleNameModelDeps(
      Collection<String> tRoleNames, Drop depDrop, IRNode locInIR) {
    if (tRoleNames == null) return;

    for (String aName : tRoleNames) {
      TRoleNameModel cnm = getInstance(aName, locInIR);
      cnm.addDependent(depDrop);
    }
  }

  public static synchronized Collection<TRoleNameModel> getTRoleNameModelInstances(
      Collection<String> tRoleNames, IRNode locInIR) {
    if ((tRoleNames == null) || tRoleNames.isEmpty())
      return Collections.emptySet();

    Collection<TRoleNameModel> res = new HashSet<TRoleNameModel>(tRoleNames
        .size());
    for (String trName : tRoleNames) {
      res.add(getInstance(trName, locInIR));
    }
    return res;
  }

  public void setCanonicalTRole(TRoleNameModel master) {
    canonicalTRNM = master;
  }
  
  public TRoleNameModel getCanonicalNameModel() {
    if (canonicalTRNM == null) {
    	canonicalTRNM = this;
    }
    return canonicalTRNM;
  }
  /**
   * @return Returns the theBddVar.
   */
  public JBddVariable getTheBddVar() {
    if (theBddVar != null) return theBddVar;
    
    final TRoleNameModel canonModel = getCanonicalNameModel();
//    final JBddVariable canonBddVar = canonModel.theBddVar;
    
    if (canonModel.theBddVar == null) {
      canonModel.theBddVar = 
        TRoleBDDPack.getBddFactory().newVariable(canonModel.getTRoleName());
      synchronized (TRoleNameModel.class) {
        bddVarToDrop.put(canonModel.theBddVar, canonModel);
      }
      
    }
    theBddVar = canonModel.theBddVar;
    return theBddVar;
  }

  /**
   * @return Returns the incompatibleSummary.
   */
  public TRoleIncSummaryDrop getIncompatibleSummary() {
    if (incompatibleSummary == null) {
      final TRoleNameModel canonModel = getCanonicalNameModel();
      if (canonModel.incompatibleSummary == null) {
        canonModel.incompatibleSummary = new TRoleIncSummaryDrop(canonModel);
      }
      incompatibleSummary = canonModel.incompatibleSummary;
    }
    return incompatibleSummary;
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.sea.PromiseDrop#isCheckedByAnalysis()
   */
  public boolean isCheckedByAnalysis() {
    // ColorNameModelDrops are "checked by analysis" if and only if they are
    // actually
    // declared. This is needed so that declared colorNameModels can
    // always be treated as locally consistent.
    return isDeclared();
  }

  /**
   * @return Returns the tRoleName.
   */
  public String getTRoleName() {
    return tRoleName;
  }
  
  /**
   * @return Returns the conflictExpr.
   */
  public JBDD getConflictExpr() {
    return getCanonicalNameModel().getIncompatibleSummary().getConflictExpr().copy();
  }
  /**
   * @return Returns the selfExpr.
   */
  public JBDD getSelfExpr() {
    if (selfExpr == null) {
      selfExpr = TRoleBDDPack.getBddFactory().posBddOf(getCanonicalNameModel().getTheBddVar());
    }
    return selfExpr.copy();
  }
  
  public JBDD getSelfExprNeg() {
    return getSelfExpr().not();
  }

/* (non-Javadoc)
 * @see java.lang.Comparable#compareTo(java.lang.Object)
 */
public int compareTo(TRoleNameModel o) {
	final TRoleNameModel canonModel = getCanonicalNameModel();
	return canonModel.tRoleName.compareTo(o.getCanonicalNameModel().tRoleName);
}

}