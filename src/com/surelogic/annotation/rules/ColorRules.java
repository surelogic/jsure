/*$Header$*/
package com.surelogic.annotation.rules;

import java.util.*;

import org.antlr.runtime.RecognitionException;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.DefaultSLColorAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLColorAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.PromiseDropSeqStorage;
import com.surelogic.promise.SinglePromiseDropStorage;
import com.surelogic.sea.drops.callgraph.SimpleCallGraphDrop;
import com.surelogic.sea.drops.colors.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;
import edu.cmu.cs.fluid.tree.Operator;

public class ColorRules extends AnnotationRules {
  public static final String TRANSPARENT = "Transparent";
  public static final String COLOR_CONSTRAINT = "ColorConstraint";
  public static final String COLOR_IMPORT = "ColorImport";
  public static final String COLOR_DECLARATION = "ColorDeclaration";
  public static final String COLOR_INCOMPATIBLE = "IncompatibleColors";
  public static final String COLOR_GRANT = "ColorGrant";
  public static final String COLOR_REVOKE = "ColorRevoke";
  public static final String COLOR_RENAME = "ColorRename";
  public static final String COLOR = "Color";
  public static final String COLOR_CARDINALITY = "ColorCardinality";
  public static final String COLORIZED_REGION = "ColorizedRegion";
  public static final String COLORCONSTRAINEDREGIONS = "ColorConstrainedRegions";
  
  public static boolean colorDropsEnabled;

  private static final AnnotationRules instance = new ColorRules();
  
  private static final Transparent_ParseRule transparentRule = new Transparent_ParseRule();
  private static final ColorConstraint_ParseRule colorConstraintRule = new ColorConstraint_ParseRule();
  private static final ColorImport_ParseRule colorImportRule = new ColorImport_ParseRule();
  private static final ColorDeclare_ParseRule colorDeclarationRule = new ColorDeclare_ParseRule();
  private static final ColorIncompatible_ParseRule colorIncompatibleRule = new ColorIncompatible_ParseRule();
  private static final ColorGrant_ParseRule colorGrantRule = new ColorGrant_ParseRule();
  private static final ColorRevoke_ParseRule colorRevokeRule = new ColorRevoke_ParseRule();
  private static final ColorRename_ParseRule colorRenameRule = new ColorRename_ParseRule();
  private static final Color_ParseRule colorRule = new Color_ParseRule();
//  private static final ColorCardinality_ParseRule colorCardinalityRule = new ColorCardinality_ParseRule();
//  private static final ColorizedRegion_ParseRule colorizedRegionRule = new ColorizedRegion_ParseRule();
//  private static final ColorConstrainedRegion_ParseRule colorConstrainedRegionRule = new ColorConstrainedRegion_ParseRule();
  
  
  private static SlotInfo<Set<ColorRequireDrop>> reqInheritDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
   
  private static SlotInfo<ColorReqSummaryDrop> reqSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  private static SlotInfo<ColorCtxSummaryDrop> ctxSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<ColorReqSummaryDrop> reqInheritSummDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<SimpleCallGraphDrop> simpleCGDropSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  private static SlotInfo<ColorReqSummaryDrop> regionColorDeclDropSI = 
    SimpleSlotFactory.prototype.newAttribute(null);
  private static SlotInfo<Set<RegionColorDeclDrop>> regionColorDeclDropSetSI = 
    SimpleSlotFactory.prototype.newAttribute(null);
  
  private static SlotInfo<Set<ColorRequireDrop>> reqDropSetSI = SimpleSlotFactory.prototype
  .newAttribute(null);
  
  
//  private static SlotInfo colorizedDropSI =
//    SimpleSlotFactory.prototype.newAttribute(null);
  

  
  
  
  
  public static AnnotationRules getInstance() {
    return instance;
  }
  @Override
  public void register(PromiseFramework fw) {
    registerParseRuleStorage(fw, transparentRule);
    registerParseRuleStorage(fw, colorConstraintRule);
    registerParseRuleStorage(fw, colorImportRule);
    registerParseRuleStorage(fw, colorDeclarationRule);
    registerParseRuleStorage(fw, colorIncompatibleRule);
    registerParseRuleStorage(fw, colorGrantRule);
    registerParseRuleStorage(fw, colorRevokeRule);
    registerParseRuleStorage(fw, colorRenameRule);
    registerParseRuleStorage(fw, colorRule);
//    registerParseRuleStorage(fw, colorImportRule);
//    registerParseRuleStorage(fw, colorImportRule);
//    registerParseRuleStorage(fw, colorImportRule);
  }

  static class Transparent_ParseRule extends
      DefaultSLColorAnnotationParseRule<TransparentNode, TransparentPromiseDrop> {
    protected Transparent_ParseRule() {
      super(TRANSPARENT, methodDeclOps, TransparentNode.class);
    }

    @Override
    protected IAASTRootNode makeAAST(int offset) throws Exception {
      return new TransparentNode(offset);
    }

    @Override
    protected Object parse(IAnnotationParsingContext context,
        SLColorAnnotationsParser parser) throws Exception, RecognitionException {
      return parser.colorTransparent().getTree();
    }

    @Override
    protected IPromiseDropStorage<TransparentPromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(),
          TransparentPromiseDrop.class);
    }

    @Override
    protected IAnnotationScrubber<TransparentNode> makeScrubber() {
      return new AbstractAASTScrubber<TransparentNode>(this) {
        @Override
        protected PromiseDrop<TransparentNode> makePromiseDrop(TransparentNode a) {
          TransparentPromiseDrop d = new TransparentPromiseDrop(a);
          return storeDropIfNotNull(getStorage(), a, d);
        }
      };
    }
  }
  
  static class ColorConstraint_ParseRule 
  extends DefaultSLColorAnnotationParseRule<ColorConstraintNode,
                                            ColorConstraintDrop> {
    protected ColorConstraint_ParseRule() {
      super(COLOR_CONSTRAINT, methodDeclOps, ColorConstraintNode.class);
    }

    @Override
    protected Object parse(IAnnotationParsingContext context,
        SLColorAnnotationsParser parser) throws Exception, RecognitionException {
      return parser.colorConstraint().getTree();
    }
    
    @Override
    protected IPromiseDropStorage<ColorConstraintDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), ColorConstraintDrop.class);
    }
    @Override
    protected IAnnotationScrubber<ColorConstraintNode> makeScrubber() {
      return new AbstractAASTScrubber<ColorConstraintNode>(this,  
                                                     ScrubberType.UNORDERED) {
        @Override
        protected PromiseDrop<ColorConstraintNode> makePromiseDrop(ColorConstraintNode a) {
          return storeDropIfNotNull(getStorage(), a, new ColorConstraintDrop(a));
        }
      };
    }
  }
  
  static class Color_ParseRule 
  extends DefaultSLColorAnnotationParseRule<ColorNode,ColorRequireDrop> {
    protected Color_ParseRule() {
      super(COLOR, methodDeclOps, ColorNode.class);
    }

    @Override
    protected Object parse(IAnnotationParsingContext context,
        SLColorAnnotationsParser parser) throws Exception, RecognitionException {
      return parser.color().getTree();
    }
    
    @Override
    protected IPromiseDropStorage<ColorRequireDrop> makeStorage() {
      return  SinglePromiseDropStorage.create(name(), ColorRequireDrop.class);
    }
    @Override
    protected IAnnotationScrubber<ColorNode> makeScrubber() {
      return new AbstractAASTScrubber<ColorNode>(this, ScrubberType.UNORDERED) {
        @Override
        protected ColorRequireDrop makePromiseDrop(ColorNode a) {
          return storeDropIfNotNull(getStorage(), a, new ColorRequireDrop(a));
        }
      };
    }
  }
  
  static class ColorImport_ParseRule 
  extends DefaultSLColorAnnotationParseRule<ColorImportNode,ColorImportDrop> {
    protected ColorImport_ParseRule() {
      super(COLOR_IMPORT, typeDeclOps, ColorImportNode.class);
    }

    @Override
    protected Object parse(IAnnotationParsingContext context,
        SLColorAnnotationsParser parser) throws Exception, RecognitionException {
      return parser.colorImport().getTree();
    }
    
    
    @Override
    protected IPromiseDropStorage<ColorImportDrop> makeStorage() {
      return PromiseDropSeqStorage.create(name(), ColorImportDrop.class);
    }

    @Override
    protected IAnnotationScrubber<ColorImportNode> makeScrubber() {
      return new AbstractAASTScrubber<ColorImportNode>(this,  
                                                       ScrubberType.UNORDERED) {
          @Override
          protected PromiseDrop<ColorImportNode> makePromiseDrop(ColorImportNode a) {
            return storeDropIfNotNull(getStorage(), a, 
                                      scrubColorImport(getContext(), 
                                                       a, 
                                                       new ColorImportDrop(a)));
          }
        };
    
    }
  }
  
  private static ColorImportDrop scrubColorImport(IAnnotationScrubberContext context,
      ColorImportNode a, ColorImportDrop result) {
    return result;
  }
  
  static abstract class ColorNameList_ParseRule<A extends ColorNameListNode, 
                                                P extends PromiseDrop<? super A>> 
   extends DefaultSLColorAnnotationParseRule<A, P> {
    Class<P> pcls;
    protected ColorNameList_ParseRule(String name, Operator[] ops, Class<A> dt, Class<P> pcl) {
      super(name, ops, dt);
      pcls = pcl;
    }


   
      @Override
      protected IPromiseDropStorage<P> makeStorage() {
        return PromiseDropSeqStorage.create(name(), pcls);
      }
  }
  
  static class ColorGrant_ParseRule extends ColorNameList_ParseRule<ColorGrantNode, ColorGrantDrop> {

    protected ColorGrant_ParseRule() {
      super(COLOR_GRANT, blockOps, ColorGrantNode.class, ColorGrantDrop.class);
    }
    
    @Override
    protected Object parse(IAnnotationParsingContext context,
                           SLColorAnnotationsParser parser) throws Exception,
        RecognitionException {
      return parser.colorGrant().getTree();
    }
    
    @Override
    protected IAnnotationScrubber<ColorGrantNode> makeScrubber() {
      return new AbstractAASTScrubber<ColorGrantNode>(this) {
        @Override
        protected ColorGrantDrop makePromiseDrop(ColorGrantNode a) {
          ColorGrantDrop d = new ColorGrantDrop(a);
          return storeDropIfNotNull(getStorage(), a, d);
        }
      };
    }
  }

  static class ColorRevoke_ParseRule extends ColorNameList_ParseRule<ColorRevokeNode, ColorRevokeDrop> {

    protected ColorRevoke_ParseRule() {
      super("ColorRevoke", blockOps, ColorRevokeNode.class, ColorRevokeDrop.class);
    }
    
    @Override
    protected Object parse(IAnnotationParsingContext context,
                           SLColorAnnotationsParser parser) throws Exception,
        RecognitionException {
      return parser.colorRevoke().getTree();
    }
    
    @Override
    protected IAnnotationScrubber<ColorRevokeNode> makeScrubber() {
      return new AbstractAASTScrubber<ColorRevokeNode>(this) {
        @Override
        protected ColorRevokeDrop makePromiseDrop(ColorRevokeNode a) {
          ColorRevokeDrop d = new ColorRevokeDrop(a);
          return storeDropIfNotNull(getStorage(), a, d);
        }
      };
    }
  }

  static class ColorDeclare_ParseRule extends ColorNameList_ParseRule<ColorDeclarationNode, ColorDeclareDrop> {

    protected ColorDeclare_ParseRule() {
      super("ColorDeclare", declOps, ColorDeclarationNode.class, ColorDeclareDrop.class);
    }
    
    @Override
    protected Object parse(IAnnotationParsingContext context,
                           SLColorAnnotationsParser parser) throws Exception,
        RecognitionException {
      return parser.colorDeclare().getTree();
    }
    
    @Override
    protected IAnnotationScrubber<ColorDeclarationNode> makeScrubber() {
      return new AbstractAASTScrubber<ColorDeclarationNode>(this) {
        @Override
        protected ColorDeclareDrop makePromiseDrop(ColorDeclarationNode a) {
          ColorDeclareDrop d = new ColorDeclareDrop(a);
          return storeDropIfNotNull(getStorage(), a, d);
        }
      };
    }
  }

  static class ColorIncompatible_ParseRule extends ColorNameList_ParseRule<ColorIncompatibleNode, ColorIncompatibleDrop> {

    protected ColorIncompatible_ParseRule() {
      super(COLOR_INCOMPATIBLE, declOps, ColorIncompatibleNode.class, ColorIncompatibleDrop.class);
    }
    
    @Override
    protected Object parse(IAnnotationParsingContext context,
                           SLColorAnnotationsParser parser) throws Exception,
        RecognitionException {
      return parser.colorIncompatible().getTree();
    }
    
    @Override
    protected IAnnotationScrubber<ColorIncompatibleNode> makeScrubber() {
      return new AbstractAASTScrubber<ColorIncompatibleNode>(this) {
        @Override
        protected ColorIncompatibleDrop makePromiseDrop(ColorIncompatibleNode a) {
          ColorIncompatibleDrop d = new ColorIncompatibleDrop(a);
          return storeDropIfNotNull(getStorage(), a, d);
        }
      };
    }
  }
  

  static class ColorRename_ParseRule 
  extends DefaultSLColorAnnotationParseRule<ColorRenameNode,ColorRenameDrop> {
    protected ColorRename_ParseRule() {
      super(COLOR_RENAME, declOps, ColorRenameNode.class);
    }

    @Override
    protected Object parse(IAnnotationParsingContext context,
        SLColorAnnotationsParser parser) throws Exception, RecognitionException {
      return parser.colorRename().getTree();
    }
    
    @Override
    protected IPromiseDropStorage<ColorRenameDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), ColorRenameDrop.class);
    }
    @Override
    protected IAnnotationScrubber<ColorRenameNode> makeScrubber() {
      return new AbstractAASTScrubber<ColorRenameNode>(this,
          ScrubberType.UNORDERED) {
        @Override
        protected PromiseDrop<ColorRenameNode> makePromiseDrop(ColorRenameNode a) {
          return storeDropIfNotNull(getStorage(), 
                                    a, 
                                    ColorRenameDrop.buildColorRenameDrop(a));
        }
      };
    }
  }
  
//  static class ColorCardinality_ParseRule 
//  extends DefaultSLColorAnnotationParseRule<ColorCardSpecNode,ColorCardSpecDrop> {
//    protected ColorCardinality_ParseRule() {
//      super(COLOR_CARDINALITY, declOps, ColorCardSpecNode.class);
//    }
//
//    @Override
//    protected Object parse(IAnnotationParsingContext context,
//        SLColorAnnotationsParser parser) throws Exception, RecognitionException {
//      return parser.colorCardSpec().getTree();
//    }
//    
//    @Override
//    protected IPromiseDropStorage<ColorRenameDrop> makeStorage() {
//      return SinglePromiseDropStorage.create(name(), ColorCardSpecDrop.class);
//    }
//    @Override
//    protected IAnnotationScrubber<ColorRenameNode> makeScrubber() {
//      return new AbstractAASTScrubber<ColorCardSpecNode>(this,
//          ScrubberType.UNORDERED, ColorRules.COLOR_CARDINALITY) {
//        @Override
//        protected PromiseDrop<ColorRenameNode> makePromiseDrop(ColorCardSpecNode a) {
//          return storeDropIfNotNull(getStorage(), 
//                                    a, 
//                                    ColorCardSpecDrop.buildColorCardSpecDrop(a));
//        }
//      };
//    }
//  }

//  static class ColorizedRegion_ParseRule 
//  extends DefaultSLColorAnnotationParseRule<ColorizedRegionNode,ColorizedRegionDeclDrop> {
//    protected ColorizedRegion_ParseRule() {
//      super(COLOR_RENAME, declOps, ColorizedRegionNode.class);
//    }
//
//    @Override
//    protected Object parse(IAnnotationParsingContext context,
//        SLColorAnnotationsParser parser) throws Exception, RecognitionException {
//      return parser.colorizedRegion().getTree();
//    }
//    
//    @Override
//    protected IPromiseDropStorage<ColorizedRegionDrop> makeStorage() {
//      return SinglePromiseDropStorage.create(name(), ColorizedRegionDrop.class);
//    }
//    @Override
//    protected IAnnotationScrubber<ColorizedRegionNode> makeScrubber() {
//      return new AbstractAASTScrubber<ColorizedRegionNode>(this,
//          ScrubberType.UNORDERED, ColorRules.COLOR_CONSTRAINT) {
//        @Override
//        protected PromiseDrop<ColorizedRegionNode> makePromiseDrop(ColorizedRegionNode a) {
//          return storeDropIfNotNull(getStorage(), 
//                                    a, 
//                                    ColorizedRegionDrop.buildColorizedRegionDrop(a));
//        }
//      };
//    }
//  }
  
  public static SimpleCallGraphDrop getCGDrop(IRNode node) {
    return node.getSlotValue(simpleCGDropSI);
  }
  
  public static void setCGDrop(IRNode node, SimpleCallGraphDrop cgDrop) {
    if (!colorDropsEnabled) return;
    node.setSlotValue(simpleCGDropSI, cgDrop);
    cgDrop.setAttachedTo(node, simpleCGDropSI);
    }
  
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
  
  public static Collection<? extends ColorImportDrop> getColorImports(IRNode node) {
    Collection<ColorImportDrop> res = new HashSet<ColorImportDrop>();
    for (ColorImportDrop cid: getDrops(colorImportRule.getStorage(), node)) {
      res.add(cid);
    }
    return res;
  }


  public static Collection<ColorIncompatibleDrop> getColorIncompatibles(IRNode node) {
    Iterable<ColorIncompatibleDrop> incIter = getDrops(colorIncompatibleRule.getStorage(), node);
    Collection<ColorIncompatibleDrop> res = new ArrayList<ColorIncompatibleDrop>(1);
    for (ColorIncompatibleDrop cid : incIter) {
      res.add(cid);
    }
    return res;
  }


  public static boolean isColorRelevant(IRNode node) {
    boolean res = (getBooleanDrop(transparentRule.getStorage(), node) == null);
    return res;
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
  
//  public static Set<ColorContextDrop> getMutableColorContextSet(IRNode forNode) {
//    return getMutableSet(forNode, contextDropSetSI);
//  }
  
  public static Set<RegionColorDeclDrop> getMutableRegionColorDeclsSet(IRNode forNode) {
    return getMutableSet(forNode, regionColorDeclDropSetSI);
  }
  
  public static Set<ColorRequireDrop> getMutableRequiresColorSet(IRNode forNode) {
    return getMutableSet(forNode, reqDropSetSI);
  }
  
  public static Set<ColorRequireDrop> getMutableInheritedRequiresSet(IRNode forNode) {
    return getMutableSet(forNode, reqInheritDropSetSI);
  }
  
  

}
