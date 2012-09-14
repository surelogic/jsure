/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.promises.threadroles;

import java.util.Collection;

import com.surelogic.aast.promise.ThreadRoleRevokeNode;
import com.surelogic.dropsea.ir.Drop;



/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TRoleRevokeDrop extends TRoleNameListDrop<ThreadRoleRevokeNode> {
  private static final String myKind = "TRoleRevoke";
//  public TRoleRevokeDrop(Collection names) {
//    super(names, myKind);
//  }
  
  public TRoleRevokeDrop(ThreadRoleRevokeNode trrn) {
    // note that super... takes care of the ColorAnnoMap for us...  
    super(trrn, myKind, false);
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof TRoleSummaryDrop) {
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
    return getListedTRoles();
  }
}
