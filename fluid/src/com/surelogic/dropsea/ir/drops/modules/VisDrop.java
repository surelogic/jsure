/*
 * Created on Oct 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.modules;

import java.util.HashSet;
import java.util.Set;

import com.surelogic.aast.promise.ModuleAnnotationNode;
import com.surelogic.aast.promise.VisClauseNode;

import edu.cmu.cs.fluid.ir.IRNode;

public class VisDrop extends VisibilityDrop<VisClauseNode> {

  private VisDrop(VisClauseNode a) {
    super(a);
  }
  
  public static VisDrop buildVisDrop(VisClauseNode a) {
    VisDrop res = new VisDrop(a);
    res = VisibilityDrop.buildVisDrop(res);
    return res;
  }
  
  public static Set<VisDrop> findVisDrop(IRNode promisedFor, String modName) {
    
    Set<VisDrop> res = new HashSet<VisDrop>(1);
    if (promisedFor == null || modName == null) {
      return res;
    }
    
    final Set<VisibilityDrop<? extends ModuleAnnotationNode>> pfSet = 
      findVisibilityDrops(promisedFor, modName);
    if (pfSet == null) {
      return res;
    }
    for (VisibilityDrop<? extends ModuleAnnotationNode> edV : pfSet) {
      if (edV instanceof VisDrop) {
        VisDrop ed = (VisDrop) edV;
        if (ed.refdModule.equals(modName)) {
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
      sb.append(refdModule);
      image = sb.toString();
    }
    return image;
  }

  
}
