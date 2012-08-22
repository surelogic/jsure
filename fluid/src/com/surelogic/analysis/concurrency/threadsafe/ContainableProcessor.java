package com.surelogic.analysis.concurrency.threadsafe;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.TypeImplementationProcessor;
import com.surelogic.analysis.concurrency.annotationbounds.AnnotationBoundsTypeFormalEnv;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;
import edu.cmu.cs.fluid.sea.drops.promises.BorrowedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ContainablePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.IUniquePromise;
import edu.cmu.cs.fluid.sea.drops.promises.UniquePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.VouchFieldIsPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ProposedPromiseBuilder;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;

public final class ContainableProcessor extends
		TypeImplementationProcessor<ContainablePromiseDrop> {
	public ContainableProcessor(
      final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
			final ContainablePromiseDrop cDrop,
			final IRNode typeDecl, final IRNode typeBody) {
		super(a, cDrop, typeDecl, typeBody);
	}

	@Override
	protected String message2string(final int msg) {
		return Messages.toString(msg);
	}

	@Override
	protected void processSuperType(final IRNode tdecl) {
	  final ContainablePromiseDrop pDrop =
		  LockRules.getContainableImplementation(tdecl);
	  if (pDrop != null) {
		final ResultDropBuilder result = createResult(tdecl, true,
			Messages.CONTAINABLE_SUPERTYPE,
			JavaNames.getQualifiedTypeName(tdecl));
		result.addTrustedPromise(pDrop);
	  }
	}
	
	@Override
	protected void processConstructorDeclaration(final IRNode cdecl) {
		final IRNode rcvrDecl = JavaPromise.getReceiverNodeOrNull(cdecl);
		final BorrowedPromiseDrop bpd = UniquenessRules
				.getBorrowed(rcvrDecl);

		final IRNode returnDecl = JavaPromise.getReturnNodeOrNull(cdecl);
		final UniquePromiseDrop upd = UniquenessRules.getUnique(returnDecl);

		// Prefer unique return over borrowed receiver
		final String id = JavaNames.genSimpleMethodConstructorName(cdecl);
		if (upd != null) {
			final ResultDropBuilder result = createResult(cdecl, true,
					Messages.CONSTRUCTOR_UNIQUE_RETURN, id);
			result.addTrustedPromise(upd);
		} else if (bpd != null) {
			final ResultDropBuilder result = createResult(cdecl, true,
					Messages.CONSTRUCTOR_BORROWED_RECEVIER, id);
			result.addTrustedPromise(bpd);
		} else {
			final ResultDropBuilder result = createResult(cdecl, false,
					Messages.CONSTRUCTOR_BAD, id);
			result.addProposal(new ProposedPromiseBuilder("Unique",
					"return", cdecl, cdecl, Origin.MODEL));
		}
	}

	@Override
	protected void processMethodDeclaration(final IRNode mdecl) {
		// Must borrow the receiver if the method is not static
		if (!TypeUtil.isStatic(mdecl)) {
			final String id = JavaNames
					.genSimpleMethodConstructorName(mdecl);
			final IRNode rcvrDecl = JavaPromise
					.getReceiverNodeOrNull(mdecl);
			final BorrowedPromiseDrop bpd = UniquenessRules
					.getBorrowed(rcvrDecl);
			if (bpd == null) {
				final ResultDropBuilder result = createResult(mdecl, false,
						Messages.METHOD_BAD, id);
				result.addProposal(new ProposedPromiseBuilder("Borrowed",
						"this", mdecl, mdecl, Origin.MODEL));
			} else {
				final ResultDropBuilder result = createResult(mdecl, true,
						Messages.METHOD_BORROWED_RECEIVER, id);
				result.addTrustedPromise(bpd);
			}
		}
	}

	@Override
	protected void processVariableDeclarator(final IRNode fieldDecl,
			final IRNode varDecl, final boolean isStatic) {
		final String id = VariableDeclarator.getId(varDecl);
		final IJavaType type = binder.getJavaType(varDecl);
		
		if (type instanceof IJavaPrimitiveType) {
			createResult(varDecl, true, Messages.FIELD_CONTAINED_PRIMITIVE,
					id);
		} else {
			final VouchFieldIsPromiseDrop vouchDrop = LockRules
					.getVouchFieldIs(varDecl);
			if (vouchDrop != null && vouchDrop.isContainable()) {
				final String reason = vouchDrop.getReason();
				final ResultDropBuilder result = reason == VouchFieldIsNode.NO_REASON ? createResult(
						varDecl, true, Messages.FIELD_CONTAINED_VOUCHED, id)
						: createResult(
								varDecl,
								true,
								Messages.FIELD_CONTAINED_VOUCHED_WITH_REASON,
								id, reason);
				result.addTrustedPromise(vouchDrop);
			} else {
				final IUniquePromise uniqueDrop = UniquenessUtils.getUnique(varDecl);
				final ContainableAnnotationTester tester =
					new ContainableAnnotationTester(
						binder, AnnotationBoundsTypeFormalEnv.INSTANCE);
	  final boolean isContainable = tester.testType(type);
				  
				if (isContainable && uniqueDrop != null) {
					final ResultDropBuilder result = createResult(varDecl,
							true, Messages.FIELD_CONTAINED_OBJECT, id);
					result.addSupportingInformation(varDecl,
							Messages.DECLARED_TYPE_IS_CONTAINABLE,
							type.toString());
					for (final PromiseDrop<? extends IAASTRootNode> p : tester.getPromises()) {
						result.addTrustedPromise(p);
					}
					result.addSupportingInformation(varDecl, Messages.FIELD_IS_UNIQUE);
					result.addTrustedPromise(uniqueDrop.getDrop());
				} else {
					final ResultDropBuilder result =
						createResult(varDecl, false, Messages.FIELD_BAD, id);

					// Always suggest @Vouch("Containable")
					result.addProposal(new ProposedPromiseBuilder("Vouch",
							"Containable", varDecl, varDecl, Origin.MODEL));

					if (isContainable) {
						result.addSupportingInformation(varDecl,
								Messages.DECLARED_TYPE_IS_CONTAINABLE,
								type.toString());
			for (final PromiseDrop<? extends IAASTRootNode> p : tester.getPromises()) {
			  result.addTrustedPromise(p);
			}
					} else {
						// no @Containable annotation --> Default
						// "annotation" of not containable
						result.addSupportingInformation(varDecl,
								Messages.DECLARED_TYPE_NOT_CONTAINABLE,
								type.toString());
						for (final IRNode t : tester.getTested()) {
							result.addProposal(new ProposedPromiseBuilder(
									"Containable", null, t, varDecl, Origin.MODEL));
						}
					}

					if (uniqueDrop != null) {
						result.addSupportingInformation(varDecl,
								Messages.FIELD_IS_UNIQUE);
						result.addTrustedPromise(uniqueDrop.getDrop());
					} else {
						result.addSupportingInformation(varDecl,
								Messages.FIELD_NOT_UNIQUE);
						result.addProposal(new ProposedPromiseBuilder(
								"Unique", null, varDecl, varDecl,
								Origin.MODEL));
					}
				}
			}
		}
	}
}
