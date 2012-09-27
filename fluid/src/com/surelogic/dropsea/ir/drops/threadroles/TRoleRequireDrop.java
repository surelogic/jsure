/*
 * Created on Oct 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.threadroles;


import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.promise.ThreadRoleNode;
import com.surelogic.analysis.threadroles.TRExpr;
import com.surelogic.analysis.threadroles.TRoleMessages;
import com.surelogic.analysis.threadroles.TRolesFirstPass;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.xml.XML;
import edu.cmu.cs.fluid.parse.JJNode;


/**
 * @author dfsuther
 */
public class TRoleRequireDrop extends PromiseDrop<ThreadRoleNode> 
implements IThreadRoleDrop {
  private static final String kind = "ThreadRoleConstraint";
  
  public TRoleRequireDrop(ThreadRoleNode a) {
    super(a);
    final IRNode locInIR = a.getPromisedFor();
    rawExpr = buildTRExpr(a, locInIR);
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("built ThreadRoleConstraint " + getRawExpr() + " for " + JJNode.getInfo(locInIR));
    }
    setMessage(12,"ThreadRoleConstraint " + getRawExpr());
  }
  
//  public TRoleRequireDrop(TRoleRequireDrop parentDrop, IRNode locInIR) {
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
    // All ThreadRoleRequired annos are always "good" by default.  Their _callers_
    // may not be, the the requirement is.
    return true;
  }
  
  private final TRExpr rawExpr;
  private TRExpr renamedExpr = null;
  
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
//    this.setCategory(TRoleMessages.assuranceCategory);
//  }
  
//  private ColorExprDrop(N n, String kind) {
//
//  }
  
  protected TRoleRequireDrop(ThreadRoleNode n, String kind, IRNode locInIR) {
    super(n);
    rawExpr = buildTRExpr(n, locInIR);
    setMessage(12,kind + ' ' + rawExpr);
    
    XML e = XML.getDefault();
    if (e == null || e.processingXML()) {
      setFromSrc(false);
    } else { 
      setFromSrc(true);
    }
    this.setCategorizingString(TRoleMessages.assuranceCategory);
   // setNodeAndCompilationUnitDependency(locInIR);
    // build the dependency on the TCNDeclDrop placeholders for all the names in
    // this expression.
    final Set<String> referencedNames = new HashSet<String>(2);
    rawExpr.referencedColorNames(referencedNames);
    TRoleNameModel.makeTRoleNameModelDeps(referencedNames, this, locInIR);
  }
  
  protected TRoleRequireDrop(TRExpr exp, String kind, IRNode locInIR) {
    super(null);
    rawExpr = exp;
    setMessage(12,kind + ' ' + rawExpr);
    setFromSrc(false);
    this.setCategorizingString(TRoleMessages.assuranceCategory);
 //   setNodeAndCompilationUnitDependency(locInIR);

    final Set<String> referencedNames = new HashSet<String>(2);
    rawExpr.referencedColorNames(referencedNames);
    TRoleNameModel.makeTRoleNameModelDeps(referencedNames, this, locInIR);
  }

  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof TRoleSummaryDrop) {
      return;
    }
    TRolesFirstPass.trackCUchanges(this);

    super.deponentInvalidAction(invalidDeponent);
  }

  /**
   * @return Returns the rawExpr.
   */
  public TRExpr getRawExpr() {
    return rawExpr;
  }

  /**
   * @return Returns the renamedExpr.
   */
  public TRExpr getRenamedExpr() {
    if (renamedExpr == null) return rawExpr;
    return renamedExpr;
  }

  /**
   * @param renamedExpr The renamedExpr to set.
   */
  public void setRenamedExpr(TRExpr renamedExpr) {
    this.renamedExpr = renamedExpr;
  }
  
  public static TRExpr buildTRExpr(ThreadRoleNode n, IRNode where) {
    return n.getTheExprNode().getTheExpr().buildTRExpr(where);
  }

}
