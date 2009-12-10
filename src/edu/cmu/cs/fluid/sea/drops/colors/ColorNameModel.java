/*
 * Created on Nov 2, 2004
 *
 */
package edu.cmu.cs.fluid.sea.drops.colors;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.analysis.colors.*;
import com.surelogic.common.logging.SLLogger;

import SableJBDD.bdd.JBddVariable;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.*;

/**
 * @author dfsuther
 * 
 * Promise drop for "Color Name" models. We have one of these drops for each
 * unique spelling of "thread color name" (in the sense of "the string a user
 * wrote in an annotation where a thread color name should appear").
 * 
 * 
 * 
 * @lock ColorNameModelLock is class protects globalNameToDrop
 */
public class ColorNameModel extends PhantomDrop {

  Logger LOG = SLLogger.getLogger("ColorDropBuilding");

  /**
   * Map from color names to drop instances. globalNameToDrop maps fully qualified
   * color names to ColorNameModels.
   * simpleNameToLocalDrop maps canonical CU's (according to ColorDeclareDrop) to
   * a map from the simpleName of a color to the drop in that CU.
   */
  private static Map<String, ColorNameModel> globalNameToDrop = 
    new HashMap<String, ColorNameModel>();
  private static Map<IRNode, Map<String, ColorNameModel>> simpleNameToLocalDrop =
    new HashMap<IRNode, Map<String, ColorNameModel>>();
  private static Map<JBddVariable,ColorNameModel> bddVarToDrop =
    new HashMap<JBddVariable,ColorNameModel>();
  
  private TColor canonicalColor = null;

  private JBddVariable theBddVar = null;
  
//  private boolean simpleNameOnly = false;

  private ColorIncSummaryDrop incompatibleSummary = null;

//  public static synchronized ColorNameModel getInstance(String colorName) {
//    // purgeUnusedColorNames(); // cleanup the colors
//
//    String key = colorName;
//    ColorNameModel result = globalNameToDrop.get(key);
//    if (result == null) {
//      key = colorName.intern();
//      result = new ColorNameModel(key);
//
//      globalNameToDrop.put(key, result);
//    }
//    return result;
//  }

  public static synchronized ColorNameModel getInstance(String colorName,
      IRNode where) {
    // purgeUnusedColorNames(); // cleanup the colors

    final String simpleName = JavaNames.genSimpleName(colorName);
    boolean simpleNameOnly = simpleName.equals(colorName);
    final String qualName;
    if (where != null) {
      if (simpleNameOnly) {
        qualName = JavaNames.computeQualForOutermostTypeOrCU(where) + '.' + simpleName;
      } else {
        qualName = colorName;
      }
    } else {
      if (simpleNameOnly) {
        qualName = null;
      } else {
        qualName = colorName;
      }
    }
//    simpleNameOnly = !simpleName.equals(qualName);
    
    String key = qualName;
    final IRNode canonCU = VisitUtil.computeOutermostEnclosingTypeOrCU(where);

    
    ColorNameModel result = null;
    if (!simpleNameOnly) {
     result = globalNameToDrop.get(key);
      if (result == null) {
        key = key.intern();
        result = new ColorNameModel(key, null);
//        result.simpleNameOnly = !key.contentEquals(qualName);
        globalNameToDrop.put(key, result);
      }
    }
    
    key = simpleName.intern();
    Map<String, ColorNameModel> simpleMap = simpleNameToLocalDrop.get(canonCU);
    if (simpleMap == null) {
      simpleMap = new HashMap<String, ColorNameModel>(1);
      simpleNameToLocalDrop.put(canonCU, simpleMap);
    }
    ColorNameModel simpleRes = simpleMap.get(key);
    if (simpleRes == null)  {
      simpleRes = new ColorNameModel(key, where);
//      simpleRes.simpleNameOnly = true;
      simpleMap.put(key, simpleRes);
      if (result != null) {
        simpleRes.canonicalColor = new TColor(result);
      }
    }
    
    if (result != null) {
      return result;
    } else {
      return simpleRes;
    }
  }

  public static synchronized ColorNameModel getCanonicalInstance(
      final String colorName, IRNode locInIR) {
    final ColorNameModel model = ColorNameModel.getInstance(colorName, locInIR);
//    final TColor tc = model.getCanonicalTColor();
    final ColorNameModel canonModel = model.getCanonicalNameModel();
    return canonModel;
  }

  /**
   * The global Color name this drop represents the declaration for.
   */
  private final String colorName;

  /**
   * private constructor invoked by {@link #getInstance(String)}.
   * 
   * @param name
   *          the lock name
   */
  private ColorNameModel(String name, final IRNode locInIR) {
    colorName = name;
    this.setMessage("color " + name);
    setCategory(ColorMessages.assuranceCategory);
    if (locInIR != null) {
      setNodeAndCompilationUnitDependency(locInIR);
    }
  }

  private static DropPredicate definingDropPred = new DropPredicate() {

    public boolean match(Drop d) {
      return d instanceof ColorDeclareDrop || d instanceof ColorRevokeDrop
          || d instanceof ColorGrantDrop || d instanceof ColorIncompatibleDrop
          || d instanceof ColorRenameDrop || d instanceof ColorImportDrop;
    }
  };

  /**
   * Removes color names that are no longer defined by any promise definitions.
   */
  public static synchronized void purgeUnusedColorNames() {
    Map<String, ColorNameModel> newMap = new HashMap<String, ColorNameModel>();
    Set<String> keySet = globalNameToDrop.keySet();
    for (Iterator<String> i = keySet.iterator(); i.hasNext();) {
      String key = i.next();
      ColorNameModel drop = globalNameToDrop.get(key);

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

  public static Collection<ColorNameModel> getAllValidColorNameModels() {
    int workingSize = 0;
    Set<ColorNameModel> safeCopy = null;
    synchronized (ColorNameModel.class) {
      workingSize = globalNameToDrop.size();
      safeCopy = new HashSet<ColorNameModel>(workingSize);
      safeCopy.addAll(globalNameToDrop.values());
    }
    Set<ColorNameModel> res = new HashSet<ColorNameModel>(workingSize);
    // Iterator<ColorNameModel> scIter = safeCopy.iterator();
    // while (scIter.hasNext()) {
    // ColorNameModel cnModel = scIter.next();
    for (ColorNameModel cnModel : safeCopy) {
      if (cnModel.isValid()) {
        res.add(cnModel);
      }
    }
    return res;
  }

  public boolean isDeclared() {
    return hasMatchingDependents(DropPredicateFactory.matchType(ColorDeclareDrop.class));
  }

  public boolean isReallyARename() {
    return hasMatchingDependents(DropPredicateFactory.matchType(ColorRenameDrop.class));
  }

  public static synchronized void makeColorNameModelDeps(
      Collection<String> colorNames, Drop depDrop, IRNode locInIR) {
    if (colorNames == null) return;

    // Iterator<String> lcIter = colorNames.iterator();
    // while (lcIter.hasNext()) {
    // final String aName = lcIter.next();
    for (String aName : colorNames) {
      ColorNameModel cnm = getInstance(aName, locInIR);
      cnm.addDependent(depDrop);
    }
  }

  public static synchronized Collection<ColorNameModel> getColorNameModelInstances(
      Collection<String> colorNames, IRNode locInIR) {
    if ((colorNames == null) || colorNames.isEmpty())
      return Collections.emptySet();

    Collection<ColorNameModel> res = new HashSet<ColorNameModel>(colorNames
        .size());
    // Iterator<String> iter = colorNames.iterator();
    // while (iter.hasNext()) {
    // final String cName = iter.next();
    for (String cName : colorNames) {
      res.add(getInstance(cName, locInIR));
    }
    return res;
  }

  public void setCanonicalTColor(TColor masterColor) {
    canonicalColor = masterColor;
  }
  
  /**
   * @return Returns the canonicalColor.
   */
  public TColor getCanonicalTColor() {
    // if ((canonicalColor == null) && isDeclared()) {
    if (canonicalColor == null) {
      canonicalColor = new TColor(this);
    }
    return canonicalColor;
  }

  public ColorNameModel getCanonicalNameModel() {
    final TColor tc = getCanonicalTColor();
    return tc.getCanonicalNameModel();
  }
  /**
   * @return Returns the theBddVar.
   */
  public JBddVariable getTheBddVar() {
    if (theBddVar != null) return theBddVar;
    
    final ColorNameModel canonModel = getCanonicalNameModel();
//    final JBddVariable canonBddVar = canonModel.theBddVar;
    
    if (canonModel.theBddVar == null) {
      canonModel.theBddVar = 
        ColorBDDPack.getBddFactory().newVariable(canonModel.getColorName());
      synchronized (ColorNameModel.class) {
        bddVarToDrop.put(canonModel.theBddVar, canonModel);
      }
      
    }
    theBddVar = canonModel.theBddVar;
    return theBddVar;
  }

  /**
   * @return Returns the incompatibleSummary.
   */
  public ColorIncSummaryDrop getIncompatibleSummary() {
    if (incompatibleSummary == null) {
      final ColorNameModel canonModel = getCanonicalNameModel();
      if (canonModel.incompatibleSummary == null) {
        canonModel.incompatibleSummary = new ColorIncSummaryDrop(canonModel);
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
   * @return Returns the colorName.
   */
  public String getColorName() {
    return colorName;
  }

}