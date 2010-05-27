/*
 * Created on Oct 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.threadroles;

import java.util.*;

import com.surelogic.analysis.threadroles.TRoleMessages;

import SableJBDD.bdd.JBDD;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;

import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ModelDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;



public class RegionTRoleModel extends ModelDrop implements IThreadRoleDrop {

  private final RegionModel masterRegion;
  private final IRNode protoIR;
  
  private JBDD andOfUserConstraints = null;
  private JBDD computedContext = null;
  
  private Set<PromiseDrop> userDeponents;
  
  private static final Set<RegionTRoleModel> allRegTRoleMods =
    new HashSet<RegionTRoleModel>();
  
  private RegionTRoleModel(RegionModel forRegion, IRNode rdsIR) {
    masterRegion = forRegion;
    protoIR = rdsIR;
    setMessage("regionTRoles " + forRegion.getMessage());
    userDeponents = new HashSet<PromiseDrop>(1);
    setCategory(TRoleMessages.assuranceCategory);
  }

  /** Get an appropriate ColorizedRegionModel for a region.  Create a new one if
   * necessary.
   * @param forRegion A region we want to get Colorizer information for
   * @return The (possibly freshly created) ColorizedRegionModel for that region.
   */
  public static RegionTRoleModel getRegionTRoleModel(RegionModel forRegion, IRNode rdsIR) {
    RegionTRoleModel rtrm = (RegionTRoleModel) forRegion.getColorInfo();
    if (rtrm == null) {
      rtrm = new RegionTRoleModel(forRegion, rdsIR);
      forRegion.setColorInfo(rtrm);
      forRegion.setCategory(JavaGlobals.THREAD_ROLE_REPORT_REGION_CAT);
      allRegTRoleMods.add(rtrm);
    }
    
    rtrm.setCategory(JavaGlobals.THREAD_ROLE_REPORT_REGION_CAT);
    return rtrm;
  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    allRegTRoleMods.remove(this);
    if (masterRegion.isValid()) {
      masterRegion.setColorInfo(null);
    }
    // These drops are created and managed by colorSECONDpass, so don't report
    // invalidating them to colorFIRSTpass!
//    TRolesFirstPass.trackCUchanges(this);

    super.deponentInvalidAction(invalidDeponent);
  }

  public static boolean haveTRoleRegions() {
    return !allRegTRoleMods.isEmpty();
  }
  
  public static Collection<RegionTRoleModel> getAllValidRegionTRoleMods() {
    List<RegionTRoleModel> res = new ArrayList<RegionTRoleModel>(allRegTRoleMods.size());
    synchronized (RegionTRoleModel.class) {
      for (RegionTRoleModel tRoleMod : allRegTRoleMods) {
        if (tRoleMod.isValid()) {
          res.add(tRoleMod);
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
