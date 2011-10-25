package com.surelogic.analysis.effects;

import java.util.*;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.targets.DefaultTargetFactory;
import com.surelogic.analysis.effects.targets.EmptyEvidence;
import com.surelogic.analysis.effects.targets.EvidenceProcessor;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.NoEvidence;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.EmptyEvidence.Reason;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.MethodEffectsRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaCaptureType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
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
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ReadOnlyPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.sea.proxy.InfoDropBuilder;
import edu.cmu.cs.fluid.sea.proxy.ProposedPromiseBuilder;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;

public class EffectsAnalysis extends AbstractAnalysisSharingAnalysis<BindingContextAnalysis,Effects,IRNode> {	
	/** Should we try to run things in parallel */
	private static boolean wantToRunInParallel = false;

	/**
	 * Are we actually going to run things in parallel?  Not all JRE have the
	 * libraries we need to actually run in parallel.
	 */
	private static boolean willRunInParallel = wantToRunInParallel && !singleThreaded;
	
  private IJavaDeclaredType javaLangObject;
  
  private final Effects.ElaborationCallback callback;
  
	public EffectsAnalysis() {
		super(willRunInParallel, IRNode.class, "EffectAssurance2", BindingContextAnalysis.factory);
		callback = new ElaborationErrorReporter();
		if (runInParallel() == ConcurrencyType.INTERNALLY) {
			setWorkProcedure(new Procedure<IRNode>() {
				public void op(IRNode compUnit) {
					checkEffectsForFile(compUnit);
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
			queueWork(compUnit);
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
			if (isConstructor || isMethod) {
			  // NULL if there are no declared effects
				final List<Effect> declFx =
					Effects.getDeclaredMethodEffects(member, member);

				/*
				 * Compare the declared effects to the actual effects of the
				 * implementation. (First make sure the method is not abstract or
				 * native.)
				 */
				if (!JavaNode.getModifier(member, JavaNode.ABSTRACT)
						&& !JavaNode.getModifier(member, JavaNode.NATIVE)) {
          final Set<Effect> implFx = getAnalysis().getImplementationEffects(
              member, getSharedAnalysis(), callback);
					// only assure if there is declared intent
					if (declFx != null) {
						final Set<Effect> maskedFx = getAnalysis().maskEffects(implFx);

						// This won't be null because we know we have declared effects
						final RegionEffectsPromiseDrop declaredEffectsDrop =
							MethodEffectsRules.getRegionEffectsDrop(member);

						if (maskedFx.isEmpty()) {
							final ResultDropBuilder rd = ResultDropBuilder.create(this, Messages.toString(Messages.EMPTY_EFFECTS));
							rd.addCheckedPromise(declaredEffectsDrop);
							setResultDependUponDrop(rd, member);
							rd.setConsistent();
							rd.setResultMessage(Messages.EMPTY_EFFECTS);
						} else {
							if (isConstructor) {
								checkConstructor(declaredEffectsDrop, member, declFx, maskedFx);
							} else {
								checkMethod(declaredEffectsDrop, member, declFx, maskedFx);
							}
						}
					} else {
					  // Infer effects
					  final Set<Effect> inferredEffects = 
					    inferEffects(isConstructor, member, implFx);
            final ProposedPromiseBuilder pb =
              new ProposedPromiseBuilder("RegionEffects",
                  Effects.unparseForPromise(inferredEffects), member, member);
            handleBuilder(pb);
					}
				}
			} else if(TypeUtil.isTypeDecl(member)) {
			  reportClassInitializationEffects(member);
			}			  
		}
	}

	
	
	private Set<Effect> inferEffects(
	    final boolean isConstructor, final IRNode member, 
	    final Set<Effect> implFx) {
    final Set<Effect> inferred = new HashSet<Effect>();
    for (final Effect effect : implFx) {
      final IRNode rcvrNode;
      if (isConstructor) {
        rcvrNode = JavaPromise.getReceiverNodeOrNull(member);
      } else {
        rcvrNode = null;
      }

      if (!effect.isEmpty()
          && !effect.isMaskable(getBinder())
          && !(isConstructor && rcvrNode != null && effect
              .affectsReceiver(rcvrNode))) {
        Target target = effect.getTarget();
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
              }
            }
            target = DefaultTargetFactory.PROTOTYPE.createAnyInstanceTarget(
                (IJavaReferenceType) ty, region, NoEvidence.INSTANCE);
          }
        }

        final Target cleanedTarget = cleanInferredTarget(member, target);
        final Effect cleanedEffect = Effect.newEffect(null, effect.isRead(),
            cleanedTarget);
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
	    final IJavaDeclaredType methodInType = JavaTypeFactory.getMyThisType(enclosingTypeNode);
	    final String enclosingPackageName = JavaNames.getPackageName(enclosingTypeNode);
	    boolean good = false;
	    while (region.getVisibility() == Visibility.PROTECTED && !good) {
	      final IRNode regionClassNode = VisitUtil.getClosestType(region.getNode());
	      final String regionPackageName = JavaNames.getPackageName(regionClassNode);
	      final IJavaDeclaredType regionClass = JavaTypeFactory.getMyThisType(regionClassNode);
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
	
	private boolean equals(String s1, String s2) {
		if (s1 != null) {
			return s1.equals(s2);
		}
		// s1 is null
		return s1 == s2;
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
	    final InfoDropBuilder drop = InfoDropBuilder.create(this, "EffectAssurance", InfoDrop.factory);
	    drop.setCategory(null);
	    final IRNode src = e.getSource() == null ? typeDecl : e.getSource();
      setResultDependUponDrop(drop, src);
      drop.setCategory(Messages.DSC_EFFECTS_IN_CLASS_INIT);
      drop.setResultMessage(Messages.CLASS_INIT_EFFECT,
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
    final Set<ResultDropBuilder> badDrops = new HashSet<ResultDropBuilder>();
		for (final Effect eff : implFx) {
			/*
			 * First see if the effect is accounted for by the special case of
			 * affecting the Instance region of the receiver.
			 */
			if (eff.affectsReceiver(receiverNode)) {
				constructResultDrop(constructor, declEffDrop, true, eff, Messages.CONSTRUCTOR_RULE, eff);
			} else {
        final ResultDropBuilder r = 
          checkEffect(constructor, declEffDrop, eff, declFx, missing);
        if (r != null) badDrops.add(r);				
			}
		}
		
    if (!missing.isEmpty()) {
      // Needed effects are those that are declared plus those that are missing
      missing.addAll(declFx);
      final Set<Effect> inferred = inferEffects(true, constructor, missing);
      final ProposedPromiseBuilder proposed = 
        new ProposedPromiseBuilder("RegionEffects", 
            Effects.unparseForPromise(inferred), constructor, constructor);
      for (final ResultDropBuilder r : badDrops) {
        r.addProposal(proposed);
      }
    }       
	}

	/**
	 * @param declEffDrop
	 * @param eff
	 */
	private ResultDropBuilder constructResultDrop(
	    final IRNode methodBeingChecked, final RegionEffectsPromiseDrop declEffDrop,
			final boolean isConsistent, final Effect eff, final int msgTemplate,
			final Object... msgArgs) {
		final ResultDropBuilder rd =
		  ResultDropBuilder.create(this, Messages.toString(msgTemplate));
		rd.addCheckedPromise(declEffDrop);

		final IRNode src = eff.getSource();
		final Operator op = JJNode.tree.getOperator(src);
		if (op instanceof CallInterface) {
			final IRNode mdecl = getBinder().getBinding(src);
			final RegionEffectsPromiseDrop cutpoint =
				MethodEffectsRules.getRegionEffectsDrop(mdecl);
			// No drops to make if there are no declared effects
			if (cutpoint != null) {
				rd.addTrustedPromise(cutpoint);
				// Add parameter bindings
				final Map<IRNode, IRNode> bindings =
					MethodCallUtils.constructFormalToActualMap(
							// XXX: This is not correct because it should really use the
							// specific constructor implementations when analyzing field inits
							// and instance inits, but this is not possible from here. Yet
							// another reason why I need to chang the EffectsVisitor to build
							// it's own COE.
							getBinder(), src, getBinder().getBinding(src), methodBeingChecked);
				for (final Map.Entry<IRNode, IRNode> binding : bindings.entrySet()) {
					final IRNode formal = binding.getKey();
					final Operator formalOp = JJNode.tree.getOperator(formal);
					final String formalString;
					if (ParameterDeclaration.prototype.includes(formalOp)) {
						formalString = ParameterDeclaration.getId(formal);
					} else if (ReceiverDeclaration.prototype.includes(formalOp)) {
						formalString = "this";
					} else if (QualifiedReceiverDeclaration.prototype.includes(formalOp)) {
						formalString = JavaNames.getFullTypeName(QualifiedReceiverDeclaration.getType(getBinder(), formal)) + " .this";
					} else {
						// Shouldn't get here
						throw new IllegalStateException("Formal parameter is not a ParameterDeclaration or a Receiver");
					}
					final IRNode actual = binding.getValue();
					final String actualString = DebugUnparser.toString(actual);

					rd.addSupportingInformation(actual, 
                            Messages.PARAMETER_EVIDENCE,
					        formalString, actualString);
				}
			}
		}

		(new EvidenceAdder(rd)).accept(eff.getTarget().getEvidence());

//		addElaborationEvidence(rd, eff.getTargetElaborationEvidence());
//		addAdditionalEvidence(rd, eff.getTarget());
		
		// Finish the drop
		setResultDependUponDrop(rd, src);
		rd.setConsistent(isConsistent);
		rd.setResultMessage(msgTemplate, msgArgs);
		
		return rd;
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
	  final Set<ResultDropBuilder> badDrops = new HashSet<ResultDropBuilder>();
	  for (final Effect eff : implFx) {
	    final ResultDropBuilder r = 
	      checkEffect(method, declEffDrop, eff, declFx, missing);
	    if (r != null) badDrops.add(r);
		}
	  if (!missing.isEmpty()) {
	    // Needed effects are those that are declared plus those that are missing
	    missing.addAll(declFx);
	    final Set<Effect> inferred = inferEffects(false, method, missing);
	    final ProposedPromiseBuilder proposed = 
	      new ProposedPromiseBuilder("RegionEffects", 
	          Effects.unparseForPromise(inferred), 
	          declEffDrop.getAST().toString().substring("RegionEffects".length()).trim(), 
	          method, method);
	    for (final ResultDropBuilder r : badDrops) {
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
	private ResultDropBuilder checkEffect(final IRNode methodBeingChecked,
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
	
	
	
	private final class ElaborationErrorReporter implements Effects.ElaborationCallback {
	  public ElaborationErrorReporter() {
	    super();
	  }
	  
    public void writeToBorrowedReadOnly(
        final ReadOnlyPromiseDrop pd, final IRNode expr, final Target t) {
      final ResultDropBuilder rd = ResultDropBuilder.create(
          EffectsAnalysis.this, Messages.toString(Messages.READONLY_REFERENCE));
      rd.addCheckedPromise(pd);
      setResultDependUponDrop(rd, expr);
      rd.setConsistent(false);
      rd.setResultMessage(Messages.READONLY_REFERENCE);
      (new EvidenceAdder(rd)).accept(t.getEvidence());
//      addElaborationEvidence(rd, t.getElaborationEvidence()); // Definitely useful: we get here from elaboration
//      addAdditionalEvidence(rd, t); // XXX: Useless?
    }	  
	}
	
	 
//  /**
//   * Recurses through the elaboration evidence, and adds it as supporting 
//   * information to the given result drop, in the order that the elaboration
//   * occurred.
//   */
//  private void addElaborationEvidence(
//      final ResultDropBuilder rd, final ElaborationEvidence elabEvidence) {
//    if (elabEvidence != null) {
//      addElaborationEvidence(rd, elabEvidence.getElaboratedFrom().getElaborationEvidence());
//      rd.addSupportingInformation(elabEvidence.getMessage(), elabEvidence.getLink());
//    }
//  }
//
//  private void addAdditionalEvidence(final ResultDropBuilder rd, final Target t) {
//    if (t instanceof EmptyTarget) {
//      final Reason r = ((EmptyTarget) t).getReason();
//      if (r != null) {
//        rd.addSupportingInformation(null, r.getMessage());
//      }
//    }
//  }

	
	private static final class EvidenceAdder extends EvidenceProcessor {
	  private final ResultDropBuilder resultDrop;
	  
	  public EvidenceAdder(final ResultDropBuilder rd) {
	    resultDrop = rd;
	  }
    
    @Override
    public void visitAggregationEvidence(final AggregationEvidence e) {
      final IRNode originalExpression = e.getOriginalExpression();
      resultDrop.addSupportingInformation(
          e.getLink(), Messages.AGGREGATION_EVIDENCE,
          e.getOriginalRegion().getName(),
          DebugUnparser.toString(originalExpression),
          e.getMappedRegion().getName(),
          DebugUnparser.toString(FieldRef.getObject(originalExpression)));
          
      accept(e.getMoreEvidence());
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
	}
}
