/*
 * Created on Nov 10, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.RequiresLock;
import com.surelogic.analysis.effects.ConflictChecker;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.targets.DefaultTargetFactory;
import com.surelogic.analysis.effects.targets.NoEvidence;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;

public class WholeModuleFXDrop extends IRReferenceDrop {

  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis.effects");

  private static final Map<IRNode, WholeModuleFXDrop> modFxMap = new HashMap<IRNode, WholeModuleFXDrop>();

  private static final Set<IRegion> currentInterestingRegions = new HashSet<IRegion>();
  private static final Set<Effect> interestingRegionWriteFX = new HashSet<Effect>();

  private static IBinder binder = null;
  private static ConflictChecker conflicter = null;

  private final Set<Effect> methodFX;
  private final Set<Effect> filteredMethodFX;
  private final List<Effect> fixedPointFX;

  private WholeModuleFXDrop() {
    super(null); // will blow up!
    methodFX = new HashSet<Effect>(4);
    filteredMethodFX = new HashSet<Effect>(0);
    fixedPointFX = new ArrayList<Effect>();
  }

  public static WholeModuleFXDrop getMethodFX(final IRNode mDecl) {
    WholeModuleFXDrop res;
    synchronized (WholeModuleFXDrop.class) {
      res = modFxMap.get(mDecl);
      if (res == null) {
        res = new WholeModuleFXDrop();
        modFxMap.put(mDecl, res);
        // res.setNodeAndCompilationUnitDependency(mDecl);
      }
    }

    // ??do something here about computing the FX???

    return res;
  }

  public static synchronized Set<WholeModuleFXDrop> getAllWholeModuleFXDrops() {
    Set<WholeModuleFXDrop> res = new HashSet<WholeModuleFXDrop>();
    res.addAll(modFxMap.values());
    return res;
  }

  public static void addInterestingRegions(final Set<IRegion> interestingRegions) {
    currentInterestingRegions.addAll(interestingRegions);
  }

  public static int numInterestingRegions() {
    return currentInterestingRegions.size();
  }

  public static Set<Effect> getInterestingRegionWriteFX() {
    if (!interestingRegionWriteFX.isEmpty()) {
      return interestingRegionWriteFX;
    }

    for (IRegion r : currentInterestingRegions) {
      final IRNode type = VisitUtil.getEnclosingType(r.getNode());
      final IJavaType jType = binder.getJavaType(type);
      if (!(jType instanceof IJavaDeclaredType) && !(jType instanceof IJavaArrayType)) {
        // don't know what to do for this case!
        LOG.severe("Interesting region " + r + " not for a declared or array type.  Now what?");
        continue;
      }

      final IJavaReferenceType jRefType = (IJavaReferenceType) jType;
      final Target t = DefaultTargetFactory.PROTOTYPE.createAnyInstanceTarget(jRefType, r, NoEvidence.INSTANCE);
      final Effect e = Effect.newWrite(null, t); // bogus src expression
      interestingRegionWriteFX.add(e);
    }
    return interestingRegionWriteFX;
  }

  private static final boolean filteringNYI = false;

  public void filterFX() {
    // globally filter the computed effects to include only those that are
    // relevant
    // to the "interesting regions." This may be done either before or after
    // computing the local effects. It must be done before computing the fixed
    // point.
    // We will end up using the union of all the interesting regions as a filter
    // to
    // shrink the problem before computing a fixed point.

    if (filteringNYI || currentInterestingRegions.isEmpty()) {
      filteredMethodFX.addAll(methodFX);
    } else {
      filteredMethodFX.addAll(filterSomeFX(methodFX));
    }
  }

  public static Set<Effect> filterSomeFX(Set<Effect> fxToFilter) {
    Set<Effect> res = new HashSet<Effect>(1);

    if (fxToFilter == null || fxToFilter.isEmpty()) {
      // nothing to do
    } else if (filteringNYI || currentInterestingRegions.isEmpty()) {
      res.addAll(fxToFilter);
    } else {
      final Set<Effect> intRegionWrites = getInterestingRegionWriteFX();

      for (Effect efx : fxToFilter) {
        if (conflicter.mayConflict(intRegionWrites, Collections.singleton(efx))) {
          res.add(efx);
        }
      }
    }
    return res;
  }

  /**
   * Starting a new analysis run. Blow away any invalid WMFXdrops completely,
   * including removing them from the modFxMap. Also clear all the FX sets in
   * all drops. We'll need to recompute the FX sets from scratch, anyway.
   */
  public static void startNewAnalysisRun() {
    currentInterestingRegions.clear();
    interestingRegionWriteFX.clear();
    for (WholeModuleFXDrop fxDrop : getAllWholeModuleFXDrops()) {
      if (!fxDrop.isValid()) {
        modFxMap.remove(fxDrop.getNode());
      }
      fxDrop.methodFX.clear();
      fxDrop.filteredMethodFX.clear();
      fxDrop.fixedPointFX.clear();
    }
  }

  public static void resetAnalysis(IBinder b, ConflictChecker c) {
    binder = b;
    conflicter = c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  @RequiresLock("SeaLock")
  protected void deponentInvalidAction(Drop invalidDeponent) {
    modFxMap.remove(getNode());
    super.deponentInvalidAction(invalidDeponent);
  }

  /**
   * @return Returns the filteredMethodFX.
   */
  public Set<Effect> getFilteredMethodFX() {
    return filteredMethodFX;
  }

  /**
   * @return Returns the fixedPointFX.
   */
  public List<Effect> getFixedPointFX() {
    return fixedPointFX;
  }

  /**
   * @return Returns the methodFX.
   */
  public Set<Effect> getMethodFX() {
    return methodFX;
  }

}
