/*
 * Created on Jul 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.promises.threadroles;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import SableJBDD.bdd.JBDD;

import com.surelogic.RequiresLock;
import com.surelogic.analysis.threadroles.TRoleBDDPack;
import com.surelogic.analysis.threadroles.TRolesFirstPass;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.IRReferenceDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public class TRoleRenamePerCU extends IRReferenceDrop implements IThreadRoleDrop {

 public static int lastLoopCheck = 0;
  
  public List<TRoleRenameDrop> chainRule = null;

  private Map<String, TRoleRenameDrop> renameFromString = null;
  private Set<TRoleRenameDrop> currRenames = new HashSet<TRoleRenameDrop>();
  private Map<String, String> simpleRenames = null;
  private Map<JBDD, TRoleRenameDrop> renameFromJBDD = null;
  
  private static boolean emptyOrNull(String str) {
    return (str == null) || (str.equals(""));
  }
  
  public String getSimpleRename(final String origName) {
    if (simpleRenames == null) {
      simpleRenames = new HashMap<String, String>(); 
    }
    return simpleRenames.get(origName);
  }
  
  public TRoleRenameDrop getThisRenameFromString(final String theName) {
    if (renameFromString == null) return null;
    return renameFromString.get(theName);
  }
  
  public void addRename(final TRoleRenameDrop rename) {
    if (renameFromString == null) {
      renameFromString = new HashMap<String, TRoleRenameDrop>();
    }
    
    if (simpleRenames == null) {
      simpleRenames = new HashMap<String, String>(); 
    }
    
    currRenames.add(rename);
    if (!emptyOrNull(rename.simpleName)) {
      renameFromString.put(rename.simpleName, rename);
    }
    
    if (rename.isSimpleRename()) {
      simpleRenames.put(rename.simpleName, rename.getRawExpr().toString());
    }
    
  }
  
  public void addRenames(final Collection<TRoleRenameDrop> renames) {
    for (TRoleRenameDrop rename: renames) {
      addRename(rename);
    }
  }
  
  public Set getRFSKeySet() {
    if (renameFromString == null) return Collections.emptySet();
    
    return renameFromString.keySet();
  }
  public void removeRename(final TRoleRenameDrop rename) {
    if (rename == null) return;
    
    if (renameFromString != null && !emptyOrNull(rename.simpleName)) {
      renameFromString.remove(rename.simpleName);
    }
    
    currRenames.remove(rename);
    
    if (rename.isSimpleRename() && simpleRenames != null) {
      simpleRenames.remove(rename.simpleName);
    }
    
    chainsChecked = false;
    
  }
  public boolean chainsChecked = false;
  
  public boolean haveSomeRenames() {
    return currRenames.size() > 0;
  }

  /**
   * Logger for this class
   */
  protected static final Logger LOG = SLLogger.getLogger("ColorLogger");

  /**
   * Get the TRoleRenamePerCU stored away for cu, if any. Creates the map if it
   * is null.
   * 
   * @param f_cu
   * @return
   */
  private static synchronized TRoleRenamePerCU getPerCU(final IRNode maybeCU) {
	final IRNode cu = canonicalCU(maybeCU);
    if (nodeToPerCU == null) {
      nodeToPerCU = new HashMap<IRNode, TRoleRenamePerCU>(0);
    }

    return nodeToPerCU.get(cu);
  }
  
  public static TRoleRenamePerCU currentPerCU = null;
  
  public static Object startACU(final IRNode cu) {
    final Object res = currentPerCU;
    TRoleRenamePerCU tPerCu = getPerCU(cu);
    if (tPerCu != null) {
      currentPerCU = tPerCu;
    }
    return res;
  }
  
  public static void endACU(final Object cookie) {
    if (cookie instanceof TRoleRenamePerCU || cookie == null) {
      currentPerCU = (TRoleRenamePerCU) cookie;
    } else {
      LOG.severe("Bad cookie presented to TRoleRenamePerCU.endACU!");
      currentPerCU = null;
    }
  }

  /**
   * Set the map so that the value for cu is val. Remove the map entry for cu if
   * val is null. Create the map if necessary. Does NOTHING if cu is null.
   * 
   * @param cu
   * @param val
   */
  private static synchronized void setPerCU(final IRNode cu,
      final TRoleRenamePerCU val) {
    if (cu == null) return;

    if (val == null) {
      if (nodeToPerCU != null) nodeToPerCU.remove(cu);
    } else {
      if (nodeToPerCU == null)
        nodeToPerCU = new HashMap<IRNode, TRoleRenamePerCU>();

      nodeToPerCU.put(cu, val);
    }
  }

  final IRNode cu;

  private static Map<IRNode, TRoleRenamePerCU> nodeToPerCU = null;

  private TRoleRenamePerCU(IRNode cu) {
    super(cu); // may blow up if null
    this.cu = cu;
  }

  private TRoleRenamePerCU() {
    super(null); // will blow up
    cu = null;
  }

  public static TRoleRenamePerCU getTRoleRenamePerCU(final IRNode node) {
//    Operator op = JJNode.tree.getOperator(node);
    final IRNode cu = canonicalCU(node);
    if (cu == null) {
    	return null;
    }

    TRoleRenamePerCU res = getPerCU(cu);
    if (res == null) {
      res = new TRoleRenamePerCU(cu);
      setPerCU(cu, res);
      res.setMessage(12,"Another colorRenamePerCU...");
    }
   
    return res;
  }

  /**
   * @param node
   * @return
   */
  private static IRNode canonicalCU(final IRNode node) {
	  IRNode cu;
	  cu = VisitUtil.computeOutermostEnclosingTypeOrCU(node);
	  if (cu == null) {
		  LOG.log(Level.SEVERE, "[edu.emu.cs.fluid.sea.Drop] "
				  + " unable to find enclosing compilation unit for "
				  + DebugUnparser.toString(node));
		  return null;
	  }
	  return cu;
  }

  private void computeRenameExprs() {
    if (!haveSomeRenames()) return;
    
    if (renameFromJBDD != null) return;
    
    renameFromJBDD = new HashMap<JBDD, TRoleRenameDrop>();
    for (TRoleRenameDrop ren : currRenames) {
      renameFromJBDD.put(ren.getFullExpr(), ren);
    }
  }
  
  public static String jbddMessageName(JBDD expr) {
    return jbddMessageName(expr, false);

  }
  
  public static String jbddMessageName(JBDD expr, boolean wantQualName) {
    if (currentPerCU == null) {
      return TRoleBDDPack.userStr(expr);
    }
    
    if (!currentPerCU.haveSomeRenames()) {
      return TRoleBDDPack.userStr(expr);
    }
    
    currentPerCU.computeRenameExprs();
    
    TRoleRenameDrop ren = currentPerCU.renameFromJBDD.get(expr);
    if (ren != null) {
      if (wantQualName) {
        return ren.qualName;
      } else {
        return ren.simpleName;
      }
    } else {
      return TRoleBDDPack.userStr(expr);
    }
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  @RequiresLock("SeaLock")
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof TRoleSummaryDrop) {
      return;
    }
    setPerCU(cu, null);
    TRolesFirstPass.trackCUchanges(this);
    super.deponentInvalidAction(invalidDeponent);
  }

  /**
   * @return Returns the currRenames.
   */
  public Set<TRoleRenameDrop> getCurrRenames() {
    return currRenames;
  }

  
  /**
   * @return Returns the cu.
   */
  public IRNode getCu() {
    return cu;
  }

}
