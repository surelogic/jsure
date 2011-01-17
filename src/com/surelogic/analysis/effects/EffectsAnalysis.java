package com.surelogic.analysis.effects;

import java.util.*;

import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.targets.DefaultTargetFactory;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.MethodEffectsRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
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
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.sea.proxy.ProposedPromiseBuilder;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;

public class EffectsAnalysis extends AbstractWholeIRAnalysis<Effects,Void> {	
  private BindingContextAnalysis bca;
  
	public EffectsAnalysis() {
		super("EffectAssurance2");
	}

	@Override
	protected Effects constructIRAnalysis(final IBinder binder) {
	  bca = new BindingContextAnalysis(binder, true);
    return new Effects(binder);
	}

	@Override
	protected void clearCaches() {
		getAnalysis().clearCaches();
    if (bca != null) bca.clear();
	}
  
  @Override
  public Iterable<IRNode> analyzeEnd(IIRProject p) {
    finishBuild();
    return super.analyzeEnd(p);
  }
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
        checkEffectsForFile(compUnit);
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
				final Set<Effect> declFx =
					Effects.getDeclaredMethodEffects(member, member);

				/*
				 * Compare the declared effects to the actual effects of the
				 * implementation. (First make sure the method is not abstract or
				 * native.)
				 */
				if (!JavaNode.getModifier(member, JavaNode.ABSTRACT)
						&& !JavaNode.getModifier(member, JavaNode.NATIVE)) {
          final Set<Effect> implFx =
            getAnalysis().getImplementationEffects(member, bca);
					// only assure if there is declared intent
					if (declFx != null) {
						final Set<Effect> maskedFx = getAnalysis().maskEffects(implFx);

						final RegionEffectsPromiseDrop declaredEffectsDrop =
							MethodEffectsRules.getRegionEffectsDrop(member);

						if (maskedFx.isEmpty()) {
							final ResultDrop rd = new ResultDrop(Messages.toString(Messages.EMPTY_EFFECTS));
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
      
      if (!effect.isEmpty() &&
          !effect.isMaskable(getBinder()) &&
          !(isConstructor && rcvrNode != null && effect.affectsReceiver(rcvrNode))) {
        Target target = effect.getTarget();
        if (target instanceof InstanceTarget) {
          final IRNode ref = ((InstanceTarget) target).getReference();
          final Operator refOp = JJNode.tree.getOperator(ref);
          if (!(ReceiverDeclaration.prototype.includes(refOp) ||
              QualifiedReceiverDeclaration.prototype.includes(refOp) ||
              ParameterDeclaration.prototype.includes(refOp))) {
            // Convert to any instance
        	final IJavaType ty0 = getBinder().getJavaType(ref);
            IJavaType ty = ty0;
            IRegion region = target.getRegion();
            if (ty instanceof IJavaWildcardType) {
                // This is probably going to break in the future as another
                // missed case is discovered.
                IJavaType upper = ((IJavaWildcardType) ty).getUpperBound();
                if (upper == null) {
                  ty = getBinder().getTypeEnvironment().getObjectType();
                } else {
              	  ty = upper;
                }
              }
            if (ty instanceof IJavaTypeFormal) {
              // Cannot handle type formals in region annotations yet, convert
              // to Object
              ty = getBinder().getTypeEnvironment().getObjectType();
            } else if (ty instanceof IJavaArrayType) {
              // not presently supported in region annotations, convert to
              // any(Object):Instance
              ty = getBinder().getTypeEnvironment().getObjectType();
              region = RegionModel.getInstanceRegion();
            } 
            target = DefaultTargetFactory.PROTOTYPE.createAnyInstanceTarget(
                (IJavaReferenceType) ty, region); 
          }
        }

        final Target cleanedTarget = cleanInferredTarget(member, target);
        final Effect cleanedEffect =
          Effect.newEffect(null, effect.isRead(), cleanedTarget);
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
	  
	  return target.setRegion(region);
	}
	
	private boolean equals(String s1, String s2) {
		if (s1 != null) {
			return s1.equals(s2);
		}
		// s1 is null
		return s1 == s2;
	}
	
	private Set<Effect> filterEffects(final Set<Effect> input) {
	  final Set<Effect> result = new HashSet<Effect>();
	  for (final Effect testing : input) {
	    boolean isRedundant = false;
	    for (final Effect e : input) {
	      if ((testing != e) && testing.isCheckedBy(getBinder(), e)) {
	        isRedundant = true;
	        break;
	      }
	    }
	    if (!isRedundant) result.add(testing);
	  }
	  return result;
	}
	
	
	private void reportClassInitializationEffects(final IRNode typeDecl) {
	  final IRNode flowUnit = JavaPromise.getClassInitOrNull(typeDecl);
	  final Set<Effect> effects = 
	    getAnalysis().getEffectsQuery(
	        flowUnit, bca.getExpressionObjectsQuery(flowUnit)).getResultFor(flowUnit);
	  final Set<Effect> masked = getAnalysis().maskEffects(effects);
	  final String id = JJNode.getInfo(typeDecl);
	  for (final Effect e : masked) {
	    final InfoDrop drop = new InfoDrop("EffectAssurance");
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
			final IRNode constructor, final Set<Effect> declFx,
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

		addElaborationEvidence(rd, eff.getTargetElaborationEvidence());
		
		// Finish the drop
		setResultDependUponDrop(rd, src);
		rd.setConsistent(isConsistent);
		rd.setResultMessage(msgTemplate, msgArgs);
		
		return rd;
	}
	
	/**
	 * Recurses through the elaboration evidence, and adds it as supporting 
	 * information to the given result drop, in the order that the elaboration
	 * occurred.
	 */
	private void addElaborationEvidence(
	    final ResultDropBuilder rd, final ElaborationEvidence elabEvidence) {
	  if (elabEvidence != null) {
	    addElaborationEvidence(rd, elabEvidence.getElaboratedFrom().getElaborationEvidence());
	    rd.addSupportingInformation(elabEvidence.getMessage(), elabEvidence.getLink());
	  }
	}

	/**
	 * @param report
	 * @param method
	 * @param declFx
	 * @param implFx
	 */
	private void checkMethod(final RegionEffectsPromiseDrop declEffDrop, final IRNode method,
			final Set<Effect> declFx, final Set<Effect> implFx) {
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
	          Effects.unparseForPromise(inferred), method, method);
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
			final Set<Effect> declFx, final Set<Effect> missing) {
	  if (implEff.isEmpty()) {
	    constructResultDrop(methodBeingChecked, declEffDrop, true, implEff,
	        Messages.NO_EFFECTS, DebugUnparser.toString(implEff.getSource()));
	    return null;
	  } else {
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
	}
}
