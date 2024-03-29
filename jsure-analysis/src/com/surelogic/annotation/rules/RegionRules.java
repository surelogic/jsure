/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/RegionRules.java,v 1.45 2007/12/21 18:36:37 aarong Exp $*/
package com.surelogic.annotation.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.AASTNode;
import com.surelogic.aast.AnnotationOrigin;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.promise.FieldMappingsNode;
import com.surelogic.aast.promise.InRegionNode;
import com.surelogic.aast.promise.NewRegionDeclarationNode;
import com.surelogic.aast.promise.RegionMappingNode;
import com.surelogic.aast.promise.RegionNameNode;
import com.surelogic.aast.promise.RegionSpecificationNode;
import com.surelogic.aast.promise.UniqueInRegionNode;
import com.surelogic.aast.promise.UniqueMappingNode;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.regions.FieldRegion;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.NullAnnotationParseRule;
import com.surelogic.annotation.ParseResult;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AASTStore;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.AnnotationScrubberContext;
import com.surelogic.annotation.scrub.IAnnotationTraversalCallback;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.annotation.scrub.SimpleScrubber;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.InRegionPromiseDrop;
import com.surelogic.dropsea.ir.drops.MapFieldsPromiseDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.uniqueness.ExplicitUniqueInRegionPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.SimpleUniqueInRegionPromiseDrop;
import com.surelogic.javac.Projects;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.PromiseDropSeqStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.AnnotationDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.Visibility;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

public class RegionRules extends AnnotationRules {
  public static final String REGION = "Region";
  public static final String IN_REGION = "InRegion";
  public static final String MAP_FIELDS = "MapFields";
  public static final String SIMPLE_UNIQUE_IN_REGION = "UniqueInRegion";
  public static final String EXPLICIT_UNIQUE_IN_REGION = "Unique Mapping"; // Never meant to be parsed
  public static final String EXPLICIT_BORROWED_IN_REGION = "Borrowed Mapping"; // Never meant to be parsed
  public static final String REGION_INITIALIZER = "RegionInitializer";
  public static final String REGIONS_DONE = "RegionsDone";
  public static final String STATIC = "Static";
  public static final String STATIC_SUFFIX = '.'+STATIC;
  
  private static final AnnotationRules instance = new RegionRules();  

  private static final IGlobalRegionState globalRegionState = new GlobalRegionState();
  private static final Map<IRNode, ImplicitRegions> implicitRegionMap =
      new ConcurrentHashMap<IRNode, ImplicitRegions>();
  
  private static final InitGlobalRegionState initState     = new InitGlobalRegionState(globalRegionState);
  private static final Region_ParseRule regionRule         = new Region_ParseRule(globalRegionState);
  private static final InRegion_ParseRule inRegionRule     = new InRegion_ParseRule();
  private static final MapFields_ParseRule mapFieldsRule   = new MapFields_ParseRule();

  private static final ExplicitUniqueInRegion_ParseRule explicitUniqueInRegionRule =
    new ExplicitUniqueInRegion_ParseRule();
  private static final SimpleUniqueInRegion_ParseRule simpleUniqueInRegionRule = 
	  new SimpleUniqueInRegion_ParseRule();
  
  private static final SimpleScrubber regionsDone = new SimpleScrubber(
      REGIONS_DONE, REGION, IN_REGION,
      SIMPLE_UNIQUE_IN_REGION, EXPLICIT_UNIQUE_IN_REGION) {
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
  
  public static ExplicitUniqueInRegionPromiseDrop getExplicitUniqueInRegion(IRNode vdecl) {
    return getDrop(explicitUniqueInRegionRule.getStorage(), vdecl);
  }

  public static SimpleUniqueInRegionPromiseDrop getSimpleUniqueInRegion(IRNode vdecl) {
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
    protected IAnnotationScrubber makeScrubber() {
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
      final AnnotationScrubberContext context,
      final IGlobalRegionState regionState, final NewRegionDeclarationNode a) {
    final IRNode promisedFor = a.getPromisedFor();

    boolean annotationIsGood = true;
    
    // Region must be uniquely named
    final String simpleName = a.getId();
    final String qualifiedName = computeQualifiedName(a);
    
    if (!a.isStatic()) {
      if (AnnotationDeclaration.prototype.includes(promisedFor)) {
        context.reportModelingProblem(a, "Non-static regions may not be declared in annotation types");
      } else if (InterfaceDeclaration.prototype.includes(promisedFor)) {
        context.reportModelingProblem(a, "Non-static regions may not be declared in interfaces");
      }
    }
    
    if (regionState.isNameAlreadyUsed(promisedFor, simpleName, qualifiedName)) {
      context.reportModelingProblem(a, "Region \"{0}\" is already declared in class", simpleName);
      annotationIsGood = false;
    }
    
    final RegionSpecificationNode parentRegionNode = a.getRegionParent();
    RegionModel parentModel = null;
    if (parentRegionNode != null) {
      final String parentName = parentRegionNode.getId();
      final IRegionBinding boundParent = parentRegionNode.resolveBinding();
      // The parent region must exist
      if (boundParent == null) {
        context.reportModelingProblem(a, "Parent region \"{0}\" does not exist", parentName);
        annotationIsGood = false;
      } else {
        parentModel = boundParent.getModel();
        
        
        // The region cannot be final and cannot be volatile
        if (parentModel.isFinal()) {
          context.reportModelingProblem(a, "Parent region \"{0}\" is final", parentName);
          annotationIsGood = false;
        } else if(parentModel.isVolatile()) {
          context.reportModelingProblem(a, "Parent region \"{0}\" is volatile", parentName);
          annotationIsGood = false;
        } else {
          // The parent region must be accessible
          if (!parentModel.isAccessibleFromType(context.getBinder(promisedFor).getTypeEnvironment(), promisedFor)) {
            context.reportModelingProblem(a, "Region \"{0}\" is not accessible to type \"{1}\"", 
                parentModel.getName(), JavaNames.getRelativeTypeNameDotSep(promisedFor));
            annotationIsGood = false;
          }
          
          // Region cannot be more visible than its parent 
          final Visibility parentViz = parentModel.getVisibility();
          final Visibility myViz = a.getVisibility();
          if (!parentViz.atLeastAsVisibleAs(myViz)) {
            context.reportModelingProblem(
                a, "Region \"{0}\" ({1}) is more visible than its parent \"{2}\" ({3})",
                simpleName, myViz.nameLowerCase(),
                parentName, parentViz.nameLowerCase());
            annotationIsGood = false;
          }
    
          // Instance region cannot contain a static region
          final boolean regionIsStatic = a.isStatic();
          final boolean parentIsStatic = parentModel.isStatic();
          if (regionIsStatic && !parentIsStatic) {
            context.reportModelingProblem(a, "Static region cannot have a non-static parent");
            annotationIsGood = false;
          }
        }
        
        /* Cycles are prevented by the binder: Names cannot be used if they
         * haven't been seen lexically.  (No forward lookups)
         */
      }
    } else {
      /* Parent model is INSTANCE if the region is not static, STATIC of the
       * current class if region is static (or ALL if it is STATIC).  Region ALL has no parent. 
       */
      if (!qualifiedName.equals(RegionModel.ALL)) {
    	  if (a.isStatic()) {
    		  if (STATIC.equals(simpleName)) {
    			  parentModel = RegionModel.getAllRegion(a.getPromisedFor());
    		  } else {
    			  parentModel = RegionModel.getStaticRegionForClass(a.getPromisedFor());
    		  }
    	  } else {
    		  parentModel = RegionModel.getInstanceRegion(a.getPromisedFor());
    	  }
      }
    }
    
    if (annotationIsGood) {
      RegionModel model = RegionModel.create(a, qualifiedName);  
      //System.out.println("Adding region "+model.getName()+" to "+JavaNames.getFullTypeName(a.getPromisedFor())+" -- "+a.getPromisedFor());
      
      if (parentModel != null) { // parentModel == null if region is ALL
        model.addDependent(parentModel);
      }
      return model;
    } else {
      //RegionModel.invalidate(qualifiedName, a.getPromisedFor());
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
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<InRegionNode, InRegionPromiseDrop>(
          this, ScrubberType.BY_HIERARCHY, REGION, SIMPLE_UNIQUE_IN_REGION) {
      	@Override
      	protected AnnotationHandler<InRegionNode> getPreprocessor() {
      	  return new SimpleHandler<InRegionNode>() {
            @Override
            public void processAASTs(
                final IAnnotationTraversalCallback<InRegionNode> cb,
                final IRNode decl, final List<InRegionNode> l) {
              preprocessInRegion(decl, l);
            }
            @Override
        	public void finishProcessing() {
              generateNeededRegions();
              super.finishProcessing();
            }
          };
        }

        @Override
        protected PromiseDrop<InRegionNode> makePromiseDrop(InRegionNode a) {
          return storeDropIfNotNull(a, scrubInRegion(getContext(), a));          
        }
      };
    }
  }

  
  private static class ImplicitRegionInfo {
    private final IAASTRootNode comesFrom;
    private boolean isStatic;
    private Visibility visibility;
    
    public ImplicitRegionInfo(
        final IRNode fieldDecl, final IAASTRootNode InRegionNode) {
      comesFrom = InRegionNode;
      final int mods =JavaNode.getModifiers(fieldDecl);
      isStatic = JavaNode.getModifier(mods, JavaNode.STATIC);
      visibility = Visibility.getVisibilityOf(fieldDecl);
    }
    
    public void update(final IRNode fieldDecl) {
      final int mods = JavaNode.getModifiers(fieldDecl);
      isStatic |= JavaNode.getModifier(mods, JavaNode.STATIC);
      final Visibility fieldViz = Visibility.getVisibilityOf(fieldDecl);
      if (fieldViz.atLeastAsVisibleAs(visibility)) {
        visibility = fieldViz;
      }
    }
    
    public void createRegion(final IRNode classDecl, final String name) {
      final int mods = (isStatic ? JavaNode.STATIC : 0) | visibility.getModifier();
      final NewRegionDeclarationNode regionDecl = 
          new NewRegionDeclarationNode(0, mods, name, null);
      regionDecl.setPromisedFor(classDecl, null);
      
      final RegionModel regionModel =
          RegionModel.create(regionDecl, computeQualifiedName(regionDecl));
      final RegionModel parentModel = isStatic ?
        RegionModel.getStaticRegionForClass(classDecl) :
        RegionModel.getInstanceRegion(classDecl);
      regionModel.addDependent(parentModel);
      regionModel.setVirtual(true);
      
      regionRule.getStorage().add(classDecl, regionModel);

      AASTStore.linkAsInferred(comesFrom, regionModel);
    }
  }
  
  private static class ImplicitRegions {
    private final Map<String, ImplicitRegionInfo> nameToInfo =
        new HashMap<String, ImplicitRegionInfo>();
    
    public void addOrUpdateRegion(
        final String name, final IRNode varDecl, final IAASTRootNode inRegion) {
      final IRNode fieldDecl = JJNode.tree.getParent(JJNode.tree.getParent(varDecl));
      ImplicitRegionInfo regionInfo = nameToInfo.get(name);
      if (regionInfo == null) {
        regionInfo = new ImplicitRegionInfo(fieldDecl, inRegion);
        nameToInfo.put(name, regionInfo);
      } else {
        regionInfo.update(fieldDecl);
      }
    }
    
    public void createRegionDeclarations(final IRNode classDecl) {
      for (final Map.Entry<String, ImplicitRegionInfo> entry : nameToInfo.entrySet()) {
        entry.getValue().createRegion(classDecl, entry.getKey());
      }
    }
  }
  
  private static void preprocessUniqueInRegion(
      final IRNode classDecl, final List<UniqueInRegionNode> annos) {
    /*
     * Look at each UniqueInRegion on a final field and see if the named region
     * actually exists. If not we add it to a list of regions to be created.
     * This list is shared with InRegion. We only care about final fields
     * because non-final fields will have @InRegion annotations generated for
     * them, and the preprocessor for InRegion will handle them.
     */
    final ImplicitRegions neededRegions = new ImplicitRegions();
    implicitRegionMap.put(classDecl, neededRegions); // save so that InRegions can get it later
    for (final UniqueInRegionNode uniqueInRegion : annos) {
      final IRNode promisedFor = uniqueInRegion.getPromisedFor();
      if (TypeUtil.isJavaFinal(promisedFor)) {
        final RegionSpecificationNode parent = uniqueInRegion.getSpec();
        if (parent.resolveBinding() == null) {
          neededRegions.addOrUpdateRegion(
              parent.getId(), promisedFor, uniqueInRegion);
        }
      }
    }
  }
  
  private static void preprocessInRegion(
      final IRNode classDecl, final List<InRegionNode> annos) {
    /* 
     * Look at each InRegion and see if the named region actually exists
     */
    ImplicitRegions neededRegions = implicitRegionMap.get(classDecl); // See if UniqueInRegion left us a map 
    if (neededRegions == null) {
    	neededRegions = new ImplicitRegions();
    } else {
    	//System.out.println("Got ImplicitRegions for "+JavaNames.getTypeName(classDecl));
    }
    for (final InRegionNode inRegion : annos) {
      final RegionSpecificationNode parent = inRegion.getSpec();
      if (parent.resolveBinding() == null) {
        final IRNode promisedFor = parent.getPromisedFor();
        neededRegions.addOrUpdateRegion(
        		parent.getId(), promisedFor, inRegion);
      }
    }
    neededRegions.createRegionDeclarations(classDecl);
    implicitRegionMap.remove(classDecl); // clean up 
  }
  
  private static void generateNeededRegions() {
	// Should supplement the code above in preprocessInRegion(),
	// but could actually replace the last two lines
	for(Map.Entry<IRNode, ImplicitRegions> e : implicitRegionMap.entrySet()) {
		ImplicitRegions neededRegions = e.getValue();
		IRNode classDecl = e.getKey();
	    neededRegions.createRegionDeclarations(classDecl);
	}
	implicitRegionMap.clear();
  }
  
  private static InRegionPromiseDrop scrubInRegion(
      final AnnotationScrubberContext context, final InRegionNode a) {
    /* The name of a region must be unique. There is nothing to check here in
     * practice because the Java compiler makes sure that a class does not
     * declare two fields with the same name, and we already checking that
     * regions declared using @Region do not have the same name as a field.
     */
  
    if (a != null) {
      final IRNode promisedFor = a.getPromisedFor();
      final String parentName = a.getSpec().getId();
      boolean annotationIsGood = true;
      
      /* scrubUniqueInRegion(), which always runs before us, checks that the
       * field doesn't have both UniqueInRegion and InRegion.  Must run before
       * us because it can generate derived InRegion annotations.
       */
      
      if (TypeUtil.isJSureFinal(promisedFor)) {
        context.reportModelingProblem(a, "Field \"{0}\" is final: it cannot be given a super region because it is not a region",
            VariableDeclarator.getId(promisedFor));
        annotationIsGood = false;
      }
      
      // The region's parent region must exist in the class.
      final IRegionBinding parentDecl = a.getSpec().resolveBinding();
      if (parentDecl == null) {
        context.reportModelingProblem(a, "Parent region \"{0}\" does not exist", parentName);
        annotationIsGood = false;
      } else {
        final RegionModel parentModel = parentDecl.getModel();
        
        // The region cannot be final and cannot be volatile
        if (parentModel.isFinal()) {
          context.reportModelingProblem(a, "Parent region \"{0}\" is final", parentName);
          annotationIsGood = false;
        } else if(parentModel.isVolatile()) {
          context.reportModelingProblem(a, "Parent region \"{0}\" is volatile", parentName);
          annotationIsGood = false;
        } else {
          // Parent region must not create a cycle
          if (RegionModel.getInstance(promisedFor).ancestorOf(parentModel)) {
            context.reportModelingProblem(a, "Cycle detected: Field \"{0}\" is already an ancestor of region \"{1}\"",
                VariableDeclarator.getId(promisedFor), parentModel.getName());
            annotationIsGood = false;
          }
          
          // The parent region must be accessible
          final IRNode enclosingType = VisitUtil.getEnclosingType(promisedFor);
          if (!parentModel.isAccessibleFromType(
              context.getBinder(enclosingType).getTypeEnvironment(), enclosingType)) {
            context.reportModelingProblem(a, "Region \"{0}\" is not accessible to type \"{1}\"", 
                parentModel.getName(), JavaNames.getRelativeTypeNameDotSep(enclosingType));
            annotationIsGood = false;
          }

          // Region cannot be more visible than its parent 
          final Visibility parentViz = parentModel.getVisibility();
          final Visibility myViz = Visibility.getVisibilityOf(
              JJNode.tree.getParent(JJNode.tree.getParent(promisedFor)));
          if (!parentViz.atLeastAsVisibleAs(myViz)) {
            context.reportModelingProblem(a, "Region \"{0}\" ({1}) is more visible than its parent \"{2}\" ({3})",
                VariableDeclarator.getId(promisedFor), myViz.nameLowerCase(),
                parentName, parentViz.nameLowerCase());
            annotationIsGood = false;
          }
    
          // Instance region cannot contain a static region
          final boolean regionIsStatic = TypeUtil.isStatic(promisedFor);
          final boolean parentIsStatic = parentModel.isStatic();
          if (regionIsStatic && !parentIsStatic) {
            context.reportModelingProblem(a, "Static region cannot have a non-static parent");
            annotationIsGood = false;
          }
        }
      }
      
      if (annotationIsGood) {
        final InRegionPromiseDrop mip = new InRegionPromiseDrop(a);
        setupRegionModelForField(mip, parentDecl, mip.getPromisedFor());
        return mip;
      }
    }
    return null;
  }

  public static void setupRegionModelForField(PromiseDrop<?> promise, IRegionBinding parentDecl, IRNode field) {
      final RegionModel fieldModel = RegionModel.getInstance(field).getModel();
      final RegionModel parentModel = parentDecl.getModel();
      fieldModel.addDependent(parentModel);
      fieldModel.addDependent(promise);
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
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<FieldMappingsNode, MapFieldsPromiseDrop>(this, ScrubberType.UNORDERED, 
                                                         new String[] { IN_REGION }, REGION) {
        @Override
        protected PromiseDrop<FieldMappingsNode> makePromiseDrop(FieldMappingsNode a) {
          return storeDropIfNotNull(a, scrubMapFields(getContext(), a));          
        }
      };
    }     
  }
  
  private static MapFieldsPromiseDrop scrubMapFields(AnnotationScrubberContext context, FieldMappingsNode a) {
    // Check fields
    MapFieldsPromiseDrop drop = new MapFieldsPromiseDrop(a);
    
    for(RegionSpecificationNode spec : a.getFieldsList()) {
      if (!spec.bindingExists()) {
    	  return null;
  	  }	  
      FieldRegion field = (FieldRegion) spec.resolveBinding().getRegion();
      InRegionNode mapInto = inRegionRule.makeRoot((RegionSpecificationNode)a.getTo().cloneTree());
      mapInto.copyPromisedForContext(field.getNode(), a, AnnotationOrigin.SCOPED_ON_TYPE);
      
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
      // Also creates UniqueMappingNodes
      return parser.uniqueInRegion().getTree();
    }
  
    @Override
    protected IPromiseDropStorage<SimpleUniqueInRegionPromiseDrop> makeStorage() {
      return SinglePromiseDropStorage.create(name(), SimpleUniqueInRegionPromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<UniqueInRegionNode, SimpleUniqueInRegionPromiseDrop>(
          this, ScrubberType.UNORDERED, REGION, UniquenessRules.UNIQUE) {
      	@Override
      	protected boolean needToScrubBindings(UniqueInRegionNode a) {
    	    return false;
     	  }
      	
        @Override
        protected AnnotationHandler<UniqueInRegionNode> getPreprocessor() {
          return new SimpleHandler<UniqueInRegionNode>() {
            @Override
            public void processAASTs(
                final IAnnotationTraversalCallback<UniqueInRegionNode> cb,
                final IRNode decl, final List<UniqueInRegionNode> l) {
              preprocessUniqueInRegion(decl, l);
            }
          };
        }

        @Override
        protected SimpleUniqueInRegionPromiseDrop makePromiseDrop(UniqueInRegionNode a) {
          return storeDropIfNotNull(a, scrubSimpleUniqueInRegion(getContext(), a));          
        }
      };
    }
  }
  
  static SimpleUniqueInRegionPromiseDrop scrubSimpleUniqueInRegion(
  	  final AnnotationScrubberContext context,
  	  final UniqueInRegionNode a) {
    final IRNode promisedFor = a.getPromisedFor();
    
    boolean isGood = true;
    
    if (UniquenessRules.isBorrowed(promisedFor)) {
      context.reportModelingProblem(
          a, "Cannot be annotated with both @UniqueInRegion and @Borrowed");
      isGood = false;
    }
    if (UniquenessRules.isUnique(promisedFor)) {
      context.reportModelingProblem(
          a, "Cannot be annotated with both @UniqueInRegion and @Unique");
      isGood = false;
    }

    // Cannot already have an @InRegion annotation
    /* This is tricky to check.  UniqueInRegion can generate an InRegion
     * promise.  So we need to check if there is already an InRegion promise
     * before we generate it.  If we don't do this, another part of the
     * promise framework barfs because there will be two InRegion annotations
     * on the same node.  So we have to check for the presence of InRegion
     * before the InRegion promise has been processed.  This requires looking
     * at the raw AST, which is slower than dealing with the promises directly.
     */
    for (final InRegionNode n : AASTStore.getASTsByPromisedFor(promisedFor, InRegionNode.class)) {
      context.reportModelingProblem(a, "Cannot be annotated with both @UniqueInRegion and @InRegion");
      AASTStore.removeAST(n);
      isGood = false;
    }
        
    // Field must be reference typed
    final IJavaType type = context.getBinder(promisedFor).getJavaType(promisedFor);
    if (type instanceof IJavaPrimitiveType) {
      context.reportModelingProblem(a, "Annotated field must have a reference type");
      isGood = false;
    }
    
    // Field cannot be volatile
    if (TypeUtil.isVolatile(promisedFor)) {
      context.reportModelingProblem(a, "Annotated field cannot be volatile");
      isGood = false;
    }
    
    /* If the region exists, we need to check that it is compatible with the 
     * the field being aggregated.  If the field doesn't exist we pass it through
     * so that @InRegion can infer a new @Region annotation.
     */
    final String name = a.getSpec().getId();
    final IRegionBinding destDecl = a.getSpec().resolveBinding();
    if (destDecl == null) {
//      context.reportError(a, "Destination region \"{0}\" does not exist", name);
//      isGood = false;
    } else {
      // Named region cannot be final
      final RegionModel destRegion = destDecl.getModel();
      if (destRegion.isFinal()) {
        context.reportModelingProblem(a, "Destination region \"{0}\" is final", name);
        isGood = false;
      }
      
      // Named region cannot be volatile
      if (destRegion.isVolatile()) {
        context.reportModelingProblem(a, "Destination region \"{0}\" is volatile", name);
        isGood = false;
      }
      
      // Named region must be static if the field is static
      if (TypeUtil.isStatic(promisedFor) && !destRegion.isStatic()) {
        context.reportModelingProblem(a, "Destination region \"{0}\" must be static because the annotated field is static", name);
        isGood = false;
      }
      
      // Named region must be accessible
      final IRNode enclosingType = VisitUtil.getEnclosingType(promisedFor);
      if (!destRegion.isAccessibleFromType(
          context.getBinder(enclosingType).getTypeEnvironment(), enclosingType)) {
        context.reportModelingProblem(a, "Destination region \"{0}\" is not accessible to type \"{1}\"",
            name, JavaNames.getRelativeTypeNameDotSep(enclosingType));
        isGood = false;
      }
    }
    
    if (isGood) {
      final SimpleUniqueInRegionPromiseDrop drop = 
        new SimpleUniqueInRegionPromiseDrop(a);

      // If field is not final then add an @InRegion annotation
      if (!TypeUtil.isJSureFinal(promisedFor)) {
        final RegionSpecificationNode regionSpec =
          (RegionSpecificationNode) a.getSpec().cloneTree();
        final InRegionNode inRegion =
          new InRegionNode(a.getOffset(), regionSpec);
        inRegion.copyPromisedForContext(promisedFor, a, AnnotationOrigin.GENERATED_FOR_DECL);
        AASTStore.addDerived(inRegion, drop);
      } else {
//      setupRegionModelForField((PromiseDrop<?>) drop, destDecl, (drop).getPromisedFor());
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
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<UniqueMappingNode, ExplicitUniqueInRegionPromiseDrop>(
          this, ScrubberType.UNORDERED, IN_REGION, REGION, UniquenessRules.UNIQUE) {
        @Override
        protected ExplicitUniqueInRegionPromiseDrop makePromiseDrop(UniqueMappingNode a) {
          return storeDropIfNotNull(a, scrubExplicitUniqueInRegion(getContext(), a));          
        }
      };
    }
  }
  
  @SuppressWarnings("null")
  private static ExplicitUniqueInRegionPromiseDrop scrubExplicitUniqueInRegion(
      final AnnotationScrubberContext context, final UniqueMappingNode a) {
    final IRNode promisedFor = a.getPromisedFor();   
    final IRNode enclosingType = VisitUtil.getEnclosingType(promisedFor);
    
    boolean isGood = true;
    
    if (UniquenessRules.isBorrowed(promisedFor)) {
      context.reportModelingProblem(
          a, "Cannot be annotated with both @UniqueInRegion and @Borrowed");
      isGood = false;
    }
    if (UniquenessRules.isUnique(promisedFor)) {
      context.reportModelingProblem(
          a, "Cannot be annotated with both @UniqueInRegion and @Unique");
      isGood = false;
    }
    
    // Field must be reference typed
    final IJavaType type = context.getBinder(promisedFor).getJavaType(promisedFor);
    if (type instanceof IJavaPrimitiveType) {
      context.reportModelingProblem(a, "Annotated field must have a reference type");
      isGood = false;
    }
    
    // Field must be final
    if (!TypeUtil.isJSureFinal(promisedFor)) {
      context.reportModelingProblem(a, "Annotated field must be final");
      isGood = false;
    }

    // process the mapping
    final Set<RegionModel> srcRegions = new HashSet<RegionModel>();
    final Map<IRegion, IRegion> regionMap = new HashMap<IRegion, IRegion>();
    final boolean promisedForIsStatic = TypeUtil.isStatic(promisedFor);
    for(final RegionMappingNode mapping : a.getSpec().getMappingList()) {
      final RegionNameNode fromNode = mapping.getFrom();
      final IRegionBinding fromDecl = fromNode.resolveBinding();
      final String fromId           = fromNode.getId();
      final RegionSpecificationNode toNode = mapping.getTo();
      final IRegionBinding toDecl   = toNode.resolveBinding();
      final String toId             = toNode.getId();

      // Make sure the source region exists
      if (fromDecl == null) {
        context.reportModelingProblem(a, "Source region \"{0}\" not found in aggregated class", fromId);
        isGood = false;
      } else {
        final RegionModel fromRegion = fromDecl.getModel();
        
        // Cannot be static
        if (fromDecl.getRegion().isStatic()) {
          context.reportModelingProblem(a, "Source region \"{0}\" is static", fromId);
          isGood = false;
        }
        // Cannot be aggregated more than once
        if (!srcRegions.add(fromRegion) ) {
          context.reportModelingProblem(a, "Source region \"{0}\" is aggregated more than once", fromId);
          isGood = false;
        }
        // Must be accessible
        if (!fromRegion.isAccessibleFromType(
            context.getBinder(enclosingType).getTypeEnvironment(), enclosingType)) {
          context.reportModelingProblem(a,
              "Source region \"{0}\" is not accessible to type \"{1}\"",
              fromRegion.getName(),
              JavaNames.getRelativeTypeNameDotSep(enclosingType));
          isGood = false;
        }
      }
      
      // Make sure the dest region exists
      if (toDecl == null)  {
        context.reportModelingProblem(a, "Destination region \"{0}\" not found", toId);
        isGood = false;
      } else {
        // Named region cannot be final
        final RegionModel toRegion = toDecl.getModel();
        if (toRegion.isFinal()) {
          context.reportModelingProblem(a, "Destination region \"{0}\" is final", toId);
          isGood = false;
        }
        
        // Named region cannot be volatile
        if (toRegion.isVolatile()) {
          context.reportModelingProblem(a, "Destination region \"{0}\" is volatile", toId);
          isGood = false;
        }
        
        // Region must be static if the field is static
        if (promisedForIsStatic && !toRegion.isStatic()) {
          context.reportModelingProblem(a, "Destination region \"{0}\" must be static because the annotated field is static", toId);
          isGood = false;
        }
        if (!toRegion.isAccessibleFromType(
            context.getBinder(enclosingType).getTypeEnvironment(), enclosingType)) {
          context.reportModelingProblem(a,
              "Source region \"{0}\" is not accessible to type \"{1}\"",
              toRegion.getName(),
              JavaNames.getRelativeTypeNameDotSep(enclosingType));
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
      context.reportModelingProblem(a, "The region \"Instance\" must be mapped");
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
              context.reportModelingProblem(a,
                      "Region \"{0}\" is a subregion of \"{1}\" in the aggregated class, but region \"{2}\" is not a subregion of \"{3}\" in the aggregating class",
                      truncateName(secondKey.toString()),
                      truncateName(firstKey.toString()),
                      truncateName(secondVal.toString()),
                      truncateName(firstVal.toString()));
              isGood = false;
            }
          } else if (secondKey.ancestorOf(firstKey)) {
            if (!secondVal.ancestorOf(firstVal)) {
              context.reportModelingProblem(a,
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

	  @Override
    public void clearState() {
		  projects.clear();
	  }

	  @Override
    public boolean isNameAlreadyUsed(IRNode type, String simpleName, String qualifiedName) {
		  final IIRProject p = Projects.getEnclosingProject(type);
		  if (p == null) {
			  System.out.println("No project for "+qualifiedName);
			  Projects.getProject(type);
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
    
    @Override
    public synchronized boolean isNameAlreadyUsed(
        final IRNode type, final String simpleName, final String qualifiedName) {
      boolean isDuplicate = getFieldNames(type).contains(simpleName) || qualifiedRegionNames.contains(qualifiedName);
      qualifiedRegionNames.add(qualifiedName);
      return isDuplicate;
    }
       
    @Override
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
