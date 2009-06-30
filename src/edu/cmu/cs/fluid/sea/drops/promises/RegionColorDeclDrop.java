/*
 * Created on Oct 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.CExpr;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.sea.PhantomDrop;

/**
 * Phantom drop for "region" promise declarations. The RegionModel is used as
 * the actual promise drop. This drop is used to maintain compilation unit
 * dependencies.
 * 
 * @see edu.cmu.cs.fluid.sea.drops.promises.RegionModel
 * @see edu.cmu.cs.fluid.java.analysis.Region
 * @see edu.cmu.cs.fluid.java.bind.RegionAnnotation
 * @see edu.cmu.cs.fluid.sea.drops.promises.ColorRegionModel
 */
@Deprecated
public class RegionColorDeclDrop extends PhantomDrop {

  private final CExpr userConstraint;

  private CExpr renamedConstraint = null;

  private final RegionModel masterRegion;

  private final String regionName;
  
  private ColorizedRegionModel colorizedRegionInfo = null;

  
  /** Create the promise drop for a RegionColorConstraint declaration.
   * @param regionName Name of the region we are constraining
   * @param constraint Expression representing the constraint (as written by the user!)
   * @param where Location of the decl in the tree.
   */
  private RegionColorDeclDrop(final String regionName, final CExpr constraint) {
    super();
    this.regionName = regionName;
    masterRegion = RegionModel.getInstance(regionName);
    userConstraint = constraint.doClone();
  }

public static RegionColorDeclDrop buildRegionColorDecl(final String regionName, final CExpr constraint,
    final IRNode where) {
  RegionColorDeclDrop res = new RegionColorDeclDrop(regionName, constraint);
  res.setNodeAndCompilationUnitDependency(where);
  
  ColorizedRegionModel tCRDrop = (ColorizedRegionModel) res.masterRegion.getColorInfo();
  if (tCRDrop == null) {
    
    final IRNode vdecl = BindUtil.findRegionInType(where, regionName);
    
    tCRDrop = ColorizedRegionModel.getColorizedRegionModel(res.masterRegion, vdecl);
    res.masterRegion.setColorInfo(tCRDrop);
  } 
  res.colorizedRegionInfo = tCRDrop;
  

  res.addDependent(res.colorizedRegionInfo);
  
  res.masterRegion.addDependent(res);
  
  res.setMessage("colorConstraint " +constraint+ " for region " +res.masterRegion.regionName);
  
  return res;
}
  
  /**
   * @return Returns the renamedConstraint.
   */
  public CExpr getRenamedConstraint() {
    return renamedConstraint;
  }


  
  /**
   * @param renamedConstraint The renamedConstraint to set.
   */
  public void setRenamedConstraint(CExpr renamedConstraint) {
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
  public CExpr getUserConstraint() {
    return userConstraint;
  }

  
  /**
   * @return Returns the colorizedRegionInfo.
   */
  public ColorizedRegionModel getColorizedRegionInfo() {
    return colorizedRegionInfo;
  }

  
  /**
   * @param colorizedRegionInfo The colorizedRegionInfo to set.
   */
  public void setColorizedRegionInfo(ColorizedRegionModel colorizedRegionInfo) {
    this.colorizedRegionInfo = colorizedRegionInfo;
  }

  
}
