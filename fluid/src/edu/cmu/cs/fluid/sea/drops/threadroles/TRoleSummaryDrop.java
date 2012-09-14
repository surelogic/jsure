/*
 * Created on Dec 8, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.threadroles;

import com.surelogic.analysis.threadroles.TRoleMessages;
import com.surelogic.common.xml.XMLCreator;
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
     setCategory(TRoleMessages.assuranceCategory);
   }
   
   public void setCount(int count) {
     numIssues = count;
   }
   
   public int count() {
     return numIssues;
   }
   
   @Override
   public void snapshotAttrs(XMLCreator.Builder s) {
 	  super.snapshotAttrs(s);
 	  s.addAttribute("num-count", (long) count());
   } 	  
}
