/*
 * Created on Dec 8, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.sea.drops.colors;

import com.surelogic.analysis.colors.ColorMessages;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.*;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ColorSummaryDrop extends PromiseDrop implements PleaseFolderize, PleaseCount {
   int numIssues;
   
   public ColorSummaryDrop(String msg) {
     setMessage(msg);
     setCategory(ColorMessages.assuranceCategory);
   }
   
   public void setCount(int count) {
     numIssues = count;
   }
   
   public int count() {
     return numIssues;
   }
}
