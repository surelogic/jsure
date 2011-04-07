/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/RegionRules.java,v 1.45 2007/12/21 18:36:37 aarong Exp $*/
package com.surelogic.annotation.rules;

import java.util.*;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.promise.*;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.analysis.regions.FieldRegion;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.Visibility;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;

public class RegionRules extends AnnotationRules {
  public static final String REGION = "Region";
  public static final String IN_REGION = "InRegion";
  public static final String MAP_FIELDS = "MapFields";
  public static final String SIMPLE_UNIQUE_IN_REGION = "UniqueInRegion";
  public static final String EXPLICIT_UNIQUE_IN_REGION = "Unique Mapping"; // Never meant to be parsed
  public static final String REGION_INITIALIZER = "RegionInitializer";
  public static final String REGIONS_DONE = "RegionsDone";
  
  private static final AnnotationRules instance = new RegionRules();  

  private static final IGlobalRegionState globalRegionState = new GlobalRegionState();
  private static final InitGlobalRegionState initState     = new InitGlobalRegionState(globalRegionState);
  private static final Region_ParseRule regionRule         = new Region_ParseRule(globalRegionState);
  private static final InRegion_ParseRule inRegionRule     = new InRegion_ParseRule();
  private static final MapFields_ParseRule mapFieldsRule   = new MapFields_ParseRule();

  private static final ExplicitUniqueInRegion_ParseRule explicitUniqueInRegionRule =
    new ExplicitUniqueInRegion_ParseRule();
  private static final SimpleUniqueInRegion_ParseRule simpleUniqueInRegionRule = 
	  new SimpleUniqueInRegion_ParseRule();
  
  private static final SimpleScrubber regionsDone = new SimpleScrubber(
      REGIONS_DONE, REGION, IN_REGION, SIMPLE_UNIQUE_IN_REGION,
      EXPLICIT_UNIQUE_IN_REGION) {
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
  
  public static void printRegionModels(IRNode type) {
	  System.out.println("For "+JavaNames.getFullTypeName(type));
	  for(RegionModel m : getModels(type)) {
		  System.out.println("  "+m.getName());
	  }
  }
  
  public static InRegionPromiseDrop getInRegion(IRNode vdecl) {
    return getDrop(inRegionRule.getStorage(), vdecl);
  }
  
  public static ExplicitUniqueInRegionPromiseDrop getAggregate(IRNode vdecl) {
    throw new UnsupportedOperationException();
  }

  public static SimpleUniqueInRegionPromiseDrop getAggregateInRegion(IRNode vdecl) {
	  return getDrop(simpleUniqueInRegionRule.getStorage(), vdecl);
  }
  
  @Override
  public void register(PromiseFramework fw) {
    registerScrubber(fw, initState);
    registerParseRuleStorage(fw, regionRule);
    registerParseRuleStorage(fw, inRegionRule);
    registerParseRuleStorage(fw, mapFieldsRule);
    // Hack to use one rule or another, depending on where it is
    fw.registerParseDropRule(new NullAnnotationParseRule(IN_REGION, PromiseConstants.fieldOrTypeOp) {
    	@Override
		public ParseResult parse(IAnnotationParsingContext context, String contents) {
			if (FieldDeclaration.prototype.includes(context.getOp())) {
				return inRegionRule.parse(context, contents);
			} else {
				return mapFieldsRule.parse(context, contents);
			}
		}    	
    }, true);
    registerParseRuleStorage(fw, explicitUniqueInRegionRule);
    registerParseRuleStorage(fw, simpleUniqueInRegionRule);
    registerScrubber(fw, regionsDone);
//    registerScrubber(fw, new UniquelyNamed_NoCycles());
  }
  
  public static class Region_ParseRule 
  extends DefaultSLAnnotationParseRule<NewRegionDeclarationNode,RegionModel> {
    private final IGlobalRegionState regionState;
    
    protected Region_ParseRule(final IGlobalRegionState grs) {
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
      return new AbstractAASTScrubber<NewRegionDeclarationNode, RegionModel>(
          this, ScrubberType.BY_HIERARCHY, REGION_INITIALIZER) {
        @Override
        protected PromiseDrop<NewRegionDeclarationNode> makePromiseDrop(NewRegionDeclarationNode a) {
          final RegionModel m = scrubRegion(getContext(), regionState, a);          
          //System.out.println("Created region "+m.getName());
          return storeDropIfNotNull(a, m);
        }
      };
    }
  }

  private static RegionModel scrubRegion(
      final IAnnotationScrubberContext context,
      final IGlobalRegionState regionState, final NewRegionDeclarationNode a) {
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
        
        
        // The region cannot be final and cannot be volatile
        if (parentModel.isFinal()) {
          context.reportError(a, "Parent region \"{0}\" is final", parentName);
          annotationIsGood = false;
        } else if(parentModel.isVolatile()) {
          context.reportError(a, "Parent region \"{0}\" is volatile", parentName);
          annotationIsGood = false;
        } else {
          // The parent region must be accessible
          if (!parentModel.isAccessibleFromType(context.getBinder().getTypeEnvironment(), promisedFor)) {
            context.reportError(a, "Region \"{0}\" is not accessible to type \"{1}\"", 
                parentModel.regionName, JavaNames.getQualifiedTypeName(promisedFor));
            annotationIsGood = false;
          }
          
          // Region cannot be more visible than its parent 
          if (!parentModel.getVisibility().atLeastAsVisibleAs(a.getVisibility())) {
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
      if (!qualifiedName.equals(RegionModel.ALL)) {
        parentModel = a.isStatic() ? RegionModel.getAllRegion(a.getPromisedFor()) : RegionModel.getInstanceRegion(a.getPromisedFor());
      }
    }
    
    if (annotationIsGood) {
      RegionModel model = RegionModel.getInstance(qualifiedName, a.getPromisedFor());  
      model.setAST(a); // Set to keep it from being purged
      System.out.println("Adding region "+model.getName()+" to "+JavaNames.getFullTypeName(a.getPromisedFor())+" -- "+a.getPromisedFor());
      
      if (parentModel != null) { // parentModel == null if region is ALL
        model.addDependent(parentModel);
      }
      return model;
    } else {
      RegionModel.invalidate(qualifiedName, a.getPromisedFor());
      return null;
    }
  }
  
  public static class InRegion_ParseRule 
  extends DefaultSLAnnotationParseRule<InRegionNode,InRegionPromiseDrop> {
    protected InRegion_ParseRule() {
      super(IN_REGION, fieldDeclOp, InRegionNode.class);
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
      return new AbstractAASTScrubber<InRegionNode, InRegionPromiseDrop>(
          this, ScrubberType.BY_HIERARCHY, REGION, SIMPLE_UNIQUE_IN_REGION) {
        @Override
        protected PromiseDrop<InRegionNode> makePromiseDrop(InRegionNode a) {
          return storeDropIfNotNull(a, scrubInRegion(getContext(), a));          
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
     */
  
    if (a != null) {
      final IRNode promisedFor = a.getPromisedFor();
      final String parentName = a.getSpec().getId();
      boolean annotationIsGood = true;
      
      if (TypeUtil.isFinal(promisedFor)) {
        context.reportError(a, "Field \"{0}\" is final: it cannot be given a super region because it is not a region",
            VariableDeclarator.getId(promisedFor));
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
          // Parent region must not create a cycle
          if (RegionModel.getInstance(promisedFor).ancestorOf(parentModel)) {
            context.reportError(a, "Cycle detected: Field \"{0}\" is already an ancestor of region \"{1}\"",
                VariableDeclarator.getId(promisedFor), parentModel.regionName);
            annotationIsGood = false;
          }
          
          // The parent region must be accessible
          final IRNode enclosingType = VisitUtil.getEnclosingType(promisedFor);
          if (!parentModel.isAccessibleFromType(
              context.getBinder().getTypeEnvironment(), enclosingType)) {
            context.reportError(a, "Region \"{0}\" is not accessible to type \"{1}\"", 
                parentModel.regionName, JavaNames.getQualifiedTypeName(enclosingType));
            annotationIsGood = false;
          }

          // Region cannot be more visible than its parent 
          if (!parentModel.getVisibility().atLeastAsVisibleAs(
              Visibility.getVisibilityOf(JJNode.tree.getParent(JJNode.tree.getParent(promisedFor))))) {
            context.reportError(a, "Region \"{0}\" is more visible than its parent \"{1}\"",
                VariableDeclarator.getId(promisedFor), parentName);
            annotationIsGood = false;
          }
    
          // Instance region cannot contain a static region
          final boolean regionIsStatic = TypeUtil.isStatic(promisedFor);
          final boolean parentIsStatic = parentModel.isStatic();
          if (regionIsStatic && !parentIsStatic) {
            context.reportError(a, "Static region cannot have a non-static parent");
            annotationIsGood = false;
          }
        }
      }

      if (annotationIsGood) {
        final InRegionPromiseDrop mip = new InRegionPromiseDrop(a);
        final RegionModel fieldModel = RegionModel.getInstance(promisedFor).getModel();
        final RegionModel parentModel = parentDecl.getModel();
        fieldModel.addDependent(parentModel);
        fieldModel.addDependent(mip);
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
      return PromiseDropSeqStorage.create(name(), MapFieldsPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<FieldMappingsNode> makeScrubber() {
      return new AbstractAASTScrubber<FieldMappingsNode, MapFieldsPromiseDrop>(this, ScrubberType.UNORDERED, 
                                                         new String[] { IN_REGION }, REGION) {
        @Override
        protected PromiseDrop<FieldMappingsNode> makePromiseDrop(FieldMappingsNode a) {
          return storeDropIfNotNull(a, scrubMapFields(getContext(), a));          
        }
      };
    }     
  }
  
  private static MapFieldsPromiseDrop scrubMapFields(IAnnotationScrubberContext context, FieldMappingsNode a) {
    // Check fields
    MapFieldsPromiseDrop drop = new MapFieldsPromiseDrop(a);
    
    for(RegionSpecificationNode spec : a.getFieldsList()) {
      if (!spec.bindingExists()) {
    	  return null;
  	  }	  
      FieldRegion field = (FieldRegion) spec.resolveBinding().getRegion();
      InRegionNode mapInto = inRegionRule.makeRoot((RegionSpecificationNode)a.getTo().cloneTree());
      mapInto.setPromisedFor(field.getNode());
      mapInto.setSrcType(a.getSrcType()); // FIX
      
      AASTStore.addDerived(mapInto, drop);
    }
    return drop;
  }
  
  public static class SimpleUniqueInRegion_ParseRule 
  extends DefaultSLAnnotationParseRule<UniqueInRegionNode,SimpleUniqueInRegionPromiseDrop> {
    protected SimpleUniqueInRegion_ParseRule() {
      super(SIMPLE_UNIQUE_IN_REGION, fieldDeclOp, UniqueInRegionNode.class);
    }
    @Override
  protected boolean producesOtherAASTRootNodes() {
      return true;
    }
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      return parser.uniqueInRegion().getTree();
    }
  
    @Override
    protected IPromiseDropStorage<SimpleUniqueInRegionPromiseDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), SimpleUniqueInRegionPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<UniqueInRegionNode> makeScrubber() {
      return new AbstractAASTScrubber<UniqueInRegionNode, SimpleUniqueInRegionPromiseDrop>(
          this, ScrubberType.UNORDERED, REGION, UniquenessRules.UNIQUE) {
        @Override
        protected SimpleUniqueInRegionPromiseDrop makePromiseDrop(UniqueInRegionNode a) {
          return storeDropIfNotNull(a, scrubSimpleUniqueInRegion(getContext(), a));          
        }
      };
    }
  }

  static SimpleUniqueInRegionPromiseDrop scrubSimpleUniqueInRegion(
  	  final IAnnotationScrubberContext context,
  	  final UniqueInRegionNode a) {
    final IRNode promisedFor = a.getPromisedFor();
    
    // Cannot also be @Unique
    final UniquePromiseDrop uniqueDrop = UniquenessRules.getUniqueDrop(promisedFor);
    if (uniqueDrop != null) {
  	  context.reportError(a, "Cannot be annotated with both @Unique and @UniqueInRegion");
  	  uniqueDrop.invalidate();
  	  return null;
    }

    boolean isGood = true;
    
    // Field must be reference typed
    final IJavaType type = context.getBinder().getJavaType(promisedFor);
    if (type instanceof IJavaPrimitiveType) {
      context.reportError(a, "Annotated field must have a reference type");
      isGood = false;
    }
    
    // Named region must exist
    final String name = a.getSpec().getId();
    final IRegionBinding destDecl = a.getSpec().resolveBinding();
    if (destDecl == null) {
      context.reportError(a, "Destination region \"{0}\" does not exist", name);
      isGood = false;
    } else {
      // Named region cannot be final
      final RegionModel destRegion = destDecl.getModel();
      if (destRegion.isFinal()) {
        context.reportError(a, "Destination region \"{0}\" is final", name);
        isGood = false;
      }
      
      // Named region cannot be volatile
      if (destRegion.isVolatile()) {
        context.reportError(a, "Destination region \"{0}\" is volatile", name);
        isGood = false;
      }
      
      // Named region must be static if the field is static
      if (TypeUtil.isStatic(promisedFor) && !destRegion.isStatic()) {
        context.reportError(a, "Destination region \"{0}\" must be static because the annotated field is static", name);
        isGood = false;
      }
      
      // Named region must be accessible
      final IRNode enclosingType = VisitUtil.getEnclosingType(promisedFor);
      if (!destRegion.isAccessibleFromType(
          context.getBinder().getTypeEnvironment(), enclosingType)) {
        context.reportError(a, "Destination region \"{0}\" is not accessible to type \"{1}\"",
            name, JavaNames.getQualifiedTypeName(enclosingType));
        isGood = false;
      }
    }
    
    if (isGood) {
      final SimpleUniqueInRegionPromiseDrop drop = 
        new SimpleUniqueInRegionPromiseDrop(a);

      // If field is not final then add an @InRegion annotation
      if (!TypeUtil.isFinal(promisedFor)) {
        final RegionSpecificationNode regionSpec =
          (RegionSpecificationNode) a.getSpec().cloneTree();
        final InRegionNode inRegion =
          new InRegionNode(a.getOffset(), regionSpec);
        inRegion.setPromisedFor(promisedFor);
        inRegion.setSrcType(a.getSrcType());
        AASTStore.addDerived(inRegion, drop);
      }
      
      return drop;
    } else {
      return null;
    }
  }

  public static class ExplicitUniqueInRegion_ParseRule
  extends DefaultSLAnnotationParseRule<UniqueMappingNode,ExplicitUniqueInRegionPromiseDrop> {
    protected ExplicitUniqueInRegion_ParseRule() {
      super(EXPLICIT_UNIQUE_IN_REGION, fieldDeclOp, UniqueMappingNode.class);
    }
    
    @Override
    protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) throws RecognitionException {
      throw new UnsupportedOperationException(context.getAllText());
    }
    
    @Override
    protected IPromiseDropStorage<ExplicitUniqueInRegionPromiseDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), ExplicitUniqueInRegionPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<UniqueMappingNode> makeScrubber() {
      return new AbstractAASTScrubber<UniqueMappingNode, ExplicitUniqueInRegionPromiseDrop>(
          this, ScrubberType.UNORDERED, REGION, UniquenessRules.UNIQUE) {
        @Override
        protected ExplicitUniqueInRegionPromiseDrop makePromiseDrop(UniqueMappingNode a) {
          return storeDropIfNotNull(a, scrubExplicitUniqueInRegion(getContext(), a));          
        }
      };
    }
  }
  
  private static ExplicitUniqueInRegionPromiseDrop scrubExplicitUniqueInRegion(
      final IAnnotationScrubberContext context, final UniqueMappingNode a) {
    final IRNode promisedFor = a.getPromisedFor();   
    final IRNode enclosingType = VisitUtil.getEnclosingType(promisedFor);
    final UniquePromiseDrop uniqueDrop = UniquenessRules.getUniqueDrop(promisedFor);
    if (uniqueDrop != null) {
      context.reportError(a, "Cannot be annotated with both @Unique and @UniqueInRegion");
      uniqueDrop.invalidate();
      return null;
    }
    
    boolean isGood = true;
    
    // Field must be reference typed
    final IJavaType type = context.getBinder().getJavaType(promisedFor);
    if (type instanceof IJavaPrimitiveType) {
      context.reportError(a, "Annotated field must have a reference type");
      isGood = false;
    }
    
    // Field must be final
    if (!TypeUtil.isFinal(promisedFor)) {
      context.reportError(a, "Annotated field must be final");
      isGood = false;
    }

    // process the mapping
    final Set<RegionModel> srcRegions = new HashSet<RegionModel>();
    final Map<IRegion, IRegion> regionMap = new HashMap<IRegion, IRegion>();
    final boolean promisedForIsStatic = TypeUtil.isStatic(promisedFor);
    for(final RegionMappingNode mapping : a.getMapping().getMappingList()) {
      final RegionNameNode fromNode = mapping.getFrom();
      final IRegionBinding fromDecl = fromNode.resolveBinding();
      final String fromId           = fromNode.getId();
      final RegionSpecificationNode toNode = mapping.getTo();
      final IRegionBinding toDecl   = toNode.resolveBinding();
      final String toId             = toNode.getId();

      // Make sure the source region exists
      if (fromDecl == null) {
        context.reportError(a, "Source region \"{0}\" not found in aggregated class", fromId);
        isGood = false;
      } else {
        final RegionModel fromRegion = fromDecl.getModel();
        
        // Cannot be static
        if (fromDecl.getRegion().isStatic()) {
          context.reportError(a, "Source region \"{0}\" is static", fromId);
          isGood = false;
        }
        // Cannot be aggregated more than once
        if (!srcRegions.add(fromRegion) ) {
          context.reportError(a, "Source region \"{0}\" is aggregated more than once", fromId);
          isGood = false;
        }
        // Must be accessible
        if (!fromRegion.isAccessibleFromType(
            context.getBinder().getTypeEnvironment(), enclosingType)) {
          context.reportError(a,
              "Source region \"{0}\" is not accessible to type \"{1}\"",
              fromRegion.regionName,
              JavaNames.getQualifiedTypeName(enclosingType));
          isGood = false;
        }
      }
      
      // Make sure the dest region exists
      if (toDecl == null)  {
        context.reportError(a, "Destination region \"{0}\" not found", toId);
        isGood = false;
      } else {
        // Named region cannot be final
        final RegionModel toRegion = toDecl.getModel();
        if (toRegion.isFinal()) {
          context.reportError(a, "Destination region \"{0}\" is final", toId);
          isGood = false;
        }
        
        // Named region cannot be volatile
        if (toRegion.isVolatile()) {
          context.reportError(a, "Destination region \"{0}\" is volatile", toId);
          isGood = false;
        }
        
        // Region must be static if the field is static
        if (promisedForIsStatic && !toRegion.isStatic()) {
          context.reportError(a, "Destination region \"{0}\" must be static because the annotated field is static", toId);
          isGood = false;
        }
        if (!toRegion.isAccessibleFromType(
            context.getBinder().getTypeEnvironment(), enclosingType)) {
          context.reportError(a,
              "Source region \"{0}\" is not accessible to type \"{1}\"",
              toRegion.regionName,
              JavaNames.getQualifiedTypeName(enclosingType));
          isGood = false;
        }
      }
      
      /* If the annotation is still okay, record the mapping. If it's bad, we
       * won't look at this map anyway, so forget about it. But we have to check
       * the result so we know that both the src and dest regions exist.
       */
      if (isGood) {
        regionMap.put(fromDecl.getRegion(), toDecl.getRegion());
      }
    }
    
    // The Instance region must be mapped
    if (!srcRegions.contains(RegionModel.getInstanceRegion(promisedFor))) {
      context.reportError(a, "The region \"Instance\" must be mapped");
      isGood = false;
    }

    if (isGood) {
      /* Aggregation must respect the region hierarchy: if the annotation maps Ri
       * into Qi and Rj into Qj, and Ri is a subregion of Rj, then it must be that
       * Qi is a subregion of Qj.
       * 
       * We check each pair of mappings.
       */
      final List<Map.Entry<IRegion, IRegion>> entries =
        new ArrayList<Map.Entry<IRegion, IRegion>>(regionMap.entrySet());
      final int numEntries = entries.size();
      for (int first = 0; first < numEntries - 1; first++) {
        for (int second = first + 1; second < numEntries; second++) {
          final IRegion firstKey = entries.get(first).getKey();
          final IRegion firstVal = entries.get(first).getValue();
          final IRegion secondKey = entries.get(second).getKey();
          final IRegion secondVal = entries.get(second).getValue();
          if (firstKey.ancestorOf(secondKey)) {
            if (!firstVal.ancestorOf(secondVal)) {
              context.reportError(a,
                      "Region \"{0}\" is a subregion of \"{1}\" in the aggregated class, but region \"{2}\" is not a subregion of \"{3}\" in the aggregating class",
                      truncateName(secondKey.toString()),
                      truncateName(firstKey.toString()),
                      truncateName(secondVal.toString()),
                      truncateName(firstVal.toString()));
              isGood = false;
            }
          } else if (secondKey.ancestorOf(firstKey)) {
            if (!secondVal.ancestorOf(firstVal)) {
              context.reportError(a,
                      "Region \"{0}\" is a subregion of \"{1}\" in the aggregated class, but region \"{2}\" is not a subregion of \"{3}\" in the aggregating class",
                      truncateName(firstKey.toString()),
                      truncateName(secondKey.toString()),
                      truncateName(firstVal.toString()),
                      truncateName(secondVal.toString()));
              isGood = false;
            }
          }
        }
      }
    }
    
    if (isGood) {
      return new ExplicitUniqueInRegionPromiseDrop(a);
    } else {
      return null;
    }
    
//    boolean annotationIsGood = true;
//    final IRNode promisedFor = a.getPromisedFor();
//    final IRNode enclosingType = VisitUtil.getEnclosingType(promisedFor);
//    final IRNode declStmt = tree.getParent(tree.getParent(promisedFor));
//    
//    /* Check that we don't also have an @AggregateInRegion.  This is tricky
//     * because @AggregateInRegion generates an @Aggregate, so we have to see if
//     * we are the @Aggregate that was generated or not.
//     */
//    final SimpleUniqueInRegionPromiseDrop aggInRegion = getAggregateInRegion(promisedFor);
//    if (aggInRegion != null) {
//      if (a.getOffset() != aggInRegion.getAST().getOffset()) { // not generated
//        // bad, cannot have both
//        context.reportError(a, "Cannot have both @Aggregate and @AggregateInRegion on the same field, ignoring the @Aggregate annotation");
//        annotationIsGood = false;
//      }
//    }
//    
//    /* Check that the field is unique
//     */
//    final UniquePromiseDrop unique = UniquenessRules.getUniqueDrop(promisedFor);
//    if (unique == null) {
//      context.reportError(a, "Field \"{0}\" is not declared @Unique: its regions cannot be aggregated",
//          VariableDeclarator.getId(promisedFor));
//      annotationIsGood = false;
//    }
//
//    // process the mapping
//    final Set<String> srcRegions = new HashSet<String>();
//    final Map<IRegion, IRegion> regionMap = new HashMap<IRegion, IRegion>();
//    final boolean promisedForIsStatic = TypeUtil.isStatic(declStmt);
//    final IRegion fieldAsRegion = RegionModel.getInstance(promisedFor);
//    for(final RegionMappingNode mapping : a.getMapping().getMappingList()) {
//      final RegionNameNode fromNode = mapping.getFrom();
//      final IRegionBinding fromDecl = fromNode.resolveBinding();
//      final String fromId           = fromNode.getId();
//      final RegionSpecificationNode toNode = mapping.getTo();
//      final IRegionBinding toDecl   = toNode.resolveBinding();
//      final String toId             = toNode.getId();
//
//      /* Make sure the source region exists, is not static, is uniquely named,
//       * and is accessible.
//       */
//      if (fromDecl == null) {
//        context.reportError(a, "Source region \"{0}\" not found in aggregated class", fromId);
//        annotationIsGood = false;
//      } else {
//        final RegionModel fromRegion = fromDecl.getModel();
//        if (!fromRegion.isAccessibleFromType(context.getBinder().getTypeEnvironment(), enclosingType)) {
//          context.reportError(a, "Source region \"{0}\" is not accessible to type \"{1}\"",
//              fromRegion.regionName, JavaNames.getQualifiedTypeName(enclosingType));
//          annotationIsGood = false;
//        }
//        if (fromRegion.isStatic()) {
//          context.reportError(a, "Source region \"{0}\" is static; cannot aggregate static regions", fromId);
//          annotationIsGood = false;
//        }
//        if (!srcRegions.add(fromDecl.getModel().regionName) ) {
//          context.reportError(a, "Source region \"{0}\" is aggregated more than once", fromId);
//          annotationIsGood = false;
//        }
//      }
//       
//      /* Make sure the destination region exists and is accessible. If the field
//       * is not final, then the region must be equal to or contain the field. If
//       * the field is final, then the destination region must be static if the
//       * field is static.  Destination region cannot be final or volatile.
//       */
//      if (toDecl == null) {
//        context.reportError(a, "Destination region \"{0}\" not found", toId);
//        annotationIsGood = false;        
//      } else {
//        final RegionModel toRegion = toDecl.getModel();
//        if (!toRegion.isAccessibleFromType(context.getBinder().getTypeEnvironment(), enclosingType)) {
//          context.reportError(a, "Source region \"{0}\" is not accessible to type \"{1}\"",
//              toRegion.regionName, JavaNames.getQualifiedTypeName(enclosingType));
//          annotationIsGood = false;
//        }
//
//        if (TypeUtil.isFinal(promisedFor)) {
//          if (promisedForIsStatic && !toRegion.isStatic()) {
//            context.reportError(a, "Destination region \"{0}\" is not static; must be static because the annotated field \"{1}\" is final and static",
//                toId, VariableDeclarator.getId(promisedFor));
//            annotationIsGood = false;
//          }
//        } else {
//          if (!toRegion.ancestorOf(fieldAsRegion)) {
//            context.reportError(a, "Destination region \"{0}\" is not an ancestor of the annotated non-final field \"{1}\"",
//                toId, VariableDeclarator.getId(promisedFor));
//            annotationIsGood = false;
//          }
//        }
//        
//        if (toRegion.isFinal()) {
//          context.reportError(a, "Destination region \"{0}\" is final", toId);
//          annotationIsGood = false;
//        }
//        if (toRegion.isVolatile()) {
//          context.reportError(a, "Destination region \"{0}\" is volatile", toId);
//          annotationIsGood = false;
//        }
//      }
//      
//      /* If the annotation is still okay, record the mapping.  If it's bad, we 
//       * won't look at this map anyway, so forget about it.  But we have to check
//       * result so we know that both the src and dest regions exist.
//       */
//      if (annotationIsGood) {
//        regionMap.put(fromDecl.getRegion(), toDecl.getRegion());
//      }
//    }
//    
//    if (annotationIsGood) {
//      // The Instance region must be mapped
//      if (!srcRegions.contains(RegionModel.INSTANCE)) {
//        context.reportError(a, "The region \"Instance\" must be mapped");
//        annotationIsGood = false;
//      }
//      
//      /* Aggregation must respect the region hierarchy: if the annotation maps
//       * Ri into Qi and Rj into Qj, and Ri is a subregion of Rj, then it must be
//       * that Qi is a subregion of Qj.
//       * 
//       * We check each pair of mappings.
//       */
//      final List<Map.Entry<IRegion, IRegion>> entries =
//        new ArrayList<Map.Entry<IRegion, IRegion>>(regionMap.entrySet());
//      final int numEntries = entries.size();
//      for (int first = 0; first < numEntries-1; first++) {
//        for (int second = first+1; second < numEntries; second++) {
//          final IRegion firstKey = entries.get(first).getKey();
//          final IRegion firstVal = entries.get(first).getValue();
//          final IRegion secondKey = entries.get(second).getKey();
//          final IRegion secondVal = entries.get(second).getValue();
//          if (firstKey.ancestorOf(secondKey)) {
//            if (!firstVal.ancestorOf(secondVal)) {
//              context.reportError(a, "Region \"{0}\" is a subregion of \"{1}\" in the referenced class, but region \"{2}\" is not a subregion of \"{3}\" in the referring class",
//                  truncateName(secondKey.toString()), truncateName(firstKey.toString()),
//                  truncateName(secondVal.toString()), truncateName(firstVal.toString()));
//              annotationIsGood = false;
//            }
//          } else if (secondKey.ancestorOf(firstKey)) {
//            if (!secondVal.ancestorOf(firstVal)) {
//              context.reportError(a, "Region \"{0}\" is a subregion of \"{1}\" in the referenced class, but region \"{2}\" is not a subregion of \"{3}\" in the referring class",
//                  truncateName(firstKey.toString()), truncateName(secondKey.toString()),
//                  truncateName(firstVal.toString()), truncateName(secondVal.toString()));
//              annotationIsGood = false;
//            }
//          }
//        }
//      }
//    }
//    
//    if (annotationIsGood) {
//      // Create the annotation drop and link it to the annotated field
//      final ExplicitUniqueInRegionPromiseDrop ap = new ExplicitUniqueInRegionPromiseDrop(a);
//      // We know unique is not null because annotationIsGood is true
//      ap.addDependent(unique);
//      fieldAsRegion.getModel().addDependent(ap);
//      return ap;
//    } else {
//      return null;
//    }
  }
  
  private static String truncateName(final String qualifiedName) {
    final int lastDot = qualifiedName.lastIndexOf('.');
    if (lastDot == -1) {
      return qualifiedName;
    } else {
      return qualifiedName.substring(lastDot+1);
    }
  }
  
  private interface IGlobalRegionState {
	  void clearState();
	  boolean isNameAlreadyUsed(IRNode type, String simpleName, String qualifiedName);
  }
  
  private static final class GlobalRegionState implements IGlobalRegionState {
	  private final Map<String,IGlobalRegionState> projects = 
		  new HashMap<String,IGlobalRegionState>();
	  
	  public GlobalRegionState() {
		  super();
	  }

	  public void clearState() {
		  projects.clear();
	  }

	  public boolean isNameAlreadyUsed(IRNode type, String simpleName, String qualifiedName) {
		  final IIRProject p = JavaProjects.getEnclosingProject(type);
		  if (p == null) {
			  System.out.println("No project for "+qualifiedName);
			  JavaProjects.getProject(type);
		  }
		  IGlobalRegionState state = projects.get(p.getName());
		  if (state == null) {
			  state = new ProjectRegionState();
			  projects.put(p.getName(), state);
		  }
		  return state.isNameAlreadyUsed(type, simpleName, qualifiedName);
	  }
  }
	  
  /**
   * Region state for a given project
   * @author Edwin
   */
  private static final class ProjectRegionState implements IGlobalRegionState {  
    private final Map<IRNode, Set<String>> classToFields = new HashMap<IRNode, Set<String>>();
    private final Set<String> qualifiedRegionNames = new HashSet<String>();
    
    public ProjectRegionState() {
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
    private final IGlobalRegionState regionState;
    
    public InitGlobalRegionState(final IGlobalRegionState grs) {
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
