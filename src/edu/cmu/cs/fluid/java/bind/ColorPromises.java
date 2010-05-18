/*
 * Created on Oct 30, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import com.surelogic.analysis.threadroles.TRBinExpr;
import com.surelogic.analysis.threadroles.TRExpr;
import com.surelogic.analysis.threadroles.TRLeafExpr;
import com.surelogic.analysis.threadroles.TRUnaryExpr;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.promise.ColorAnd;
import edu.cmu.cs.fluid.java.promise.ColorAndParen;
import edu.cmu.cs.fluid.java.promise.ColorConstrainedRegions;
import edu.cmu.cs.fluid.java.promise.ColorContext;
import edu.cmu.cs.fluid.java.promise.ColorDeclaration;
import edu.cmu.cs.fluid.java.promise.ColorGrant;
import edu.cmu.cs.fluid.java.promise.ColorImport;
import edu.cmu.cs.fluid.java.promise.ColorIncompatible;
import edu.cmu.cs.fluid.java.promise.ColorName;
import edu.cmu.cs.fluid.java.promise.ColorNot;
import edu.cmu.cs.fluid.java.promise.ColorOr;
import edu.cmu.cs.fluid.java.promise.ColorRename;
import edu.cmu.cs.fluid.java.promise.ColorRequire;
import edu.cmu.cs.fluid.java.promise.ColorRevoke;
import edu.cmu.cs.fluid.java.promise.ColorizedRegion;
import edu.cmu.cs.fluid.java.promise.RegionName;
import edu.cmu.cs.fluid.java.promise.RegionSpecifications;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.promise.IPromiseAnnotation;
import edu.cmu.cs.fluid.promise.IPromiseParsedCallback;
import edu.cmu.cs.fluid.promise.IPromiseRule;
import edu.cmu.cs.fluid.promise.IPromiseStorage;
import edu.cmu.cs.fluid.promise.parse.BooleanTagRule;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ColorContextDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.sea.drops.promises.SimpleCallGraphDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.RegionTRoleDeclDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.RegionTRoleModel;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleCtxSummaryDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleDeclareDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleGrantDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleImportDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleIncompatibleDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleNameModel;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleRenameDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleRenamePerCU;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleReqSummaryDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleRequireDrop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleRevokeDrop;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * @author chance
 *  
 */
@Deprecated
public final class ColorPromises extends AbstractPromiseAnnotation {
  public static boolean colorDropsEnabled = false;

  private ColorPromises() {
  }

  private static final ColorPromises instance = new ColorPromises();

  public static final IPromiseAnnotation getInstance() {
    return instance;
  }

  static SlotInfo<IRSequence<IRNode>> declsSI;

  static SlotInfo<IRSequence<IRNode>> grantSI;

  static SlotInfo<IRSequence<IRNode>> revokeSI;

  static SlotInfo<IRSequence<IRNode>> reqSI;

  static SlotInfo<IRSequence<IRNode>> contextSI;

  static SlotInfo<IRSequence<IRNode>> incompatSI;

  static SlotInfo<Boolean> transparentSI;
  
  static SlotInfo<IRSequence<IRNode>> renamesSI;
  
  static SlotInfo<IRNode> cardinalitySI;
  
  static SlotInfo<IRSequence<IRNode>> importSI;
  
  static SlotInfo<IRSequence<IRNode>> regionColorDeclSI;
  
  static SlotInfo<IRSequence<IRNode>> colorizedSI;
  
//  private static SlotInfo<TRoleImportDrop> importDropSI = SimpleSlotFactory.prototype.newAttribute(null);

  private static SlotInfo<Set<TRoleImportDrop>> importDropSetSI = SimpleSlotFactory.prototype.newAttribute(null);

  //private static SlotInfo renameDropSI = SimpleSlotFactory.prototype.newAttribute(null);

  private static SlotInfo<TRoleDeclareDrop> declDropSI = SimpleSlotFactory.prototype
      .newAttribute(null);
  private static SlotInfo<Set<TRoleDeclareDrop>> declDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<TRoleGrantDrop> grantDropSI = SimpleSlotFactory.prototype
      .newAttribute(null);
  private static SlotInfo<Set<TRoleGrantDrop>> grantDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<TRoleRevokeDrop> revokeDropSI = SimpleSlotFactory.prototype
      .newAttribute(null);
  private static SlotInfo<Set<TRoleRevokeDrop>> revokeDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<TRoleIncompatibleDrop> incompDropSI = SimpleSlotFactory.prototype
      .newAttribute(null);
  private static SlotInfo<Set<TRoleIncompatibleDrop>> incompDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<TRoleRequireDrop> reqDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<Set<TRoleRequireDrop>> reqDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<ColorContextDrop> contextDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<Set<ColorContextDrop>> contextDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<Set<TRoleRequireDrop>> reqInheritDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<Set<ColorContextDrop>> contextInheritDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<TRoleReqSummaryDrop> reqSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<TRoleCtxSummaryDrop> ctxSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<TRoleReqSummaryDrop> reqInheritSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<TRoleCtxSummaryDrop> ctxInheritSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<SimpleCallGraphDrop> simpleCGDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<TRoleReqSummaryDrop> regionColorDeclDropSI = 
    SimpleSlotFactory.prototype.newAttribute(null);
  private static SlotInfo<Set<RegionTRoleDeclDrop>> regionColorDeclDropSetSI = 
    SimpleSlotFactory.prototype.newAttribute(null);
  
//  private static SlotInfo colorizedDropSI =
//    SimpleSlotFactory.prototype.newAttribute(null);
  
  private static SlotInfo<Boolean> colorImportsProcessedSI =
    SimpleSlotFactory.prototype.newAttribute(Boolean.FALSE);

  private static SlotInfo<Boolean> colorRenamesProcessedSI =
    SimpleSlotFactory.prototype.newAttribute(Boolean.FALSE);
  
  private static SlotInfo<Boolean> colorStructureBuiltSI = 
    SimpleSlotFactory.prototype.newAttribute(Boolean.FALSE);
  
  private static Collection<String> getNames(Iterator<IRNode> nameEnum) {
    Collection<String> res = new HashSet<String>(1);
    while (nameEnum.hasNext()) {
      final String id = ColorName.getId(nameEnum.next());
      res.add(id);
    }
    return res;
  }

  private static CUDrop getCUDropOf(IRNode node) {
    if (node == null) return null;
    try {
      Operator op = JJNode.tree.getOperator(node);
      IRNode cu;
      if (CompilationUnit.prototype.includes(op)) {
        cu = node;
      } else {
        cu = VisitUtil.getEnclosingCompilationUnit(node);
      }
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
  
  /**
   * Process the tree for a requiresColor annotation into canonical form. That
   * form is a top-level collection of terms being ORd together. The terms are
   * each collections containing terms being ANDd together. Thus, the simple
   * case (for
   * 
   * @requiresColor foo) has a top-level collection with a single element. That
   *                element is a collection that contains a single
   *                ColorNameEntry (for foo).
   * @param node
   *          The root of the requiresColor promise tree.
   * @return A collection as specified above. Can be a single-level empty
   *         collection if there is an error.
   */
  static private TRExpr buildTRExpr(IRNode node, IRNode where) {
    TRExpr res = null;

    if (node == null) {
      LOG.warning("node was null, but should not have been");
      return null;
    }

    //    if (JJNode.tree.numChildren(node) != 1) {
    //      // we have a scary problem. just return res for the moment!
    //      LOG.severe("RequiresColor with too many children!");
    //      return new ArrayList(0);
    //    }

    final Operator op = JJNode.tree.getOperator(node);
    boolean first = true;
    if (ColorOr.prototype.includes(op)) {
      Iterator<IRNode> orEnum = ColorOr.getOrElemsIterator(node);
      while (orEnum.hasNext()) {
        IRNode n = orEnum.next();
        TRExpr t = buildTRExpr(n, where);
        if (first) {
          res = t;
          first = false;
        } else {
          res = TRBinExpr.cOr(res, t);
        }
      }
    } else if (ColorAnd.prototype.includes(op)) {
      Iterator<IRNode> andEnum = ColorAnd.getAndElemsIterator(node);
      while (andEnum.hasNext()) {
        IRNode n = andEnum.next();
        TRExpr t = buildTRExpr(n, where);
        if (first) {
          res = t;
          first = false;
        } else {
          res = TRBinExpr.cAnd(res, t);
        }
      }
    } else if (ColorAndParen.prototype.includes(op)) {
      Iterator<IRNode> andEnum = ColorAndParen.getAndElemsIterator(node);
      while (andEnum.hasNext()) {
        IRNode n = andEnum.next();
        TRExpr t = buildTRExpr(n, where);
        if (first) {
          res = t;
          first = false;
        } else {
          res = TRBinExpr.cAndParen(res, t);
        }
      }
    } else if (ColorName.prototype.includes(op)) {
      final String name = ColorName.getId(node);
      res = new TRLeafExpr(TRoleNameModel.getInstance(name, where));
    } else if (ColorNot.prototype.includes(op)) {
      final String name = ColorName.getId(ColorNot.getTarget(node));
      res = TRUnaryExpr.trNot(new TRLeafExpr(TRoleNameModel.getInstance(name, where)));
    } else {
      LOG.severe("colorConstraint did not have one of ColorName, "
          + "ColorNot, ColorAnd, ColorOr");
    }
    return res;
  }
  
  public static Set<TRoleDeclareDrop> getMutableColorDeclSet(IRNode forNode) {
    return getMutableSet(forNode, declDropSetSI);
  }
  /**
   * Add a Color declaration node to the list of Colors for this Color
   * declaration node. It does not check to see this Color declaration node is
   * already in the list.
   */
  public static void addColorDecl(IRNode toThisNode, IRNode theDeclPromise) {
    if (!colorDropsEnabled) return;
    // theDeclPromise is an IRNode that is a TRoleDeclare. Its single child
    // is a ColorNames node.
    addToSeq_mapped(declsSI, toThisNode, theDeclPromise);
    Collection<String> names = getNames(ColorDeclaration.getColorIterator(theDeclPromise));
    Collection<String> processedNames = new ArrayList<String>(names.size());
    final Operator op = JJNode.tree.getOperator(toThisNode);

    for (String aName: names) {
      final String simpleName = JavaNames.genSimpleName(aName);
      final String qualName;
      if (op instanceof TypeDeclInterface) {
        qualName = JavaNames.getFullTypeName(toThisNode) + '.' + simpleName;
      } else { 
        qualName = JavaNames.genPackageQualifier(toThisNode) + simpleName;
      }
      processedNames.add(qualName);
    }
    //Next code line has bogus null argument so we can compile for now.  This is OLD-STYLE 
    // promise-building code that cannot work correctly anyway and must be replaced!
    // DFS 2010/05/18
    TRoleDeclareDrop declDrop = new TRoleDeclareDrop(null);
    theDeclPromise.setSlotValue(declDropSI, declDrop);
    getMutableColorDeclSet(toThisNode).add(declDrop);
  }
  
  public static void addColorDecl(IRNode toThisNode, TRoleDeclareDrop cdDrop) {
    if (!colorDropsEnabled) return;
    getMutableColorDeclSet(toThisNode).add(cdDrop);
  }

//  private static TRoleDeclareDrop getColorDecl(IRNode node) {
//    return  node.getSlotValue(declDropSI);
//  }

  public static Collection<TRoleDeclareDrop> getColorDecls(IRNode node) {
    return getCopyOfMutableSet(node, declDropSetSI);
  }

  
  public static Set<TRoleImportDrop> getMutableTRoleImportSet(IRNode forNode) {
    return getMutableSet(forNode, importDropSetSI);
  }
  
  /**
   * Add a Color Import node to the list of Colors for this 
   * node. It does not check to see whether this imported place is
   * already in the list.
   */
  public static void addTRoleImport(IRNode toThisNode, IRNode theDeclPromise) {
    if (!colorDropsEnabled) return;
    // theDeclPromise is an IRNode that is a TRoleDeclare. Its single child
    // is a ColorNames node.
    addToSeq_mapped(importSI, toThisNode, theDeclPromise);
    IRNode item = ColorImport.getItem(theDeclPromise);
    //Next code line has bogus null argument so we can compile for now.  This is OLD-STYLE 
    // promise-building code that cannot work correctly anyway and must be replaced!
    // DFS 2010/05/18
    TRoleImportDrop importDrop = new TRoleImportDrop(null);
//    theDeclPromise.setSlotValue(importDropSI, importDrop);
    getMutableTRoleImportSet(toThisNode).add(importDrop);
  }
  
  public static void addTRoleImport(IRNode toThisNode, TRoleImportDrop cdDrop) {
    if (!colorDropsEnabled) return;
    getMutableTRoleImportSet(toThisNode).add(cdDrop);
  }

//  private static TRoleImportDrop getTRoleImport(IRNode node) {
//    return (TRoleImportDrop) node.getSlotValue(importDropSI);
//  }

  public static Collection<TRoleImportDrop> getTRoleImports(IRNode node) {
    return getCopyOfMutableSet(node, importDropSetSI);
  }
  
  public static Set<TRoleGrantDrop> getMutableTRoleGrantSet(IRNode node) {
    return getMutableSet(node, grantDropSetSI);
  }
  
  /**
   * Add a TRoleGrant node and associated drop to the list of granted colors
   * attached to "toThisNode". Does not check to see whether thePromise has
   * previously been added to the list.
   */
  public static void addTRoleGrant(IRNode toThisNode, IRNode thePromise) {
    if (!colorDropsEnabled) return;
    // thePromise is an IRNode that is a TRoleDeclare. Its single child
    // is a TRoleNames node.
    addToSeq_mapped(grantSI, toThisNode, thePromise);
//    Collection<String> names = getNames(ColorGrant.getColorIterator(thePromise));
    //Next code line has bogus null argument so we can compile for now.  This is OLD-STYLE 
    // promise-building code that cannot work correctly anyway and must be replaced!
    // DFS 2010/05/18    
    TRoleGrantDrop declDrop = new TRoleGrantDrop(null);
    thePromise.setSlotValue(grantDropSI, declDrop);
    getMutableTRoleGrantSet(toThisNode).add(declDrop);
  }
  
  public static void addTRoleGrant(IRNode toThisNode, TRoleGrantDrop gDrop) {
    if (!colorDropsEnabled) return;
    getMutableTRoleGrantSet(toThisNode).add(gDrop);
  }

//  private static TRoleGrantDrop getTRoleGrant(IRNode node) {
//    return node.getSlotValue(grantDropSI);
//  }

  public static Collection<TRoleGrantDrop> getTRoleGrants(IRNode node) {
    return getCopyOfMutableSet(node, grantDropSetSI);
  }

  public static Set<TRoleRevokeDrop> getMutableColorRevokeSet(IRNode forNode) {
    return getMutableSet(forNode, revokeDropSetSI);
  }
  
  /**
   * Add a ColorRevoke node and associated drop to the list of granted colors
   * attached to "toThisNode". Does not check to see whether thePromise has
   * previously been added to the list.
   */
  public static void addColorRevoke(IRNode toThisNode, IRNode thePromise) {
    if (!colorDropsEnabled) return;
    // theDeclPromise is an IRNode that is a TRoleDeclare. Its single child
    // is a ColorNames node.
    addToSeq_mapped(revokeSI, toThisNode, thePromise);
    Collection<String> names = getNames(ColorRevoke.getColorIterator(thePromise));
    //Next code line has bogus null argument so we can compile for now.  This is OLD-STYLE 
    // promise-building code that cannot work correctly anyway and must be replaced!
    // DFS 2010/05/18
    TRoleRevokeDrop declDrop = new TRoleRevokeDrop(null);
    thePromise.setSlotValue(revokeDropSI, declDrop);
    getMutableColorRevokeSet(toThisNode).add(declDrop);
  }
  
  public static void addColorRevoke(IRNode toThisNode, TRoleRevokeDrop trrDrop) {
    if (!colorDropsEnabled) return;
    getMutableColorRevokeSet(toThisNode).add(trrDrop);
  }

//  private static TRoleRevokeDrop getColorRevoke(IRNode node) {
//    return  node.getSlotValue(revokeDropSI);
//  }

  public static Collection<TRoleRevokeDrop> getColorRevokes(IRNode node) {
    return getCopyOfMutableSet(node, revokeDropSetSI);
  }

  public static Set<TRoleIncompatibleDrop> getMutableColorIncompatibleSet(IRNode forNode) {
    return getMutableSet(forNode, incompDropSetSI);
  }
  
  /**
   * Add a ColorIncompatible node and associated drop to the list of incompatible 
   * color annos
   * attached to "toThisNode". Does not check to see whether thePromise has
   * previously been added to the list.
   */
  public static void addColorIncompatible(IRNode toThisNode, IRNode thePromise) {
    if (!colorDropsEnabled) return;
    // theDeclPromise is an IRNode that is a ColorIncompatible. Its single child
    // is a ColorNames node.
    addToSeq_mapped(incompatSI, toThisNode, thePromise);
    //Operator op = tree.getOperator(toThisNode);
    Collection<String> names = getNames(ColorIncompatible.getColorIterator(thePromise));
    //Next code line has bogus null argument so we can compile for now.  This is OLD-STYLE 
    // promise-building code that cannot work correctly anyway and must be replaced!
    // DFS 2010/05/18
    TRoleIncompatibleDrop declDrop = new TRoleIncompatibleDrop(null);
    
    thePromise.setSlotValue(incompDropSI, declDrop);
    getMutableColorIncompatibleSet(toThisNode).add(declDrop);
  }
  
  public static void addColorIncompatible(IRNode toThisNode, TRoleIncompatibleDrop ciDrop) {
    if (!colorDropsEnabled) return;
    getMutableColorIncompatibleSet(toThisNode).add(ciDrop);
  }

//  private static ColorIncompatibleDrop getColorIncompatible(IRNode node) {
//    return node.getSlotValue(incompDropSI);
//  }

  public static Collection<TRoleIncompatibleDrop> getColorIncompatibles(IRNode node) {
    return getCopyOfMutableSet(node, incompDropSetSI);
  }
  @SuppressWarnings("unused")
  public static void addColorized(IRNode toThisNode, IRNode thePromise, IBinder binder) {
    if (!colorDropsEnabled) return;
    addToSeq_mapped(colorizedSI, toThisNode, thePromise);
    IRNode temp = VisitUtil.getEnclosingType(toThisNode);
    final IRNode type = (temp != null) ? temp : toThisNode;
//    final IRNode body = VisitUtil.getClassBody(type);
    
    
      
    IRNode cRegions = ColorizedRegion.getCRegions(thePromise);
    Iterator<IRNode> specs = RegionSpecifications.getSpecsIterator(cRegions);
    while (specs.hasNext()) {
      final IRNode name  = specs.next();
      final String field = RegionName.getId(name);
//      final IRNode vdecl = BindUtil.findFieldInBody(body, field);
      
      final IRNode binding = binder.getBinding(name);
      final Operator bop = (binding != null) ? tree.getOperator(binding) : null;
      
      RegionModel aRegionDrop = RegionAnnotation.getRegionDrop(binding);
      RegionTRoleModel regTroleMod = 
        RegionTRoleModel.getRegionTRoleModel(aRegionDrop, binding);
      regTroleMod.setNodeAndCompilationUnitDependency(toThisNode);
    }
  }
  @SuppressWarnings("unused")
  public static void addRegionColorDecl(IRNode toThisNode, IRNode thePromise, IBinder binder) {
    if (!colorDropsEnabled) return;
    addToSeq_mapped(regionColorDeclSI, toThisNode, thePromise);
    
    IRNode temp = VisitUtil.getEnclosingType(toThisNode);
    final IRNode type = (temp != null) ? temp : toThisNode;
//    final IRNode body = VisitUtil.getClassBody(type);
    
    IRNode cRegions = ColorConstrainedRegions.getCRegions(thePromise);
    IRNode irExpr = ColorConstrainedRegions.getCSpec(thePromise);
    TRExpr expr = buildTRExpr(irExpr, toThisNode);
    
    final Operator ttnOp = tree.getOperator(toThisNode);
    
    Set<RegionTRoleDeclDrop> rcDecls = getMutableRegionColorDeclsSet(toThisNode);
    Iterator<IRNode> specs = RegionSpecifications.getSpecsIterator(cRegions);
    while (specs.hasNext()) {
      IRNode name = specs.next();
      final String field = RegionName.getId(name);
//    final IRNode vdecl = BindUtil.findFieldInBody(body, field);
    
    final IRNode binding = binder.getBinding(name);
    final Operator bop = (binding != null) ? tree.getOperator(binding) : null;
    
    RegionModel aRegionDrop = RegionAnnotation.getRegionDrop(binding);
//      RegionModel aRegionDrop = RegionAnnotation.getRegionDrop(vdecl);
      aRegionDrop.setCategory(JavaGlobals.THREAD_ROLE_CONSTRAINED_REGION_CAT);
      RegionTRoleDeclDrop rtrdDrop =
        RegionTRoleDeclDrop.buildRegionTRoleDecl(aRegionDrop.regionName,
                                               expr, toThisNode);
      rcDecls.add(rtrdDrop);
//      LOG.severe("added rtrd for region " + aRegionDrop.regionName + " to " + JavaNames.getFullTypeName(toThisNode));
    }    
  }
  
  public static Set<TRoleRequireDrop> getMutableRequiresColorSet(IRNode forNode) {
    return getMutableSet(forNode, reqDropSetSI);
  }
  
  public static Set<TRoleRequireDrop> getMutableInheritedRequiresSet(IRNode forNode) {
    return getMutableSet(forNode, reqInheritDropSetSI);
  }
  
  /**
   * Add a requiresColor node and associated drop to the list of req annos for this 
   * node. It does not check to see this Color declaration node is already in the 
   * list.
   */
  public static void addRequiresColorAnno(IRNode toThisNode, IRNode theReqPromise) {
    if (!colorDropsEnabled) return;
    // theReqPromise is an IRNode that is a ColorRequire. Its single child
    // is a ColorSpec node.
    addToSeq_mapped(reqSI, toThisNode, theReqPromise);
    IRNode irExpr = ColorRequire.getCSpec(theReqPromise);
    TRExpr expr = buildTRExpr(irExpr, toThisNode);
    //Next code line has bogus null argument so we can compile for now.  This is OLD-STYLE 
    // promise-building code that cannot work correctly anyway and must be replaced!
    // DFS 2010/05/18
    TRoleRequireDrop reqDrop = new TRoleRequireDrop(null);
    theReqPromise.setSlotValue(reqDropSI, reqDrop);
    getMutableRequiresColorSet(toThisNode).add(reqDrop);
  }
  
  public static void addInheritedRequireDrop(IRNode toThisNode, TRoleRequireDrop trrDrop) {
    if (!colorDropsEnabled) return;
    getMutableInheritedRequiresSet(toThisNode).add(trrDrop);
  }

//  private static TRoleRequireDrop getReqDrop(IRNode node) {
//    return node.getSlotValue(reqDropSI);
//  }

  public static Collection<TRoleRequireDrop> getReqDrops(IRNode node) {
    final Set<TRoleRequireDrop> mrcs = getMutableRequiresColorSet(node);
    Collection<TRoleRequireDrop> res = new HashSet<TRoleRequireDrop>(mrcs.size());
    res.addAll(mrcs);
    return res;
  }
  
  public static Collection<TRoleRequireDrop> getInheritedRequireDrops(IRNode node) {
    return getCopyOfMutableSet(node, reqInheritDropSetSI);
  }
  
  public static TRoleReqSummaryDrop getReqSummDrop(IRNode node) {
    return node.getSlotValue(reqSummDropSI);
  }
  
  public static void setReqSummDrop(IRNode node, TRoleReqSummaryDrop summ) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(reqSummDropSI, summ);
    summ.setAttachedTo(node, reqSummDropSI);
  }
  
  public static TRoleReqSummaryDrop getInheritedReqSummDrop(IRNode node) {
    return node.getSlotValue(reqInheritSummDropSI);
  }
  
  public static void setInheritedReqSummDrop(IRNode node, TRoleReqSummaryDrop summ) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(reqInheritSummDropSI, summ);
    summ.setAttachedTo(node, reqInheritSummDropSI);
  }

  public static TRoleCtxSummaryDrop getCtxSummDrop(IRNode node) {
    return node.getSlotValue(ctxSummDropSI);
  }
  
  public static void setCtxSummDrop(IRNode node, TRoleCtxSummaryDrop summ) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(ctxSummDropSI, summ);
    summ.setAttachedTo(node, ctxSummDropSI);
  }
  
  public static TRoleCtxSummaryDrop getInheritedCtxSummDrop(IRNode node) {
    return node.getSlotValue(ctxInheritSummDropSI);
  }
  
  public static void setInheritedCtxSummDrop(IRNode node, TRoleCtxSummaryDrop summ) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(ctxInheritSummDropSI, summ);
    summ.setAttachedTo(node, ctxInheritSummDropSI);
  }
  
  public static SimpleCallGraphDrop getCGDrop(IRNode node) {
    return node.getSlotValue(simpleCGDropSI);
  }
  
  public static void setCGDrop(IRNode node, SimpleCallGraphDrop cgDrop) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(simpleCGDropSI, cgDrop);
    cgDrop.setAttachedTo(node, simpleCGDropSI);
  }

  public static TRoleReqSummaryDrop getDataColorReqDrop(IRNode node) {
    return node.getSlotValue(regionColorDeclDropSI);
  }
  
  public static void setDataColorReqDrop(IRNode node, TRoleReqSummaryDrop rDrop) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(regionColorDeclDropSI, rDrop);
    rDrop.setAttachedTo(node, regionColorDeclDropSI);
  }
  
  private static <T extends Drop> Set<T> getMutableSet(IRNode forNode, SlotInfo<Set<T>> si) {
    Set<T> result = forNode.getSlotValue(si);
    if (result == null) {
      result = new HashSet<T>();
      forNode.setSlotValue(si, result);
    }
    return result;
  }
  
  private static <T extends Drop> Set<T> getCopyOfMutableSet(IRNode forNode, SlotInfo<Set<T>> si) {
    Set<T> current = getMutableSet(forNode, si);
    if (current.size() == 0) {
      return new HashSet<T>(0);
    }
    Set<T> result = new HashSet<T>(current.size());
    Iterator<T> currIter = current.iterator();
    while (currIter.hasNext()) {
      T dr = currIter.next();
      if (dr.isValid()) {
        result.add(dr);
      }
    }
    if (result.size() < current.size()) {
      // we must have skipped over some invalid entries.  update the saved
      // set.
      current = new HashSet<T>(result.size());
      current.addAll(result);
      forNode.setSlotValue(si, current);
    }
    return result;
  }
  
  
  /** Remove all invalid drops from a MutableXXXSet.  Do this by building a new set
   * and transferring only valid drops from old to newSet.  Finish by installing
   * the new set as the mutableXXXSet for node.
   * 
   * @param node the node whose set should be updated
   * @param si the SlotInfo to get the set from.
   */
  private static <T extends Drop> void purgeMutableSet(IRNode node, SlotInfo<Set<T>> si) {
    Set<T> old = getMutableSet(node, si);
    final int newSize = Math.max(old.size()-1, 0);
    Set<T> newSet = new HashSet<T>(newSize);
    Iterator<T> oldIter = old.iterator();
    while (oldIter.hasNext()) {
      T dr = oldIter.next();
      if (dr.isValid()) {
        newSet.add(dr);
      }
    }
    node.setSlotValue(si, newSet);
  }
  
  public static Set<ColorContextDrop> getMutableColorContextSet(IRNode forNode) {
    return getMutableSet(forNode, contextDropSetSI);
  }
  
  public static Set<RegionTRoleDeclDrop> getMutableRegionColorDeclsSet(IRNode forNode) {
    return getMutableSet(forNode, regionColorDeclDropSetSI);
  }
  
  public static Set<ColorContextDrop> getMutableInheritedContextSet(IRNode forNode) {
    return getMutableSet(forNode, contextInheritDropSI);
  }
  
  public static void addColorContextAnno(IRNode toThisNode, IRNode theCtxPromise) {
    if (!colorDropsEnabled) return;
    addToSeq_mapped(contextSI, toThisNode, theCtxPromise);
    IRNode irExpr = ColorContext.getCSpec(theCtxPromise);
    TRExpr expr = buildTRExpr(irExpr, toThisNode);
    ColorContextDrop ctxDrop = new ColorContextDrop(expr, toThisNode);
    theCtxPromise.setSlotValue(contextDropSI, ctxDrop);
    getMutableColorContextSet(toThisNode).add(ctxDrop);
  }
  
  public static void addInheritedContextDrop(IRNode toThisNode, ColorContextDrop theDrop) {
    if (!colorDropsEnabled) return;
    getMutableInheritedContextSet(toThisNode).add(theDrop);
  }
  
//  private static ColorContextDrop getCtxDrop(IRNode node) {
//    return node.getSlotValue(contextDropSI);
//  }
  
  public static void addColorRename(IRNode toThisNode, IRNode value) {
    if (!colorDropsEnabled) return;
    addToSeq_mapped(renamesSI, toThisNode, value);
    
    final String  name = ColorName.getId(ColorRename.getColor(value));
    final IRNode irExpr = ColorRename.getCSpec(value);
    final TRExpr expr = buildTRExpr(irExpr, toThisNode);
    //Next code line has bogus null argument so we can compile for now.  This is OLD-STYLE 
    // promise-building code that cannot work correctly anyway and must be replaced!
    // DFS 2010/05/18
    final TRoleRenameDrop trrDrop = TRoleRenameDrop.buildTRoleRenameDrop(null);
    final IRNode myCu = VisitUtil.computeOutermostEnclosingTypeOrCU(toThisNode);
    final CUDrop cud = getCUDropOf(myCu);
    final TRoleRenamePerCU trp = TRoleRenamePerCU.getTRoleRenamePerCU(cud.cu);
    trp.addRename(trrDrop);
  }
  /**
   * @param node The node whose color context drops we want
   * @return a copy of the mutableColorContextSet.
   */
  public static Collection<ColorContextDrop> getCtxDrops(IRNode node) {
    return getCopyOfMutableSet(node, contextDropSetSI);
  }
  
  public static Collection<ColorContextDrop> getInheritedContextDrops(IRNode node) {
    return getCopyOfMutableSet(node, contextInheritDropSI);
  }
  
  /**
   * Add a Color node to the list of Colors for this Color note node. It does
   * not check to see this Color node is already in the list.
   */
  public static void addColorToNote(IRNode noteNode, IRNode colorNode) {
//    if (!colorDropsEnabled) return;
    LOG.severe("Color Note annotation is now obsolete!");
  }

  public static boolean isColorRelevant(IRNode node) {
    return !isXorFalse_filtered(transparentSI, node);
  }

  public static void settransparent(IRNode node, boolean notRelevant) {
    if (!colorDropsEnabled) return;
    setX_mapped(transparentSI, node, notRelevant);
  }
  
  public static boolean areTRoleImportsProcessed(IRNode node) {
    return isXorFalse_filtered(colorImportsProcessedSI, node);
  }
  
  public static void setTRoleImportsProcessed(IRNode node, boolean processed) {
    setX_mapped(colorImportsProcessedSI, node, processed);
  }
  
  public static boolean areColorRenamesProcessed(IRNode node) {
    return isXorFalse_filtered(colorRenamesProcessedSI, node);
  }
  
  public static void setColorRenamesProcessed(IRNode node, boolean processed) {
    setX_mapped(colorRenamesProcessedSI, node, processed);
  }
  
  public static boolean isColorStructureBuilt(IRNode node) {
    return isXorFalse_filtered(colorStructureBuiltSI, node);
  }
  
  public static void setColorStructureBuilt(IRNode node, boolean processed) {
    setX_mapped(colorStructureBuiltSI, node, processed);
  }
  
//  public static void setColorRequired(IRNode node, IRNode val) {
//    if (!colorDropsEnabled) return;
//    setX_mapped(reqSI, node, val);
//  }
//
//  public static void setColorContext(IRNode node, IRNode val) {
//    if (!colorDropsEnabled) return;
//    setX_mapped(contextSI, node, val);
//  }

  public static void setColorCardinality(IRNode node, IRNode val) {
    if (!colorDropsEnabled) return;
    setX_mapped(cardinalitySI, node, val);
  }
  
  public static boolean hasColorCardinality(IRNode node) {
    return false;
  }
  
  public static boolean isColorCard1(IRNode node) {
    return false;
  }
  
  public static boolean isColorCardN(IRNode node) {
    return false;
  }
  /*
   * (non-Javadoc)
   * 
   * @see edu.cmu.cs.fluid.java.bind.AbstractPromiseAnnotation#getRules()
   */
  @Override
  protected IPromiseRule[] getRules() {
    return new IPromiseRule[] {
                               new AbstractPromiseStorageAndCheckRule<Boolean>(
                                   "Transparent", IPromiseStorage.BOOL,
                                   declOrConstructorOps) {

                                 public TokenInfo<Boolean> set(SlotInfo<Boolean> si) {
                                   transparentSI = si;
                                   return new TokenInfo<Boolean>("Color Transparent",
                                                        si, name);
                                 }
                               },
                               new Colors_ParseRule("Color", declOps) {

                                 public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
                                   declsSI = si;
                                   return new TokenInfo<IRSequence<IRNode>>("Declared colors", si,
                                                        "color");
                                 }
                                 @Override                                 
                                 protected boolean processResult(final IRNode n,
                                     final IRNode result,
                                     IPromiseParsedCallback cb) {
                                   addColorDecl(n, result);
                                   return true;
                                 }
                               },
                               new Colors_ParseRule("Grant", blockOps) {

                                 public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
                                   grantSI = si;
                                   return new TokenInfo<IRSequence<IRNode>>("Granted colors", si,
                                                        "grant");
                                 }
                                 @Override                                 
                                 protected boolean processResult(final IRNode n,
                                     final IRNode result,
                                     IPromiseParsedCallback cb) {
                                   addTRoleGrant(n, result);
                                   return true;
                                 }
                               },
                               new Colors_ParseRule("Revoke", blockOps) {

                                 public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
                                   revokeSI = si;
                                   return new TokenInfo<IRSequence<IRNode>>("Revoked colors", si,
                                                        "revoke");
                                 }
                                 @Override
                                 protected boolean processResult(final IRNode n,
                                     final IRNode result,
                                     IPromiseParsedCallback cb) {
                                   addColorRevoke(n, result);
                                   return true;
                                 }
                               },
                               new RequiresColor_ParseRule("ColorConstraint",
                                   methodDeclOps),
                               new ColorContext_ParseRule("ColorContext",
                                   methodDeclOps),
                               new Colors_ParseRule("IncompatibleColors",
                                   declOps) {

                                 public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
                                   incompatSI = si;
                                   return new TokenInfo<IRSequence<IRNode>>("incompatibleColors",
                                                        si,
                                                        "incompatibleColors");
                                 }
                                 @Override
                                 protected boolean processResult(final IRNode n,
                                     final IRNode result,
                                     IPromiseParsedCallback cb) {
                                   addColorIncompatible(n, result);
                                   return true;
                                 }
                               },

                               new BooleanTagRule("Transparent",
                                   declOrConstructorOps) {
                                 @Override
                                protected SlotInfo<Boolean> getSI() {
                                   return transparentSI;
                                 }
                               },
                               new TRoleImport_ParseRule("TRoleImport", 
                                                         packageTypeDeclOps),
                               new ColorRename_ParseRule("ColorRename",
                                                         packageTypeDeclOps),
                               new ColorCard_ParseRule("ColorCardinality",
                                                       packageTypeDeclOps),
                               new RegionColorize_ParseRule("ColorizedRegion",
                                                            typeDeclOps),
                               new RegionColor_ParseRule("ColorConstrainedRegions",
                                                         typeDeclOps),
    };
  }

  abstract class Colors_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {
    Colors_ParseRule(String tag, Operator[] ops) {
      super(tag, SEQ, false, ops, ops);
    }
  }

  static class RequiresColor_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {

    RequiresColor_ParseRule(String tag, Operator[] ops) {
      super(tag, SEQ, false, ops, ops);
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb) {
      addRequiresColorAnno(n, result);
      return true;
    }

    public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
      reqSI = si;
      return new TokenInfo<IRSequence<IRNode>>("Required colors", si, "colorConstraint");
    }
  }

  
  static class ColorContext_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {

    ColorContext_ParseRule(String tag, Operator[] ops) {
      super(tag, SEQ, false, ops, ops);
    }

    @Override
    protected boolean processResult(final IRNode n, final IRNode result,
        IPromiseParsedCallback cb) {
      addColorContextAnno(n, result);
      return true;
    }

    public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
      contextSI = si;
      return new TokenInfo<IRSequence<IRNode>>("Calling color context", si, "colorContext");
    }
  }
  static class TRoleImport_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {

    TRoleImport_ParseRule(String tag, Operator[] ops) {
      super(tag, SEQ, true, ops, ops);
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.promise.IPromiseStorage#set(edu.cmu.cs.fluid.ir.SlotInfo)
     */
    public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
      importSI = si;
      return new TokenInfo<IRSequence<IRNode>>("Color Import", si, "colorImport");
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.promise.parse.AbstractPromiseParserRule#processResult(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.promise.IPromiseParsedCallback)
     */
    @Override
    protected boolean processResult(IRNode n, IRNode result,
        IPromiseParsedCallback cb) {
      addTRoleImport(n, result);
      return true;
    } 
  }
  
  static class ColorRename_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {
    
    ColorRename_ParseRule(String tag, Operator[] ops) {
      super(tag, SEQ, false, ops, ops);
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.promise.IPromiseStorage#set(edu.cmu.cs.fluid.ir.SlotInfo)
     */
    public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
      renamesSI = si;
      return new TokenInfo<IRSequence<IRNode>>("Color Renames", si, "ColorRename");
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.promise.parse.AbstractPromiseParserRule#processResult(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.promise.IPromiseParsedCallback)
     */
    @Override
    protected boolean processResult(IRNode n, IRNode result,
        IPromiseParsedCallback cb) {
      addColorRename(n, result);
      return true;
    }
  }
  
  static class ColorCard_ParseRule extends AbstractPromiseParserCheckRule<IRNode> {
    ColorCard_ParseRule(String tag, Operator[] ops) {
      super(tag, NODE, false, ops, ops);
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.promise.IPromiseStorage#set(edu.cmu.cs.fluid.ir.SlotInfo)
     */
    public TokenInfo<IRNode> set(SlotInfo<IRNode> si) {
      cardinalitySI = si;
      return new TokenInfo<IRNode>("color Cardinality", si, "colorCardinality");
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.promise.parse.AbstractPromiseParserRule#processResult(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.promise.IPromiseParsedCallback)
     */
    @Override
    protected boolean processResult(IRNode n, IRNode result,
        IPromiseParsedCallback cb) {
      setColorCardinality(n, result);
      return true;
    }
  }

  class RegionColor_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {
    RegionColor_ParseRule(String tag, Operator[] ops) {
      super(tag, SEQ, false, ops, ops);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.promise.IPromiseStorage#set(edu.cmu.cs.fluid.ir.SlotInfo)
     */
    public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
      regionColorDeclSI = si;
      return new TokenInfo<IRSequence<IRNode>>("region Color Constraint", si, "colorConstrainedRegions");
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.promise.parse.AbstractPromiseParserRule#processResult(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.promise.IPromiseParsedCallback)
     */
    @Override
    protected boolean processResult(IRNode n, IRNode result,
        IPromiseParsedCallback cb) {
      addRegionColorDecl(n, result, binder);
      return true;
    } 
  }
  
  class RegionColorize_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {
    RegionColorize_ParseRule(String tag, Operator[] ops) {
      super(tag, SEQ, false, ops, ops);
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.promise.IPromiseStorage#set(edu.cmu.cs.fluid.ir.SlotInfo)
     */
    public TokenInfo<IRSequence<IRNode>> set(SlotInfo<IRSequence<IRNode>> si) {
      colorizedSI = si;
      return new TokenInfo<IRSequence<IRNode>>("colorized Region", si, "colorizedRegion");
    }
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.promise.parse.AbstractPromiseParserRule#processResult(edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.promise.IPromiseParsedCallback)
     */
    @Override
    protected boolean processResult(IRNode n, IRNode result,
        IPromiseParsedCallback cb) {
      addColorized(n, result, binder);
      return true;
    } 
  }
}