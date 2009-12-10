/*
 * Created on Oct 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.colors;

import java.util.*;

import com.surelogic.analysis.colors.ColorMessages;

import SableJBDD.bdd.JBDD;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;

import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ModelDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;



public class ColorizedRegionModel extends ModelDrop {

  private final RegionModel masterRegion;
  private final IRNode protoIR;
  
  private JBDD andOfUserConstraints = null;
  private JBDD computedContext = null;
  
  private Set<PromiseDrop> userDeponents;
  
  private static final Set<ColorizedRegionModel> allCRMs =
    new HashSet<ColorizedRegionModel>();
  
  private ColorizedRegionModel(RegionModel forRegion, IRNode rdsIR) {
    masterRegion = forRegion;
    protoIR = rdsIR;
    setMessage("colorized " + forRegion.getMessage());
    userDeponents = new HashSet<PromiseDrop>(1);
    setCategory(ColorMessages.assuranceCategory);
  }

  /** Get an appropriate ColorizedRegionModel for a region.  Create a new one if
   * necessary.
   * @param forRegion A region we want to get Colorizer information for
   * @return The (possibly freshly created) ColorizedRegionModel for that region.
   */
  public static ColorizedRegionModel getColorizedRegionModel(RegionModel forRegion, IRNode rdsIR) {
    ColorizedRegionModel crm = (ColorizedRegionModel) forRegion.getColorInfo();
    if (crm == null) {
      crm = new ColorizedRegionModel(forRegion, rdsIR);
      forRegion.setColorInfo(crm);
      forRegion.setCategory(JavaGlobals.COLORIZED_REGION_CAT);
      allCRMs.add(crm);
    }
    
    crm.setCategory(JavaGlobals.COLORIZED_REGION_CAT);
    return crm;
  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    allCRMs.remove(this);
    if (masterRegion.isValid()) {
      masterRegion.setColorInfo(null);
    }
    // These drops are created and managed by colorSECONDpass, so don't report
    // invalidating them to colorFIRSTpass!
//    ColorFirstPass.trackCUchanges(this);

    super.deponentInvalidAction(invalidDeponent);
  }

  public static boolean haveColorizedRegions() {
    return !allCRMs.isEmpty();
  }
  
  public static Collection<ColorizedRegionModel> getAllValidCRMs() {
    List<ColorizedRegionModel> res = new ArrayList<ColorizedRegionModel>(allCRMs.size());
    synchronized (ColorizedRegionModel.class) {
      for (ColorizedRegionModel crm : allCRMs) {
        if (crm.isValid()) {
          res.add(crm);
        }
      }
    }
    return res;
  }
  
  /**
   * @return Returns the andOfUserConstraints.
   */
  public JBDD getAndOfUserConstraints() {
    return andOfUserConstraints;
  }

  
  /**
   * @param andOfUserConstraints The andOfUserConstraints to set.
   */
  public void setAndOfUserConstraints(JBDD andOfUserConstraints) {
    this.andOfUserConstraints = andOfUserConstraints;
  }

  
  /**
   * @return Returns the computedContext.
   */
  public JBDD getComputedContext() {
    return computedContext;
  }

  
  /**
   * @param computedContext The computedContext to set.
   */
  public void setComputedContext(JBDD computedConstraint) {
    this.computedContext = computedConstraint;
  }

  
  /**
   * @return Returns the masterRegion.
   */
  public RegionModel getMasterRegion() {
    return masterRegion;
  }

  
  /**
   * @return Returns the userDeponents.
   */
  public Set<PromiseDrop> getUserDeponents() {
    return userDeponents;
  }

  
  /**
   * @param userDeponents The userDeponents to set.
   */
  public void setUserDeponents(Set<PromiseDrop> userDeponents) {
    this.userDeponents = userDeponents;
  }

  
  /**
   * @return Returns the protoIR.
   */
  public IRNode getProtoIR() {
    return protoIR;
  }

  
}
