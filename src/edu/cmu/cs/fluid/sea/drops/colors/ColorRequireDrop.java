/*
 * Created on Oct 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.colors;


import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.promise.ColorNode;
import com.surelogic.analysis.colors.CExpr;
import com.surelogic.analysis.colors.ColorFirstPass;
import com.surelogic.analysis.colors.ColorMessages;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.xml.XML;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ColorRequireDrop extends PromiseDrop<ColorNode> implements PleaseFolderize {
  private static final String kind = "Color";
  
  public ColorRequireDrop(ColorNode a) {
    super(a);
    final IRNode locInIR = a.getPromisedFor();
    rawExpr = buildCExpr(a, locInIR);
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("built colorConstraint " + getRawExpr() + " for " + JJNode.getInfo(locInIR));
    }
    setMessage("colorConstraint " + getRawExpr());
  }
  
//  public ColorRequireDrop(ColorRequireDrop parentDrop, IRNode locInIR) {
//    super(parentDrop.getRawExpr(), kind, locInIR);
//    parentDrop.addDependent(this);
//    if (LOG.isLoggable(Level.FINER)) {
//      LOG.finer("built colorRequired " + getRawExpr() + " for " + JJNode.getInfo(locInIR));
//    }
//    setMessage("(inherited)" +parentDrop.getMessage());
//  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.PromiseDrop#isCheckedByAnalysis()
   */
  @Override
  public boolean isCheckedByAnalysis() {
    // All colorRequired annos are always "good" by default.  Their _callers_
    // may not be, the the requirement is.
    return true;
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
  
  protected ColorRequireDrop(ColorNode n, String kind, IRNode locInIR) {
    super(n);
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
  
  protected ColorRequireDrop(CExpr exp, String kind, IRNode locInIR) {
    super(null);
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
  
  public static CExpr buildCExpr(ColorNode n, IRNode where) {
    return n.getTheExprNode().getTheExpr().buildCExpr(where);
  }

}
