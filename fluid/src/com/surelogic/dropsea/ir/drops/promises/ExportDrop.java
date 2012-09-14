/*
 * Created on Oct 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.promises;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.promise.*;


public class ExportDrop extends VisibilityDrop {
    private static final Logger LOG = SLLogger.getLogger("edu.cmu.cs.fluid.Modules");


  private ExportDrop(IRNode locInIR, IRNode modIR) {
    super(locInIR, modIR);
  }
  
  public static ExportDrop buildExportDrop(IRNode locInIR, IRNode modIR) {
    if (modIR == null && LOG.isLoggable(Level.INFO)) {
      LOG.info("found an ExportDrop with null ModIR");
    }
    ExportDrop res = new ExportDrop(locInIR, modIR);
    res = buildVisDrop(res, locInIR, modIR);
    return res;
  }
  
  

 

  public static Set<ExportDrop> findExportDrop(IRNode promisedFor, IRNode modIR) {
    
    Set<ExportDrop> res = new HashSet<ExportDrop>(1);
    if (promisedFor == null || modIR == null) {
      return res;
    }
    
    final Set<VisibilityDrop> pfSet = findVisibilityDrops(promisedFor, modIR);
    if (pfSet == null) {
      return res;
    }
    for (VisibilityDrop edV : pfSet) {
      if (edV instanceof ExportDrop) {
        ExportDrop ed = (ExportDrop) edV;
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
      sb.append("@export ");
      sb.append(API.getId(refdModule));
      image = sb.toString();
    }
    return image;
  }

  
}
