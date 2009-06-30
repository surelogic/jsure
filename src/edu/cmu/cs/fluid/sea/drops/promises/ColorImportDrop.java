/*
 * Created on Sep 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.analysis.ColorFirstPass;
import edu.cmu.cs.fluid.java.analysis.ColorStaticCU;
import edu.cmu.cs.fluid.java.analysis.ColorStaticClass;
import edu.cmu.cs.fluid.java.analysis.ColorStaticMeth;
import edu.cmu.cs.fluid.java.analysis.ColorStaticStructure;
import edu.cmu.cs.fluid.java.analysis.ColorStaticWithChildren;
import edu.cmu.cs.fluid.java.analysis.ColorStructVisitor;
import edu.cmu.cs.fluid.java.bind.ColorPromises;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.DemandName;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.TypeRef;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * 
 * @author dfsuther
 * @region static StaticMap
 * @lock MapLock is this.class protects StaticMap
 */
@Deprecated
public class ColorImportDrop extends PromiseDrop {
  /**
   * @mapInto StaticMap
   * @aggregate Instance into StaticMap
   */
  private static final Map<IRNode, Set<IRNode>> importMap = 
    new HashMap<IRNode, Set<IRNode>>();

  protected static final Logger LOG = SLLogger.getLogger("ColorDropBuilding");

  private String importedUnit;

  private IRNode rawImportedUnit;

  private IRNode boundImportedUnit;

//  private ColorImportDrop(final IRNode node) {
//    dependUponCompilationUnitOf(node);
//    setMessage("@colorImport <NULL ARG>;");
//    importedUnit = null;
//    rawImportedUnit = null;
//    setCategory(JavaGlobals.THREAD_COLORING_CAT);
//  }

  public ColorImportDrop(final IRNode node, final IRNode rawImport) {
    setNodeAndCompilationUnitDependency(node);
//    dependUponCompilationUnitOf(node);
    rawImportedUnit = rawImport;
    importedUnit = null;
    setMessage("@colorImport " + rawImport + " (incomplete)");
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
    final Operator op = JJNode.tree.getOperator(rawImportedUnit);
    if (NamedType.prototype.includes(op)) {
      importedUnit = NamedType.getType(rawImportedUnit);
//      boundImportedUnit = binder.getBinding(rawImportedUnit);
//      importedUnit = JavaNames.getFullTypeName(boundImportedUnit);
      if (debuggingNames) {
        importedUnit = importedUnit + " (namedType)";
      }
    } else if (TypeRef.prototype.includes(op)) {
      importedUnit = TypeRef.getId(rawImportedUnit);
      if (debuggingNames) {
        importedUnit = importedUnit + " (TypeRef)";
      }
    } else if (DemandName.prototype.includes(op)) {
//      boundImportedUnit = binder.getBinding(rawImportedUnit);
//      importedUnit = JavaNames.getFullTypeName(boundImportedUnit);
      importedUnit = DemandName.getPkg(rawImportedUnit);
      if (debuggingNames) {
        importedUnit = importedUnit + " (DemandName)";
      }
    } else {
      LOG.severe("computeImports doesn't know what to do with this import "
          + op);
      importedUnit = "<arg not NamedType or DemandName>";
    }
    boundImportedUnit = binder.getBinding(rawImportedUnit);
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
  
  @Deprecated
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
      ColorPromises.setColorRenamesProcessed(nd, false);
      ColorPromises.setColorImportsProcessed(nd, false);
      super.visitClass(node);
    }

    @Override
    public void visitCU(ColorStaticCU node) {
      ColorPromises.setColorImportsProcessed(node.getNode(), false);
      super.visitCU(node);
    }

    @Override
    public void visitMeth(ColorStaticMeth node) {
      ColorPromises.setColorRenamesProcessed(node.getNode(), false);
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
  
  

  public IRNode getRawImportedUnit() {
    return rawImportedUnit;
  }

  
  /**
   * @return Returns the boundImportedUnit.
   */
  public IRNode getBoundImportedUnit() {
    return boundImportedUnit;
  }

}
