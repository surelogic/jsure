/*
 * Created on Oct 14, 2004
 *
 */
package edu.cmu.cs.fluid.sea.drops.colors;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PhantomDrop;
import edu.cmu.cs.fluid.tree.Operator;


/**
 * @author Edwin
 *
 */
public class ColoredClassDrop extends PhantomDrop {
  Object file;
  
  @SuppressWarnings("unused")
  private static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");
  
  private static final Map<IRNode, ColoredClassDrop> allCCDs = 
    new HashMap<IRNode,ColoredClassDrop>();
  
  private ColoredClassDrop(IRNode cu) {
    allCCDs.put(cu, this);
  }
  
  public static ColoredClassDrop getColorClassDrop(IRNode node) {
    IRNode cu;
    Operator op = JJNode.tree.getOperator(node);
    if (CompilationUnit.prototype.includes(op)) {
      cu = node;
    } else {
      cu = VisitUtil.getEnclosingCompilationUnit(node);
    }
    ColoredClassDrop res = allCCDs.get(cu);
    if (res == null) {
      res = new ColoredClassDrop(cu);
      res.setMessage("another ColoredClassDrop...");
    }
    return res;
  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    super.deponentInvalidAction(invalidDeponent);
    allCCDs.remove(file);
  }
  
  public Object getCompUnit() {
    return file;
  }
//  public void setCompUnit(ICompilationUnit cu) {
//    file = cu;
//  }
}
