package com.surelogic.annotation.rules;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.java.*;
import com.surelogic.aast.promise.*;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.AASTAdaptor.Node;
import com.surelogic.annotation.parse.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.annotation.test.TestResult;
import com.surelogic.annotation.test.TestResultType;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.drops.effects.RegionEffectsPromiseDrop;
import com.surelogic.dropsea.ir.drops.regions.RegionModel;
import com.surelogic.parse.AbstractNodeAdaptor;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.util.*;

public class MethodEffectsRules extends AnnotationRules {
	public static final String EFFECTS = "Effects";
	public static final String REGIONEFFECTS = "RegionEffects";
	
	private static final AnnotationRules instance = new MethodEffectsRules();

	private static final RegionEffects_ParseRule regionEffectsRule = new RegionEffects_ParseRule();

	
	
	public static AnnotationRules getInstance() {
		return instance;
	}

	public static RegionEffectsPromiseDrop getRegionEffectsDrop(IRNode mdecl) {
	  return getDrop(regionEffectsRule.getStorage(), mdecl);
	}

	@Override
	public void register(PromiseFramework fw) {
		registerParseRuleStorage(fw, regionEffectsRule);
	}

	/**
	 * Parse rules for the @RegionEffects annotation
	 * @author ethan
	 */
	public static class RegionEffects_ParseRule extends
			DefaultSLAnnotationParseRule<RegionEffectsNode, RegionEffectsPromiseDrop> {

		public RegionEffects_ParseRule() {
			super(REGIONEFFECTS, functionDeclOps, RegionEffectsNode.class);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.surelogic.annotation.AbstractAntlrParseRule#parse(com.surelogic.annotation.IAnnotationParsingContext,
		 *      org.antlr.runtime.Parser)
		 */
		@Override
		protected Object parse(IAnnotationParsingContext context,
				SLAnnotationsParser parser) throws Exception, RecognitionException {
			return parser.regionEffects().getTree();
		}

    @Override
    protected AASTNode finalizeAST(IAnnotationParsingContext context,
        AbstractNodeAdaptor.Node tn) {
      if (tn.getType() != SLAnnotationsParser.RegionEffects) {
        throw new IllegalArgumentException("Wrong type of node: "+
                                           SLAnnotationsParser.tokenNames[tn.getType()]);
      }
      final int num = tn.getChildCount();
      for (int i = 0; i < num; i++) {        
        AASTAdaptor.Node n = (Node) tn.getChild(i);
        if (n.getType() == SLAnnotationsParser.Writes) {
          markAsWrites(n);
        }
      }
      return tn.finalizeAST(context);
    }   
		
    private void markAsWrites(Node n) {
      final int num = n.getChildCount();
      for (int i = 0; i < num; i++) {
        AASTAdaptor.Node child = (Node) n.getChild(i);
        child.setModifier(JavaNode.WRITE);
      }
    }
    
		@Override
		protected IPromiseDropStorage<RegionEffectsPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					RegionEffectsPromiseDrop.class);
		}
		
		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<RegionEffectsNode, RegionEffectsPromiseDrop>(this,
					ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY, RegionRules.REGIONS_DONE) {
				@Override
				protected RegionEffectsPromiseDrop makePromiseDrop(
						RegionEffectsNode a) {
					return storeDropIfNotNull(a, scrubRegionEffects(getContext(), a));
				}
				
        @Override
        protected boolean processUnannotatedMethodRelatedDecl(final IRNode decl) {
          /*
          String qname = JavaNames.genQualifiedMethodConstructorName(decl);
          if ("java.util.ArrayList.iterator()".equals(qname)) {
           	  System.err.println("No declared effects for "+qname+" within "+VisitUtil.findRoot(decl));   
          }     	
          */
          boolean allGood = true;
          final IRegion regionAll = RegionModel.getAllRegion(decl);
          for (final IBinding context : getContext().getBinder(decl).findOverriddenParentMethods(decl)) {
            final IRNode overriddenMethod = context.getNode();
            final RegionEffectsPromiseDrop fxDrop = getRegionEffectsDrop(overriddenMethod);            
            if (fxDrop != null) {
              if (fxDrop.isAssumed()) {
            	  // Don't try to check consistency since it's an assumption
            	  continue;
              }
              final RegionEffectsNode overriddenFx = fxDrop.getAAST();
              // Each overridden effect must be satisfied by write(All)
              boolean good = true;
              outer: for (final EffectsSpecificationNode n1 : overriddenFx.getEffectsList()) {
                for (final EffectSpecificationNode ancestorSpec : n1.getEffectList()) {
                  if (!ancestorSpec.satisfiedByWritesAll(regionAll)) {
                    good = false;
                    allGood = false;
                    break outer;
                  }
                }
              }
              if (!good) {
                final RegionEffectsNode cloned = overriddenFx.cloneForProposal(
                    new ParameterMap(overriddenMethod, decl));
                final ProposedPromiseDrop p =
                    new ProposedPromiseDrop(
                        REGIONEFFECTS, cloned.unparseForPromise(),
                        decl, overriddenMethod, Origin.PROBLEM);
                getContext().reportErrorAndProposal(p,
                    "Cannot add effect writes java.lang.Object:All to the declared effects of {0}",
                    JavaNames.genQualifiedMethodConstructorName(overriddenMethod));
              }          
            } else {
              /* No annotation is same as @RegionEffects("writes java.lang.Object:All").  
               * Nothing to check in this case.  
               */
            }
          } 
          return allGood;
        }
			};
		}
	}

	/**
	 * Performs scrubbing identical to that of the above EffectsScrubber but does it at creation time since we have 1 node representing both reads and writes
	 * @param scrubberContext The {@link IAnnotationScrubberContext} to use while scrubbing
	 * @param node The {@link RegionEffectsNode} to be scrubbed
	 * @return A correct {@link RegionEffectsPromiseDrop} if everything in the AAST checked out, null otherwise
	 */
	private static RegionEffectsPromiseDrop scrubRegionEffects(
			IAnnotationScrubberContext scrubberContext, RegionEffectsNode node) {
		RegionEffectsPromiseDrop drop = null;
		final List<EffectsSpecificationNode> readsAndWrites = node.getEffectsList();

    final ITypeEnvironment typeEnv = scrubberContext.getBinder(node.getPromisedFor()).getTypeEnvironment();
		final IRNode promisedFor = node.getPromisedFor();
		final IRNode enclosingTypeNode = VisitUtil.getEnclosingType(promisedFor);
    final IJavaSourceRefType methodInType = JavaTypeFactory.getMyThisType(enclosingTypeNode);
    final String enclosingPackageName = JavaNames.getPackageName(enclosingTypeNode);
		final boolean isConstructor =
		  ConstructorDeclaration.prototype.includes(promisedFor);
		final boolean isStatic = TypeUtil.isStatic(promisedFor);

		// Used in consistency checking below
    final List<EffectSpecificationNode> overridingEffects = 
      new ArrayList<EffectSpecificationNode>();
    boolean allGood = true;
		// Iterate over the ReadNodes and WriteNodes - there should be a max of one
		// each
		for (final EffectsSpecificationNode esNode : readsAndWrites) {
			final List<EffectSpecificationNode> effects = esNode.getEffectList();
			overridingEffects.addAll(effects);
			
			boolean good = true;
			// For each ReadNode and WriteNode, check their effects specification
			for (final EffectSpecificationNode effectNode : effects) {
				final RegionSpecificationNode regionSpec = effectNode.getRegion();
				final ExpressionNode context = effectNode.getContext();

				boolean checkStaticStatus = true;
	      boolean regionShouldBeStatic = false;
	      boolean delayCheckingForConstructor = false;
	      String staticMsgTemplate = null;
	      good = true;
	      if (context instanceof ImplicitQualifierNode) {
	        /* Region must static if the method is static.  Otherwise,
	         * it can be static or instance.  But constructors cannot declare
	         * effects on  the receiver, so if the region is instance, and
	         * we are a constructor, we need to flag an error.  Check this below.
	         */
	        if (isStatic) {
	          regionShouldBeStatic = true;
	          staticMsgTemplate = "Because region \"{0}\" is not static it cannot be referenced unqualified on a static method";
	        } else {
	          checkStaticStatus = false;
	          delayCheckingForConstructor = isConstructor;
	        }
	      } else if (context instanceof ThisExpressionNode) {
	        /* (1) The annotation must be on a non-static method. (2) Not a
	         * constructor. (3) Region must not be static.
	         */
	        if (isStatic) {
	          scrubberContext.reportError(context, "Cannot refer to \"this\" from a static method");
	          good = false;
	        }
	        if (isConstructor) {
	          scrubberContext.reportError(context,
	              "Constructors cannot declare effects on the receiver because they are masked");
	          good = false;
	        }
	        regionShouldBeStatic = false;
	        staticMsgTemplate = "Because region \"{0}\" is static, it cannot be used in an instance target";
				} else if (context instanceof VariableUseExpressionNode) {
	        /* (1) The parameter parameter_name must be a parameter of the annotated
	         * method/constructor and be of a non-primitive type. (2) Region must be
	         * non-static.
	         */
	        // Note: Region will fail to bind if the parameter has primitive type
	        final VariableUseExpressionNode varUse = (VariableUseExpressionNode) context;
	        if (!varUse.bindingExists()) {
	          scrubberContext.reportError(context, "Parameter \"{0}\" does not exist", varUse.getId());
	          good = false;
	        }
	        regionShouldBeStatic = false;
	        staticMsgTemplate = "Because region \"{0}\" is static, it cannot be used in an instance target";
				} else if (context instanceof QualifiedThisExpressionNode) {
	        /* (1) The method/constructor being annotated must be in a non-static
           * inner class, or in the outermost class. (2) The class named by named_type must exist and be a
           * lexically enclosing class of the class that contains the
           * method/constructor being annotated. (3) The region identified by
           * region_name must be a non-static region that exists in type_name.
           */
	        final IRNode enclosingType = VisitUtil.getClosestType(promisedFor);
	        if (!TypeUtil.isOuter(enclosingType) && TypeUtil.isStatic(enclosingType)) {
	          scrubberContext.reportError(context, "Cannot reference qualified receivers from a static nested type.");
	          good = false;
	        }
          final QualifiedThisExpressionNode qthis = (QualifiedThisExpressionNode) context;
          
          if (!qthis.getType().typeExists()) { // (2a) does the type exist?
            scrubberContext.reportError(context,
                "Outer type \"{0}\" does not exist",
                qthis.getType().unparse(false));
            good = false;
          } else { // (2b) is it a lexically enclosing type?
            if (!VisitUtil.isAncestor(qthis.resolveType().getNode(), promisedFor)) {
              scrubberContext.reportError(context,
                  "Outer type \"{0}\" is not a lexically enclosing type",
                  qthis.getType().unparse(false));
              good = false;
            }
          }
	        regionShouldBeStatic = false;
	        staticMsgTemplate = "Because region \"{0}\" is static, it cannot be used in an instance target";
	      } else if (context instanceof AnyInstanceExpressionNode) {
	        /* (1) The class/interface identified by type_name must exist. (2) The
	         * region identified by region_name must a non-static region that exists
	         * in type_name.
	         */
	        final NamedTypeNode type = ((AnyInstanceExpressionNode) context).getType();
	        if (!type.typeExists()) {
	          scrubberContext.reportError(type, "Type \"{0}\" does not exist", type.getType());
	          good = false;
	        }
	        regionShouldBeStatic = false;
	        staticMsgTemplate = "Because region \"{0}\" is static, it cannot be used in an any-instance target";
	      } else if (context instanceof TypeExpressionNode) {
	        /* (1) The class/interface identified by type_name must exist. (2) The
	         * region identified by region_name must a static region that exists in
	         * type_name.
	         */
	        final ReturnTypeNode type = ((TypeExpressionNode) context).getType();
	        if (!type.typeExists()) {
	          scrubberContext.reportError(type, "Type does not exist");
	          good = false;
	        }
	        regionShouldBeStatic = true;
	        staticMsgTemplate = "Because region \"{0}\" is not static, it cannot be used in a class target";
	      } else { // Shouldn't get here
	        throw new IllegalArgumentException(
	            "Unknown target context: " + context.getClass().getCanonicalName());
	      }

        // Must always check if region exists
        if (!regionSpec.bindingExists()) {
          scrubberContext.reportError(regionSpec, "Region \"{0}\" does not exist", regionSpec.getId());
          good = false;
        } else {
          final IRegionBinding boundRegion = regionSpec.resolveBinding();
          final IRegion region = boundRegion.getRegion();
          // Check that the region has the desired static status
          final boolean regionIsStatic = region.isStatic();
          if (checkStaticStatus && (regionIsStatic != regionShouldBeStatic)) {
            scrubberContext.reportError(
                regionSpec, staticMsgTemplate, regionSpec.getId());
            good = false;
          }
          
          if (delayCheckingForConstructor && !regionIsStatic) {
            scrubberContext.reportError(context,
              "Constructors cannot declare effects on the receiver because they are masked");
            good = false;
          } else {
            /* Check that the method may access the region, and whether the
             * method-region combination preserved abstraction.
             */
            if (!region.isAccessibleFromType(typeEnv, enclosingTypeNode)) {
              scrubberContext.reportError(regionSpec, "Region \"{0}\" may not be accessed by {1,choice,0#constructor|1#method} \"{2}\": Visiblity is {3}",
                  regionSpec.getId(), (isConstructor ? 0 : 1), JavaNames.genMethodConstructorName(promisedFor), region.getVisibility());
              good = false;
            }          
            if (!isAccessibleToAllCallers(enclosingPackageName, methodInType, promisedFor, region, typeEnv)) {
              scrubberContext.reportError(regionSpec, "Region \"{0}\" is not accessible by all potential callers of {1,choice,0#constructor|1#method} \"{2}\": Region''s visibility is {3}",
                  regionSpec.getId(), (isConstructor ? 0 : 1), JavaNames.genMethodConstructorName(promisedFor), region.getVisibility().toString());
              good = false;
            }
          }
        }

				if (!good) {
					// Mark as unassociated
					TestResult expected = AASTStore.getTestResult(node);
					TestResult.checkIfMatchesResult(expected, TestResultType.UNASSOCIATED);
					allGood = false;
				}
			}
		}

		/* Check the annotation against the annotation on any declarations that are
		 * being overridden.
		 */
		if (allGood) {
      if (SomeFunctionDeclaration.prototype.includes(promisedFor)) { // Not a class init or other node
        // Compare against previous method declarations
        for (final IBinding context : scrubberContext.getBinder(promisedFor).findOverriddenParentMethods(promisedFor)) {
          final IRNode overriddenMethod = context.getNode();
          final ParameterMap paramMap = new ParameterMap(overriddenMethod, promisedFor);
          
          final RegionEffectsPromiseDrop fxDrop = getRegionEffectsDrop(overriddenMethod);
          if (fxDrop != null) {
            final RegionEffectsNode overriddenFx = fxDrop.getAAST();
            // Each overriding effect must satisfy one of the ancestor effects
            for (EffectSpecificationNode overridingSpec : overridingEffects) {
              boolean found = false;
              outer: for (final EffectsSpecificationNode n1 : overriddenFx.getEffectsList()) {
                for (final EffectSpecificationNode ancestorSpec : n1.getEffectList()) {
                  if (overridingSpec.satisfiesSpecfication(ancestorSpec, paramMap, typeEnv)) {
                    found = true;
                    break outer;
                  }
                }
              }
              if (!found) {
                allGood = false;
                final RegionEffectsNode cloned =
                    overriddenFx.cloneForProposal(paramMap);
                final ProposedPromiseDrop p = new ProposedPromiseDrop(
                    REGIONEFFECTS, cloned.unparseForPromise(),
                    node.toString().substring("RegionEffects".length()).trim(),
                    promisedFor, overriddenMethod, Origin.PROBLEM);
                scrubberContext.reportErrorAndProposal(p, 
                    "Cannot add effect {0} to the declared effects of {1}",
                    overridingSpec.standAloneUnparse(),
                    JavaNames.genQualifiedMethodConstructorName(overriddenMethod));
              }
            }          
          } else {
            /* No annotation is same as @RegionEffects("writes java.lang.Object:All").  
             * Nothing to check in this case.  
             */
          }
        }
      }
		}

		if (allGood) {
			drop = new RegionEffectsPromiseDrop(node);
		}
		return drop;
	}
	
	
	
	private static boolean isAccessibleToAllCallers(
	    final String packageName, final IJavaSourceRefType type,
	    final IRNode methodDecl, final IRegion region,
	    final ITypeEnvironment typeEnv) {
	  final Visibility methodViz = Visibility.getVisibilityOf(methodDecl);
	  final Visibility regionViz = region.getVisibility();
	  if (regionViz == Visibility.PUBLIC) {
	    // public is always accessible
	    return true;
	  } else if (regionViz == Visibility.PROTECTED) {
	    if (methodViz != Visibility.PUBLIC) {
	      /* See if the region is declared in a superclass S of the class C that
         * contains the method being annotated, and S is not in the same package
         * as the class that contains the method being annotated.
         */
	      final IRNode regionClassNode = VisitUtil.getClosestType(region.getNode());
        final String regionPackageName = JavaNames.getPackageName(regionClassNode);
	      final IJavaSourceRefType regionClass = JavaTypeFactory.getMyThisType(regionClassNode);
	      if (type.isSubtype(typeEnv, regionClass) && !packageName.equals(regionPackageName)) {
	        return false;
	      } else {
	        return true;
	      }	      
	    } else {
	      return false;
	    }
	  } else if (regionViz == Visibility.DEFAULT) {
	    return methodViz.compareTo(Visibility.DEFAULT) <= 0;
	  } else if (regionViz == Visibility.PRIVATE) {
	    return (methodViz == Visibility.PRIVATE);
	  } else {
	    // should never get here
	    return false;
	  }
	}
}
