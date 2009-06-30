/*
 * Created on Oct 30, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.analysis.*;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.promise.*;
import edu.cmu.cs.fluid.promise.parse.BooleanTagRule;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
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
  
//  private static SlotInfo<ColorImportDrop> importDropSI = SimpleSlotFactory.prototype.newAttribute(null);

  private static SlotInfo<Set<ColorImportDrop>> importDropSetSI = SimpleSlotFactory.prototype.newAttribute(null);

  //private static SlotInfo renameDropSI = SimpleSlotFactory.prototype.newAttribute(null);

  private static SlotInfo<ColorDeclareDrop> declDropSI = SimpleSlotFactory.prototype
      .newAttribute(null);
  private static SlotInfo<Set<ColorDeclareDrop>> declDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<ColorGrantDrop> grantDropSI = SimpleSlotFactory.prototype
      .newAttribute(null);
  private static SlotInfo<Set<ColorGrantDrop>> grantDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<ColorRevokeDrop> revokeDropSI = SimpleSlotFactory.prototype
      .newAttribute(null);
  private static SlotInfo<Set<ColorRevokeDrop>> revokeDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<ColorIncompatibleDrop> incompDropSI = SimpleSlotFactory.prototype
      .newAttribute(null);
  private static SlotInfo<Set<ColorIncompatibleDrop>> incompDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);

  private static SlotInfo<ColorRequireDrop> reqDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<Set<ColorRequireDrop>> reqDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<ColorContextDrop> contextDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<Set<ColorContextDrop>> contextDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<Set<ColorRequireDrop>> reqInheritDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<Set<ColorContextDrop>> contextInheritDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<ColorReqSummaryDrop> reqSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<ColorCtxSummaryDrop> ctxSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<ColorReqSummaryDrop> reqInheritSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<ColorCtxSummaryDrop> ctxInheritSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<SimpleCallGraphDrop> simpleCGDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<ColorReqSummaryDrop> regionColorDeclDropSI = 
    SimpleSlotFactory.prototype.newAttribute(null);
  private static SlotInfo<Set<RegionColorDeclDrop>> regionColorDeclDropSetSI = 
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
  static private CExpr buildCExpr(IRNode node, IRNode where) {
    CExpr res = null;

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
        CExpr t = buildCExpr(n, where);
        if (first) {
          res = t;
          first = false;
        } else {
          res = CBinExpr.cOr(res, t);
        }
      }
    } else if (ColorAnd.prototype.includes(op)) {
      Iterator<IRNode> andEnum = ColorAnd.getAndElemsIterator(node);
      while (andEnum.hasNext()) {
        IRNode n = andEnum.next();
        CExpr t = buildCExpr(n, where);
        if (first) {
          res = t;
          first = false;
        } else {
          res = CBinExpr.cAnd(res, t);
        }
      }
    } else if (ColorAndParen.prototype.includes(op)) {
      Iterator<IRNode> andEnum = ColorAndParen.getAndElemsIterator(node);
      while (andEnum.hasNext()) {
        IRNode n = andEnum.next();
        CExpr t = buildCExpr(n, where);
        if (first) {
          res = t;
          first = false;
        } else {
          res = CBinExpr.cAndParen(res, t);
        }
      }
    } else if (ColorName.prototype.includes(op)) {
      final String name = ColorName.getId(node);
      res = new CLeafExpr(ColorNameModel.getInstance(name, where));
    } else if (ColorNot.prototype.includes(op)) {
      final String name = ColorName.getId(ColorNot.getTarget(node));
      res = CUnaryExpr.cNot(new CLeafExpr(ColorNameModel.getInstance(name, where)));
    } else {
      LOG.severe("colorConstraint did not have one of ColorName, "
          + "ColorNot, ColorAnd, ColorOr");
    }
    return res;
  }
  
  public static Set<ColorDeclareDrop> getMutableColorDeclSet(IRNode forNode) {
    return getMutableSet(forNode, declDropSetSI);
  }
  /**
   * Add a Color declaration node to the list of Colors for this Color
   * declaration node. It does not check to see this Color declaration node is
   * already in the list.
   */
  public static void addColorDecl(IRNode toThisNode, IRNode theDeclPromise) {
    if (!colorDropsEnabled) return;
    // theDeclPromise is an IRNode that is a ColorDeclare. Its single child
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
    ColorDeclareDrop declDrop = new ColorDeclareDrop(processedNames, toThisNode);
    theDeclPromise.setSlotValue(declDropSI, declDrop);
    getMutableColorDeclSet(toThisNode).add(declDrop);
  }
  
  public static void addColorDecl(IRNode toThisNode, ColorDeclareDrop cdDrop) {
    if (!colorDropsEnabled) return;
    getMutableColorDeclSet(toThisNode).add(cdDrop);
  }

//  private static ColorDeclareDrop getColorDecl(IRNode node) {
//    return  node.getSlotValue(declDropSI);
//  }

  public static Collection<ColorDeclareDrop> getColorDecls(IRNode node) {
    return getCopyOfMutableSet(node, declDropSetSI);
  }

  
  public static Set<ColorImportDrop> getMutableColorImportSet(IRNode forNode) {
    return getMutableSet(forNode, importDropSetSI);
  }
  
  /**
   * Add a Color Import node to the list of Colors for this 
   * node. It does not check to see whether this imported place is
   * already in the list.
   */
  public static void addColorImport(IRNode toThisNode, IRNode theDeclPromise) {
    if (!colorDropsEnabled) return;
    // theDeclPromise is an IRNode that is a ColorDeclare. Its single child
    // is a ColorNames node.
    addToSeq_mapped(importSI, toThisNode, theDeclPromise);
    IRNode item = ColorImport.getItem(theDeclPromise);
    ColorImportDrop importDrop = new ColorImportDrop(toThisNode, item);
//    theDeclPromise.setSlotValue(importDropSI, importDrop);
    getMutableColorImportSet(toThisNode).add(importDrop);
  }
  
  public static void addColorImport(IRNode toThisNode, ColorImportDrop cdDrop) {
    if (!colorDropsEnabled) return;
    getMutableColorImportSet(toThisNode).add(cdDrop);
  }

//  private static ColorImportDrop getColorImport(IRNode node) {
//    return (ColorImportDrop) node.getSlotValue(importDropSI);
//  }

  public static Collection<ColorImportDrop> getColorImports(IRNode node) {
    return getCopyOfMutableSet(node, importDropSetSI);
  }
  
  public static Set<ColorGrantDrop> getMutableColorGrantSet(IRNode node) {
    return getMutableSet(node, grantDropSetSI);
  }
  
  /**
   * Add a ColorGrant node and associated drop to the list of granted colors
   * attached to "toThisNode". Does not check to see whether thePromise has
   * previously been added to the list.
   */
  public static void addColorGrant(IRNode toThisNode, IRNode thePromise) {
    if (!colorDropsEnabled) return;
    // theDeclPromise is an IRNode that is a ColorDeclare. Its single child
    // is a ColorNames node.
    addToSeq_mapped(grantSI, toThisNode, thePromise);
    Collection<String> names = getNames(ColorGrant.getColorIterator(thePromise));
    ColorGrantDrop declDrop = new ColorGrantDrop(names, toThisNode);
    thePromise.setSlotValue(grantDropSI, declDrop);
    getMutableColorGrantSet(toThisNode).add(declDrop);
  }
  
  public static void addColorGrant(IRNode toThisNode, ColorGrantDrop gDrop) {
    if (!colorDropsEnabled) return;
    getMutableColorGrantSet(toThisNode).add(gDrop);
  }

//  private static ColorGrantDrop getColorGrant(IRNode node) {
//    return node.getSlotValue(grantDropSI);
//  }

  public static Collection<ColorGrantDrop> getColorGrants(IRNode node) {
    return getCopyOfMutableSet(node, grantDropSetSI);
  }

  public static Set<ColorRevokeDrop> getMutableColorRevokeSet(IRNode forNode) {
    return getMutableSet(forNode, revokeDropSetSI);
  }
  
  /**
   * Add a ColorRevoke node and associated drop to the list of granted colors
   * attached to "toThisNode". Does not check to see whether thePromise has
   * previously been added to the list.
   */
  public static void addColorRevoke(IRNode toThisNode, IRNode thePromise) {
    if (!colorDropsEnabled) return;
    // theDeclPromise is an IRNode that is a ColorDeclare. Its single child
    // is a ColorNames node.
    addToSeq_mapped(revokeSI, toThisNode, thePromise);
    Collection<String> names = getNames(ColorRevoke.getColorIterator(thePromise));
    ColorRevokeDrop declDrop = new ColorRevokeDrop(names, toThisNode);
    thePromise.setSlotValue(revokeDropSI, declDrop);
    getMutableColorRevokeSet(toThisNode).add(declDrop);
  }
  
  public static void addColorRevoke(IRNode toThisNode, ColorRevokeDrop crDrop) {
    if (!colorDropsEnabled) return;
    getMutableColorRevokeSet(toThisNode).add(crDrop);
  }

//  private static ColorRevokeDrop getColorRevoke(IRNode node) {
//    return  node.getSlotValue(revokeDropSI);
//  }

  public static Collection<ColorRevokeDrop> getColorRevokes(IRNode node) {
    return getCopyOfMutableSet(node, revokeDropSetSI);
  }

  public static Set<ColorIncompatibleDrop> getMutableColorIncompatibleSet(IRNode forNode) {
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
    ColorIncompatibleDrop declDrop = new ColorIncompatibleDrop(names,
                                                               toThisNode);
    
    thePromise.setSlotValue(incompDropSI, declDrop);
    getMutableColorIncompatibleSet(toThisNode).add(declDrop);
  }
  
  public static void addColorIncompatible(IRNode toThisNode, ColorIncompatibleDrop ciDrop) {
    if (!colorDropsEnabled) return;
    getMutableColorIncompatibleSet(toThisNode).add(ciDrop);
  }

//  private static ColorIncompatibleDrop getColorIncompatible(IRNode node) {
//    return node.getSlotValue(incompDropSI);
//  }

  public static Collection<ColorIncompatibleDrop> getColorIncompatibles(IRNode node) {
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
      ColorizedRegionModel crm = 
        ColorizedRegionModel.getColorizedRegionModel(aRegionDrop, binding);
      crm.setNodeAndCompilationUnitDependency(toThisNode);
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
    CExpr expr = buildCExpr(irExpr, toThisNode);
    
    final Operator ttnOp = tree.getOperator(toThisNode);
    
    Set<RegionColorDeclDrop> rcDecls = getMutableRegionColorDeclsSet(toThisNode);
    Iterator<IRNode> specs = RegionSpecifications.getSpecsIterator(cRegions);
    while (specs.hasNext()) {
      IRNode name = specs.next();
      final String field = RegionName.getId(name);
//    final IRNode vdecl = BindUtil.findFieldInBody(body, field);
    
    final IRNode binding = binder.getBinding(name);
    final Operator bop = (binding != null) ? tree.getOperator(binding) : null;
    
    RegionModel aRegionDrop = RegionAnnotation.getRegionDrop(binding);
//      RegionModel aRegionDrop = RegionAnnotation.getRegionDrop(vdecl);
      aRegionDrop.setCategory(JavaGlobals.COLOR_CONSTRAINED_REGION_CAT);
      RegionColorDeclDrop rcdDrop =
        RegionColorDeclDrop.buildRegionColorDecl(aRegionDrop.regionName,
                                               expr, toThisNode);
      rcDecls.add(rcdDrop);
//      LOG.severe("added rcd for region " + aRegionDrop.regionName + " to " + JavaNames.getFullTypeName(toThisNode));
    }    
  }
  
  public static Set<ColorRequireDrop> getMutableRequiresColorSet(IRNode forNode) {
    return getMutableSet(forNode, reqDropSetSI);
  }
  
  public static Set<ColorRequireDrop> getMutableInheritedRequiresSet(IRNode forNode) {
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
    CExpr expr = buildCExpr(irExpr, toThisNode);
    ColorRequireDrop reqDrop = new ColorRequireDrop(expr, toThisNode);
    theReqPromise.setSlotValue(reqDropSI, reqDrop);
    getMutableRequiresColorSet(toThisNode).add(reqDrop);
  }
  
  public static void addInheritedRequireDrop(IRNode toThisNode, ColorRequireDrop crDrop) {
    if (!colorDropsEnabled) return;
    getMutableInheritedRequiresSet(toThisNode).add(crDrop);
  }

//  private static ColorRequireDrop getReqDrop(IRNode node) {
//    return node.getSlotValue(reqDropSI);
//  }

  public static Collection<ColorRequireDrop> getReqDrops(IRNode node) {
    final Set<ColorRequireDrop> mrcs = getMutableRequiresColorSet(node);
    Collection<ColorRequireDrop> res = new HashSet<ColorRequireDrop>(mrcs.size());
    res.addAll(mrcs);
    return res;
  }
  
  public static Collection<ColorRequireDrop> getInheritedRequireDrops(IRNode node) {
    return getCopyOfMutableSet(node, reqInheritDropSetSI);
  }
  
  public static ColorReqSummaryDrop getReqSummDrop(IRNode node) {
    return node.getSlotValue(reqSummDropSI);
  }
  
  public static void setReqSummDrop(IRNode node, ColorReqSummaryDrop summ) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(reqSummDropSI, summ);
    summ.setAttachedTo(node, reqSummDropSI);
  }
  
  public static ColorReqSummaryDrop getInheritedReqSummDrop(IRNode node) {
    return node.getSlotValue(reqInheritSummDropSI);
  }
  
  public static void setInheritedReqSummDrop(IRNode node, ColorReqSummaryDrop summ) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(reqInheritSummDropSI, summ);
    summ.setAttachedTo(node, reqInheritSummDropSI);
  }

  public static ColorCtxSummaryDrop getCtxSummDrop(IRNode node) {
    return node.getSlotValue(ctxSummDropSI);
  }
  
  public static void setCtxSummDrop(IRNode node, ColorCtxSummaryDrop summ) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(ctxSummDropSI, summ);
    summ.setAttachedTo(node, ctxSummDropSI);
  }
  
  public static ColorCtxSummaryDrop getInheritedCtxSummDrop(IRNode node) {
    return node.getSlotValue(ctxInheritSummDropSI);
  }
  
  public static void setInheritedCtxSummDrop(IRNode node, ColorCtxSummaryDrop summ) {
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

  public static ColorReqSummaryDrop getDataColorReqDrop(IRNode node) {
    return node.getSlotValue(regionColorDeclDropSI);
  }
  
  public static void setDataColorReqDrop(IRNode node, ColorReqSummaryDrop rDrop) {
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
  
  public static Set<RegionColorDeclDrop> getMutableRegionColorDeclsSet(IRNode forNode) {
    return getMutableSet(forNode, regionColorDeclDropSetSI);
  }
  
  public static Set<ColorContextDrop> getMutableInheritedContextSet(IRNode forNode) {
    return getMutableSet(forNode, contextInheritDropSI);
  }
  
  public static void addColorContextAnno(IRNode toThisNode, IRNode theCtxPromise) {
    if (!colorDropsEnabled) return;
    addToSeq_mapped(contextSI, toThisNode, theCtxPromise);
    IRNode irExpr = ColorContext.getCSpec(theCtxPromise);
    CExpr expr = buildCExpr(irExpr, toThisNode);
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
    final CExpr expr = buildCExpr(irExpr, toThisNode);
    final ColorRenameDrop crDrop = ColorRenameDrop.buildColorRenameDrop(name, expr, toThisNode);
    final IRNode myCu = VisitUtil.computeOutermostEnclosingTypeOrCU(toThisNode);
    final CUDrop cud = getCUDropOf(myCu);
    final ColorRenamePerCU crp = ColorRenamePerCU.getColorRenamePerCU(cud.cu);
    crp.addRename(crDrop);
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
  
  public static boolean areColorImportsProcessed(IRNode node) {
    return isXorFalse_filtered(colorImportsProcessedSI, node);
  }
  
  public static void setColorImportsProcessed(IRNode node, boolean processed) {
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
                                   addColorGrant(n, result);
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
                               new ColorImport_ParseRule("ColorImport", 
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
  static class ColorImport_ParseRule extends AbstractPromiseParserCheckRule<IRSequence<IRNode>> {

    ColorImport_ParseRule(String tag, Operator[] ops) {
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
      addColorImport(n, result);
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