/*
 * Created on Mar 3, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.threadroles;


import com.surelogic.analysis.threadroles.TRoleMessages;

import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.drops.MaybeTopLevel;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;


/**
 * @author dfsuther
 */
public class TRoleResultDrop extends ResultDrop 
implements MaybeTopLevel, PleaseFolderize, IThreadRoleDrop {
  public TRoleResultDrop(String resultDropKind) {
    super(resultDropKind);
  }
  
  /** (non-Javadoc)
   * ColorErrors want to show up at the top level of the result view.  
   * Other ColorResults should follow their default behavior.
   * @see edu.cmu.cs.fluid.sea.drops.MaybeTopLevel#requestTopLevel()
   */
  public boolean requestTopLevel() {
    if (getCategory() == TRoleMessages.problemCategory) {
        return true;
      } else {
        return false;
      }
  }
}
