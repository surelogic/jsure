package com.surelogic.jsure.client.eclipse.refactor;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;

public class ProposedPromisesChange {

	private final IJavaProject selectedProject;
	private final List<ProposedPromiseDrop> drops;

	public ProposedPromisesChange(final IJavaProject selectedProject,
			final List<ProposedPromiseDrop> drops) {
		this.selectedProject = selectedProject;
		this.drops = drops;
	}

	public IJavaProject getSelectedProject() {
		return selectedProject;
	}

	public List<ProposedPromiseDrop> getDrops() {
		return drops;
	}

}
