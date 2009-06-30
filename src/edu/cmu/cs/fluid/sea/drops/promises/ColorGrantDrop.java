/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.Collection;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.Drop;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
@Deprecated
public class ColorGrantDrop extends ColorNameListDrop {
  private static final String myKind = "grant";

//  public ColorGrantDrop(Collection declColors) {
//    super(declColors, myKind);
//  }
  
  public ColorGrantDrop(Collection<String> declColors, IRNode locInIR) {
    // note that super handles the ColorAnnoMap stuff for us...
    super(declColors, myKind, locInIR);
  }
  
  public Collection<String> getGrantedNames() {
    return super.getListedColors();
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof ColorSummaryDrop) {
      return;
    }
    // Any GrantDrop specific action would go here.
    // note that super... takes care of the ColorAnnoMap for us...
    super.deponentInvalidAction(invalidDeponent);
  }
  
}
