package com.surelogic.analysis.concurrency.threadsafe;

import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.TypeImplementationProcessor;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.type.constraints.AnnotationBoundsTypeFormalEnv;
import com.surelogic.analysis.type.constraints.ContainableAnnotationTester;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.drops.VouchFieldIsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ContainablePromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.BorrowedPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.IUniquePromise;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;

public final class ContainableProcessor extends
		TypeImplementationProcessor<ContainablePromiseDrop> {
	public ContainableProcessor(
      final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
			final ContainablePromiseDrop cDrop,
			final IRNode typeDecl, final IRNode typeBody) {
		super(a, cDrop, typeDecl, typeBody);
	}

	@Override
	protected void processSuperType(final IRNode name, final IRNode tdecl) {
	  final ContainablePromiseDrop pDrop =
		  LockRules.getContainableImplementation(tdecl);
	  if (pDrop != null) {
  		final ResultDrop result = createResult(
  		    name, true, Messages.CONTAINABLE_SUPERTYPE,
  		    JavaNames.getQualifiedTypeName(tdecl));
  		result.addTrusted_and(pDrop);
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
			final ResultDrop result = createResult(
			    cdecl, true, Messages.CONSTRUCTOR_UNIQUE_RETURN, id);
			result.addTrusted_and(upd);
		} else if (bpd != null) {
			final ResultDrop result = createResult(
			    cdecl, true, Messages.CONSTRUCTOR_BORROWED_RECEVIER, id);
			result.addTrusted_and(bpd);
		} else {
			final ResultDrop result = createResult(
			    cdecl, false, Messages.CONSTRUCTOR_BAD, id);
			result.addProposal(new ProposedPromiseDrop(
			    "Unique", "return", cdecl, cdecl, Origin.MODEL));
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
				final ResultDrop result = createResult(
				    mdecl, false, Messages.METHOD_BAD, id);
				result.addProposal(new ProposedPromiseDrop(
				    "Borrowed",	"this", mdecl, mdecl, Origin.MODEL));
			} else {
				final ResultDrop result = createResult(
				    mdecl, true, Messages.METHOD_BORROWED_RECEIVER, id);
				result.addTrusted_and(bpd);
			}
		}
	}

	@Override
	protected void processVariableDeclarator(final IRNode fieldDecl,
			final IRNode varDecl, final boolean isStatic) {
		final String id = VariableDeclarator.getId(varDecl);
		final IJavaType type = binder.getJavaType(varDecl);
		
		if (type instanceof IJavaPrimitiveType) {
			createResult(
			    varDecl, true, Messages.FIELD_CONTAINED_PRIMITIVE, id);
		} else {
			final VouchFieldIsPromiseDrop vouchDrop = LockRules
					.getVouchFieldIs(varDecl);
			if (vouchDrop != null && vouchDrop.isContainable()) {
				final String reason = vouchDrop.getReason();
        final ResultDrop result =
            (reason == VouchFieldIsNode.NO_REASON) ?
            createResult(varDecl, true, Messages.FIELD_CONTAINED_VOUCHED, id) :
            createResult(varDecl, true,
                Messages.FIELD_CONTAINED_VOUCHED_WITH_REASON, id, reason);
				result.addTrusted_and(vouchDrop);
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
		    final ResultFolderDrop folder = createResultFolder(varDecl); 
		    if (isContainable && uniqueDrop != null) { // GOOD!
		      folder.setMessage(Messages.FIELD_CONTAINED_OBJECT, id);
          final ResultDrop cResult = createResultInFolder(
              folder, FieldDeclaration.getType(fieldDecl), true,
              Messages.DECLARED_TYPE_IS_CONTAINABLE, type.toSourceText());
		      cResult.addTrusted_and(tester.getPromises());

          final ResultDrop uResult = createResultInFolder(
              folder, fieldDecl, true, Messages.FIELD_IS_UNIQUE);
          uResult.addTrusted_and(uniqueDrop.getDrop());
				} else {
          folder.setMessage(Messages.FIELD_BAD, id);
          folder.addProposal(new ProposedPromiseDrop(
              "Vouch", "Containable", varDecl, varDecl, Origin.MODEL));

          final ResultDrop cResult;
          if (isContainable) {
            cResult = createResultInFolder(
                folder, FieldDeclaration.getType(fieldDecl), true,
                Messages.DECLARED_TYPE_IS_CONTAINABLE, type.toSourceText());
            cResult.addTrusted_and(tester.getPromises());
          } else {
            cResult = createResultInFolder(
                folder, FieldDeclaration.getType(fieldDecl), false,
                Messages.DECLARED_TYPE_NOT_CONTAINABLE, type.toSourceText());
            for (final IRNode t : tester.getTested()) {
              cResult.addProposal(new ProposedPromiseDrop(
                  "Containable", null, t, varDecl, Origin.MODEL));
            }
          }
          folder.add(cResult);

          final ResultDrop uResult;
          if (uniqueDrop != null) {
            uResult = createResultInFolder(
                folder, fieldDecl, true, Messages.FIELD_IS_UNIQUE);
            uResult.addTrusted_and(uniqueDrop.getDrop());
          } else {
            uResult = createResultInFolder(
                folder, fieldDecl, false, Messages.FIELD_NOT_UNIQUE);
            uResult.addProposal(new ProposedPromiseDrop(
                "Unique", null, varDecl, varDecl, Origin.MODEL));
          }
          folder.add(uResult);
				}
			}
		}
	}
}
