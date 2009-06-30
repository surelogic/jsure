/*
 * Created on Apr 15, 2003
 */
package com.surelogic.analysis.colors;

import static com.surelogic.annotation.rules.ColorPromisesSupport.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import SableJBDD.bdd.JBDD;

import com.surelogic.aast.promise.ColorNameListNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.ColorRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.sea.drops.callgraph.SimpleCallGraphDrop;
import com.surelogic.sea.drops.colors.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.analysis.InstanceInitVisitor;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.BinaryCUDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Iteratable;

/**
 * @author dfsuther
 * 
 */

public final class ColorFirstPass {

  private static int cuCount = 0;
  //----------------------------------------------------------------------
  // Helper methods
  //----------------------------------------------------------------------
  
  final private Collection<IRNode> compUnitsToVisit = new ArrayList<IRNode>();
  
  private static IRNode getBinding(final IRNode node) {
    return INSTANCE.binder.getBinding(node);
  }
  
  public static void preBuild() {
    ColorRules.colorDropsEnabled = true;
  }
  
  private static IRNode getExtension(final IRNode node) {
    if (node == null) return null;
    
    final Operator op = JJNode.tree.getOperator(node);
    if (ClassDeclaration.prototype.includes(op)) {
      return ClassDeclaration.getExtension(node);
    } else if (InterfaceDeclaration.prototype.includes(op)) {
      return InterfaceDeclaration.getExtensions(node);
    } else if (NestedClassDeclaration.prototype.includes(op)) {
      return NestedClassDeclaration.getExtension(node);
    } else if (NestedInterfaceDeclaration.prototype.includes(op)) {
      return NestedInterfaceDeclaration.getExtensions(node);
    }
    return null;
  }
  
  private static IRNode getImpls(final IRNode node) {
 if (node == null) return null;
    
    final Operator op = JJNode.tree.getOperator(node);
    
    if (ClassDeclaration.prototype.includes(op)) {
      return ClassDeclaration.getImpls(node);
    } else if (NestedClassDeclaration.prototype.includes(op)) {
      return NestedClassDeclaration.getImpls(node);
    } else if (EnumDeclaration.prototype.includes(op)) {
      return EnumDeclaration.getImpls(node);
    }
    return null;
  }

  public static CUDrop getCUDropOf(IRNode node) {
    if (node == null) return null;
    try {
      final IRNode cu = VisitUtil.getEnclosingCUorHere(node);
      if (cu == null) {
        LOG.log(Level.SEVERE,
                " unable to find enclosing compilation unit for "
                    + DebugUnparser.toString(node));
      } else {
        CUDrop cuDrop = CUDrop.queryCU(cu);
        if (cuDrop == null) {
          LOG.log(Level.WARNING, "Unable to find compilation unit drop for "
              + DebugUnparser.toString(node));
        }
        return cuDrop;
      }
    } catch (Throwable e) {
      LOG.log(Level.WARNING, "Unable to find compilation unit drop for "
          + DebugUnparser.toString(node));
    }
    return null;
  }
  
  private static IRNode jlsCU = null;
  public static void trackCUchanges(IRReferenceDrop iDrop) {
    
    final CUDrop cud = iDrop.getCUDeponent();
    trackCUchanges(cud);
   
  }
  
  public static void trackCUchanges(CUDrop cud) {
    if (cud == null || !cud.isValid()) {
      // no need to track CUs that are invalid. They'll get re-processed through
      // the normal double-checker process.
      return;
    }
    
    if (cud.cu == jlsCU) {
      LOG.severe("trying to invalidate something in Java.Lang.String!");
    }
    getInstance().compUnitsToVisit.add(cud.cu);
  }
  
  class ColorImportWalkerNew extends ColorStructVisitor {
    /** Process the imports for some BinaryCU (or some part thereof). Process as much
     * as is needed to make sure that imports are OK for the reference to node.
     * @param node A node referred to by some code we're processing import for. We
     * will do just enough processing to be sure that the ref to node can be used
     * for future processing.
     */
    private void doLibraryRefImportWalk(IRNode node) {
     final IRNode cu = VisitUtil.computeOutermostEnclosingTypeOrCU(node);
 //    final String cuName = JavaNames.getFullTypeName(cu);
      
      if (areColorImportsProcessed(cu)) {
        return;
      }
      
      final CUDrop cud = getCUDropOf(cu);
//      final String name = JavaNames.genMethodConstructorName(node);
      if (!(cud instanceof BinaryCUDrop)) return;
      
 
      final IRNode theCUsRoot = cud.cu;
      // next line has side effect of creating the ColorRenamePerCU if it didn't already exist.
      final ColorRenamePerCU theCUsCRpCU = ColorRenamePerCU.getColorRenamePerCU(theCUsRoot);
      final Object saveCookie = ColorRenamePerCU.startACU(theCUsRoot);
      
      try {
        if (!areColorImportsProcessed(theCUsRoot)) {
          setColorImportsProcessed(theCUsRoot, true);
          final ColorImportWalkerNew importWalkNew = new ColorImportWalkerNew();
          importWalkNew.visit(ColorStaticCU.getStaticCU(theCUsRoot));
        }
      } finally {
        ColorRenamePerCU.endACU(saveCookie);
      }
    }
    
    /**
     * @param parents Iteratable of the IRNodes of the REFERENCES to CUs that
     * are the current CU either extends or implements.  Note that we need to 
     * bind the references before calling doLibraryRefImportWalk!
     */
    private void doParentImportWalk(Iteratable<IRNode> parents) {
      
      for (IRNode n : parents) {
        IRNode theParent = ColorFirstPass.getBinding(n);
        doLibraryRefImportWalk(theParent);
      }
    }
    
    private DropPredicate colorDeclPred = new DropPredicate() {

      public boolean match(Drop d) {
        return d instanceof ColorDeclareDrop;
      }
    };
    
    private void processColorImports(final IRNode node, 
                                     final Collection<ColorImportDrop> imports,
                                     final Collection<ColorStaticCU> importsToRename) {
      if (areColorImportsProcessed(node)) return;
//      Collection<ColorImportDrop> imports = ColorRules.getColorImports(node);
      if (imports == null || imports.isEmpty()) return;

      Collection<ColorRenameDrop> importedRenameDrops = new HashSet<ColorRenameDrop>();
      Collection<ColorDeclareDrop> importedDeclareDrops = new HashSet<ColorDeclareDrop>();
      for (ColorImportDrop ciDrop : imports) {
        ciDrop.computeImports(binder);
        // make sure we go look for imports and/or renames in the place we're 
        // importing from.  This is our last chance to catch things referenced only
        // in colorImport annos!
        final IRNode boundImportedUnit = ciDrop.getBoundImportedUnit();
        final ColorStaticCU importedCU = ColorStaticCU.getStaticCU(VisitUtil.getEnclosingCompilationUnit(boundImportedUnit));
        importsToRename.add(importedCU);
        doLibraryRefImportWalk(boundImportedUnit);
        
        Collection<ColorRenameDrop> someImportedRenames = ColorRenameDrop
            .getRenamesHere(ciDrop.getImportedUnit());
        if (someImportedRenames != null)
          importedRenameDrops.addAll(someImportedRenames);
        Collection<ColorDeclareDrop> someImportedDecls = ColorDeclareDrop
            .getColorDeclsForCU(ciDrop.getImportedUnit());
        if (someImportedDecls != null)
          importedDeclareDrops.addAll(someImportedDecls);
      }

      ColorRenamePerCU localPerCU = ColorRenamePerCU.getColorRenamePerCU(node);
      localPerCU.addRenames(importedRenameDrops);

      CUDrop cuDrop = getCUDropOf(node);
//      Set<Drop> nameModels = new HashSet<Drop>();
//      Sea.addMatchingDropsFrom(cuDrop.getDependents(), colorDeclPred,
//                               nameModels);

      for (ColorDeclareDrop impCDD : importedDeclareDrops) {
        final Collection<String> impColNames = impCDD.getDeclaredColors();
        for (String impColName : impColNames) {
          String impColShortName = JavaNames.genSimpleName(impColName);
          ColorNameModel localCNM = ColorNameModel.getInstance(impColShortName,
                                                               node);
          ColorNameModel globalCNM = ColorNameModel.getInstance(impColName,
                                                                null);
          localCNM.setCanonicalTColor(globalCNM.getCanonicalTColor());
        }
      }
      
      

    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visit(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visit(ColorStaticStructure node) {
      if (node instanceof ColorStaticWithChildren) {
        doAcceptForChildren((ColorStaticWithChildren) node);
      }
    }

    
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitBlock(edu.cmu.cs.fluid.java.analysis.ColorStaticBlock)
     */
    @Override
    public void visitBlock(ColorStaticBlock node) {
      visitChildReferences(node);
      super.visitBlock(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitCall(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visitCall(ColorStaticCall node) {
      final IRNode mDecl = ColorFirstPass.getBinding(node.getNode());

      doLibraryRefImportWalk(mDecl);
      
//      doAcceptForChildren(node);
    }
    
 

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitClass(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visitClass(ColorStaticClass node) {
      final ColorStaticClass csClass = node;
      final IRNode nd = csClass.getNode();
      
      final String className = JavaNames.getFullTypeName(nd);
      
      processColorImports(nd, csClass.colorImports, csClass.colorImportsToRename);
      
      final IRNode ext = ColorFirstPass.getExtension(nd);
      if (ext != null) {
        final Operator extOp = JJNode.tree.getOperator(ext);
        if (Extensions.prototype.includes(extOp)) {
          for (IRNode ext1:Extensions.getSuperInterfaceIterator(ext)) {
            final IRNode boundExt = binder.getBinding(ext1);
            doLibraryRefImportWalk(boundExt);
          }
        } else {
          final IRNode boundExt = binder.getBinding(ext);
          doLibraryRefImportWalk(boundExt);
        }
      }
      
      final IRNode impl = ColorFirstPass.getImpls(nd);
      if (impl != null) {
        final Iteratable<IRNode> impls = Implements.getIntfIterator(impl);
        doParentImportWalk(impls);
      }

      setColorImportsProcessed(nd, true);
      
      
      doAcceptForChildren(csClass);
    }
    
    private void visitChildReferences(ColorStaticBlockish node) {
      for (ColorStaticRef ref: node.allRefs) {
        final Operator refOp = JJNode.tree.getOperator(ref.getNode());
        if (FieldRef.prototype.includes(refOp)) {
          final IRNode fieldRef = ref.getNode();
//          final IRNode obj = FieldRef.getObject(fieldRef);
          final IRNode field = getBinding(fieldRef);

          final IRegion fieldAsRegion = RegionModel.getInstance(field);

          final RegionModel rModel = fieldAsRegion.getModel();

          if (rModel.getColorInfo() != null) {
            doLibraryRefImportWalk(fieldRef);
          }
        }
      }
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitMeth(edu.cmu.cs.fluid.java.analysis.ColorStaticMeth)
     */
    @Override
    public void visitMeth(ColorStaticMeth node) {
      visitChildReferences(node);
      super.visitMeth(node);
    }

  }
  
  class ColorRenameWalkerNew extends ColorStructVisitor {
    
    /**
     * @param cnl
     */
    public <A extends ColorNameListNode> void doSimpleRenames(ColorNameListDrop<A> cnl) {
      List<String> newNames = new ArrayList<String>(cnl.getListedColors()
          .size());
      for (String name : cnl.getListedColors()) {
        final String newName = 
          ColorRenamePerCU.currentPerCU.getSimpleRename(name);
        if (newName != null) {
          newNames.add(newName);
          ColorRenamePerCU.currentPerCU.getThisRenameFromString(name)
              .addDependent(cnl);
        } else {
          newNames.add(name);
        }
      }
      cnl.setListedRenamedColors(newNames);
    }
    
    private CExpr renameACExpr(CExpr origExpr, Drop exprsDrop) {
      if (!ColorRenameDrop.currCuHasSomeRenames()) {
        return origExpr;
      }
      
      CExpr newExpr = origExpr;
      for (ColorRenameDrop rename : ColorRenameDrop.getChainRule()) {
        final Set<String> refdNames = newExpr.referencedColorNames();
        if (refdNames.contains(rename.simpleName)) {
          newExpr = newExpr.cloneWithRename(rename);
          rename.addDependent(exprsDrop);
        }
      }
      return newExpr;
    }

    public void doRenameReqDrops(IRNode node) {
      // see if the current method overrides something
      Iterator<IRNode> parentIter = binder.findOverriddenParentMethods(node);
      while (parentIter.hasNext()) {
        //recur our way up the inherit/implement hierarchy, processing renames
        // as needed.
        doLibraryMthRenameWalk(parentIter.next());
      }
      
      Collection<ColorRequireDrop> reqDrops = ColorRules.getReqDrops(node);
      for (ColorRequireDrop reqDrop : reqDrops) {
        if (ColorRenameDrop.currCuHasSomeRenames()) {
          CExpr newExpr = renameACExpr(reqDrop.getRawExpr(), reqDrop);
          reqDrop.setRenamedExpr(newExpr);
        } else {
          reqDrop.setRenamedExpr(reqDrop.getRawExpr());
        }
      }
    }

    
    public void doMethodLevelRenames(IRNode node) {

      final Operator op = JJNode.tree.getOperator(node);

      if (MethodDeclaration.prototype.includes(op) || 
          ConstructorDeclaration.prototype.includes(op)) {
        final ColorStaticMeth csMeth = ColorStaticMeth.getStaticMeth(node);
        visit(csMeth);
      }
    }
    
    private void doLibraryMthRenameWalk(IRNode node) {
      if (!ColorRenameDrop.globalHaveRenames() || 
          areColorRenamesProcessed(node)) {
        return;
      }
      
      final CUDrop cud = getCUDropOf(node);
      final String name = JavaNames.genMethodConstructorName(node);
      
      // see if the current method overrides something
      Iterator<IRNode> parentIter = binder.findOverriddenParentMethods(node);
      while (parentIter.hasNext()) {
        //recur our way up the inherit/implement hierarchy, processing renames
        // as needed.
        doLibraryMthRenameWalk(parentIter.next());
      }
      
      if (!(cud instanceof BinaryCUDrop)) return;
      
      final IRNode cu = VisitUtil.computeOutermostEnclosingTypeOrCU(node);
      final Object saveCookie = ColorRenamePerCU.startACU(cu);
      
      try {
        if (ColorRenameDrop.currCuHasSomeRenames()) {
          final ColorRenameWalkerNew renameWalk = new ColorRenameWalkerNew();
          renameWalk.doMethodLevelRenames(node);
        }
        setColorRenamesProcessed(node, true);
      } finally {
        ColorRenamePerCU.endACU(saveCookie);
      }
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visit(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visit(ColorStaticStructure node) {
      if (node instanceof ColorStaticWithChildren) {
        doAcceptForChildren((ColorStaticWithChildren) node);
      }
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitBlock(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visitBlock(ColorStaticBlock node) {
      final ColorStaticBlock block = node;
      
      if (block.grants != null) {
        // process the grants
        for (ColorNameListDrop cnl : block.grants) {
          doSimpleRenames(cnl);
        }
      }

  
      if (block.revokes != null) {
        for (ColorNameListDrop cnl : block.revokes) {
          doSimpleRenames(cnl);
        }
      }
      visitChildReferences(block);
      doAcceptForChildren(block);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitCall(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visitCall(ColorStaticCall node) {
      final IRNode nd = node.getNode();
      final IRNode mDecl = binder.getBinding(nd);
      
      doLibraryMthRenameWalk(mDecl);
 
      // Calls can't have children, because they can't change the color context!
    }
    
    private void visitChildReferences(ColorStaticBlockish node) {
      for (ColorStaticRef ref: node.allRefs) {
        final Operator refOp = JJNode.tree.getOperator(ref.getNode());
        if (FieldRef.prototype.includes(refOp)) {
          final IRNode fieldRef = ref.getNode();
//          final IRNode obj = FieldRef.getObject(fieldRef);
          final IRNode field = getBinding(fieldRef);

          final IRegion fieldAsRegion = RegionModel.getInstance(field);

          final RegionModel rModel = fieldAsRegion.getModel();

          if (rModel.getColorInfo() != null) {
            doLibraryRefRename(fieldRef);
//            doLibraryRefImportWalk(fieldRef);
          }
        }
      }
    }
    
    private void doLibraryRefRename(final IRNode fieldRef) {
      final IRNode encClass = VisitUtil.getEnclosingType(fieldRef);
      if (areColorRenamesProcessed(encClass)) {
	return;
      }
      final CUDrop cud = getCUDropOf(fieldRef);

      if (!(cud instanceof BinaryCUDrop)) return;

//      final ColorStaticClass hisClass = ColorStaticClass.getStaticClass(encClass);
      final IRNode hisCU = cud.cu;

      final Object saveCookie = ColorRenamePerCU.startACU(hisCU);
      try {
	Set<RegionColorDeclDrop> rcdDrops = 
	  ColorRules.getMutableRegionColorDeclsSet(encClass);
	for (RegionColorDeclDrop anRCD : rcdDrops) {
	  CExpr newExpr = renameACExpr(anRCD.getUserConstraint(), anRCD);
	  anRCD.setRenamedConstraint(newExpr);
    
    
	}

	setColorRenamesProcessed(encClass, true);
      }  finally {
	ColorRenamePerCU.endACU(saveCookie);
      }

      
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitClass(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visitClass(ColorStaticClass node) {
      final ColorStaticClass csc = node;
      final IRNode nd = csc.getNode();
      
      final String className = JavaNames.getFullTypeName(nd);
      
      // possible annos here are: ColorIncompatible, ColorDeclare, ColorRename.
      // ColorRenames were already processed while creating the drops
      // ColorDeclare doesn't get processed for renaming, because they are
      // declarations.
      // ColorIncompatible must be processed now.
      final Object saveCookie = ColorRenamePerCU.startACU(nd);
      Void res;
      try {
        for (ColorStaticCU impCU: csc.colorImportsToRename) {
          Collection<ColorIncompatibleDrop> colorIncs = impCU.colorIncompatibles;
          for (ColorIncompatibleDrop inc : colorIncs) {
            doSimpleRenames(inc);
          }
        }
        
        
//        final CUDrop cud = getCUDropOf(nd);
//
//        if (!(cud instanceof BinaryCUDrop)) {
//          LOG.severe("visiting " + className);
//        }
        Set<RegionColorDeclDrop> rcdDrops = 
          ColorRules.getMutableRegionColorDeclsSet(nd);
        for (RegionColorDeclDrop anRCD : rcdDrops) {
          CExpr newExpr = renameACExpr(anRCD.getUserConstraint(), anRCD);
          anRCD.setRenamedConstraint(newExpr);
        }
        
        setColorRenamesProcessed(nd, true);
        
        doAcceptForChildren(csc);
      } finally {
        ColorRenamePerCU.endACU(saveCookie);
      }
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitCU(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visitCU(ColorStaticCU node) {
//      final ColorStaticCU csCU = node;
      final IRNode nd = node.getNode();
      // make sure that the startACU call below is guaranteed to find at least an
      // empty PerCU. This way, there will be AT LEAST one perCU for each comp
      // unit (there could be more, if we have some for internal or toplevel classes).
      // Goal is to make sure that ColorRenamePerCU.currentPerCU is never null.
      ColorRenamePerCU.getColorRenamePerCU(nd);
      final Object saveCookie = ColorRenamePerCU.startACU(nd);

      try {
        for (ColorIncompatibleDrop inc : node.colorIncompatibles) {
          doSimpleRenames(inc);
        }
        
        doAcceptForChildren(node);
      } finally {
        ColorRenamePerCU.endACU(saveCookie);
      }
      
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitMeth(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visitMeth(ColorStaticMeth node) {
      final IRNode nd = node.getNode();
      final String mName = JavaNames.genMethodConstructorName(nd);
      doRenameReqDrops(nd);
      
      visitChildReferences(node);
      
      doAcceptForChildren(node);
    }
  }
  
  static class ColorStaticStructureDumper extends ColorStructVisitor {

    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visit(edu.cmu.cs.fluid.java.analysis.ColorStaticStructure)
     */
    @Override
    public void visit(ColorStaticStructure node) {
//      LOG.severe("Visiting " + node.getClass());
      if (node instanceof ColorStaticWithChildren) {
        doAcceptForChildren((ColorStaticWithChildren) node);
      }
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitBlock(edu.cmu.cs.fluid.java.analysis.ColorStaticBlock)
     */
    @Override
    public void visitBlock(ColorStaticBlock node) {
      visitInterestingRefs(node);
      super.visitBlock(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitCall(edu.cmu.cs.fluid.java.analysis.ColorStaticCall)
     */
    @Override
    public void visitCall(ColorStaticCall node) {
      final IRNode mDecl = ColorFirstPass.getBinding(node.getNode());
      final String mDeclName = JavaNames.genMethodConstructorName(mDecl);
//      LOG.severe("call to " + mDeclName);
      super.visitCall(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitClass(edu.cmu.cs.fluid.java.analysis.ColorStaticClass)
     */
    @Override
    public void visitClass(ColorStaticClass node) {
      final IRNode cl = node.getNode();
      final String clName = JavaNames.getFullTypeName(cl);
//      LOG.severe("Class " + clName);
      
      super.visitClass(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitCU(edu.cmu.cs.fluid.java.analysis.ColorStaticCU)
     */
    @Override
    public void visitCU(ColorStaticCU node) {
      
      super.visitCU(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitMeth(edu.cmu.cs.fluid.java.analysis.ColorStaticMeth)
     */
    @Override
    public void visitMeth(ColorStaticMeth node) {
      final IRNode mDecl = node.getNode();
      final String mDeclName = JavaNames.genMethodConstructorName(mDecl);
//      LOG.severe("Decl of " + mDeclName);
      visitInterestingRefs(node);
      super.visitMeth(node);
    }

    private void visitInterestingRefs(ColorStaticBlockish node) {
   //   LOG.severe(node.interestingRefs.size() + " interesting refs");
    }
    
  }
  class ColorStaticStructureBuilder extends Visitor<Void> {
    ColorStaticMeth currMethStruct = null;
    ColorStaticCU currCUStruct = null;
    ColorStaticClass currClassStruct = null;
    ColorStaticBlockish currBlockish = null;
    ColorStaticWithChildren currStruct = null;
    
    InstanceInitVisitor<Void> initHelper = null;
    
    
    
    /**
     * Build the structure for all class-like ops.
     * @param node The node we're processing.
     * @return Always null
     */
    private Void handleClassLikeOps(IRNode node) {
      final ColorStaticClass saveCurrClassStruct = currClassStruct;
      final ColorStaticWithChildren saveCurrStruct = currStruct;
      final String className = JavaNames.getFullTypeName(node);
      final Object saveCookie = ColorRenamePerCU.startACU(node);
      try {
        currClassStruct = new ColorStaticClass(node, currStruct);
        currStruct.addChild(currClassStruct);
        currStruct = currClassStruct;
        currClassStruct.colorImports.addAll(ColorRules.getColorImports(node));
        if (saveCurrClassStruct != null) {
          currClassStruct.colorImports.addAll(saveCurrClassStruct.colorImports);
        }
        currClassStruct.colorRenames.addAll(ColorRenamePerCU.getColorRenamePerCU(node).getCurrRenames());
        super.doAcceptForChildren(node);
      } finally {
        currClassStruct = saveCurrClassStruct;
        currStruct = saveCurrStruct;
        ColorRenamePerCU.endACU(saveCookie);
      }
      return null;
    }
    
    /**
     * @param node
     */
    private Void handleBlockLikeOps(IRNode node) {
      Set<ColorGrantDrop> grants = getMutableColorGrantSet(node);
      Set<ColorRevokeDrop> revokes = getMutableColorRevokeSet(node);
      if (!(grants.isEmpty() && revokes.isEmpty())) {
        final ColorStaticWithChildren saveCurrStruct = currStruct;
        final ColorStaticBlockish saveCurrBlockish = currBlockish;
        try {
          final ColorStaticBlock theBlock = 
            new ColorStaticBlock(node, currStruct, grants, revokes);
          currBlockish = theBlock;
          currStruct.addChild(theBlock);
          currStruct = theBlock;
          super.doAcceptForChildren(node);
        } finally {
          currStruct = saveCurrStruct;
          currBlockish = saveCurrBlockish;
        }
      } else {
        super.doAcceptForChildren(node);
      }
      return null;
    }
    
    private Void handleCallLikeOps(final IRNode node) {
//      final ColorStaticWithChildren saveCurrStruct = currStruct;
//      try {
        final ColorStaticCall theCall = new ColorStaticCall(node, currStruct);
        super.doAcceptForChildren(node);
//      } finally {
//        currStruct = saveCurrStruct;
//      }
      return null;
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visit(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visit(IRNode node) {
      super.doAcceptForChildren(node);
      return null;
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitAnonClassExpression(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitAnonClassExpression(IRNode node) {
      return handleClassLikeOps(node);
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitArrayRefExpression(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitArrayRefExpression(IRNode node) {
      if (ColorizedRegionModel.haveColorizedRegions()) {
        List<ColorizedRegionModel> crms = ColorTargets.getCRMsForArrayRef(node);
//          ColorTargets.filterColorizedTargets(node, ColorTargets.getTargetsForArrayRef(node));
        
          ColorStaticRef theRef = new ColorStaticRef(node, currBlockish, crms);
//          currStruct.addChild(theRef);
        
      }
      return super.visitArrayRefExpression(node);
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitBlockStatement(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitBlockStatement(IRNode node) {
      return handleBlockLikeOps(node);
    }

 

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitClassDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitClassDeclaration(IRNode node) {
      return handleClassLikeOps(node);
    }
 
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitClassInitializer(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitClassInitializer(IRNode node) {
      return null;
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitCompilationUnit(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitCompilationUnit(IRNode node) {
      try {
        currCUStruct = new ColorStaticCU(node, null);
        currStruct = currCUStruct;
        
        super.visitCompilationUnit(node);
        
        //don't forget to save the static structure hook somewhere!
     
      } finally {
        currCUStruct = null;
        currStruct = null;
      }
      return null;
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitConstructorCall(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitConstructorCall(IRNode node) {
      return handleCallLikeOps(node);
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitConstructorDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitConstructorDeclaration(IRNode node) {
      final ColorStaticMeth saveCurrMethStruct = currMethStruct;
      final ColorStaticWithChildren saveCurrStruct = currStruct;
      final ColorStaticBlockish saveCurrBlockish = currBlockish;
      
      final InstanceInitVisitor<Void> saveInitHelper = initHelper;
      final String name = JavaNames.genMethodConstructorName(node);
      Void res = null;
      try {
        final ColorStaticMeth theMeth = new ColorStaticMeth(node, currStruct);
        currStruct.addChild(theMeth);
        currStruct = theMeth;
        currMethStruct = theMeth;
        currBlockish = theMeth;
        
        initHelper = new InstanceInitVisitor<Void>(structureBuilder);
        // note that doVisitInstanceInits will only do the traversal when
        // appropriate, and will call back into this visitor to traverse the
        // inits themselves.
        initHelper.doVisitInstanceInits(node);

        res = super.visitConstructorDeclaration(node);
        
      } finally {
        currMethStruct = saveCurrMethStruct;
        currStruct = saveCurrStruct;
        initHelper = saveInitHelper;
        currBlockish = saveCurrBlockish;
      }
      return res;
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitEnumDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitEnumDeclaration(IRNode node) {
      return handleClassLikeOps(node);
    }
    
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitFieldDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitFieldDeclaration(IRNode node) {
      // Don't traverse inside field declarations except via the init helper!
      return null;
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitFieldRef(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitFieldRef(IRNode node) {
      if (ColorizedRegionModel.haveColorizedRegions()) {
        List<ColorizedRegionModel> crms = ColorTargets.getCRMsForFieldRef(node);
//          ColorTargets.filterColorizedTargets(node, ColorTargets.getTargetsForFieldRef(node));
        
          ColorStaticRef theRef = new ColorStaticRef(node, currBlockish, crms);
//          currStruct.addChild(theRef);
      }
      return super.visitFieldRef(node);
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitInitDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitInitDeclaration(IRNode node) {
      // TODO Auto-generated method stub
      return super.visitInitDeclaration(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitInitialization(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitInitialization(IRNode node) {
      // TODO Auto-generated method stub
      return super.visitInitialization(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitInitializer(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitInitializer(IRNode node) {
      // TODO Auto-generated method stub
      return super.visitInitializer(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitInterfaceDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitInterfaceDeclaration(IRNode node) {
      return handleClassLikeOps(node);
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitMethodBody(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitMethodBody(IRNode node) {
      SimpleCallGraphDrop cgDrop = SimpleCallGraphDrop.getCGDropFor(currMethStruct.getNode());
      cgDrop.setTheBody(node);
      cgDrop.setFoundABody(true);
      return handleBlockLikeOps(node);
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitMethodCall(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitMethodCall(IRNode node) {
      return handleCallLikeOps(node);
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitMethodDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitMethodDeclaration(IRNode node) {

        final ColorStaticMeth saveCurrMethStruct = currMethStruct;
        final ColorStaticWithChildren saveCurrStruct = currStruct;
        final ColorStaticBlockish saveCurrBlockish = currBlockish;
        
        final String name = JavaNames.genMethodConstructorName(node);

        try {
          final ColorStaticMeth theMeth = new ColorStaticMeth(node, currStruct);
          currStruct.addChild(theMeth);
          currStruct = theMeth;
          currMethStruct = theMeth;
          currBlockish = theMeth;

          super.visitMethodDeclaration(node);
          
        } finally {
          currMethStruct = saveCurrMethStruct;
          currStruct = saveCurrStruct;
          currBlockish = saveCurrBlockish;
        }  
      return null;
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitNamedPackageDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitNamedPackageDeclaration(IRNode node) {
      currCUStruct.colorIncompatibles =
        ColorRules.getColorIncompatibles(node);
      
//      if (!currCUStruct.colorIncompatibles.isEmpty()) {
//        LOG.severe("found some incompatibles");
//      }
      return super.visitNamedPackageDeclaration(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitNestedClassDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitNestedClassDeclaration(IRNode node) {
      return handleClassLikeOps(node);
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitNestedInterfaceDeclaration(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitNestedInterfaceDeclaration(IRNode node) {
      return handleClassLikeOps(node);
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitNewExpression(edu.cmu.cs.fluid.ir.IRNode)
     */
    @Override
    public Void visitNewExpression(IRNode node) {
      return handleCallLikeOps(node);
    }
    
    
  }
 

//  class ColorImportWalker extends Visitor<Void> {
//
//    IBinder binder;
//
//
//    ColorImportWalker(IBinder bind) {
//      binder = bind;
//    }
//
//    
//    /** Process the imports for some BinaryCU (or some part thereof). Process as much
//     * as is needed to make sure that imports are OK for the reference to node.
//     * @param node A node referred to by some code we're processing import for. We
//     * will do just enough processing to be sure that the ref to node can be used
//     * for future processing.
//     */
//    private void doLibraryRefImportWalk(IRNode node) {
//     final IRNode cu = VisitUtil.computeOutermostEnclosingTypeOrCU(node);
// //    final String cuName = JavaNames.getFullTypeName(cu);
//      
//      if (areColorImportsProcessed(cu)) {
//        return;
//      }
//      
//      final CUDrop cud = getCUDropOf(cu);
////      final String name = JavaNames.genMethodConstructorName(node);
//      if (!(cud instanceof BinaryCUDrop)) return;
//      
// 
//      final IRNode theCUsRoot = cud.cu;
//      // next line has side effect of creating the ColorRenamePerCU if it didn't already exist.
//      final ColorRenamePerCU theCUsCRpCU = ColorRenamePerCU.getColorRenamePerCU(theCUsRoot);
//      final Object saveCookie = ColorRenamePerCU.startACU(theCUsRoot);
//      
//      try {
//        if (ColorRenameDrop.currCuHasSomeRenames()) {
//          // if the guy we're looking at contains renames, go ahead and process
//          // his color Imports. That way his renames will be OK later on.
//          final ColorImportWalker importWalk = new ColorImportWalker(binder);
//          importWalk.doAccept(theCUsRoot);
//          setColorImportsProcessed(theCUsRoot, true);
//        }
//      } finally {
//        ColorRenamePerCU.endACU(saveCookie);
//      }
//    }
//    
//    private void doParentImportWalk(Iteratable<IRNode> parents) {
//      
//      for (IRNode n : parents) {
//        doLibraryRefImportWalk(n);
//      }
//    }
//    
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visit(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visit(IRNode node) {
//      super.doAcceptForChildren(node);
//      return null;
//    }
//
//    private DropPredicate colorDeclPred = new DropPredicate() {
//
//      public boolean match(Drop d) {
//        return d instanceof ColorDeclareDrop;
//      }
//    };
//
//    private void processColorImports(final IRNode node) {
//      if (areColorImportsProcessed(node)) return;
//      Collection<ColorImportDrop> imports = ColorRules.getColorImports(node);
//      if (imports == null || imports.isEmpty()) return;
//
//      Collection<ColorRenameDrop> importedRenameDrops = new HashSet<ColorRenameDrop>();
//      Collection<ColorDeclareDrop> importedDeclareDrops = new HashSet<ColorDeclareDrop>();
//      for (ColorImportDrop ciDrop : imports) {
//        ciDrop.computeImports(binder);
//        // make sure we go look for imports and/or renames in the place we're 
//        // importing from.  This is our last chance to catch things referenced only
//        // in colorImport annos!
//        doLibraryRefImportWalk(ciDrop.getBoundImportedUnit());
//        
//        Collection<ColorRenameDrop> someImportedRenames = ColorRenameDrop
//            .getRenamesHere(ciDrop.getImportedUnit());
//        if (someImportedRenames != null)
//          importedRenameDrops.addAll(someImportedRenames);
//        Collection<ColorDeclareDrop> someImportedDecls = ColorDeclareDrop
//            .getColorDeclsForCU(ciDrop.getImportedUnit());
//        if (someImportedDecls != null)
//          importedDeclareDrops.addAll(someImportedDecls);
//      }
//
//      ColorRenamePerCU localPerCU = ColorRenamePerCU.getColorRenamePerCU(node);
//      localPerCU.addRenames(importedRenameDrops);
//
//      CUDrop cuDrop = getCUDropOf(node);
//      Set<Drop> nameModels = new HashSet<Drop>();
//      Sea.addMatchingDropsFrom(cuDrop.getDependents(), colorDeclPred,
//                               nameModels);
//
//      for (ColorDeclareDrop impCDD : importedDeclareDrops) {
//        final Collection<String> impColNames = impCDD.getDeclaredColors();
//        for (String impColName : impColNames) {
//          String impColShortName = JavaNames.genSimpleName(impColName);
//          ColorNameModel localCNM = ColorNameModel.getInstance(impColShortName,
//                                                               node);
//          ColorNameModel globalCNM = ColorNameModel.getInstance(impColName,
//                                                                null);
//          localCNM.setCanonicalTColor(globalCNM.getCanonicalTColor());
//        }
//      }
//
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitClassDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitClassDeclaration(IRNode node) {
//      final String name = ClassDeclaration.getId(node);
//      
//      processColorImports(node);
//      
//      final IRNode ext = ClassDeclaration.getExtension(node);
//      if (ext != null) {
//        final IRNode boundExt = binder.getBinding(ext);
//        doLibraryRefImportWalk(boundExt);
//      }
//      
//      final IRNode impl = ClassDeclaration.getImpls(node);
//      if (impl != null) {
//        final Iteratable<IRNode> impls = Implements.getIntfIterator(impl);
//        doParentImportWalk(impls);
//      }
//
//      setColorImportsProcessed(node, true);
//      
//      return super.visitClassDeclaration(node);
//    }
//
//    
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitConstructorCall(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitConstructorCall(IRNode node) {
//      final IRNode mDecl = binder.getBinding(node);
//
//      doLibraryRefImportWalk(mDecl);
//   
//      return super.visitConstructorCall(node);
//    }
//
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitEnumDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitEnumDeclaration(IRNode node) {
//      final String name = EnumDeclaration.getId(node);
//      
//      processColorImports(node);
//      
//      
//      final IRNode impl = EnumDeclaration.getImpls(node);
//      if (impl != null) {
//        final Iteratable<IRNode> impls = Implements.getIntfIterator(impl);
//        doParentImportWalk(impls);
//      }
//
//      setColorImportsProcessed(node, true);
//      
//      return super.visitEnumDeclaration(node);
//    }
//
//
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitFieldRef(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitFieldRef(IRNode fieldRef) {
//      final IRNode obj = FieldRef.getObject(fieldRef);
//      final IRNode field = getBinding(fieldRef);
//      
//      final Region fieldAsRegion = new Region(field);
//      
//      final RegionModel rModel = RegionModel.getInstance(fieldAsRegion.toString());
//      
//      if (rModel.getColorInfo() != null) {
//        doLibraryRefImportWalk(fieldRef);
//      }
//      
//      return super.visitFieldRef(fieldRef);
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitInterfaceDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitInterfaceDeclaration(IRNode node) {
//      final String name = InterfaceDeclaration.getId(node);
//      processColorImports(node);
//      
//      final IRNode ext = InterfaceDeclaration.getExtensions(node);
//      if (ext != null) {
//        final Iteratable<IRNode> exts = Extensions.getSuperInterfaceIterator(ext);
//        doParentImportWalk(exts);
//      }
//
//      setColorImportsProcessed(node, true);
//      
//      return super.visitInterfaceDeclaration(node);
//    }
//
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitMethodCall(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitMethodCall(IRNode node) {
//
//      final IRNode mDecl = binder.getBinding(node);
//      
//      doLibraryRefImportWalk(mDecl);
// 
//      return super.visitMethodCall(node);
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitNestedClassDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitNestedClassDeclaration(IRNode node) {
//      processColorImports(node);
//
//      final IRNode ext = NestedClassDeclaration.getExtension(node);
//      if (ext != null) {
//        final IRNode boundExt = binder.getBinding(ext);
//        doLibraryRefImportWalk(boundExt);
//      }
//      
//      final IRNode impl = NestedClassDeclaration.getImpls(node);
//      if (impl != null) {
//        final Iteratable<IRNode> impls = Implements.getIntfIterator(impl);
//        doParentImportWalk(impls);
//      }
//
//      setColorImportsProcessed(node, true);
//      
//      return super.visitNestedClassDeclaration(node);
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitNestedInterfaceDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitNestedInterfaceDeclaration(IRNode node) {
//      processColorImports(node);
//
//      final IRNode ext = NestedInterfaceDeclaration.getExtensions(node);
//      if (ext != null) {
//        final Iteratable<IRNode> exts = Extensions.getSuperInterfaceIterator(ext);
//        doParentImportWalk(exts);
//      }
//      
//      ColorRules.setColorImportsProcessed(node, true);
//      
//      return super.visitNestedInterfaceDeclaration(node);
//    }
//
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitNewExpression(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitNewExpression(IRNode node) {
//
//      final IRNode mDecl = binder.getBinding(node);
//
//      doLibraryRefImportWalk(mDecl);
//      
//      return super.visitNewExpression(node);
//    }
//
//  }

  /**
   * @author dfsuther
   * 
   */
//  class ColorRenameWalker extends Visitor<Void> {
//    
////    public Void doClassLevelRenames(IRNode forClass) {
////      final boolean saveClassOnly = classOnly;
////      final boolean saveMethodOnly = methodOnly;
////      Void res = null;
////      try {
////        classOnly = true;
////        methodOnly = false;
////        final Operator op = JJNode.tree.getOperator(forClass);
////        
////        if (ClassDeclaration.prototype.includes(op) || 
////            NestedClassDeclaration.prototype.includes(op) ||
////            InterfaceDeclaration.prototype.includes(op) ||
////            NestedInterfaceDeclaration.prototype.includes(op)) {
////          res = visit(forClass);
////        }
////        
////      } finally {
////        classOnly = saveClassOnly;
////        methodOnly = saveMethodOnly;
////      }
////      return res;
////    }
//    
//    public Void doMethodLevelRenames(IRNode forClass) {
//      final boolean saveClassOnly = classOnly;
//      final boolean saveMethodOnly = methodOnly;
//      Void res = null;
//      try {
//        classOnly = false;
//        methodOnly = true;
//        final Operator op = JJNode.tree.getOperator(forClass);
//        
//        if (MethodDeclaration.prototype.includes(op) || 
//            ConstructorDeclaration.prototype.includes(op)) {
//          res = doAccept(forClass);
//        }
//        
//      } finally {
//        classOnly = saveClassOnly;
//        methodOnly = saveMethodOnly;
//      }
//      return res;
//    }
//    
//
//    
//    private boolean classOnly = false;
//    private boolean methodOnly = false;
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visit(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visit(IRNode node) {
//      super.doAcceptForChildren(node);
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitBlockStatement(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitBlockStatement(IRNode node) {
//
//      Collection<ColorGrantDrop> grants = ColorRules.getColorGrants(node);
//      if (grants != null) {
//        // process the grants
//        for (ColorNameListDrop cnl : grants) {
//          doSimpleRenames(cnl);
//        }
//      }
//
//      Collection<ColorRevokeDrop> revokes = ColorRules.getColorRevokes(node);
//      if (revokes != null) {
//        for (ColorNameListDrop cnl : revokes) {
//          doSimpleRenames(cnl);
//        }
//      }
//
//      return super.visitBlockStatement(node);
//    }
//
//    /**
//     * @param cnl
//     */
//    public void doSimpleRenames(ColorNameListDrop cnl) {
//      List<String> newNames = new ArrayList<String>(cnl.getListedColors()
//          .size());
//      for (String name : cnl.getListedColors()) {
//        final String newName = 
//          ColorRenamePerCU.currentPerCU.getSimpleRename(name);
//        if (newName != null) {
//          newNames.add(newName);
//          ColorRenamePerCU.currentPerCU.getThisRenameFromString(name)
//              .addDependent(cnl);
//        } else {
//          newNames.add(name);
//        }
//      }
//      cnl.setListedRenamedColors(newNames);
//    }
//    
//    private CExpr renameACExpr(CExpr origExpr, Drop exprsDrop) {
//      if (!ColorRenameDrop.currCuHasSomeRenames()) {
//        return origExpr;
//      }
//      
//      CExpr newExpr = origExpr;
//      for (ColorRenameDrop rename : ColorRenameDrop.getChainRule()) {
//        final Set<String> refdNames = newExpr.referencedColorNames();
//        if (refdNames.contains(rename.simpleName)) {
//          newExpr = newExpr.cloneWithRename(rename);
//          rename.addDependent(exprsDrop);
//        }
//      }
//      return newExpr;
//    }
//
//    public void doRenameReqDrops(IRNode node) {
//      // see if the current method overrides something
//      Iterator<IRNode> parentIter = binder.findOverriddenParentMethods(node);
//      while (parentIter.hasNext()) {
//        //recur our way up the inherit/implement hierarchy, processing renames
//        // as needed.
//        doLibraryMthRenameWalk(parentIter.next());
//      }
//      
//      Collection<ColorRequireDrop> reqDrops = ColorRules.getReqDrops(node);
//      for (ColorRequireDrop reqDrop : reqDrops) {
//        if (ColorRenameDrop.currCuHasSomeRenames()) {
//          CExpr newExpr = renameACExpr(reqDrop.getRawExpr(), reqDrop);
//          reqDrop.setRenamedExpr(newExpr);
//        } else {
//          reqDrop.setRenamedExpr(reqDrop.getRawExpr());
//        }
//      }
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitClassDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitClassDeclaration(IRNode node) {
//      // possible annos here are: ColorIncompatible, ColorDeclare, ColorRename.
//      // ColorRenames were already processed while creating the drops
//      // ColorDeclare doesn't get processed for renaming, because they are
//      // declarations.
//      // ColorIncompatible must be processed now.
//      final Object saveCookie = ColorRenamePerCU.startACU(node);
//      Void res;
//      try {
//        Collection<ColorIncompatibleDrop> colorIncs = 
//          ColorRules.getColorIncompatibles(node);
//        for (ColorIncompatibleDrop inc : colorIncs) {
//          doSimpleRenames(inc);
//        }
//        
//        Set<RegionColorDeclDrop> rcdDrops = 
//          ColorRules.getMutableRegionColorDeclsSet(node);
//        for (RegionColorDeclDrop anRCD : rcdDrops) {
//          CExpr newExpr = renameACExpr(anRCD.getUserConstraint(), anRCD);
//          anRCD.setRenamedConstraint(newExpr);
//        }
//        
//        ColorRules.setColorRenamesProcessed(node, true);
//        
//        res = super.visitClassDeclaration(node);
//      } finally {
//        ColorRenamePerCU.endACU(saveCookie);
//      }
//      return res;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitClassInitializer(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitClassInitializer(IRNode node) {
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitCompilationUnit(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitCompilationUnit(IRNode node) {
//
//      // make sure that the startACU call below is guaranteed to find at least an
//      // empty PerCU. This way, there will be AT LEAST one perCU for each comp
//      // unit (there could be more, if we have some for internal or toplevel classes).
//      // Goal is to make sure that ColorRenamePerCU.currentPerCU is never null.
//      ColorRenamePerCU.getColorRenamePerCU(node);
//      final Object saveCookie = ColorRenamePerCU.startACU(node);
//
//      try {
//        super.visitCompilationUnit(node);
//      } finally {
//        ColorRenamePerCU.endACU(saveCookie);
//      }
//      return null;
//    }
//
//    private void doLibraryMthRenameWalk(IRNode node) {
//      if (!ColorRenameDrop.globalHaveRenames() || 
//          areColorRenamesProcessed(node)) {
//        return;
//      }
//      
//      final CUDrop cud = getCUDropOf(node);
//      final String name = JavaNames.genMethodConstructorName(node);
//      
//      // see if the current method overrides something
//      Iterator<IRNode> parentIter = binder.findOverriddenParentMethods(node);
//      while (parentIter.hasNext()) {
//        //recur our way up the inherit/implement hierarchy, processing renames
//        // as needed.
//        doLibraryMthRenameWalk(parentIter.next());
//      }
//      
//      if (!(cud instanceof BinaryCUDrop)) return;
//      
//      final IRNode cu = VisitUtil.computeOutermostEnclosingTypeOrCU(node);
//      final Object saveCookie = ColorRenamePerCU.startACU(cu);
//      
//      try {
//        if (ColorRenameDrop.currCuHasSomeRenames()) {
//          final ColorRenameWalker renameWalk = new ColorRenameWalker();
//          renameWalk.doMethodLevelRenames(node);
//        }
//        ColorRules.setColorRenamesProcessed(node, true);
//      } finally {
//        ColorRenamePerCU.endACU(saveCookie);
//      }
//    }
//    
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitConstructorCall(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitConstructorCall(IRNode node) {
//
//      final IRNode mDecl = binder.getBinding(node);
////      final IRNode object = ConstructorCall.getObject(node);
////      final IJavaType receiverType = binder.getJavaType(object);
////      final IRNode receiverNode = ((IJavaDeclaredType) receiverType)
////          .getDeclaration();
//      doLibraryMthRenameWalk(mDecl);
//     
//      return super.visitConstructorCall(node);
//    }
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitConstructorDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitConstructorDeclaration(IRNode node) {
//      final String name = JavaNames.genMethodConstructorName(node);
//      InstanceInitVisitor<Void> initHelper = new InstanceInitVisitor<Void>(
//                                                                           renameWalk);
//      // note that doVisitInstanceInits will only do the traversal when
//      // appropriate, and will call back into this visitor to travers the
//      // inits themselves.
//      initHelper.doVisitInstanceInits(node);
//
//      doRenameReqDrops(node);
//
//      return super.visitConstructorDeclaration(node);
//    }
//    
//    
//
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitEnumDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitEnumDeclaration(IRNode node) {
//      // possible annos here are: ColorIncompatible, ColorDeclare, ColorRename.
//      // ColorRenames were already processed while creating the drops
//      // ColorDeclare doesn't get processed for renaming, because they are
//      // declarations.
//      // ColorIncompatible must be processed now.
//      final Object saveCookie = ColorRenamePerCU.startACU(node);
//      Void res;
//      try {
//        Collection<ColorIncompatibleDrop> colorIncs = 
//          ColorRules.getColorIncompatibles(node);
//        for (ColorIncompatibleDrop inc : colorIncs) {
//          doSimpleRenames(inc);
//        }
//        
//        Set<RegionColorDeclDrop> rcdDrops = 
//          ColorRules.getMutableRegionColorDeclsSet(node);
//        for (RegionColorDeclDrop anRCD : rcdDrops) {
//          CExpr newExpr = renameACExpr(anRCD.getUserConstraint(), anRCD);
//          anRCD.setRenamedConstraint(newExpr);
//        }
//        
//        ColorRules.setColorRenamesProcessed(node, true);
//        
//        res = super.visitEnumDeclaration(node);
//      } finally {
//        ColorRenamePerCU.endACU(saveCookie);
//      }
//      return res;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitFieldDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitFieldDeclaration(IRNode node) {
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitInterfaceDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitInterfaceDeclaration(IRNode node) {
//      // possible annos here are: ColorIncompatible, ColorDeclare, ColorRename.
//      // ColorRenames were already processed while creating the drops
//      // ColorDeclare doesn't get processed for renaming, because they are
//      // declarations.
//      // ColorIncompatible must be processed now.
//      final Object saveCookie = ColorRenamePerCU.startACU(node);
//      Void res = null;
//      try {
//        Collection<ColorIncompatibleDrop> colorIncs = 
//          ColorRules.getColorIncompatibles(node);
//        for (ColorIncompatibleDrop inc : colorIncs) {
//          doSimpleRenames(inc);
//        }
//        
//        ColorRules.setColorRenamesProcessed(node, true);
//        
//        res = super.visitInterfaceDeclaration(node);
//      } finally {
//        ColorRenamePerCU.endACU(saveCookie);
//      }
//      return res;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitMethodCall(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitMethodCall(IRNode node) {
//
//      final IRNode mDecl = binder.getBinding(node);
//      
//      doLibraryMthRenameWalk(mDecl);
// 
//      
//      return super.visitMethodCall(node);
//    }
//    
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitMethodDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitMethodDeclaration(IRNode node) {
//      final String name = JavaNames.genMethodConstructorName(node);
//      doRenameReqDrops(node);
//      return super.visitMethodDeclaration(node);
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitNestedClassDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitNestedClassDeclaration(IRNode node) {
//      // possible annos here are: ColorIncompatible, ColorDeclare, ColorRename.
//      // ColorRenames were already processed while creating the drops
//      // ColorDeclare doesn't get processed for renaming, because they are
//      // declarations.
//      // ColorIncompatible must be processed now.
//
//      Collection<ColorIncompatibleDrop> colorIncs = ColorRules
//          .getColorIncompatibles(node);
//      for (ColorIncompatibleDrop inc : colorIncs) {
//        doSimpleRenames(inc);
//      }
//      
//      Set<RegionColorDeclDrop> rcdDrops = 
//        ColorRules.getMutableRegionColorDeclsSet(node);
//      for (RegionColorDeclDrop anRCD : rcdDrops) {
//        CExpr newExpr = renameACExpr(anRCD.getUserConstraint(), anRCD);
//        anRCD.setRenamedConstraint(newExpr);
//      }
//      
//      ColorRules.setColorRenamesProcessed(node, true);
//      
//      return super.visitNestedClassDeclaration(node);
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitNestedInterfaceDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitNestedInterfaceDeclaration(IRNode node) {
//      // possible annos here are: ColorIncompatible, ColorDeclare, ColorRename.
//      // ColorRenames were already processed while creating the drops
//      // ColorDeclare doesn't get processed for renaming, because they are
//      // declarations.
//      // ColorIncompatible must be processed now.
//
//      Collection<ColorIncompatibleDrop> colorIncs = ColorRules
//          .getColorIncompatibles(node);
//      for (ColorIncompatibleDrop inc : colorIncs) {
//        doSimpleRenames(inc);
//      }
//      
//      ColorRules.setColorRenamesProcessed(node, true);
//
//      return super.visitNestedInterfaceDeclaration(node);
//    }
//    
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitNewExpression(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitNewExpression(IRNode node) {
//
//      final IRNode mDecl = binder.getBinding(node);
//
//      doLibraryMthRenameWalk(mDecl);
//      
//      return super.visitNewExpression(node);
//    }
//
//  }

//  private Visitor<Void> renameWalk = new ColorRenameWalker();

  private Visitor<Void> importWalk = null;
  
  private ColorStructVisitor structureImportWalk = new ColorImportWalkerNew();
  private ColorStructVisitor structureRenameWalk = new ColorRenameWalkerNew();
  private Visitor<Void> structureBuilder = new ColorStaticStructureBuilder();
  private ColorStructVisitor structureCFP = new CfpWalker();
  private ColorStaticStructureDumper dumper = new ColorStaticStructureDumper();

  private IBinder binder;

  private IBinder eBinder;


  // private ColorFirstPass() {
  // }

  
  private static ColorFirstPass INSTANCE = new ColorFirstPass();

  private static final Logger LOG = SLLogger
      .getLogger("analysis.colors.managecolorannos");

  private static final Logger LOG1 = SLLogger
      .getLogger("analysis.callgraph.stats");
  
 // private static final Logger ciwnLOG = Logger.getLogger("colorImportWalker");
  private static final boolean debug = true;
  

//  private void tryStructureWalkers(IRNode cu) {
//    structureBuilder.doAccept(cu);
//    ColorStaticStructure cuStruct = ColorStaticCU.getStaticCU(cu);
//    structureImportWalk.doAccept(cuStruct);
//    structureRenameWalk.doAccept(cuStruct);
//  }
 
  public void buildStaticStructureForACU(IRNode cu) {
    final Operator op = JJNode.tree.getOperator(cu);
    if (CompilationUnit.prototype.includes(op)) {
      structureBuilder.doAccept(cu);
    }
  }
  /**
   * Process a single compilation unit for Color Promise purposes. Currently,
   * this initializes the BDD package, then walks the tree to build/update the
   * parts of SimpleCallGraph effected by this CU.
   * 
   * @param cu
   *          The compilation unit to process
   * @param useThisBinder
   *          A handle for the binder to use. Note that this method is
   *          GUARANTEED to fail if the binder is not an
   *          <code>EclipseBinder</code>
   */
  public void doOneCU(IRNode cu, IBinder useThisBinder) {
    cuCount += 1;
    
//    ColorNameModel.purgeUnusedColorNames();

    binder = useThisBinder;
    eBinder = binder;
//    final ColorStaticStructure cusStruct = doImportandRenameWalks(cu, eBinder);
    processOneCU(cu);
    // the CU has been visited during ordinary processing. Make sure we don't
    // go visit it again during late make-up processing.
    compUnitsToVisit.remove(cu);
  }
  
  /**
   * Process a single compilation unit for Color Promise purposes. Currently,
   * this initializes the BDD package, then walks the tree to build/update the
   * parts of SimpleCallGraph effected by this CU.
   * 
   * @param cu
   *          The compilation unit to process
   * @param useThisBinder
   *          A handle for the binder to use. Note that this method is
   *          GUARANTEED to fail if the binder is not an
   *          <code>EclipseBinder</code>
   */
  public void doOneCUZerothPass(IRNode cu, IBinder useThisBinder) {
 
//    ColorNameModel.purgeUnusedColorNames();

    binder = useThisBinder;
    eBinder = binder;
    final ColorStaticStructure cusStruct = doImportandRenameWalks(cu, eBinder);
  }
  
  
  

  /** Do the main ColorFirstPass processing of a CU.  Broken out here because
   * some CUs were partially processed early, some were fully processed during
   * ordinary DoubleChecker running. So... this method may be called from doOneCU
   * (which is the normal full-processing pass), or it may be called late, from
   * cfpEnd. During cfpEnd() we need to do the main colorFirstPass processing on
   * every CU that wasn't fully processed before.
   * @param cu
   */
  private void processOneCU(IRNode cu) {


    ColorInherit.startACu(eBinder);
//    cfpTW = new CfpTreeWalker(binder);
//    cfpTW.doAccept(cu);
    ColorStaticCU cuStruct = ColorStaticCU.getStaticCU(cu);
    cuStruct.accept(structureCFP);


    ColorInherit.endACu();
  }

  /** Do all the "early processing" of a CU. This consists of 1: building the
   * ColorStaticStructure for that CU (if necessary); 2: walk the CU's 
   * ColorStaticStructure tree processing imports; then 3: walk that tree again
   * doing renames (if there are any renames to do).
   * 
   * Note that this method gets called early to handle IR that has changed but
   * whose CUs won't be processed during ordinary double-checker processing.
   * 
   * @param cu The CU to process.
   */
  public ColorStaticStructure doImportandRenameWalks(IRNode cu,
                                                     IBinder useThisBinder) {
    binder = useThisBinder;
    eBinder = useThisBinder;
//    if (importWalk == null) {
//      importWalk = new ColorImportWalker(useThisBinder);
//    }
//    importWalk.doAccept(cu);
    
    final Object renameCookie = ColorRenamePerCU.startACU(cu);
    // If the structure for WalkMe needs to be build, this will happen as a
    // side effect of calling getStaticCU.
    ColorStaticStructure walkMe = ColorStaticCU.getStaticCU(cu);
//    walkMe.accept(dumper);
    walkMe.accept(structureImportWalk);
//    if (ColorRenameDrop.globalHaveRenames()) {
      
      walkMe.accept(structureRenameWalk);
//    }
    ColorRenamePerCU.endACU(renameCookie);
    return walkMe;
  }

  public void cfpStart(final IBinder binder) {
    // collect a bunch of statistics
    ColorStats.getInstance().beforeCfp = ColorStats.getInstance()
        .getColorStats("Before ColorFirstPass:");
    
    ColorTargets.initColorTargets(binder);
    
//    final IRNode jls = ac.binder.getTypeEnvironment().findNamedType("java.lang.String");
//    jlsCU = VisitUtil.getEnclosingCompilationUnit(jls);
    
//    compUnitsToVisit.clear();
  }
  
  private void computeCallGraphReachability() {
    final SimpleCallGraphDrop[] allCGD = SimpleCallGraphDrop.getAllCGDrops();
    final List<SimpleCallGraphDrop> heads = new ArrayList<SimpleCallGraphDrop>(allCGD.length);
    
    for (int i = 1; i < allCGD.length; i++) {
      if (allCGD[i].isPotentiallyCallable()) {
        heads.add(allCGD[i]);
      }
    }
    
    for (SimpleCallGraphDrop aCGD: heads) {
      reached(aCGD);
    }
  }

  private void reached(final SimpleCallGraphDrop aCGD) {
    final Collection<IRNode> irSuccs = aCGD.getCallees();

    for (IRNode irSucc:irSuccs) {
      final SimpleCallGraphDrop succ = SimpleCallGraphDrop.getCGDropFor(irSucc);
      if (!succ.isPotentiallyCallable()) {
        succ.setPotentiallyCallable(true);
        reached(succ);
      }
    }
  }
  
  
  /**
   * Finish up the first pass of Color Processing. Currently, all this does is
   * compute the API of the various modules, which is needed for Dean's Module
   * experiment.
   * @return TODO
   */
  public Iterable<IRNode> cfpEnd() {
    computeCallGraphReachability();
   
    
    Collection<IRNode> res = new ArrayList<IRNode>(compUnitsToVisit.size());
    if (!compUnitsToVisit.isEmpty()) {
      LOG.info(compUnitsToVisit.toString());
      res.addAll(compUnitsToVisit);
      return res;
    }
    
    
    Collection<? extends ColorizedRegionModel> crmDrops = 
      Sea.getDefault().getDropsOfType(ColorizedRegionModel.class);
    for (ColorizedRegionModel crm : crmDrops) {
      crm.setAndOfUserConstraints(ColorBDDPack.one());
    }
    
    Collection<? extends RegionColorDeclDrop> rcds = 
      Sea.getDefault().getDropsOfType(RegionColorDeclDrop.class);
    for (RegionColorDeclDrop rcd : rcds) {
      final ColorizedRegionModel crm = rcd.getColorizedRegionInfo();
      if (crm == null) {
        LOG.severe("no ColorizedRegionModel for " + rcd);
        continue;
      }
      JBDD newConstraint =
        crm.getAndOfUserConstraints().and(rcd.getRenamedConstraint().computeExpr(true));
      crm.setAndOfUserConstraints(newConstraint);
    }
    
    
    for (ColorizedRegionModel crm : crmDrops) {
      JBDD userConstraints = crm.getAndOfUserConstraints();
      if (userConstraints == null) {
        crm.setComputedContext(ColorBDDPack.one());
      } else {
        crm.setComputedContext(userConstraints.copy());
      }
    }
    
    ColorStats.getInstance().afterCfp = ColorStats.getInstance()
        .getColorStats("After ColorFirstPass:");
    
    LOG.info("CFP called on " + Integer.toString(cuCount) + " comp units.");
    
    if (!compUnitsToVisit.isEmpty()) {
      LOG.warning("compUnitsToVisit not empty at end of cfpEnd(); " 
                  + compUnitsToVisit);
    }
    return res;
  }

  /**
   * Checks if the given method declation is declared within an interface.
   * 
   * @param methodDeclaration
   *          the method declaration to check
   * @return <code>true</code> if the declaration is within an interface,
   *         <code>false</code> otherwise
   */
  private static boolean isDeclaredWithinAnInterface(IRNode methodDeclaration) {
    assert methodDeclaration != null;
    assert MethodDeclaration.prototype.includes(methodDeclaration);

    IRNode enclosingType = VisitUtil.getEnclosingType(methodDeclaration);
    if (enclosingType == null) {
      LOG.warning("unable to find an enclosing type for \""
          + JJNode.getInfo(methodDeclaration) + "\"");
      return false;
    }
    return InterfaceDeclaration.prototype.includes(enclosingType);
  }

  /**
   * Checks if the given method or constructor declation is concrete, that is,
   * it declares something that can be invoked at runtime. This excludes
   * abstract method declarations and methods declared within an interface.
   * 
   * @param methodOrConstructorDeclaration
   *          the method or constructor declaration to check
   * @return <code>true</code> if the declaration is concrete,
   *         <code>false</code> otherwise
   */
  public static boolean isConcrete(final IRNode methodOrConstructorDeclaration) {
    assert methodOrConstructorDeclaration != null;

    boolean isMethodDecl = MethodDeclaration.prototype
        .includes(methodOrConstructorDeclaration);
    boolean isConstructorDecl = ConstructorDeclaration.prototype
        .includes(methodOrConstructorDeclaration);
    boolean notAbstract = !JavaNode.getModifier(methodOrConstructorDeclaration,
                                                JavaNode.ABSTRACT);

    if (isMethodDecl) {
      if (notAbstract) {
        if (!isDeclaredWithinAnInterface(methodOrConstructorDeclaration)) { return true; }
      }
    } else if (isConstructorDecl) { return true; }
    return false;
  }

//  private void cgBuildOne(final IRNode caller, final IRNode callee) {
//    final SimpleCallGraphDrop callerDrop = SimpleCallGraphDrop
//        .getCGDropFor(caller);
//    final SimpleCallGraphDrop calleeDrop = SimpleCallGraphDrop
//        .getCGDropFor(callee);
//
//    callerDrop.getCallees().add(callee);
//    calleeDrop.getCallers().add(caller);
//
//  }
//
//  /**
//   * Update the call graph to indicate that caller invokes callee. Also note
//   * that caller may possibly invoke any method that overrides or implements
//   * callee.
//   * 
//   * @param caller
//   *          The caller's mDecl.
//   * @param callee
//   *          The callee's mDecl.
//   * @param receiver
//   *          The callee's receiver type.
//   */
//  private void cgBuild(final IRNode caller, final IRNode callee,
//      final IRNode receiver) {
//    cgBuildOne(caller, callee);
//    final SimpleCallGraphDrop calleeDrop = SimpleCallGraphDrop
//        .getCGDropFor(callee);
//
//    calleeDrop.numCallSitesSeen += 1;
//
//    final boolean isStatic = JavaNode.getModifier(caller, JavaNode.STATIC);
//    final boolean isFinal = JavaNode.getModifier(caller, JavaNode.FINAL);
//    final boolean isPrivate = JavaNode.getModifier(caller, JavaNode.PRIVATE);
//    final boolean methodCanBeOveridden = !(isStatic || isFinal || isPrivate);
//    if (methodCanBeOveridden) {
//      Iterator<IRNode> overrides = eBinder
//          .findOverridingMethodsFromType(callee, receiver);
//      while (overrides.hasNext()) {
//        IRNode oCallee = overrides.next();
//        cgBuildOne(caller, oCallee);
//        calleeDrop.numOverridingMethods += 1;
//      }
//    }
//  }

  class CfpWalker extends ColorStructVisitor {
    ColorStaticMeth currMeth = null;
    ColorStaticClass currClass = null;
//    boolean colorsNeedBodyTraversal = false;


    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visit(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visit(ColorStaticStructure node) {
      if (node instanceof ColorStaticWithChildren) {
        doAcceptForChildren((ColorStaticWithChildren)node);
      }
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitBlock(edu.cmu.cs.fluid.java.analysis.ColorStaticBlock)
     */
    @Override
    public void visitBlock(ColorStaticBlock node) {
      visitChildReferences(node);
      if (!node.grants.isEmpty() || !node.revokes.isEmpty()) {
        currMeth.needsBodyTraversal = true;
      }
      super.visitBlock(node);
    }

    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitCall(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visitCall(ColorStaticCall node) {
      final IRNode nd = node.getNode();
      final IRNode mDecl = ColorFirstPass.getBinding(nd);

        // ensure that the inherited summary has been built for mDecl.
        // This will invoke doInherit if need be.
        ColorReqSummaryDrop.getSummaryFor(mDecl);
//      }
      
        // Don't do anything for data coloring at method calls.
        // DFS 03/22/07
//      if (ColorizedRegionModel.haveColorizedRegions()) {
//        List<ColorizedRegionModel> crms = ColorTargets.getCRMsForMethodCall(nd);
////          ColorTargets.filterColorizedTargets(nd, ColorTargets.getTargetsForMethodCall(nd, mDecl));
//        if (!crms.isEmpty()) {
//          currMeth.needsBodyTraversal = true;
//          node.colorCRMsHere = crms;
//        } else {
//          node.colorCRMsHere = Collections.emptyList();
//        }
//      }
      
      super.visitCall(node);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitClass(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visitClass(ColorStaticClass node) {
      final ColorStaticClass saveCurrClass = currClass;
      try {
        
      super.visitClass(node); 
      } finally {
        currClass = saveCurrClass;
      }
    }

    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitCU(edu.cmu.cs.fluid.java.analysis.ColorStaticCU)
     */
    @Override
    public void visitCU(ColorStaticCU node) {
      ColorInherit.startACu(eBinder);
      final Object saveCookie = ColorRenamePerCU.startACU(node.getNode());

//      currModuleNum = ModuleSupport.getModnumFromNode(node);
//      final boolean saveColorsNeedBodyTraversal = colorsNeedBodyTraversal;
      final ColorStaticMeth saveCurrMeth = currMeth;
      try {
        Collection<ColorIncompatibleDrop> colorIncs = 
          ColorRules.getColorIncompatibles(node.getNode());
        for (ColorIncompatibleDrop inc : colorIncs) {
          LOG.warning("Found colorIncompatibles: " + inc);
        }
        super.visitCU(node);
      } finally {
//        colorsNeedBodyTraversal = saveColorsNeedBodyTraversal;
        currMeth = saveCurrMeth;
      }
      ColorInherit.endACu();
      ColorRenamePerCU.endACU(saveCookie);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.analysis.ColorStructVisitor#visitMeth(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure)
     */
    @Override
    public void visitMeth(ColorStaticMeth node) {
      final ColorStaticMeth saveCurrMeth = currMeth;
      try {
        currMeth = node;
        final IRNode nd = node.getNode();
        visitChildReferences(node);
        
        super.visitMeth(node);
        
        ColorCtxSummaryDrop ctxSumm = ColorCtxSummaryDrop.getSummaryFor(nd);
        // ctxSumm.containsGrantOrRevoke = colorsNeedBodyTraversal;
        // ensure that the inherited summary has been built for mDecl.
        // This will invoke doInherit if need be.
        ColorReqSummaryDrop reqSumm = ColorReqSummaryDrop.getSummaryFor(nd);
        // reqSumm.containsGrantOrRevoke = colorsNeedBodyTraversal;
        final SimpleCallGraphDrop cgDrop = SimpleCallGraphDrop.getCGDropFor(nd);
        cgDrop.setColorsNeedBodyTraversal(node.needsBodyTraversal);
      } finally {
        currMeth = saveCurrMeth;
      }
    }


    
    /** Visit all the References inside the blockish structure. If any of them
     * actually mark a reference to a Colorized region of some sort, record the
     * relevant targets and tell the blockish parent that this is an 
     * "interesting" reference.
     * @param blockish
     */
    private void visitChildReferences(ColorStaticBlockish blockish) {
      blockish.interestingRefs.clear();
      if (ColorizedRegionModel.haveColorizedRegions()) {
        for (ColorStaticRef aRef: blockish.allRefs) {
          aRef.colorTargetsHere.clear();
          final IRNode nd = aRef.getNode();

          List<ColorizedRegionModel> crms = ColorTargets.getCRMsForRef(nd);
//            ColorTargets.filterColorizedTargets(nd, 
//                                                ColorTargets.getTargetsForFieldRef(nd));
          if (!crms.isEmpty()) {
            currMeth.needsBodyTraversal = true;
            aRef.colorTargetsHere.addAll(crms);
            blockish.interestingRefs.add(aRef);
          }
        }
      }
    }
    
  }
//  /**
//   * @author dfsuther
//   * 
//   * Walk a tree, keeping track of the current Module number, the current
//   * method, and whether we've seen an
//   * @-grant or
//   * @-revoke annotation in the current method. "Keeping track: means that each
//   *          method that could change these values is responsible for saving
//   *          the old value on entry, and restoring it on exit. This ensures
//   *          that we survive nested classes, methods, etc. Also, we make sure
//   *          to assign the current module number, build the call graph, and
//   *          record colorsNeedBodyTraversal at method/constructor decls.
//   */
//  class CfpTreeWalker extends Visitor<Void> {
//
//    //
//    // AnalysisContext ac = null;
//
////   Integer currModuleNum = null;
//
//    boolean colorsNeedBodyTraversal = false;
//
//    IRNode currMeth = null;
// 
//
//    InstanceInitVisitor<Void> initHelper = null;
//    
//    IBinder binder;
//    
//    CfpTreeWalker(IBinder aBinder) {
//      binder = aBinder;
//    }
//
//    
//    private IRNode getBinding(final IRNode node) {
//      return this.binder.getBinding(node);
//    }
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visit(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visit(IRNode node) {
//      // TODO Auto-generated method stub
//      super.doAcceptForChildren(node);
//      return null;
//    }
//
//    
//    
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitAnonClassExpression(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitAnonClassExpression(IRNode node) {
//      if (ColorizedRegionModel.haveColorizedRegions()) {
//        IRNode mDecl = getBinding(node);
//        Set<Target> tgts = 
//          ColorTargets.filterColorizedTargets(node, ColorTargets.getTargetsForMethodCall(node, mDecl));
//        if (!tgts.isEmpty()) {
//          colorsNeedBodyTraversal = true;
//        }
//      }
//      return super.visitAnonClassExpression(node);
//    }
//
//
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitArrayRefExpression(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitArrayRefExpression(IRNode node) {
//      if (ColorizedRegionModel.haveColorizedRegions()) {
//        Set<Target> tgts = 
//          ColorTargets.filterColorizedTargets(node, ColorTargets.getTargetsForArrayRef(node));
//        if (!tgts.isEmpty()) {
//          colorsNeedBodyTraversal = true;
//        }
//      }
//      return super.visitArrayRefExpression(node);
//    }
//
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitCompilationUnit(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitCompilationUnit(IRNode node) {
//      ColorInherit.startACu(eBinder);
//      final Object saveCookie = ColorRenamePerCU.startACU(node);
//
////      currModuleNum = ModuleSupport.getModnumFromNode(node);
//      final boolean saveColorsNeedBodyTraversal = colorsNeedBodyTraversal;
//      final IRNode saveCurrMeth = currMeth;
//      try {
//        super.visitCompilationUnit(node);
//      } finally {
//        colorsNeedBodyTraversal = saveColorsNeedBodyTraversal;
//        currMeth = saveCurrMeth;
//      }
//      ColorInherit.endACu();
//      ColorRenamePerCU.endACU(saveCookie);
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitBlockStatement(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitBlockStatement(IRNode node) {
//
//      if (!ColorRules.getColorGrants(node).isEmpty()
//          || !ColorRules.getColorRevokes(node).isEmpty()) {
//        colorsNeedBodyTraversal = true;
//      }
//
//      super.visitBlockStatement(node);
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitClassDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitClassDeclaration(IRNode node) {
//      final boolean saveColorsNeedBodyTraversal = colorsNeedBodyTraversal;
//      final IRNode saveCurrMeth = currMeth;
//
//      super.visitClassDeclaration(node);
//
//      colorsNeedBodyTraversal = saveColorsNeedBodyTraversal;
//      currMeth = saveCurrMeth;
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitConstructorCall(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitConstructorCall(IRNode node) {
//
//      final IRNode mDecl = binder.getBinding(node);
//      final IRNode object = ConstructorCall.getObject(node);
//      final IJavaType receiverType = binder.getJavaType(object);
////      final IRNode receiverNode = ((IJavaDeclaredType) receiverType)
////          .getDeclaration();
//
//      if (currMeth != null) {
////        // build call graph connections
////        cgBuild(currMeth, mDecl, receiverNode);
//
//        // ensure that the requirement summary has been built for mDecl.
//        // This will invoke doInherit if need be.
//        ColorReqSummaryDrop.getSummaryFor(mDecl);
//        // ColorCtxSummaryDrop.getSummaryFor(mDecl);
//      }
//      
//      if (ColorizedRegionModel.haveColorizedRegions()) {
//        Set<Target> tgts = 
//          ColorTargets.filterColorizedTargets(node, ColorTargets.getTargetsForMethodCall(node, mDecl));
//        if (!tgts.isEmpty()) {
//          colorsNeedBodyTraversal = true;
//        }
//      }
//
//      super.visitConstructorCall(node);
//
//      if (initHelper != null) {
//        initHelper.doVisitInstanceInits(node);
//      }
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitConstructorDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitConstructorDeclaration(IRNode root) {
//      final IRNode saveCurrMeth = currMeth;
//      final boolean saveColorsNeedBodyTraversal = colorsNeedBodyTraversal;
//      final InstanceInitVisitor<Void> saveInitHelper = initHelper;
//      final String name = JavaNames.genMethodConstructorName(root);
//
//      try {
//        currMeth = root;
//        colorsNeedBodyTraversal = false;
//
//        initHelper = new InstanceInitVisitor<Void>(cfpTW);
//        // note that doVisitInstanceInits will only do the traversal when
//        // appropriate, and will call back into this visitor to travers the
//        // inits themselves.
//        initHelper.doVisitInstanceInits(root);
//
//        super.visitConstructorDeclaration(root);
//
//        ColorCtxSummaryDrop ctxSumm = ColorCtxSummaryDrop.getSummaryFor(root);
//        // ctxSumm.containsGrantOrRevoke = colorsNeedBodyTraversal;
//        ColorReqSummaryDrop reqSumm = ColorReqSummaryDrop.getSummaryFor(root);
//        // reqSumm.containsGrantOrRevoke = colorsNeedBodyTraversal;
//
//        SimpleCallGraphDrop cgDrop = SimpleCallGraphDrop.getCGDropFor(root);
////        cgDrop.moduleNum = currModuleNum;
//        cgDrop.setColorsNeedBodyTraversal(colorsNeedBodyTraversal);
//
//        // ensure that the inherited summary has been built for mDecl.
//        // This will invoke doInherit if need be.
//        ColorReqSummaryDrop.getSummaryFor(root);
//      } finally {
//        colorsNeedBodyTraversal = saveColorsNeedBodyTraversal;
//        currMeth = saveCurrMeth;
//        initHelper = saveInitHelper;
//      }
//      return null;
//    }
//
//    // don't look in class initializers or field declarations
//    // (Alternatively, we could look in if they *are* static.)
//    @Override
//    public Void visitClassInitializer(IRNode node) {
//      return null;
//    }
//
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitEnumDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitEnumDeclaration(IRNode node) {
//      final boolean saveColorsNeedBodyTraversal = colorsNeedBodyTraversal;
//      final IRNode saveCurrMeth = currMeth;
//
//      super.visitEnumDeclaration(node);
//
//      colorsNeedBodyTraversal = saveColorsNeedBodyTraversal;
//      currMeth = saveCurrMeth;
//      return null;
//    }
//
//
//    @Override
//    public Void visitFieldDeclaration(IRNode node) {
//      
//      return null;
//    }
//
//    /* (non-Javadoc)
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitFieldRef(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitFieldRef(IRNode node) {
//      if (ColorizedRegionModel.haveColorizedRegions()) {
//        Set<Target> tgts = 
//          ColorTargets.filterColorizedTargets(node, ColorTargets.getTargetsForFieldRef(node));
//        if (!tgts.isEmpty()) {
//          colorsNeedBodyTraversal = true;
//        }
//      }
//      
//      return super.visitFieldRef(node);
//    }
//
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.Visitor#visitInterfaceDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitInterfaceDeclaration(IRNode node) {
//      final boolean saveColorsNeedBodyTraversal = colorsNeedBodyTraversal;
//      final IRNode saveCurrMeth = currMeth;
//
//      super.visitInterfaceDeclaration(node);
//
//      colorsNeedBodyTraversal = saveColorsNeedBodyTraversal;
//      currMeth = saveCurrMeth;
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitMethodBody(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitMethodBody(IRNode node) {
//
//      SimpleCallGraphDrop cgDrop = SimpleCallGraphDrop.getCGDropFor(currMeth);
//      cgDrop.setTheBody(node);
//      cgDrop.setFoundABody(true);
//
//      super.visitMethodBody(node);
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitMethodCall(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitMethodCall(IRNode node) {
//
//      final IRNode mDecl = getBinding(node);
//      /*
//      final IRNode obj = MethodCall.getObject(node);
//      final IJavaType receiverType = binder.getJavaType(obj);
//      final IRNode receiverNode = ((IJavaDeclaredType) receiverType)
//          .getDeclaration();
//      final String mName = JavaNames.genQualifiedMethodConstructorName(mDecl);
//      */
//      
//      if (currMeth != null) {
////        cgBuild(currMeth, mDecl, receiverNode);
//
//        // ensure that the inherited summary has been built for mDecl.
//        // This will invoke doInherit if need be.
//        ColorReqSummaryDrop.getSummaryFor(mDecl);
//      }
//      
//      if (ColorizedRegionModel.haveColorizedRegions()) {
//        Set<Target> tgts = 
//          ColorTargets.filterColorizedTargets(node, ColorTargets.getTargetsForMethodCall(node, mDecl));
//        if (!tgts.isEmpty()) {
//          colorsNeedBodyTraversal = true;
//        }
//      }
//
//      super.visitMethodCall(node);
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitMethodDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitMethodDeclaration(IRNode root) {
//      final boolean saveColorsNeedBodyTraversal = colorsNeedBodyTraversal;
//      colorsNeedBodyTraversal = false;
//      final IRNode saveCurrMeth = currMeth;
//      currMeth = root;
//      final String name = JavaNames.genMethodConstructorName(root);
//
//      try {
//        super.visitMethodDeclaration(root);
//
//        ColorCtxSummaryDrop ctxSumm = ColorCtxSummaryDrop.getSummaryFor(root);
//        // ctxSumm.containsGrantOrRevoke = colorsNeedBodyTraversal;
//        // ensure that the inherited summary has been built for mDecl.
//        // This will invoke doInherit if need be.
//        ColorReqSummaryDrop reqSumm = ColorReqSummaryDrop.getSummaryFor(root);
//        // reqSumm.containsGrantOrRevoke = colorsNeedBodyTraversal;
//
//        SimpleCallGraphDrop cgDrop = SimpleCallGraphDrop.getCGDropFor(root);
////        cgDrop.moduleNum = currModuleNum;
//
//        cgDrop.setColorsNeedBodyTraversal(colorsNeedBodyTraversal);
//      } finally {
//        colorsNeedBodyTraversal = saveColorsNeedBodyTraversal;
//        currMeth = saveCurrMeth;
//      }
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitNestedClassDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitNestedClassDeclaration(IRNode node) {
//      final boolean saveColorsNeedBodyTraversal = colorsNeedBodyTraversal;
//      colorsNeedBodyTraversal = false;
//      final IRNode saveCurrMeth = currMeth;
//      currMeth = null;
//      try {
//        super.visitNestedClassDeclaration(node);
//      } finally {
//        colorsNeedBodyTraversal = saveColorsNeedBodyTraversal;
//        currMeth = saveCurrMeth;
//      }
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitNestedInterfaceDeclaration(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitNestedInterfaceDeclaration(IRNode node) {
//      final boolean saveColorsNeedBodyTraversal = colorsNeedBodyTraversal;
//      colorsNeedBodyTraversal = false;
//      final IRNode saveCurrMeth = currMeth;
//      currMeth = null;
//
//      try {
//        super.visitNestedInterfaceDeclaration(node);
//      } finally {
//        colorsNeedBodyTraversal = saveColorsNeedBodyTraversal;
//        currMeth = saveCurrMeth;
//      }
//      return null;
//    }
//
//    /*
//     * (non-Javadoc)
//     * 
//     * @see edu.cmu.cs.fluid.java.operator.IVisitor#visitNewExpression(edu.cmu.cs.fluid.ir.IRNode)
//     */
//    @Override
//    public Void visitNewExpression(IRNode node) {
//
//      final IRNode mDecl = getBinding(node);
//      final IRNode type = NewExpression.getType(node);
//
////      if (currMeth != null) {
////        cgBuild(currMeth, mDecl, type);
//
//        // ensure that the inherited summary has been built for mDecl.
//        // This will invoke doInherit if need be.
//        ColorReqSummaryDrop.getSummaryFor(mDecl);
////      }
//      
//      if (ColorizedRegionModel.haveColorizedRegions()) {
//        Set<Target> tgts = 
//          ColorTargets.filterColorizedTargets(node, ColorTargets.getTargetsForMethodCall(node, mDecl));
//        if (!tgts.isEmpty()) {
//          colorsNeedBodyTraversal = true;
//        }
//      }
//      
//      super.visitNewExpression(node);
//      return null;
//    }
//
//  }
//
//  CfpTreeWalker cfpTW = null;
//  

  
  public static ColorFirstPass getInstance() {
    return INSTANCE;
  }
  
//  public static IRNode getBinding(final IRNode node) {
//    return getInstance().binder.getBinding(node);
//  }

  public void resetForAFullBuild() {
    compUnitsToVisit.clear();
  }
}