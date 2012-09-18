package com.surelogic.analysis.effects;

import java.util.*;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.targets.AggregationEvidence;
import com.surelogic.analysis.effects.targets.AnonClassEvidence;
import com.surelogic.analysis.effects.targets.BCAEvidence;
import com.surelogic.analysis.effects.targets.CallEvidence;
import com.surelogic.analysis.effects.targets.DefaultTargetFactory;
import com.surelogic.analysis.effects.targets.EmptyEvidence;
import com.surelogic.analysis.effects.targets.EvidenceProcessor;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.IteratorEvidence;
import com.surelogic.analysis.effects.targets.MappedArgumentEvidence;
import com.surelogic.analysis.effects.targets.NoEvidence;
import com.surelogic.analysis.effects.targets.QualifiedReceiverConversionEvidence;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.EmptyEvidence.Reason;
import com.surelogic.analysis.effects.targets.UnknownReferenceConversionEvidence;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.dropsea.ir.AnalysisHintDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.method.constraints.RegionEffectsPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.ReadOnlyPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaCaptureType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaIntersectionType;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.bind.IJavaWildcardType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.Visibility;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class EffectsAnalysis extends AbstractAnalysisSharingAnalysis<BindingContextAnalysis,Effects,CompUnitPair> {	
	/** Should we try to run things in parallel */
	private static boolean wantToRunInParallel = false;

	/**
	 * Are we actually going to run things in parallel?  Not all JRE have the
	 * libraries we need to actually run in parallel.
	 */
	private static boolean willRunInParallel = wantToRunInParallel && !singleThreaded;
	
  private IJavaDeclaredType javaLangObject;
  
  private final Effects.ElaborationErrorCallback callback;
  
	public EffectsAnalysis() {
		super(willRunInParallel, CompUnitPair.class, "EffectAssurance2", BindingContextAnalysis.factory);
		callback = new ElaborationErrorReporter();
		if (runInParallel() == ConcurrencyType.INTERNALLY) {
			setWorkProcedure(new Procedure<CompUnitPair>() {
				public void op(CompUnitPair compUnit) {
					checkEffectsForFile(compUnit.getNode());
				}				
			});
		}
	}	
	
	@Override
	protected Effects constructIRAnalysis(final IBinder binder) {
	  javaLangObject = binder.getTypeEnvironment().getObjectType();
	  return new Effects(binder);
	}

	@Override
	protected void clearCaches() {
		// TODO mostly copied from LockAnalysis
		if (runInParallel() != ConcurrencyType.INTERNALLY) {
			Effects lv = getAnalysis();
			if (lv != null) {
				lv.clearCaches();
			}
		} else {
			analyses.clearCaches();
		}
		super.clearCaches();
	}
  
  @Override
  public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
    finishBuild();
    return super.analyzeEnd(env, p);
  }
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
		if (runInParallel() == ConcurrencyType.INTERNALLY) {
			queueWork(new CompUnitPair(cud.getCompilationUnitIRNode(), compUnit));
		} else {
			checkEffectsForFile(compUnit);
		}
        return true;
	}

	private void checkEffectsForFile(final IRNode compUnit) {
		/*
		 * Run around the tree looking for method and constructor declarations. This
		 * will catch declarations inside of inner classes and anonymous classes as
		 * well, which is good, because we used to miss those.
		 */
		final Iterator<IRNode> nodes = JJNode.tree.topDown(compUnit);
		while (nodes.hasNext()) {
			final IRNode member = nodes.next();
			final Operator op = JJNode.tree.getOperator(member);
			final boolean isConstructor =
				ConstructorDeclaration.prototype.includes(op);
			final boolean isMethod = MethodDeclaration.prototype.includes(op);
      /*
       * Compare the declared effects to the actual effects of the
       * implementation. (First make sure the method is not abstract or
       * native.)
       */
			if ((isConstructor || isMethod)
			    && !JavaNode.getModifier(member, JavaNode.NATIVE)) {
			  if (JavaNode.getModifier(member, JavaNode.ABSTRACT)) {
			    /* Abstract methods with effects annotation trivially assure. */
          final RegionEffectsPromiseDrop declaredEffectsDrop =
              MethodEffectsRules.getRegionEffectsDrop(member);
          if (declaredEffectsDrop != null) {
            final ResultDrop rd = new ResultDrop(member);
            rd.addCheckedPromise(declaredEffectsDrop);
            rd.setConsistent();
            rd.setMessage(Messages.EMPTY_EFFECTS);
          }
			  } else {
	        // NULL if there are no declared effects
	        final List<Effect> declFx =
	          Effects.getDeclaredMethodEffects(member, member);
	        final Set<Effect> implFx = getAnalysis().getImplementationEffects(
	            member, getSharedAnalysis(), callback);
	        // only assure if there is declared intent
	        if (declFx != null) {
	          final Set<Effect> maskedFx = getAnalysis().maskEffects(implFx);

	          // This won't be null because we know we have declared effects
	          final RegionEffectsPromiseDrop declaredEffectsDrop =
	            MethodEffectsRules.getRegionEffectsDrop(member);

	          if (maskedFx.isEmpty()) {
              final ResultDrop rd = new ResultDrop(member);
	            rd.addCheckedPromise(declaredEffectsDrop);
	            rd.setConsistent();
	            rd.setMessage(Messages.EMPTY_EFFECTS);
	          } else {
	            if (isConstructor) {
	              checkConstructor(declaredEffectsDrop, member, declFx, maskedFx);
	            } else {
	              checkMethod(declaredEffectsDrop, member, declFx, maskedFx);
	            }
	          }
	        } else {
	          // Infer effects
	          final Set<Effect> inferredEffects = inferEffects(
	              isConstructor, member, implFx);
	          new ProposedPromiseDrop(
	              "RegionEffects",
	              Effects.unparseForPromise(inferredEffects), member,
	              member, Origin.CODE);
	        }
			  }
			} else if (TypeUtil.isTypeDecl(member)) {
			  reportClassInitializationEffects(member);
			}			  
		}
	}

	
	
	private Set<Effect> inferEffects(
	    final boolean isConstructor, final IRNode member, 
	    final Set<Effect> implFx) {
    final IRNode rcvrNode;
    if (isConstructor) {
      rcvrNode = JavaPromise.getReceiverNodeOrNull(member);
    } else {
      rcvrNode = null;
    }
    final Set<Effect> inferred = new HashSet<Effect>();
    for (final Effect e : implFx) {
      final Effect maskedEffect = e.mask(getBinder());
      if (maskedEffect != null
          && !maskedEffect.isEmpty()
          && !(isConstructor && rcvrNode != null
              && maskedEffect.affectsReceiver(rcvrNode))) {
        Target target = maskedEffect.getTarget();
        if (target instanceof InstanceTarget) {
          final IRNode ref = ((InstanceTarget) target).getReference();
          final Operator refOp = JJNode.tree.getOperator(ref);
          if (!(ReceiverDeclaration.prototype.includes(refOp)
              || QualifiedReceiverDeclaration.prototype.includes(refOp)
              || ParameterDeclaration.prototype.includes(refOp))) {
            /* Find a declared type that we can use for the any instance 
             * effect.
             */
            IJavaType ty = getBinder().getJavaType(ref);
            IRegion region = target.getRegion();
            while (!(ty instanceof IJavaDeclaredType)) {
              if (ty instanceof IJavaCaptureType) {
                final IJavaType upper = ((IJavaCaptureType) ty).getUpperBound();
                ty = (upper == null) ? javaLangObject : upper;
              } else if (ty instanceof IJavaWildcardType) {
                // dead case?  Turned into Capture types, I think
                final IJavaType upper = ((IJavaWildcardType) ty).getUpperBound();
                ty = (upper == null) ? javaLangObject : upper;
              } else if (ty instanceof IJavaTypeFormal) {
                final IJavaType upper = ((IJavaTypeFormal) ty).getSuperclass(getBinder().getTypeEnvironment());
                ty = (upper == null) ? javaLangObject : upper;
              } else if (ty instanceof IJavaArrayType) {
                // not presently supported in region annotations, convert to
                // any(Object):Instance
                ty = javaLangObject;
                region = RegionModel.getInstanceRegion(member);
              } else if (ty instanceof IJavaIntersectionType) {
                ty = javaLangObject;
                region = RegionModel.getInstanceRegion(member);
              } else {
            	  throw new IllegalStateException("Unexpected type for "+DebugUnparser.toString(ref)+" : "+ty);
              }
            }
            target = DefaultTargetFactory.PROTOTYPE.createAnyInstanceTarget(
                (IJavaReferenceType) ty, region, NoEvidence.INSTANCE);
          }
        }

        final Target cleanedTarget = cleanInferredTarget(member, target);
        final Effect cleanedEffect =
            Effect.newEffect(null, maskedEffect.isRead(), cleanedTarget);
        inferred.add(cleanedEffect);
      }
    } 
    
    return filterEffects(inferred);
  }
	
	/**
	 * Make sure the given target would pass the effects sanity checking
	 * rules regarding region visibility.
	 */
	private Target cleanInferredTarget(final IRNode mdecl, final Target target) {
	  final Visibility methodViz = Visibility.getVisibilityOf(mdecl);
	  final Visibility promoteTo = methodViz;

	  /* Fix visibility in general.  Works because a region cannot be a subregion
	   * of a region less visible than it is.
	   */
	  IRegion region = target.getRegion();
	  while (!region.getVisibility().atLeastAsVisibleAs(promoteTo)) {
	    region = region.getParentRegion();
	  }
	  
    /* Protected regions are goofy because of package restrictions. Protected
     * region R cannot be used if the region is declared in a superclass S of
     * the class C that contains the method being annotated, and S is not in the
     * same package as C.
     */
	  if (region.getVisibility() == Visibility.PROTECTED) {
      final ITypeEnvironment typeEnvironment = getBinder().getTypeEnvironment();
	    final IRNode enclosingTypeNode = VisitUtil.getEnclosingType(mdecl);
	    final IJavaSourceRefType methodInType = JavaTypeFactory.getMyThisType(enclosingTypeNode);
	    final String enclosingPackageName = JavaNames.getPackageName(enclosingTypeNode);
	    boolean good = false;
	    while (region.getVisibility() == Visibility.PROTECTED && !good) {
	      final IRNode regionClassNode = VisitUtil.getClosestType(region.getNode());
	      final String regionPackageName = JavaNames.getPackageName(regionClassNode);
	      final IJavaSourceRefType regionClass = JavaTypeFactory.getMyThisType(regionClassNode);
        if (methodInType.isSubtype(typeEnvironment, regionClass)
            && !equals(enclosingPackageName, regionPackageName)) {
          region = region.getParentRegion();
	      } else {
	        good = true; 
	      }
	    }
	  }
	  
	  return target.degradeRegion(region);
	}
	
	private static boolean equals(final Object o1, final Object o2) {
	  return (o1 == null) ? o2 == null : o1.equals(o2);
	}
	
	/* Destroys the incoming set! */
	private Set<Effect> filterEffects(final Set<Effect> input) {
	  final Set<Effect> result = new HashSet<Effect>(input);
	  final Iterator<Effect> inputIter = input.iterator();
	  while (inputIter.hasNext()) {
	    final Effect testing = inputIter.next();
	    inputIter.remove();
	    for (final Effect e : input) {
	      if (testing.isCheckedBy(getBinder(), e)) {
	        result.remove(testing);
	        break;
	      } else if (e.isCheckedBy(getBinder(), testing)) {
	        result.remove(e);
	        // No break: testing may check other effects still
	      }
	    }
	  }
	  return result;
	}
	
	
	private void reportClassInitializationEffects(final IRNode typeDecl) {
	  final IRNode flowUnit = JavaPromise.getClassInitOrNull(typeDecl);
	  final Set<Effect> effects = 
	    getAnalysis().getEffectsQuery(
	        flowUnit, getSharedAnalysis().getExpressionObjectsQuery(flowUnit)).getResultFor(flowUnit);
	  final Set<Effect> masked = getAnalysis().maskEffects(effects);
	  final String id = JJNode.getInfo(typeDecl);
	  for (final Effect e : masked) {
	    final IRNode src = e.getSource() == null ? typeDecl : e.getSource();
	    final AnalysisHintDrop drop = AnalysisHintDrop.newSuggestion(src);
      drop.setCategory(Messages.DSC_EFFECTS_IN_CLASS_INIT);
      drop.setMessage(Messages.CLASS_INIT_EFFECT,
          id, e.toString(), DebugUnparser.toString(src));
	  }
	}

	/**
	 * Assure the effect annotations of a constructor.
	 *
	 * @param report
	 * @param constructor
	 * @param declFx
	 * @param implFx
	 */
	private void checkConstructor(final RegionEffectsPromiseDrop declEffDrop,
			final IRNode constructor, final List<Effect> declFx,
			final Set<Effect> implFx) {
		final IRNode receiverNode = PromiseUtil.getReceiverNode(constructor);
    final Set<Effect> missing = new HashSet<Effect>();
    final Set<ResultDrop> badDrops = new HashSet<ResultDrop>();
		for (final Effect eff : implFx) {
			/*
			 * First see if the effect is accounted for by the special case of
			 * affecting the Instance region of the receiver.
			 */
			if (eff.affectsReceiver(receiverNode)) {
				constructResultDrop(constructor, declEffDrop, true, eff, Messages.CONSTRUCTOR_RULE, eff);
			} else {
        final ResultDrop r = 
          checkEffect(constructor, declEffDrop, eff, declFx, missing);
        if (r != null) badDrops.add(r);				
			}
		}
		
    if (!missing.isEmpty()) {
      // Needed effects are those that are declared plus those that are missing
      missing.addAll(declFx);
      final Set<Effect> inferred = inferEffects(true, constructor, missing);
      final ProposedPromiseDrop proposed = 
        new ProposedPromiseDrop("RegionEffects", 
            Effects.unparseForPromise(inferred), 
            getPromiseContents(declEffDrop), 
            constructor, constructor, Origin.MODEL);
      for (final ResultDrop r : badDrops) {
        r.addProposal(proposed);
      }
    }       
	}

  private String getPromiseContents(final RegionEffectsPromiseDrop declEffDrop) {
	  return declEffDrop.getAAST().toString().substring("RegionEffects".length()).trim();
  }
	
	/**
   * @param report
   * @param method
   * @param declFx
   * @param implFx
   */
  private void checkMethod(final RegionEffectsPromiseDrop declEffDrop, final IRNode method,
  		final List<Effect> declFx, final Set<Effect> implFx) {
    final Set<Effect> missing = new HashSet<Effect>();
    final Set<ResultDrop> badDrops = new HashSet<ResultDrop>();
    for (final Effect eff : implFx) {
      final ResultDrop r = 
        checkEffect(method, declEffDrop, eff, declFx, missing);
      if (r != null) badDrops.add(r);
  	}
    if (!missing.isEmpty()) {
      // Needed effects are those that are declared plus those that are missing
      missing.addAll(declFx);
      final Set<Effect> inferred = inferEffects(false, method, missing);
      final ProposedPromiseDrop proposed = 
        new ProposedPromiseDrop("RegionEffects", 
            Effects.unparseForPromise(inferred), 
            getPromiseContents(declEffDrop), 
            method, method, Origin.MODEL);
      for (final ResultDrop r : badDrops) {
        r.addProposal(proposed);
      }
    }
  }

  /**
   * Check an implementation effect against a set of declared effects. Does
   * report a good chain of evidence right now if the implementation effect is
   * accounted for by multiple declared effects.
   * 
   * @param missing
   *          An <em>output</em> set to which the effect will be added if it is
   *          unaccounted for by the declared effects.
   * @return The result drop if the effect is <em>not</em> assured. Otherwise
   *         <code>null</code>.
   */
  private ResultDrop checkEffect(final IRNode methodBeingChecked,
    final RegionEffectsPromiseDrop declEffDrop, final Effect implEff,
  	final List<Effect> declFx, final Set<Effect> missing) {
  	boolean checked = false;
  	final Iterator<Effect> iter = declFx.iterator();
  	while (!checked && iter.hasNext()) {
  		final Effect eff2 = iter.next();
  		if (implEff.isCheckedBy(getBinder(), eff2)) {
  			checked = true;
  			constructResultDrop(methodBeingChecked, declEffDrop, true, implEff,
  					Messages.CHECKED_BY, implEff, eff2);
  		}
  	}
  	if (!checked) {
  	  missing.add(implEff);
  		return 
  		  constructResultDrop(methodBeingChecked, declEffDrop, false, implEff,
  		      Messages.UNACCOUNTED_FOR, implEff);
  	} else {
  	  return null;
  	}
  }

  /**
	 * @param declEffDrop
	 * @param eff
	 */
	private ResultDrop constructResultDrop(
	    final IRNode methodBeingChecked, final RegionEffectsPromiseDrop declEffDrop,
			final boolean isConsistent, final Effect eff, final int msgTemplate,
			final Object... msgArgs) {
	  final IRNode src = eff.getSource();
		final ResultDrop rd = new ResultDrop(src);
		rd.addCheckedPromise(declEffDrop);

		(new EvidenceAdder(getBinder(), rd)).accept(eff.getTarget().getEvidence());
		
		// Finish the drop
		rd.setConsistent(isConsistent);
		rd.setMessage(msgTemplate, msgArgs);
		
		return rd;
	}
	
	
	
	private final class ElaborationErrorReporter implements Effects.ElaborationErrorCallback {
	  public ElaborationErrorReporter() {
	    super();
	  }
	  
    public void writeToBorrowedReadOnly(
        final ReadOnlyPromiseDrop pd, final IRNode expr, final Target t) {
      final ResultDrop rd = new ResultDrop(expr);
      rd.addCheckedPromise(pd);
      rd.setConsistent(false);
      rd.setMessage(Messages.READONLY_REFERENCE);
      (new EvidenceAdder(getBinder(), rd)).accept(t.getEvidence());
    }	  
	}

	
	
	private static final class EvidenceAdder extends EvidenceProcessor {
	  private final ResultDrop resultDrop;
	  private final IBinder binder;
	  
	  public EvidenceAdder(final IBinder b, final ResultDrop rd) {
	    binder = b;
	    resultDrop = rd;
	  }
    
    @Override
    public void visitAggregationEvidence(final AggregationEvidence e) {
      final IRNode originalExpression = e.getOriginalExpression();
      /* Original expression is a field ref or a QualifiedReceiverDeclaration.
       * if it's an IFQR, then the destination of the aggregation is "this"
       */
      resultDrop.addSupportingInformation(
          e.getLink(), Messages.AGGREGATION_EVIDENCE,
          e.getOriginalRegion().getName(),
          DebugUnparser.toString(originalExpression),
          e.getMappedRegion().getName(),
          FieldRef.prototype.includes(originalExpression) ?
              DebugUnparser.toString(FieldRef.getObject(originalExpression)) : 
                "this");          
      accept(e.getMoreEvidence());
    }
    
    @Override
    public void visitAnonClassEvidence(final AnonClassEvidence e) {
      final Effect originalEffect = e.getOriginalEffect();
      resultDrop.addSupportingInformation(
          e.getLink(), Messages.ACE_EVIDENCE, originalEffect);
      accept(originalEffect.getTargetEvidence());
    }
    
    @Override
    public void visitBCAEvidence(final BCAEvidence e) {
      resultDrop.addSupportingInformation(
          e.getLink(), Messages.BCA_EVIDENCE,
          DebugUnparser.toString(e.getUseExpression()), 
          DebugUnparser.toString(e.getSourceExpression()));
      accept(e.getMoreEvidence());
    }
	  
    @Override
    public void visitCallEvidence(final CallEvidence e) {
      final RegionEffectsPromiseDrop cutpoint =
          MethodEffectsRules.getRegionEffectsDrop(e.getMethod());
      if (cutpoint != null) {
        resultDrop.addTrusted_and(cutpoint);
      }
    }
    
	  @Override
    public void visitEmptyEvidence(final EmptyEvidence e) {
	    final Reason reason = e.getReason();
	    if (reason == Reason.FINAL_FIELD) {
	      resultDrop.addSupportingInformation(e.getLink(), reason.getMessage(),
	          VariableDeclarator.getId(e.getLink()));
	    } else {
	      resultDrop.addSupportingInformation(e.getLink(), reason.getMessage());
	    }
	    accept(e.getMoreEvidence());
	  }
    
	  @Override
	  public void visitIteratorEvidence(final IteratorEvidence e) {
	    resultDrop.addSupportingInformation(
	        e.getLink(), Messages.ITERATOR_EFFECTS_CONVERSION);
	    accept(e.getMoreEvidence());
	  }
	  
    @Override
    public void visitMappedArgumentEvidence(final MappedArgumentEvidence e) {
      final RegionEffectsPromiseDrop cutpoint =
          MethodEffectsRules.getRegionEffectsDrop(e.getMethod());
      if (cutpoint != null) {
        resultDrop.addTrusted_and(cutpoint);
        
        final IRNode formal = e.getFormal();
        final Operator formalOp = JJNode.tree.getOperator(formal);
        final String formalString;
        if (ParameterDeclaration.prototype.includes(formalOp)) {
          formalString = ParameterDeclaration.getId(formal);
        } else if (ReceiverDeclaration.prototype.includes(formalOp)) {
          formalString = "this";
        } else if (QualifiedReceiverDeclaration.prototype.includes(formalOp)) {
          formalString = JavaNames.getFullTypeName(QualifiedReceiverDeclaration.getType(binder, formal)) + " .this";
        } else {
          // Shouldn't get here
          throw new IllegalStateException("Formal parameter is not a ParameterDeclaration or a Receiver");
        }
        final String actualString = DebugUnparser.toString(e.getActual());
        resultDrop.addSupportingInformation(
            e.getLink(), Messages.PARAMETER_EVIDENCE,
            formalString, actualString);
      }
    }
    
    @Override
    public void visitQualifiedReceiverConversionEvidence(
        final QualifiedReceiverConversionEvidence e) {
      final RegionEffectsPromiseDrop cutpoint =
          MethodEffectsRules.getRegionEffectsDrop(e.getMethod());
      if (cutpoint != null) {
        resultDrop.addTrusted_and(cutpoint);
        final String qString = JavaNames.getFullTypeName(
            QualifiedReceiverDeclaration.getType(
                binder, e.getQualifiedReceiver())) + " .this";
        final String tString = JavaNames.getQualifiedTypeName(e.getType());
        resultDrop.addSupportingInformation(
            e.getLink(), Messages.QRCVR_CONVERSION_EVIDENCE,
            qString, tString);
      }
    }
    
    @Override
    public void visitUnknownReferenceConversionEvidence(
        final UnknownReferenceConversionEvidence e) {
      resultDrop.addSupportingInformation(
          e.getUnknownRef(), Messages.UNKNOWN_REF_CONVERSION_EVIDENCE,
          DebugUnparser.toString(e.getUnknownRef()), 
          JavaNames.getQualifiedTypeName(e.getType()));
      super.visitUnknownReferenceConversionEvidence(e);
    }
	}
}
