/*
 * Created on Oct 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.modules;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.promise.ExportNode;
import com.surelogic.aast.promise.ModuleAnnotationNode;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;

public class ExportDrop extends VisibilityDrop<ExportNode> {
    private static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.Modules");


  private ExportDrop(ExportNode a) {
    super(a);
  }
  
  public static ExportDrop buildExportDrop(ExportNode en) {
    if (en.getToModuleName() == null && LOG.isLoggable(Level.INFO)) {
      LOG.info("found an ExportDrop with null fromModName");
    }
    ExportDrop res = new ExportDrop(en);
    res = buildVisDrop(res);
    return res;
  }
  
  

 

  public static Set<ExportDrop> findExportDrop(IRNode promisedFor, String fromModName) {
    
    Set<ExportDrop> res = new HashSet<ExportDrop>(1);
    if (promisedFor == null || fromModName == null) {
      return res;
    }
    
    final Set<VisibilityDrop<? extends ModuleAnnotationNode>> pfSet = findVisibilityDrops(promisedFor, fromModName);
    if (pfSet == null) {
      return res;
    }
    for (VisibilityDrop<? extends ModuleAnnotationNode> edV : pfSet) {
      if (edV instanceof ExportDrop) {
        ExportDrop ed = (ExportDrop) edV;
        if (ed.refdModule.equals(fromModName)) {
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
      sb.append("@export ");
      sb.append(refdModule);
      image = sb.toString();
    }
    return image;
  }

  
}
