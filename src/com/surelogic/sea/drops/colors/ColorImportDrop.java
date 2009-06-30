/*
 * Created on Sep 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.surelogic.sea.drops.colors;

import static com.surelogic.annotation.rules.ColorPromisesSupport.setColorImportsProcessed;
import static com.surelogic.annotation.rules.ColorPromisesSupport.setColorRenamesProcessed;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.aast.promise.ColorImportNode;
import com.surelogic.analysis.colors.*;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

/**
 * 
 * @author dfsuther
 * @region static StaticMap
 * @lock MapLock is this.class protects StaticMap
 */
public class ColorImportDrop extends PromiseDrop<ColorImportNode> {
  /**
   * @mapInto StaticMap
   * @aggregate Instance into StaticMap
   */
  private static final Map<IRNode, Set<IRNode>> importMap = 
    new HashMap<IRNode, Set<IRNode>>();

  protected static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");

  private String importedUnit;

//  private IRNode rawImportedUnit;

  private IRNode boundImportedUnit;

//  private ColorImportDrop(final IRNode node) {
//    dependUponCompilationUnitOf(node);
//    setMessage("@colorImport <NULL ARG>;");
//    importedUnit = null;
//    rawImportedUnit = null;
//    setCategory(JavaGlobals.THREAD_COLORING_CAT);
//  }

  public ColorImportDrop(ColorImportNode n) {
//    setNodeAndCompilationUnitDependency(node);
//    dependUponCompilationUnitOf(node);
//    rawImportedUnit = rawImport;
    super(n);
    importedUnit = null;
    setNodeAndCompilationUnitDependency(n.getPromisedFor());
    setMessage("@colorImport " + getAST().getId() + " (incomplete)");
    setCategory(JavaGlobals.THREAD_COLORING_CAT);
  }

  public void computeImports(IBinder binder) {
    // Look at the rawImportedUnit and compute what's really imported here.
    computeImportName(binder);
  }

  /**
   * @param binder
   */
  public void computeImportName(IBinder binder) {
    final boolean debuggingNames = false;
    importedUnit = getAST().getId();
   
    boundImportedUnit = getAST().getColorImport();
    if (boundImportedUnit == null) {
      // report an error on the @colorImport drop.
      ResultDrop error = new ResultDrop("COLORANALYSIS_BADIMPORT");
      error.addCheckedPromise(this);
      error.setInconsistent();

      error.setMessage("no binding found for \"" + importedUnit + '"');
      error.setNodeAndCompilationUnitDependency(this.getNode());
      error.setCategory(JavaGlobals.THREAD_COLORING_CAT);
    } else {
      // 
//      this.dependUponCompilationUnitOf(boundImportedUnit);
      synchronized (ColorImportDrop.class) {
        final IRNode impCU = VisitUtil.getEnclosingCUorHere(boundImportedUnit);
        Set<IRNode> importersOfImportedUnit = importMap.get(impCU);
        if (importersOfImportedUnit == null) {
          importersOfImportedUnit = new HashSet<IRNode>();
          importMap.put(impCU, importersOfImportedUnit);
        }
        importersOfImportedUnit.add(VisitUtil.getEnclosingCUorHere(getNode()));
      }
    }
    setMessage("@colorImport " + importedUnit);
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (invalidDeponent instanceof ColorSummaryDrop) {
      return;
    }
    synchronized (ColorImportDrop.class) {
      Set<IRNode> importersOfImportedUnit = importMap.get(VisitUtil.getEnclosingCUorHere(boundImportedUnit));
      importersOfImportedUnit.remove(getNode());
    }
//    ColorFirstPass.trackCUchanges(this);
    
  }
  
  static class ImportAndRenameReset extends ColorStructVisitor {

    @Override
    public void visit(ColorStaticStructure node) {
      if (node instanceof ColorStaticWithChildren) {
        doAcceptForChildren((ColorStaticWithChildren) node);
      }
    }

    @Override
    public void visitClass(ColorStaticClass node) {
      final IRNode nd = node.getNode();
      setColorRenamesProcessed(nd, false);
      setColorImportsProcessed(nd, false);
      super.visitClass(node);
    }

    @Override
    public void visitCU(ColorStaticCU node) {
      setColorImportsProcessed(node.getNode(), false);
      super.visitCU(node);
    }

    @Override
    public void visitMeth(ColorStaticMeth node) {
      setColorRenamesProcessed(node.getNode(), false);
      super.visitMeth(node);
    }
   
  }

  private static ImportAndRenameReset resetWalker = new ImportAndRenameReset();
  
  public static void reprocessImporters(final IRNode node) {
    final IRNode cu = VisitUtil.getEnclosingCUorHere(node);
    Set<IRNode> importersOfNode;
    Set<IRNode> workingImportersOfNode;
    synchronized (ColorImportDrop.class) {
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
      CUDrop cud = ColorFirstPass.getCUDropOf(nd);
      final ColorRenamePerCU perCU = ColorRenamePerCU.getColorRenamePerCU(nd);
      perCU.invalidate();
      ColorFirstPass.trackCUchanges(cud);
      ColorStaticStructure resetMe = ColorStaticCU.getStaticCU(cu);
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
