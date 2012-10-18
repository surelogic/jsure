/*
 * Created on Oct 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.threadroles;

import com.surelogic.analysis.threadroles.TRExpr;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.drops.RegionModel;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Phantom drop for "region" promise declarations. The RegionModel is used as
 * the actual promise drop. This drop is used to maintain compilation unit
 * dependencies.
 * 
 * @see com.surelogic.dropsea.ir.drops.RegionModel
 * @see edu.cmu.cs.fluid.java.analysis.Region
 * @see edu.cmu.cs.fluid.sea.drops.promises.ColorRegionModel
 */
public class RegionTRoleDeclDrop extends Drop implements IThreadRoleDrop {

  private final TRExpr userConstraint;

  private TRExpr renamedConstraint = null;

  private final RegionModel masterRegion;

  private final String regionName;
  
  private RegionTRoleModel regionTRoleModelInfo = null;

  
  /** Create the promise drop for a RegionColorConstraint declaration.
   * @param regionName Name of the region we are constraining
   * @param constraint Expression representing the constraint (as written by the user!)
   * @param where Location of the decl in the tree.
   */
  private RegionTRoleDeclDrop(final String regionName, final TRExpr constraint, IRNode where) {
    super(null); // will blow up!
    this.regionName = regionName;
    masterRegion = null; // RegionModel.getInstance(where);
    userConstraint = constraint.doClone();
  }

public static RegionTRoleDeclDrop buildRegionTRoleDecl(final String regionName, final TRExpr constraint,
    final IRNode where) {
  RegionTRoleDeclDrop res = new RegionTRoleDeclDrop(regionName, constraint, where);
  // res.setNodeAndCompilationUnitDependency(where);
  
  RegionTRoleModel tRTRDDrop = null; // (RegionTRoleModel) res.masterRegion.getColorInfo();
  if (tRTRDDrop == null) {
    
    final IRNode vdecl = null;//BindUtil.findRegionInType(where, regionName);
    
    tRTRDDrop = RegionTRoleModel.getRegionTRoleModel(res.masterRegion, vdecl);
    //res.masterRegion.setColorInfo(tRTRDDrop);
  } 
  res.regionTRoleModelInfo = tRTRDDrop;
  

  res.addDependent(res.regionTRoleModelInfo);
  
  res.masterRegion.addDependent(res);
  
  res.setMessage("ThreadRoleConstraint " +constraint+ " for region " +res.masterRegion.getRegionName());
  
  return res;
}
  
  /**
   * @return Returns the renamedConstraint.
   */
  public TRExpr getRenamedConstraint() {
    return renamedConstraint;
  }


  
  /**
   * @param renamedConstraint The renamedConstraint to set.
   */
  public void setRenamedConstraint(TRExpr renamedConstraint) {
    this.renamedConstraint = renamedConstraint;
  }


  
  /**
   * @return Returns the masterRegion.
   */
  public RegionModel getMasterRegion() {
    return masterRegion;
  }


  
  /**
   * @return Returns the regionName.
   */
  public String getRegionName() {
    return regionName;
  }


  
  /**
   * @return Returns the userConstraint.
   */
  public TRExpr getUserConstraint() {
    return userConstraint;
  }

  
  /**
   * @return Returns the regionTRoleModelInfo.
   */
  public RegionTRoleModel getRegionTRoleModelInfo() {
    return regionTRoleModelInfo;
  }

  
  /**
   * @param regionTRoleModelInfo The regionTRoleModelInfo to set.
   */
  public void setRegionTRoleModelInfo(RegionTRoleModel colorizedRegionInfo) {
    this.regionTRoleModelInfo = colorizedRegionInfo;
  }

  
}
