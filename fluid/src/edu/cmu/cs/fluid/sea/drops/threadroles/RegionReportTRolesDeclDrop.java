/*
 * Created on Oct 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.threadroles;

import java.util.Collection;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.PhantomDrop;


public class RegionReportTRolesDeclDrop extends PhantomDrop implements IThreadRoleDrop {
  
  private final Collection<IRNode> forRegions;
  private String image = null;

  public RegionReportTRolesDeclDrop(Collection<IRNode> regions, IRNode locInIR) {
    super();
    
    forRegions = regions;
    setNodeAndCompilationUnitDependency(locInIR);
    setMessage(this.toString());
  }

  
  /**
   * @return Returns the forRegions.
   */
  public Collection<IRNode> getForRegions() {
    return forRegions;
  }


  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (image != null) return image;
    
    if ((forRegions == null) || forRegions.isEmpty()) {
      image = "thread role report (UnknownRegion)";
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append("RegionReportThreadRoles ");
      boolean first = true;
      for (IRNode reg : forRegions) {
        if (!first) {
          sb.append(", ");
        } else {
          first = true;
        }
        /*
        RegionModel regModel = RegionAnnotation.getRegionDrop(reg);
        sb.append(regModel.regionName);
        */
      }
      image = sb.toString();
    }
    return image;
  }
  
  

}
