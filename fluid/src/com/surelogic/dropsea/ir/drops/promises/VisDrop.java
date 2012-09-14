/*
 * Created on Oct 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.promises;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.promise.*;


public class VisDrop extends VisibilityDrop {

  private VisDrop(IRNode locInIR, IRNode modIR) {
    super(locInIR, modIR);
  }
  
  public static VisDrop buildVisDrop(IRNode locInIR, IRNode modIR) {
    VisDrop res = new VisDrop(locInIR, modIR);
    res = buildVisDrop(res, locInIR, modIR);
    return res;
  }
  
  public static Set<VisDrop> findVisDrop(IRNode promisedFor, IRNode modIR) {
    
    Set<VisDrop> res = new HashSet<VisDrop>(1);
    if (promisedFor == null || modIR == null) {
      return res;
    }
    
    final Set<VisibilityDrop> pfSet = findVisibilityDrops(promisedFor, modIR);
    if (pfSet == null) {
      return res;
    }
    for (VisibilityDrop edV : pfSet) {
      if (edV instanceof VisDrop) {
        VisDrop ed = (VisDrop) edV;
        if (ed.refdModule.equals(modIR)) {
          res.add(ed);
        }
      }
    }
    return res;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (image == null) {
      StringBuilder sb = new StringBuilder();
      sb.append("@Vis ");
      sb.append(API.getId(refdModule));
      image = sb.toString();
    }
    return image;
  }

  
}
