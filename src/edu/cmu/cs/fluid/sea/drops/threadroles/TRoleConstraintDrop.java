/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/colors/ColorConstraintDrop.java,v 1.3 2008/06/24 19:13:14 thallora Exp $*/
package edu.cmu.cs.fluid.sea.drops.threadroles;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.ThreadRoleConstraintNode;
import com.surelogic.aast.promise.ThreadRoleExprNode;
import com.surelogic.analysis.threadroles.TRExpr;
import com.surelogic.analysis.threadroles.TRoleMessages;
import com.surelogic.analysis.threadroles.TRolesFirstPass;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.xml.XML;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;

public class TRoleConstraintDrop extends PromiseDrop<ThreadRoleConstraintNode> implements PleaseFolderize {
  public TRoleConstraintDrop(ThreadRoleConstraintNode a) {
    super(a);
    rawExpr = buildTRExpr(a, a.getPromisedFor());
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
  
  public TRoleConstraintDrop(ThreadRoleConstraintNode n, String kind, IRNode locInIR) {
    super();
    rawExpr = buildTRExpr(n, locInIR);
    setMessage(kind + ' ' + rawExpr);
    
    XML e = XML.getDefault();
    if (e == null || e.processingXML()) {
      setFromSrc(false);
    } else { 
      setFromSrc(true);
    }
    this.setCategory(TRoleMessages.assuranceCategory);
    setNodeAndCompilationUnitDependency(locInIR);
    // build the dependency on the TCNDeclDrop placeholders for all the names in
    // this expression.
    final Set<String> referencedNames = new HashSet<String>(2);
    rawExpr.referencedColorNames(referencedNames);
    TRoleNameModel.makeTRoleNameModelDeps(referencedNames, this, locInIR);
  }
  
  public TRoleConstraintDrop(TRExpr exp, String kind, IRNode locInIR) {
    super();
    rawExpr = exp;
    setMessage(kind + ' ' + rawExpr);
    setFromSrc(false);
    this.setCategory(TRoleMessages.assuranceCategory);
    setNodeAndCompilationUnitDependency(locInIR);

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
  
  public TRExpr buildTRExpr(ThreadRoleConstraintNode n, IRNode where) {
    return n.getTheExprNode().getTheExpr().buildTRExpr(where);
  }

}
