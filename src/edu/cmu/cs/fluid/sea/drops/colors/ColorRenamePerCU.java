/*
 * Created on Jul 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.colors;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.colors.*;
import com.surelogic.common.logging.SLLogger;

import SableJBDD.bdd.JBDD;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PhantomDrop;

public class ColorRenamePerCU extends PhantomDrop {

 public static int lastLoopCheck = 0;
  
  public List<ColorRenameDrop> chainRule = null;

  private Map<String, ColorRenameDrop> renameFromString = null;
  private Set<ColorRenameDrop> currRenames = new HashSet<ColorRenameDrop>();
  private Map<String, String> simpleRenames = null;
  private Map<JBDD, ColorRenameDrop> renameFromJBDD = null;
  
  private static boolean emptyOrNull(String str) {
    return (str == null) || (str.equals(""));
  }
  
  public String getSimpleRename(final String origName) {
    if (simpleRenames == null) {
      simpleRenames = new HashMap<String, String>(); 
    }
    return simpleRenames.get(origName);
  }
  
  public ColorRenameDrop getThisRenameFromString(final String theName) {
    if (renameFromString == null) return null;
    return renameFromString.get(theName);
  }
  
  public void addRename(final ColorRenameDrop rename) {
    if (renameFromString == null) {
      renameFromString = new HashMap<String, ColorRenameDrop>();
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
  
  public void addRenames(final Collection<ColorRenameDrop> renames) {
    for (ColorRenameDrop rename: renames) {
      addRename(rename);
    }
  }
  
  public Set getRFSKeySet() {
    if (renameFromString == null) return Collections.emptySet();
    
    return renameFromString.keySet();
  }
  public void removeRename(final ColorRenameDrop rename) {
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
   * Get the ColorRenamePerCU stored away for cu, if any. Creates the map if it
   * is null.
   * 
   * @param cu
   * @return
   */
  private static synchronized ColorRenamePerCU getPerCU(final IRNode cu) {
    if (nodeToPerCU == null) {
      nodeToPerCU = new HashMap<IRNode, ColorRenamePerCU>(0);
    }

    return nodeToPerCU.get(cu);
  }
  
  public static ColorRenamePerCU currentPerCU = null;
  
  public static Object startACU(final IRNode cu) {
    final Object res = currentPerCU;
    ColorRenamePerCU tPerCu = getPerCU(cu);
    if (tPerCu != null) {
      currentPerCU = tPerCu;
    }
    return res;
  }
  
  public static void endACU(final Object cookie) {
    if (cookie instanceof ColorRenamePerCU || cookie == null) {
      currentPerCU = (ColorRenamePerCU) cookie;
    } else {
      LOG.severe("Bad cookie presented to ColorRenamePerCU.endACU!");
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
      final ColorRenamePerCU val) {
    if (cu == null) return;

    if (val == null) {
      if (nodeToPerCU != null) nodeToPerCU.remove(cu);
    } else {
      if (nodeToPerCU == null)
        nodeToPerCU = new HashMap<IRNode, ColorRenamePerCU>();

      nodeToPerCU.put(cu, val);
    }
  }

  final IRNode cu;

  private static Map<IRNode, ColorRenamePerCU> nodeToPerCU = null;

  private ColorRenamePerCU(IRNode cu) {
    this.cu = cu;
  }

  private ColorRenamePerCU() {
    cu = null;
  }

  public static ColorRenamePerCU getColorRenamePerCU(final IRNode node) {
//    Operator op = JJNode.tree.getOperator(node);
    IRNode cu;
//    if (CompilationUnit.prototype.includes(op)) {
//      cu = node;
//    } else {
//      cu = VisitUtil.getEnclosingCompilationUnit(node);
//    }
    cu = VisitUtil.computeOutermostEnclosingTypeOrCU(node);
    if (cu == null) {
      LOG.log(Level.SEVERE, "[edu.emu.cs.fluid.sea.Drop] "
          + " unable to find enclosing compilation unit for "
          + DebugUnparser.toString(node));
      return null;
    }

    ColorRenamePerCU res = getPerCU(cu);
    if (res == null) {
      res = new ColorRenamePerCU(cu);
      setPerCU(cu, res);
    }
   
    res.setMessage("Another colorRenamePerCU...");
    return res;
  }

  private void computeRenameExprs() {
    if (!haveSomeRenames()) return;
    
    if (renameFromJBDD != null) return;
    
    renameFromJBDD = new HashMap<JBDD, ColorRenameDrop>();
    for (ColorRenameDrop ren : currRenames) {
      renameFromJBDD.put(ren.getFullExpr(), ren);
    }
  }
  
  public static String jbddMessageName(JBDD expr) {
    return jbddMessageName(expr, false);

  }
  
  public static String jbddMessageName(JBDD expr, boolean wantQualName) {
    if (currentPerCU == null) {
      return ColorBDDPack.userStr(expr);
    }
    
    if (!currentPerCU.haveSomeRenames()) {
      return ColorBDDPack.userStr(expr);
    }
    
    currentPerCU.computeRenameExprs();
    
    ColorRenameDrop ren = currentPerCU.renameFromJBDD.get(expr);
    if (ren != null) {
      if (wantQualName) {
        return ren.qualName;
      } else {
        return ren.simpleName;
      }
    } else {
      return ColorBDDPack.userStr(expr);
    }
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof ColorSummaryDrop) {
      return;
    }
    setPerCU(cu, null);
    ColorFirstPass.trackCUchanges(this);
    super.deponentInvalidAction(invalidDeponent);
  }

  /**
   * @return Returns the currRenames.
   */
  public Set<ColorRenameDrop> getCurrRenames() {
    return currRenames;
  }

  
  /**
   * @return Returns the cu.
   */
  public IRNode getCu() {
    return cu;
  }

}
