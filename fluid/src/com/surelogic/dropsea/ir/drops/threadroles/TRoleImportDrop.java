/*
 * Created on Sep 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.dropsea.ir.drops.threadroles;

import static com.surelogic.annotation.rules.TRolePromisesSupport.setTRoleImportsProcessed;
import static com.surelogic.annotation.rules.TRolePromisesSupport.setTRoleRenamesProcessed;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.aast.promise.ThreadRoleImportNode;
import com.surelogic.analysis.threadroles.*;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * 
 * @author dfsuther
 * @region static StaticMap
 * @lock MapLock is this.class protects StaticMap
 */
public class TRoleImportDrop extends PromiseDrop<ThreadRoleImportNode> 
implements IThreadRoleDrop {
  /**
   * @mapInto StaticMap
   * @aggregate Instance into StaticMap
   */
  private static final Map<IRNode, Set<IRNode>> importMap = 
    new HashMap<IRNode, Set<IRNode>>();

  protected static final Logger LOG = SLLogger.getLogger("TRoleDropBuilding");

  private String importedUnit;

  private IRNode boundImportedUnit;

  public TRoleImportDrop(ThreadRoleImportNode n) {
    super(n);
    importedUnit = null;
   // setNodeAndCompilationUnitDependency(n.getPromisedFor());
    setMessage(12,"@ThreadRoleImport " + getAAST().getId() + " (incomplete)");
    setCategorizingMessage(JavaGlobals.THREAD_ROLES_CAT);
  }

  public void computeImports() {
    // Look at the rawImportedUnit and compute what's really imported here.
    computeImportName();
  }

  /**
   * @param binder
   */
  public void computeImportName() {
    final boolean debuggingNames = false;
    importedUnit = getAAST().getId();
   
    boundImportedUnit = getAAST().getTRoleImport();
    if (boundImportedUnit == null) {
      // report an error on the @colorImport drop.
      ResultDrop error = new ResultDrop(this.getNode());
      error.addChecked(this);
      error.setInconsistent();

      error.setMessage("no binding found for \"" + importedUnit + '"');
      //error.setNodeAndCompilationUnitDependency(this.getNode());
      error.setCategorizingMessage(JavaGlobals.THREAD_ROLES_CAT);
    } else {
      // 
//      this.dependUponCompilationUnitOf(boundImportedUnit);
      synchronized (TRoleImportDrop.class) {
        final IRNode impCU = VisitUtil.getEnclosingCUorHere(boundImportedUnit);
        Set<IRNode> importersOfImportedUnit = importMap.get(impCU);
        if (importersOfImportedUnit == null) {
          importersOfImportedUnit = new HashSet<IRNode>();
          importMap.put(impCU, importersOfImportedUnit);
        }
        importersOfImportedUnit.add(VisitUtil.getEnclosingCUorHere(getNode()));
      }
    }
    setMessage(12,"@ThreadRoleImport " + importedUnit);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof TRoleSummaryDrop) {
      return;
    }
    synchronized (TRoleImportDrop.class) {
      Set<IRNode> importersOfImportedUnit = importMap.get(VisitUtil.getEnclosingCUorHere(boundImportedUnit));
      if (importersOfImportedUnit != null) {
    	  importersOfImportedUnit.remove(getNode());
      }
    }
//    TRolesFirstPass.trackCUchanges(this);
    
  }
  
  static class ImportAndRenameReset extends TRoleStructVisitor {

    @Override
    public void visit(TRoleStaticStructure node) {
      if (node instanceof TRoleStaticWithChildren) {
        doAcceptForChildren((TRoleStaticWithChildren) node);
      }
    }

    @Override
    public void visitClass(TRoleStaticClass node) {
      final IRNode nd = node.getNode();
      setTRoleRenamesProcessed(nd, false);
      setTRoleImportsProcessed(nd, false);
      super.visitClass(node);
    }

    @Override
    public void visitCU(TRoleStaticCU node) {
      setTRoleImportsProcessed(node.getNode(), false);
      super.visitCU(node);
    }

    @Override
    public void visitMeth(TRoleStaticMeth node) {
      setTRoleRenamesProcessed(node.getNode(), false);
      super.visitMeth(node);
    }
   
  }

  private static ImportAndRenameReset resetWalker = new ImportAndRenameReset();
  
  public static void reprocessImporters(final IRNode node) {
    final IRNode cu = VisitUtil.getEnclosingCUorHere(node);
    Set<IRNode> importersOfNode;
    Set<IRNode> workingImportersOfNode;
    synchronized (TRoleImportDrop.class) {
      importersOfNode = importMap.get(cu);
      if (importersOfNode != null) {
    	  workingImportersOfNode = new HashSet<IRNode>(importersOfNode.size());
    	  workingImportersOfNode.addAll(importersOfNode);
    	  importersOfNode.clear();
      } else {
    	  workingImportersOfNode = Collections.emptySet();
      }
    }
    
    for (IRNode nd : workingImportersOfNode) {
      CUDrop cud = TRolesFirstPass.getCUDropOf(nd);
      final TRoleRenamePerCU perCU = TRoleRenamePerCU.getTRoleRenamePerCU(nd);
      perCU.invalidate();
      TRolesFirstPass.trackCUchanges(cud);
      TRoleStaticStructure resetMe = TRoleStaticCU.getStaticCU(cu);
      resetMe.accept(resetWalker);
     
    }
    
  }
  
  /**
   * @return Returns the importedUnits.
   */
  public String getImportedUnit() {
    return importedUnit;
  }
  
  

 
  
  /**
   * @return Returns the boundImportedUnit.
   */
  public IRNode getBoundImportedUnit() {
    return boundImportedUnit;
  }

}
