/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/colors/ColorConstraintDrop.java,v 1.3 2008/06/24 19:13:14 thallora Exp $*/
package edu.cmu.cs.fluid.sea.drops.colors;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.ColorConstraintNode;
import com.surelogic.aast.promise.ColorExprNode;
import com.surelogic.analysis.colors.CExpr;
import com.surelogic.analysis.colors.ColorFirstPass;
import com.surelogic.analysis.colors.ColorMessages;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.xml.XML;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;

public class ColorConstraintDrop extends PromiseDrop<ColorConstraintNode> implements PleaseFolderize {
  public ColorConstraintDrop(ColorConstraintNode a) {
    super(a);
    rawExpr = buildCExpr(a, a.getPromisedFor());
  }
  
  private final CExpr rawExpr;
  private CExpr renamedExpr = null;
  
//  private final boolean inherited;
  
  static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");
  
//  private ColorExprDrop(String kind, CExpr theExpr, boolean inherited) {
//    super();
//    rawExpr = theExpr;
//    setMessage(kind + ' ' + rawExpr);
//    this.inherited = inherited;
//    XML e = XML.getDefault();
//    if (e == null || e.processingXML()) {
//      setFromSrc(false);
//    } else { 
//      setFromSrc(true);
//    }
//    this.setCategory(ColorMessages.assuranceCategory);
//  }
  
//  private ColorExprDrop(N n, String kind) {
//
//  }
  
  public ColorConstraintDrop(ColorConstraintNode n, String kind, IRNode locInIR) {
    super();
    rawExpr = buildCExpr(n, locInIR);
    setMessage(kind + ' ' + rawExpr);
    
    XML e = XML.getDefault();
    if (e == null || e.processingXML()) {
      setFromSrc(false);
    } else { 
      setFromSrc(true);
    }
    this.setCategory(ColorMessages.assuranceCategory);
    setNodeAndCompilationUnitDependency(locInIR);
    // build the dependency on the TCNDeclDrop placeholders for all the names in
    // this expression.
    final Set<String> referencedNames = new HashSet<String>(2);
    rawExpr.referencedColorNames(referencedNames);
    ColorNameModel.makeColorNameModelDeps(referencedNames, this, locInIR);
  }
  
  public ColorConstraintDrop(CExpr exp, String kind, IRNode locInIR) {
    super();
    rawExpr = exp;
    setMessage(kind + ' ' + rawExpr);
    setFromSrc(false);
    this.setCategory(ColorMessages.assuranceCategory);
    setNodeAndCompilationUnitDependency(locInIR);

    final Set<String> referencedNames = new HashSet<String>(2);
    rawExpr.referencedColorNames(referencedNames);
    ColorNameModel.makeColorNameModelDeps(referencedNames, this, locInIR);
  }

  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof ColorSummaryDrop) {
      return;
    }
    ColorFirstPass.trackCUchanges(this);

    super.deponentInvalidAction(invalidDeponent);
  }

  /**
   * @return Returns the rawExpr.
   */
  public CExpr getRawExpr() {
    return rawExpr;
  }

  /**
   * @return Returns the renamedExpr.
   */
  public CExpr getRenamedExpr() {
    if (renamedExpr == null) return rawExpr;
    return renamedExpr;
  }

  /**
   * @param renamedExpr The renamedExpr to set.
   */
  public void setRenamedExpr(CExpr renamedExpr) {
    this.renamedExpr = renamedExpr;
  }
  
  public CExpr buildCExpr(ColorConstraintNode n, IRNode where) {
    return n.getTheExprNode().getTheExpr().buildCExpr(where);
  }

}
