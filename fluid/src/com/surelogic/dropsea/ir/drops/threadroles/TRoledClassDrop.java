/*
 * Created on Oct 14, 2004
 *
 */
package com.surelogic.dropsea.ir.drops.threadroles;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.surelogic.RequiresLock;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.IRReferenceDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;


/**
 * @author Edwin
 *
 */
public class TRoledClassDrop extends IRReferenceDrop implements IThreadRoleDrop {
  Object file;
  
  @SuppressWarnings("unused")
  private static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");
  
  private static final Map<IRNode, TRoledClassDrop> allTRCDs = 
    new HashMap<IRNode,TRoledClassDrop>();
  
  private TRoledClassDrop(IRNode cu) {
    super(cu);
    allTRCDs.put(cu, this);
  }
  
  public static TRoledClassDrop getTRoleClassDrop(IRNode node) {
    IRNode cu;
    Operator op = JJNode.tree.getOperator(node);
    if (CompilationUnit.prototype.includes(op)) {
      cu = node;
    } else {
      cu = VisitUtil.getEnclosingCompilationUnit(node);
    }
    TRoledClassDrop res = allTRCDs.get(cu);
    if (res == null) {
      res = new TRoledClassDrop(cu);
      res.setMessage(12,"another TRoledClassDrop...");
    }
    return res;
  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  @RequiresLock("SeaLock")
  protected void deponentInvalidAction(Drop invalidDeponent) {
    super.deponentInvalidAction(invalidDeponent);
    allTRCDs.remove(file);
  }
  
  public Object getCompUnit() {
    return file;
  }
//  public void setCompUnit(ICompilationUnit cu) {
//    file = cu;
//  }
}
