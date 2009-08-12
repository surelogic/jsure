/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/RegionRules.java,v 1.45 2007/12/21 18:36:37 aarong Exp $*/
package com.surelogic.annotation.rules;

import java.util.*;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.promise.*;
import com.surelogic.analysis.regions.FieldRegion;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;

public class RegionRules extends AnnotationRules {
  public static final String REGION = "Region";
  public static final String IN_REGION = "InRegion";
  public static final String MAP_FIELDS = "MapFields";
  public static final String AGGREGATE = "Aggregate";
  public static final String REGION_INITIALIZER = "RegionInitializer";
  public static final String REGIONS_DONE = "RegionsDone";
  
  private static final AnnotationRules instance = new RegionRules();  

  private static final GlobalRegionState globalRegionState = new GlobalRegionState();
  private static final InitGlobalRegionState initState   = new InitGlobalRegionState(globalRegionState);
  private static final Region_ParseRule regionRule       = new Region_ParseRule(globalRegionState);
  private static final InRegion_ParseRule inRegionRule   = new InRegion_ParseRule();
  private static final MapFields_ParseRule mapFieldsRule = new MapFields_ParseRule();
  private static final Aggregate_ParseRule aggregateRule = new Aggregate_ParseRule();
  private static final SimpleScrubber regionsDone = new SimpleScrubber(REGIONS_DONE, REGION, IN_REGION, AGGREGATE) {
    @Override
    protected void scrub() {
      // do nothing
    }
  };
  
  
  public static AnnotationRules getInstance() {
    return instance;
  }
  
  public static Iterable<RegionModel> getModels(IRNode type) {
    return getDrops(regionRule.getStorage(), type);
  }
  
  public static InRegionPromiseDrop getInRegion(IRNode vdecl) {
    return getDrop(inRegionRule.getStorage(), vdecl);
  }
  
  public static AggregatePromiseDrop getAggregate(IRNode vdecl) {
    return getDrop(aggregateRule.getStorage(), vdecl);
  }
  
  @Override
  public void register(PromiseFramework fw) {
    registerScrubber(fw, initState);
    registerParseRuleStorage(fw, regionRule);
    registerParseRuleStorage(fw, inRegionRule);
    registerParseRuleStorage(fw, mapFieldsRule);
    registerParseRuleStorage(fw, aggregateRule);
    registerScrubber(fw, regionsDone);
//    registerScrubber(fw, new UniquelyNamed_NoCycles());
  }
  
  public static class Region_ParseRule 
  extends DefaultSLAnnotationParseRule<NewRegionDeclarationNode,RegionModel> {
    private final GlobalRegionState regionState;
    
    protected Region_ParseRule(final GlobalRegionState grs) {
      super(REGION, typeDeclOps, NewRegionDeclarationNode.class);
      regionState = grs;
    }
    
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      return parser.region().getTree();
    }
    @Override
    protected IPromiseDropStorage<RegionModel> makeStorage() {
      return PromiseDropSeqStorage.create(name(), RegionModel.class);
    }
    @Override
    protected IAnnotationScrubber<NewRegionDeclarationNode> makeScrubber() {
      return new AbstractAASTScrubber<NewRegionDeclarationNode>(
          this, ScrubberType.BY_HIERARCHY, REGION_INITIALIZER) {
        @Override
        protected PromiseDrop<NewRegionDeclarationNode> makePromiseDrop(NewRegionDeclarationNode a) {         
          return storeDropIfNotNull(getStorage(), a, scrubRegion(getContext(), regionState, a));
        }
      };
    }
  }

  private static RegionModel scrubRegion(
      final IAnnotationScrubberContext context,
      final GlobalRegionState regionState, final NewRegionDeclarationNode a) {
    final IRNode promisedFor = a.getPromisedFor();

    boolean annotationIsGood = true;
    
    // Region must be uniquely named
    final String simpleName = a.getId();
    final String qualifiedName = computeQualifiedName(a);  
    if (regionState.isNameAlreadyUsed(promisedFor, simpleName, qualifiedName)) {
      context.reportError(a, "Region \"{0}\" is already declared in class", simpleName);
      annotationIsGood = false;
    }
    
    final RegionSpecificationNode parentRegionNode = a.getRegionParent();
    RegionModel parentModel = null;
    if (parentRegionNode != null) {
      final String parentName = parentRegionNode.getId();
      final IRegionBinding boundParent = parentRegionNode.resolveBinding();
      // The parent region must exist
      if (boundParent == null) {
        context.reportError(a, "Parent region \"{0}\" does not exist", parentName);
        annotationIsGood = false;
      } else {
        parentModel = boundParent.getModel();
        
        // The region's parent must be an abstract region.
        if (parentModel.getAST() == null) {
          context.reportError(a, "Parent region \"{0}\" is a field", parentName);
          annotationIsGood = false;
        } else {
          // The parent region must be accessible
          if (!parentModel.isAccessibleFromType(context.getBinder().getTypeEnvironment(), promisedFor)) {
            context.reportError(a, "Region \"{0}\" is not accessible to type \"{1}\"", 
                parentModel.regionName, JavaNames.getQualifiedTypeName(promisedFor));
            annotationIsGood = false;
          }
          
          // Region cannot be more visible than its parent 
          if (!BindUtil.isMoreVisibleThan(parentModel.getVisibility(), a.getVisibility())) {
            context.reportError(a, "Region \"{0}\" is more visible than its parent \"{1}\"", simpleName, parentName);
            annotationIsGood = false;
          }
    
          // Instance region cannot contain a static region
          final boolean regionIsStatic = a.isStatic();
          final boolean parentIsStatic = parentModel.isStatic();
          if (regionIsStatic && !parentIsStatic) {
            context.reportError(a, "Static region cannot have a non-static parent");
            annotationIsGood = false;
          }
        }
        
        /* Cycles are prevented by the binder: Names cannot be used if they
         * haven't been seen lexically.  (No forward lookups)
         */
      }
    } else {
      /* Parent model is INSTANCE if the region is not static, ALL if 
       * region is static.  Region ALL has no parent. 
       */
//      annotationIsGood = true;
      if (!qualifiedName.equals(RegionModel.ALL)) {
        parentModel = RegionModel.getInstance(
            a.isStatic() ? RegionModel.ALL : RegionModel.INSTANCE);
      }
    }
    
    if (annotationIsGood) {
      RegionModel model = RegionModel.getInstance(qualifiedName);  
      model.setAST(a); // Set to keep it from being purged
      
      if (parentModel != null) { // parentModel == null if region is ALL
        model.addDependent(parentModel);
      }
      return model;
    } else {
      return null;
    }
  }
  
  public static class InRegion_ParseRule 
  extends DefaultSLAnnotationParseRule<InRegionNode,InRegionPromiseDrop> {
    protected InRegion_ParseRule() {
      super(IN_REGION, FieldDeclaration.prototype, InRegionNode.class);
    }
  
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      return parser.inRegion().getTree();
    }
    @Override
    protected InRegionNode makeRoot(AASTNode an) {
      return new InRegionNode(an.getOffset(), (RegionSpecificationNode) an);
    }
    @Override
    protected IPromiseDropStorage<InRegionPromiseDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), InRegionPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<InRegionNode> makeScrubber() {
      return new AbstractAASTScrubber<InRegionNode>(this, 
                                                   ScrubberType.UNORDERED, REGION) {
        @Override
        protected PromiseDrop<InRegionNode> makePromiseDrop(InRegionNode a) {
          return storeDropIfNotNull(getStorage(), a, scrubInRegion(getContext(), a));          
        }
      };
    }
  }

  private static InRegionPromiseDrop scrubInRegion(
      final IAnnotationScrubberContext context, final InRegionNode a) {
    /* The name of a region must be unique. There is nothing to check here in
     * practice because the Java compiler makes sure that a class does not
     * declare two fields with the same name, and we already checking that
     * regions declared using @Region do not have the same name as a field.
     * 
     * Cycles are prevented by the binder: Names cannot be used if they
     * haven't been seen lexically.  (No forward lookups)
     */
  
    if (a != null) {
      final IRNode promisedFor = a.getPromisedFor();
      final IRNode fieldDecl = promisedFor;
      final String parentName = a.getSpec().getId();
      boolean annotationIsGood = true;
      
      if (TypeUtil.isFinal(promisedFor)) {
        context.reportError(a, "Field \"{0}\" is final: it cannot be given a super region because it is not a region",
            JavaNames.getFieldDecl(promisedFor));
        annotationIsGood = false;
      }
      
      // The region's parent region must exist in the class.
      final IRegionBinding parentDecl = a.getSpec().resolveBinding();
      if (parentDecl == null) {
        context.reportError(a, "Parent region \"{0}\" does not exist", parentName);
        annotationIsGood = false;
      } else {
        final RegionModel parentModel = parentDecl.getModel();
        
        // The region cannot be final and cannot be volatile
        if (parentModel.isFinal()) {
          context.reportError(a, "Parent region \"{0}\" is final", parentName);
          annotationIsGood = false;
        } else if(parentModel.isVolatile()) {
          context.reportError(a, "Parent region \"{0}\" is volatile", parentName);
          annotationIsGood = false;
        } else {
          // The parent region must be accessible
          final IRNode enclosingType = VisitUtil.getEnclosingType(promisedFor);
          if (!parentModel.isAccessibleFromType(
              context.getBinder().getTypeEnvironment(), enclosingType)) {
            context.reportError(a, "Region \"{0}\" is not accessible to type \"{1}\"", 
                parentModel.regionName, JavaNames.getQualifiedTypeName(enclosingType));
            annotationIsGood = false;
          }

          // Region cannot be more visible than its parent 
          if (!BindUtil.isMoreVisibleThan(
                parentDecl.getModel().getVisibility(),
                BindUtil.getVisibility(JJNode.tree.getParent(JJNode.tree.getParent(fieldDecl))))) {
            context.reportError(a, "Region \"{0}\" is more visible than its parent \"{1}\"",
                VariableDeclarator.getId(fieldDecl), parentName);
            annotationIsGood = false;
          }
    
          // Instance region cannot contain a static region
          final boolean regionIsStatic = TypeUtil.isStatic(fieldDecl);
          final boolean parentIsStatic = parentDecl.getModel().isStatic();
          if (regionIsStatic && !parentIsStatic) {
            context.reportError(a, "Static region cannot have a non-static parent");
            annotationIsGood = false;
          }
        }
      }

      
      if (annotationIsGood) {
        final InRegionPromiseDrop mip = new InRegionPromiseDrop(a);
        final RegionModel fieldModel = RegionModel.getInstance(fieldDecl).getModel();
        final RegionModel parentModel = parentDecl.getModel();
        fieldModel.addDependent(parentModel);
        fieldModel.addSupportingInformation("via @InRegion annotation", fieldDecl);
//      IRegionBinding b = getAST().getSpec().resolveBinding();
//      b.getModel().addDependent(this);
        return mip;
      }
    }
    return null;
  }

  public static class MapFields_ParseRule 
  extends DefaultSLAnnotationParseRule<FieldMappingsNode,MapFieldsPromiseDrop> {
    protected MapFields_ParseRule() {
      super(MAP_FIELDS, typeDeclOps, FieldMappingsNode.class);
    }
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws Exception, RecognitionException {
      return parser.mapFields().getTree();
    }    
    @Override
    protected IPromiseDropStorage<MapFieldsPromiseDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), MapFieldsPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<FieldMappingsNode> makeScrubber() {
      return new AbstractAASTScrubber<FieldMappingsNode>(this, ScrubberType.UNORDERED, 
                                                         new String[] { IN_REGION }, REGION) {
        @Override
        protected PromiseDrop<FieldMappingsNode> makePromiseDrop(FieldMappingsNode a) {
          return storeDropIfNotNull(getStorage(), a, scrubMapFields(getContext(), a));          
        }
      };
    }     
  }
  
  private static MapFieldsPromiseDrop scrubMapFields(IAnnotationScrubberContext context, FieldMappingsNode a) {
    // Check fields
    MapFieldsPromiseDrop drop = new MapFieldsPromiseDrop(a);
    
    for(RegionSpecificationNode spec : a.getFieldsList()) {
      FieldRegion field = (FieldRegion) spec.resolveBinding().getRegion();
      InRegionNode mapInto = inRegionRule.makeRoot((RegionSpecificationNode)a.getTo().cloneTree());
      mapInto.setPromisedFor(field.getNode());
      mapInto.setSrcType(a.getSrcType()); // FIX
      
      AASTStore.add(mapInto);
      AASTStore.triggerWhenValidated(mapInto, drop);
    }
    return drop;
  }
  
  public static class Aggregate_ParseRule
  extends DefaultSLAnnotationParseRule<AggregateNode,AggregatePromiseDrop> {
    protected Aggregate_ParseRule() {
      super(AGGREGATE, FieldDeclaration.prototype, AggregateNode.class);
    }
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      return parser.aggregate().getTree();
    }
    @Override
    protected IPromiseDropStorage<AggregatePromiseDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), AggregatePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<AggregateNode> makeScrubber() {
      return new AbstractAASTScrubber<AggregateNode>(this,  
                                                     ScrubberType.UNORDERED, 
                                                     REGION, IN_REGION, MAP_FIELDS, UniquenessRules.UNIQUENESS_DONE) {
        @Override
        protected PromiseDrop<AggregateNode> makePromiseDrop(AggregateNode a) {
          return storeDropIfNotNull(getStorage(), a, scrubAggregate(getContext(), a));
        }
      };
    }
  }
  
  private static AggregatePromiseDrop scrubAggregate(
      final IAnnotationScrubberContext context, final AggregateNode a) {
    boolean annotationIsGood = true;
    final AggregatePromiseDrop ap = new AggregatePromiseDrop(a);
    final IRNode promisedFor = a.getPromisedFor();
    final IRNode enclosingType = VisitUtil.getEnclosingType(promisedFor);
    final IRNode declStmt = tree.getParent(tree.getParent(promisedFor));
    
    /* Check that the field is unique
     */
    final UniquePromiseDrop unique = UniquenessRules.getUniqueDrop(promisedFor);
    if (unique == null) {
      context.reportError(a, "Field \"{0}\" is not declared @Unique: its regions cannot be aggregated",
          VariableDeclarator.getId(promisedFor));
      annotationIsGood = false;
    }

    // process the mapping
    final Set<String> srcRegions = new HashSet<String>();
    final Map<IRegion, IRegion> regionMap = new HashMap<IRegion, IRegion>();
    final boolean promisedForIsStatic = TypeUtil.isStatic(declStmt);
    final IRegion fieldAsRegion = RegionModel.getInstance(promisedFor);
    for(final RegionMappingNode mapping : a.getSpec().getMappingList()) {
      final RegionNameNode fromNode = mapping.getFrom();
      final IRegionBinding fromDecl = fromNode.resolveBinding();
      final String fromId           = fromNode.getId();
      final RegionSpecificationNode toNode = mapping.getTo();
      final IRegionBinding toDecl   = toNode.resolveBinding();
      final String toId             = toNode.getId();

      /* Make sure the source region exists, is not static, is uniquely named,
       * and is accessible.
       */
      if (fromDecl == null) {
        context.reportError(a, "Source region \"{0}\" not found in aggregated class", fromId);
        annotationIsGood = false;
      } else {
        final RegionModel fromRegion = fromDecl.getModel();
        if (!fromRegion.isAccessibleFromType(context.getBinder().getTypeEnvironment(), enclosingType)) {
          context.reportError(a, "Source region \"{0}\" is not accessible to type \"{1}\"",
              fromRegion.regionName, JavaNames.getQualifiedTypeName(enclosingType));
          annotationIsGood = false;
        }
        if (fromRegion.isStatic()) {
          context.reportError(a, "Source region \"{0}\" is static; cannot aggregate static regions", fromId);
          annotationIsGood = false;
        }
        if (!srcRegions.add(fromDecl.getModel().regionName) ) {
          context.reportError(a, "Source region \"{0}\" is aggregated more than once", fromId);
          annotationIsGood = false;
        }
      }
       
      /* Make sure the destination region exists and is accessible. If the field
       * is not final, then the region must be equal to or contain the field. If
       * the field is final, then the destination region must be static if the
       * field is static.  Destination region cannot be final or volatile.
       */
      if (toDecl == null) {
        context.reportError(a, "Destination region \"{0}\" not found", toId);
        annotationIsGood = false;        
      } else {
        final RegionModel toRegion = toDecl.getModel();
        if (!toRegion.isAccessibleFromType(context.getBinder().getTypeEnvironment(), enclosingType)) {
          context.reportError(a, "Source region \"{0}\" is not accessible to type \"{1}\"",
              toRegion.regionName, JavaNames.getQualifiedTypeName(enclosingType));
          annotationIsGood = false;
        }

        if (TypeUtil.isFinal(promisedFor)) {
          if (promisedForIsStatic && !toRegion.isStatic()) {
            context.reportError(a, "Destination region \"{0}\" is not static; must be static because the annotated field \"{1}\" is final and static",
                toId, VariableDeclarator.getId(promisedFor));
            annotationIsGood = false;
          }
        } else {
          if (!toRegion.ancestorOf(fieldAsRegion)) {
            context.reportError(a, "Destination region \"{0}\" is not an ancestor of the annotated non-final field \"{1}\"",
                toId, VariableDeclarator.getId(promisedFor));
            annotationIsGood = false;
          }
        }
        
        if (toRegion.isFinal()) {
          context.reportError(a, "Destination region \"{0}\" is final", toId);
          annotationIsGood = false;
        }
        if (toRegion.isVolatile()) {
          context.reportError(a, "Destination region \"{0}\" is volatile", toId);
          annotationIsGood = false;
        }
      }
      
      /* If the annotation is still okay, record the mapping.  If it's bad, we 
       * won't look at this map anyway, so forget about it.  But we have to check
       * result so we know that both the src and dest regions exist.
       */
      if (annotationIsGood) {
        regionMap.put(fromDecl.getRegion(), toDecl.getRegion());
      }
    }
    
    if (annotationIsGood) {
      // The Instance region must be mapped
      if (!srcRegions.contains(RegionModel.INSTANCE)) {
        context.reportError(a, "The region \"Instance\" must be mapped");
        annotationIsGood = false;
      }
      
      /* Aggregation must respect the region hierarchy: if the annotation maps
       * Ri into Qi and Rj into Qj, and Ri is a subregion of Rj, then it must be
       * that Qi is a subregion of Qj.
       * 
       * We check each pair of mappings.
       */
      final List<Map.Entry<IRegion, IRegion>> entries =
        new ArrayList<Map.Entry<IRegion, IRegion>>(regionMap.entrySet());
      final int numEntries = entries.size();
      for (int first = 0; first < numEntries-1; first++) {
        for (int second = first+1; second < numEntries; second++) {
          final IRegion firstKey = entries.get(first).getKey();
          final IRegion firstVal = entries.get(first).getValue();
          final IRegion secondKey = entries.get(second).getKey();
          final IRegion secondVal = entries.get(second).getValue();
          if (firstKey.ancestorOf(secondKey)) {
            if (!firstVal.ancestorOf(secondVal)) {
              context.reportError(a, "Region \"{0}\" is a subregion of \"{1}\" in the referenced class, but region \"{2}\" is not a subregion of \"{3}\" in the referring class",
                  truncateName(secondKey.toString()), truncateName(firstKey.toString()),
                  truncateName(secondVal.toString()), truncateName(firstVal.toString()));
              annotationIsGood = false;
            }
          } else if (secondKey.ancestorOf(firstKey)) {
            if (!secondVal.ancestorOf(firstVal)) {
              context.reportError(a, "Region \"{0}\" is a subregion of \"{1}\" in the referenced class, but region \"{2}\" is not a subregion of \"{3}\" in the referring class",
                  truncateName(firstKey.toString()), truncateName(secondKey.toString()),
                  truncateName(firstVal.toString()), truncateName(secondVal.toString()));
              annotationIsGood = false;
            }
          }
        }
      }
    }
    
    if (annotationIsGood) {
      fieldAsRegion.getModel().addDependent(ap);
      return ap;
    } else {
      return null;
    }
    
//    return annotationIsGood ? ap : null;
  }
  
  private static String truncateName(final String qualifiedName) {
    final int lastDot = qualifiedName.lastIndexOf('.');
    if (lastDot == -1) {
      return qualifiedName;
    } else {
      return qualifiedName.substring(lastDot+1);
    }
  }
  
  private static final class GlobalRegionState {
    private final Map<IRNode, Set<String>> classToFields = new HashMap<IRNode, Set<String>>();
    private final Set<String> qualifiedRegionNames = new HashSet<String>();
    
    public GlobalRegionState() {
      super();
    }
    
    private Set<String> getFieldNames(final IRNode type) {
      Set<String> fieldNames = classToFields.get(type);
      if (fieldNames == null) {
        fieldNames = new HashSet<String>();
        for (final IRNode vdecl : VisitUtil.getClassFieldDeclarators(type)) {
          fieldNames.add(VariableDeclarator.getId(vdecl));
        }
        classToFields.put(type, fieldNames);
      }
      return fieldNames;
    }
    
    public synchronized boolean isNameAlreadyUsed(
        final IRNode type, final String simpleName, final String qualifiedName) {
      boolean isDuplicate = getFieldNames(type).contains(simpleName) || qualifiedRegionNames.contains(qualifiedName);
      qualifiedRegionNames.add(qualifiedName);
      return isDuplicate;
    }
       
    public synchronized void clearState() {
      classToFields.clear();
      qualifiedRegionNames.clear();
    }
  }

  static final class InitGlobalRegionState extends SimpleScrubber {
    private final GlobalRegionState regionState;
    
    public InitGlobalRegionState(final GlobalRegionState grs) {
      super(REGION_INITIALIZER);
      regionState = grs;
    }
  
    @Override
    protected void scrub() {
      // All we do is clear the region state
      regionState.clearState();
    }
  }
}
