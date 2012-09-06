/*
 * Created on Oct 15, 2004
 *
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.surelogic.analysis.threadroles.Messages;
import com.surelogic.analysis.threadroles.TRExpr;
import com.surelogic.analysis.threadroles.TRolesFirstPass;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.xml.XML;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleNameModel;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleSummaryDrop;

/**
 * @author dfsuther
 *
 */
@SuppressWarnings("unchecked")
@Deprecated
public abstract class ColorExprDrop extends PromiseDrop {
  private final TRExpr rawExpr;
  private TRExpr renamedExpr = null;
  
  final boolean inherited;
  
  static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");
  
  private ColorExprDrop(String kind, TRExpr theExpr, boolean inherited) {
    super();
    rawExpr = theExpr;
    setMessage(kind + ' ' + rawExpr);
    this.inherited = inherited;
    XML e = XML.getDefault();
    if (e == null || e.processingXML()) {
      setFromSrc(false);
    } else { 
      setFromSrc(true);
    }
    this.setCategory(Messages.assuranceCategory);
  }
  
  protected ColorExprDrop(String kind, TRExpr theExpr, IRNode locInIR, boolean inherited) {
    this(kind, theExpr, inherited);
    setNodeAndCompilationUnitDependency(locInIR);
    // build the dependency on the TCNDeclDrop placeholders for all the names in
    // this expression.
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
  
}
