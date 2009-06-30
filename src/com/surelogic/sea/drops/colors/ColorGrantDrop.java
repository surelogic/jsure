/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.sea.drops.colors;

import java.util.Collection;

import com.surelogic.aast.promise.ColorGrantNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ColorGrantDrop extends ColorNameListDrop<ColorGrantNode> implements PleaseFolderize {
  private static final String myKind = "ColorGrant";

//  public ColorGrantDrop(Collection declColors) {
//    super(declColors, myKind);
//  }
  
  public ColorGrantDrop(ColorGrantNode cgn) {
    // note that super handles the ColorAnnoMap stuff for us...
    super(cgn, myKind);
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
