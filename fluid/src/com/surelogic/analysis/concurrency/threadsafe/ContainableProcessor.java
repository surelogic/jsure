package com.surelogic.analysis.concurrency.threadsafe;

import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.TypeImplementationProcessor;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.concurrency.util.AnnotationBoundsTypeFormalEnv;
import com.surelogic.analysis.concurrency.util.ContainableAnnotationTester;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;
import edu.cmu.cs.fluid.sea.drops.promises.BorrowedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ContainablePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.IUniquePromise;
import edu.cmu.cs.fluid.sea.drops.promises.UniquePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.VouchFieldIsPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ProposedPromiseBuilder;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.sea.proxy.ResultFolderDropBuilder;

public final class ContainableProcessor extends
		TypeImplementationProcessor<ContainablePromiseDrop> {
	public ContainableProcessor(
      final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
			final ContainablePromiseDrop cDrop,
			final IRNode typeDecl, final IRNode typeBody) {
		super(a, cDrop, typeDecl, typeBody);
	}

	@Override
	protected void processSuperType(final IRNode tdecl) {
	  final ContainablePromiseDrop pDrop =
		  LockRules.getContainableImplementation(tdecl);
	  if (pDrop != null) {
		final ResultDropBuilder result = createResultBuilder(tdecl, true,
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
			final ResultDropBuilder result = createResultBuilder(cdecl, true,
					Messages.CONSTRUCTOR_UNIQUE_RETURN, id);
			result.addTrustedPromise(upd);
		} else if (bpd != null) {
			final ResultDropBuilder result = createResultBuilder(cdecl, true,
					Messages.CONSTRUCTOR_BORROWED_RECEVIER, id);
			result.addTrustedPromise(bpd);
		} else {
			final ResultDropBuilder result = createResultBuilder(cdecl, false,
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
				final ResultDropBuilder result = createResultBuilder(mdecl, false,
						Messages.METHOD_BAD, id);
				result.addProposal(new ProposedPromiseBuilder("Borrowed",
						"this", mdecl, mdecl, Origin.MODEL));
			} else {
				final ResultDropBuilder result = createResultBuilder(mdecl, true,
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
			createResultBuilder(varDecl, true, Messages.FIELD_CONTAINED_PRIMITIVE,
					id);
		} else {
			final VouchFieldIsPromiseDrop vouchDrop = LockRules
					.getVouchFieldIs(varDecl);
			if (vouchDrop != null && vouchDrop.isContainable()) {
				final String reason = vouchDrop.getReason();
				final ResultDropBuilder result = reason == VouchFieldIsNode.NO_REASON ? createResultBuilder(
						varDecl, true, Messages.FIELD_CONTAINED_VOUCHED, id)
						: createResultBuilder(
								varDecl,
								true,
								Messages.FIELD_CONTAINED_VOUCHED_WITH_REASON,
								id, reason);
				result.addTrustedPromise(vouchDrop);
			} else {
				final IUniquePromise uniqueDrop = UniquenessUtils.getUnique(varDecl);
				final ContainableAnnotationTester tester =
					new ContainableAnnotationTester(
						binder, AnnotationBoundsTypeFormalEnv.INSTANCE, true);
				final boolean isContainable = tester.testType(type);
				
				/* Use a result folder: We have two things that need to be true:
				 * (1) The type of the field is @Containable
				 * (2) The field is @Unique
				 */				
		    final ResultFolderDropBuilder folder = ResultFolderDropBuilder.create(analysis);
		    analysis.setResultDependUponDrop(folder, varDecl);
		    folder.addCheckedPromise(promiseDrop);

		    if (isContainable && uniqueDrop != null) { // GOOD!
		      folder.setResultMessage(Messages.FIELD_CONTAINED_OBJECT, id);
          final ResultDrop cResult = createResult(
              FieldDeclaration.getType(fieldDecl), true,
              Messages.DECLARED_TYPE_IS_CONTAINABLE, type.toSourceText());
		      cResult.addTrustedPromises(tester.getPromises());
		      folder.add(cResult);

          final ResultDrop uResult = createResult(
              fieldDecl, true, Messages.FIELD_IS_UNIQUE);
          uResult.addTrustedPromise(uniqueDrop.getDrop());
          folder.add(uResult);
				} else {
          folder.setResultMessage(Messages.FIELD_BAD, id);
          folder.addProposal(new ProposedPromiseBuilder("Vouch", "Containable",
              varDecl, varDecl, Origin.MODEL));

          final ResultDrop cResult;
          if (isContainable) {
            cResult = createResult(FieldDeclaration.getType(fieldDecl), true,
                Messages.DECLARED_TYPE_IS_CONTAINABLE, type.toSourceText());
            cResult.addTrustedPromises(tester.getPromises());
          } else {
            cResult = createResult(FieldDeclaration.getType(fieldDecl), false,
                Messages.DECLARED_TYPE_NOT_CONTAINABLE, type.toSourceText());
            for (final IRNode t : tester.getTested()) {
              cResult.addProposal(new ProposedPromiseDrop(
                  "Containable", null, t, varDecl, Origin.MODEL));
            }
          }
          folder.add(cResult);

          final ResultDrop uResult;
          if (uniqueDrop != null) {
            uResult = createResult(fieldDecl, true, Messages.FIELD_IS_UNIQUE);
            uResult.addTrustedPromise(uniqueDrop.getDrop());
          } else {
            uResult = createResult(fieldDecl, false, Messages.FIELD_NOT_UNIQUE);
            uResult.addProposal(new ProposedPromiseDrop(
                "Unique", null, varDecl, varDecl,
                Origin.MODEL));
          }
          folder.add(uResult);
				}
			}
		}
	}
}
