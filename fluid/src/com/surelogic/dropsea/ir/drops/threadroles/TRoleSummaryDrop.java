/*
 * Created on Dec 8, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.threadroles;

import com.surelogic.MustInvokeOnOverride;
import com.surelogic.RequiresLock;
import com.surelogic.analysis.threadroles.TRoleMessages;
import com.surelogic.common.xml.XmlCreator;
import com.surelogic.dropsea.ir.PromiseDrop;

/**
 * @author dfsuther
 */
public class TRoleSummaryDrop extends PromiseDrop 
implements IThreadRoleDrop {
   int numIssues;
   
   public TRoleSummaryDrop(String msg) {
	 super(null);
	 setMessage(12,msg);
	 setCategorizingMessage(TRoleMessages.assuranceCategory);
   }
   
   public void setCount(int count) {
     numIssues = count;
   }
   
   public int count() {
     return numIssues;
   }
   
   @MustInvokeOnOverride
   @Override
   @RequiresLock("SeaLock")
   public void snapshotAttrs(XmlCreator.Builder s) {
 	  super.snapshotAttrs(s);
 	  s.addAttribute("num-count", (long) count());
   } 	  
}
