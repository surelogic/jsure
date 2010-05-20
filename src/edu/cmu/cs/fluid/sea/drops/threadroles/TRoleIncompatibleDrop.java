/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.threadroles;

import java.util.*;

import com.surelogic.aast.promise.ThreadRoleIncompatibleNode;
import com.surelogic.analysis.threadroles.*;

import SableJBDD.bdd.JBDD;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.Drop;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TRoleIncompatibleDrop extends TRoleNameListDrop<ThreadRoleIncompatibleNode> {
  private static final String myKind = "ThreadRolesIncompatible";
//  private List tcList = null;
  
  private JBDD conflictExpr = null;
  
  
  public TRoleIncompatibleDrop(ThreadRoleIncompatibleNode cin) {
    // note that super... takes care of the ColorAnnoMap for us...
    super(cin, myKind);
  }
  
   /**
   * @return Collection holding the String representation of the names that are
   * incompatible.
   */
  public Collection<String> getIncompatibleNames() {
    return getListedTRoles();
  }
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof TRoleSummaryDrop) {
      return;
    }
    // Any IncompatibleDrop specific action would go here.
    // note that super... takes care of the ColorAnnoMap for us...
    super.deponentInvalidAction(invalidDeponent);
    
  }
  
  
  /**
   * @return Returns the conflictExpr.
   */
  public JBDD getConflictExpr() {
    if (conflictExpr == null) {
      final IRNode cu = getDeclContext();
      Collection<String> incNamesColl = getIncompatibleNames();
      String[] incNames = new String[incNamesColl.size()];
      incNames = incNamesColl.toArray(incNames);
      conflictExpr = TRoleBDDPack.zero();
      
      for (int i=0; i<incNames.length; i++) {
        JBDD inner = TRoleBDDPack.one();
        for (int j=0; j<incNames.length; j++) {
          
          final String aName = incNames[j];
          final TRoleNameModel model = TRoleNameModel.getInstance(aName, cu);
          final TRoleName aTRole = model.getCanonicalTRole();
          
          if (i == j) {
            inner.andWith(aTRole.getSelfExpr());
          } else {
            inner.andWith(aTRole.getSelfExprNeg());
          }
        }
        conflictExpr.orWith(inner);
      }
      
      // don't forget the none-of-the-above case!
      JBDD inner = TRoleBDDPack.one();
      for (int i=0; i<incNames.length; i++) {
        
        
        final String aName = incNames[i];
        final TRoleNameModel model = TRoleNameModel.getInstance(aName, cu);
        final TRoleName aTRole = model.getCanonicalTRole();
        inner.andWith(aTRole.getSelfExprNeg());
      }
      conflictExpr.orWith(inner);
    }
    return conflictExpr;
  }
}
