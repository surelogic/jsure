/*
 * Created on Nov 2, 2004
 *
 */
package edu.cmu.cs.fluid.sea.drops.threadroles;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.analysis.threadroles.*;
import com.surelogic.common.logging.SLLogger;

import SableJBDD.bdd.JBddVariable;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.*;

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
public class TRoleNameModel extends PhantomDrop {

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
  
  private TRoleName canonicalTRole = null;

  private JBddVariable theBddVar = null;
  
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
//    simpleNameOnly = !simpleName.equals(qualName);
    
    String key = qualName;
    final IRNode canonCU = VisitUtil.computeOutermostEnclosingTypeOrCU(where);

    
    TRoleNameModel result = null;
    if (!simpleNameOnly) {
     result = globalNameToDrop.get(key);
      if (result == null) {
        key = key.intern();
        result = new TRoleNameModel(key, null);
//        result.simpleNameOnly = !key.contentEquals(qualName);
        globalNameToDrop.put(key, result);
      }
    }
    
    key = simpleName.intern();
    Map<String, TRoleNameModel> simpleMap = simpleNameToLocalDrop.get(canonCU);
    if (simpleMap == null) {
      simpleMap = new HashMap<String, TRoleNameModel>(1);
      simpleNameToLocalDrop.put(canonCU, simpleMap);
    }
    TRoleNameModel simpleRes = simpleMap.get(key);
    if (simpleRes == null)  {
      simpleRes = new TRoleNameModel(key, where);
//      simpleRes.simpleNameOnly = true;
      simpleMap.put(key, simpleRes);
      if (result != null) {
        simpleRes.canonicalTRole = new TRoleName(result);
      }
    }
    
    if (result != null) {
      return result;
    } else {
      return simpleRes;
    }
  }

  public static synchronized TRoleNameModel getCanonicalInstance(
      final String colorName, IRNode locInIR) {
    final TRoleNameModel model = TRoleNameModel.getInstance(colorName, locInIR);
//    final TRoleName tc = model.getCanonicalTColor();
    final TRoleNameModel canonModel = model.getCanonicalNameModel();
    return canonModel;
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
    tRoleName = name;
    this.setMessage("ThreadRole " + name);
    setCategory(TRoleMessages.assuranceCategory);
    if (locInIR != null) {
      setNodeAndCompilationUnitDependency(locInIR);
    }
  }

  private static DropPredicate definingDropPred = new DropPredicate() {

    public boolean match(Drop d) {
      return d instanceof TRoleDeclareDrop || d instanceof TRoleRevokeDrop
          || d instanceof TRoleGrantDrop || d instanceof TRoleIncompatibleDrop
          || d instanceof TRoleRenameDrop || d instanceof TRoleImportDrop;
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

  public void setCanonicalTRole(TRoleName masterTRole) {
    canonicalTRole = masterTRole;
  }
  
  /**
   * @return Returns the canonicalTRole.
   */
  public TRoleName getCanonicalTRole() {
    // if ((canonicalTRole == null) && isDeclared()) {
    if (canonicalTRole == null) {
      canonicalTRole = new TRoleName(this);
    }
    return canonicalTRole;
  }

  public TRoleNameModel getCanonicalNameModel() {
    final TRoleName tc = getCanonicalTRole();
    return tc.getCanonicalNameModel();
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

}