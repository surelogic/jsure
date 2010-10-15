package com.surelogic.analysis.effects;

import java.text.MessageFormat;
import java.util.*;

import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.uwm.BindingContextAnalysis;
import com.surelogic.annotation.rules.MethodEffectsRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
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
	public boolean doAnalysisOnAFile(CUDrop cud, IRNode compUnit, IAnalysisMonitor monitor) {
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
					// only assure if there is declared intent
					if (declFx != null) {
					  final Set<Effect> implFx = getAnalysis().getImplementationEffects(member, bca);
						final Set<Effect> maskedFx = getAnalysis().maskEffects(implFx);

						final RegionEffectsPromiseDrop declaredEffectsDrop =
							MethodEffectsRules.getRegionEffectsDrop(member);

						if (maskedFx.isEmpty()) {
							final ResultDrop rd = new ResultDrop("EffectAssurance_msgEmptyEffects");
							rd.addCheckedPromise(declaredEffectsDrop);
							setResultDependUponDrop(rd, member);
							rd.setConsistent();
							rd.setResultMessage(Messages.EffectAssurance_msgEmptyEffects);
						} else {
							if (isConstructor) {
								checkConstructor(declaredEffectsDrop, member, declFx, maskedFx);
							} else {
								checkMethod(declaredEffectsDrop, member, declFx, maskedFx);
							}
						}
					} else {
						/*
						 * TODO: Suggest inferred effects here...
						 */
					}
				}
			} else if(TypeUtil.isTypeDecl(member)) {
			  reportClassInitializationEffects(member);
			}			  
		}
	}
	
	private void reportClassInitializationEffects(final IRNode typeDecl) {
	  final IRNode flowUnit = JavaPromise.getClassInitOrNull(typeDecl);
	  final Set<Effect> effects = 
	    getAnalysis().getEffectsQuery(
	        flowUnit, bca.getExpressionObjectsQuery(flowUnit)).getResultFor(flowUnit);
	  final Set<Effect> masked = getAnalysis().maskEffects(effects);
	  final String id = JJNode.getInfo(typeDecl);
	  for (final Effect e : masked) {
	    final InfoDrop drop = new InfoDrop();
	    drop.setCategory(null);
	    final IRNode src = e.getSource() == null ? typeDecl : e.getSource();
      setResultDependUponDrop(drop, src);
	    drop.setMessage(
	        MessageFormat.format("{0}.<clinit> has effect \"{1}\" from {2}",
	            id, e.toString(), DebugUnparser.toString(src)));
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
		for (final Effect eff : implFx) {
			/*
			 * First see if the effect is accounted for by the special case of
			 * affecting the Instance region of the receiver.
			 */
			if (eff.affectsReceiver(receiverNode)) {
				constructResultDrop(constructor, declEffDrop, true, eff, Messages.EffectAssurance_msgContructorRule, eff);
			} else {
				checkEffect(constructor, declEffDrop, eff, declFx);
			}
		}
	}

	/**
	 * @param declEffDrop
	 * @param eff
	 */
	private void constructResultDrop(
	    final IRNode methodBeingChecked, final RegionEffectsPromiseDrop declEffDrop,
			final boolean isConsistent, final Effect eff, final int msgTemplate,
			final Object... msgArgs) {
		final ResultDrop rd = new ResultDrop(Integer.toString(msgTemplate));
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
                            Messages.EffectAssurance_msgParameterEvidence,
					        formalString, actualString);
				}
			}
		}

		addElaborationEvidence(rd, eff.getTargetElaborationEvidence());
		
		// Finish the drop
		setResultDependUponDrop(rd, src);
		rd.setConsistent(isConsistent);
		rd.setResultMessage(msgTemplate, msgArgs);
	}
	
	/**
	 * Recurses through the elaboration evidence, and adds it as supporting 
	 * information to the given result drop, in the order that the elaboration
	 * occurred.
	 */
	private void addElaborationEvidence(
	    final ResultDrop rd, final ElaborationEvidence elabEvidence) {
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
	  for (final Effect eff : implFx) {
			checkEffect(method, declEffDrop, eff, declFx);
		}
	}

	/**
	 * Check an implementation effect against a set of declared effects. Does
	 * report a good chain of evidence right now if the implementation effect is
	 * accounted for by multiple declared effects.
	 *
	 * @param report
	 * @param declFx
	 * @param implEff
	 */
	private void checkEffect(final IRNode methodBeingChecked,
	    final RegionEffectsPromiseDrop declEffDrop, final Effect implEff,
			final Set<Effect> declFx) {
	  if (implEff.isEmpty()) {
	    constructResultDrop(methodBeingChecked, declEffDrop, true, implEff,
	        Messages.EffectsAssurance_msgNoEffects,
	        DebugUnparser.toString(implEff.getSource()));
	  } else {
  		boolean checked = false;
  		final Iterator<Effect> iter = declFx.iterator();
  		while (!checked && iter.hasNext()) {
  			final Effect eff2 = iter.next();
  			if (implEff.isCheckedBy(getBinder(), eff2)) {
  				checked = true;
  				constructResultDrop(methodBeingChecked, declEffDrop, true, implEff,
  						Messages.EffectAssurance_msgCheckedBy, implEff, eff2);
  			}
  		}
  		if (!checked) {
  			constructResultDrop(methodBeingChecked, declEffDrop, false, implEff,
  					Messages.EffectAssurance_msgUnaccountedFor, implEff);
  		}
	  }
	}
}
