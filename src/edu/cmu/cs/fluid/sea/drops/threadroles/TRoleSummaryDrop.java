/*
 * Created on Dec 8, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.threadroles;

import com.surelogic.analysis.threadroles.TRoleMessages;

import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.*;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TRoleSummaryDrop extends PromiseDrop implements PleaseFolderize, PleaseCount {
   int numIssues;
   
   public TRoleSummaryDrop(String msg) {
     setMessage(msg);
     setCategory(TRoleMessages.assuranceCategory);
   }
   
   public void setCount(int count) {
     numIssues = count;
   }
   
   public int count() {
     return numIssues;
   }
}
