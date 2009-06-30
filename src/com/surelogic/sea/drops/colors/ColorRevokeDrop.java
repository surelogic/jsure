/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.sea.drops.colors;

import java.util.Collection;

import com.surelogic.aast.promise.ColorRevokeNode;

import edu.cmu.cs.fluid.sea.Drop;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ColorRevokeDrop extends ColorNameListDrop<ColorRevokeNode> {
  private static final String myKind = "ColorRevoke";
//  public ColorRevokeDrop(Collection names) {
//    super(names, myKind);
//  }
  
  public ColorRevokeDrop(ColorRevokeNode crn) {
    // note that super... takes care of the ColorAnnoMap for us...  
    super(crn, myKind);
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
  
  /**
   * @return a Collection holding the String representation of the revoked color names.
   */
  public Collection<String> getRevokedNames() {
    return getListedColors();
  }
}
