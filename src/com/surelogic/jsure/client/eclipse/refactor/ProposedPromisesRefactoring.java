package com.surelogic.jsure.client.eclipse.refactor;

import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;

/**
 * Adds annotations for @RegionLock("Lock is this...") and
 * 
 * @RegionLock("Lock is class...")
 * 
 * @author nathan
 * 
 */
public class ProposedPromisesRefactoring extends Refactoring {
	private static final Logger LOG = SLLogger
			.getLoggerFor(ProposedPromisesRefactoring.class);
	private final ProposedPromisesChange info;

	public ProposedPromisesRefactoring(final ProposedPromisesChange info) {
		this.info = info;
	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor pm)
			throws CoreException, OperationCanceledException {

		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public RefactoringStatus checkInitialConditions(final IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// TODO Quick checks
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change createChange(final IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		final CompositeChange root = new CompositeChange(String.format(
				"Changes to %s", info.getSelectedProject().getProject()
						.getName()));
		info.change(root);
		return root;
	}

	@Override
	public String getName() {
		return I18N.msg("flashlight.recommend.refactor.regionIsThis");
	}

}
