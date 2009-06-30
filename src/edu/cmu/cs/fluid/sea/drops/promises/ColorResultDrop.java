/*
 * Created on Mar 3, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import edu.cmu.cs.fluid.java.analysis.ColorMessages;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.drops.MaybeTopLevel;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;


/**
 * @author dfsuther
 */
@Deprecated
public class ColorResultDrop extends ResultDrop implements MaybeTopLevel, PleaseFolderize {
  /** (non-Javadoc)
   * Color Problems would like to be top level.  Others not.
   * @see edu.cmu.cs.fluid.sea.drops.MaybeTopLevel#requestTopLevel()
   */
  public boolean requestTopLevel() {
    if (getCategory() == ColorMessages.problemCategory) {
      return true;
    } else {
      return false;
    }
  }

}
