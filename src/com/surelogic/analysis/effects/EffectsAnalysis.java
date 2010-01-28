/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/effects/EffectsAnalysis.java,v 1.94 2009/02/20 19:58:39 aarong Exp $*/
package com.surelogic.analysis.effects;

import java.text.MessageFormat;
import java.util.*;

import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.common.i18n.I18N;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.*;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.tree.Operator;

public class EffectsAnalysis extends AbstractWholeIRAnalysis<Effects,Void> {	
	public EffectsAnalysis() {
		super("EffectAssurance2");
	}
	
	public void init(IIRAnalysisEnvironment env) {
		// Nothing to do
	}

	@Override
	protected Effects constructIRAnalysis(final IBinder binder) {
        return new Effects(binder, new BindingContextAnalysis(binder));
	}

	@Override
	protected void clearCaches() {
		getAnalysis().clearCaches();
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
					  /* Can use null as the constructor context because member IS a 
					   * constructor or method declaration.
					   */
						final Set<Effect> implFx = getAnalysis().getEffects(member, null);
						final Set<Effect> maskedFx = maskEffects(implFx);
						final String modelName = genModelName(member, op, declFx);

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
								checkConstructor(declaredEffectsDrop, member, declFx, maskedFx,
										modelName);
							} else {
								checkMethod(declaredEffectsDrop, member, declFx, maskedFx,
										modelName);
							}
						}
					} else {
						/*
						 * TODO: Suggest inferred effects here...
						 */
					}
				}
			}
		}
	}

	private Set<Effect> maskEffects(final Set<Effect> effects) {
		if (effects.isEmpty()) {
			return Collections.emptySet();
		}
		final Set<Effect> newEffects = new HashSet<Effect>();
		for (final Effect e : effects) {
			if (!e.isMaskable(getBinder())) newEffects.add(e);
		}
		return Collections.unmodifiableSet(newEffects);
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
			final Set<Effect> implFx, String modelName) {
		final IRNode receiverNode = PromiseUtil.getReceiverNode(constructor);
		for (final Effect eff : implFx) {
			/*
			 * First see if the effect is accounted for by the special case of
			 * affecting the Instance region of the receiver.
			 */
			if (eff.affectsReceiver(receiverNode)) {
				constructResultDrop(declEffDrop, true, eff, Messages.EffectAssurance_msgContructorRule, eff);
			} else {
				checkEffect(declEffDrop, eff, declFx, modelName);
			}
		}
	}

	/**
	 * @param declEffDrop
	 * @param eff
	 */
	private void constructResultDrop(final RegionEffectsPromiseDrop declEffDrop,
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
							getBinder(), src, getBinder().getBinding(src), PromiseUtil.getEnclosingMethod(src));
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

					rd.addSupportingInformation(
					    I18N.res(Messages.EffectAssurance_msgParameterEvidence,
					        formalString, actualString),
					    actual);
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
			final Set<Effect> declFx, final Set<Effect> implFx, String modelName) {
		/*
		 * method parameter is currently unused, but may be useful in the future.
		 */
	  for (final Effect eff : implFx) {
			checkEffect(declEffDrop, eff, declFx, modelName);
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
	private void checkEffect(final RegionEffectsPromiseDrop declEffDrop, final Effect implEff,
			final Set<Effect> declFx, String modelName) {
		boolean checked = false;
		final Iterator<Effect> iter = declFx.iterator();
		while (!checked && iter.hasNext()) {
			final Effect eff2 = iter.next();
			if (implEff.checkEffect(getBinder(), eff2)) {
				checked = true;
				constructResultDrop(declEffDrop, true, implEff,
						Messages.EffectAssurance_msgCheckedBy, implEff, eff2);
			}
		}
		if (!checked) {
			constructResultDrop(declEffDrop, false, implEff,
					Messages.EffectAssurance_msgUnaccountedFor, implEff);
		}
	}

	/**
	 * Generates a model name from the promise.
	 *
	 * @param node
	 *          a constructor or method declaration
	 * @param op
	 *          the operator for node
	 * @return a created model name for the thread effects declaration
	 */
	private String genModelName(final IRNode node, final Operator op,
			final Set<Effect> effects) {
		// add the type we found the method within (could be the promised type)
		IRNode enclosingType = VisitUtil.getEnclosingType(node);
		String typeName = JavaNames.getTypeName(enclosingType);
		String targetName = "(none)";
		if (MethodDeclaration.prototype.includes(op)) {
			targetName = JavaNames.genMethodConstructorName(node);
			IRNode args = MethodDeclaration.getParams(node);
			targetName += JavaNames.genArgList(args);
		} else if (ConstructorDeclaration.prototype.includes(op)) {
			targetName = JavaNames.genMethodConstructorName(node);
			IRNode args = ConstructorDeclaration.getParams(node);
			targetName += JavaNames.genArgList(args);
		}
		String reads = "@reads ";
		int readCt = 0;
		String writes = "@writes ";
		int writeCt = 0;
		for (Iterator<Effect> i = effects.iterator(); i.hasNext();) {
			Effect eff = i.next();
			if (eff.isReadEffect()) {
				reads += (readCt++ > 0 ? ", " : " ") + eff.getTarget().getName();
			}
			if (eff.isWriteEffect()) {
				writes += (writeCt++ > 0 ? ", " : " ") + eff.getTarget().getName();
			}
		}
		if (readCt == 0) {
			reads += "nothing ";
		}
		if (writeCt == 0) {
			writes += "nothing";
		}
		return reads + " " + writes + " for " + typeName + "." + targetName;
	}
}
