/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.threadroles;

import java.util.Collection;

import com.surelogic.aast.promise.ThreadRoleGrantNode;
import com.surelogic.dropsea.ir.Drop;



/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TRoleGrantDrop extends TRoleNameListDrop<ThreadRoleGrantNode>  {
  private static final String myKind = "TRoleGrant";

//  public ColorGrantDrop(Collection declColors) {
//    super(declColors, myKind);
//  }
  
  public TRoleGrantDrop(ThreadRoleGrantNode cgn) {
    // note that super handles the ColorAnnoMap stuff for us...
    super(cgn, myKind, false);
  }
  
  
  
  public Collection<String> getGrantedNames() {
    return super.getListedTRoles();
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
  
}
