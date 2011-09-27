package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;

import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.jobs.*;
import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.jsure.core.xml.PromisesLibMerge;

public class TestXMLMergeAction extends AbstractMainAction {
	@Override
	public void run(IAction action) {
		SLJob job = new AbstractSLJob("Testing XML Merge") {			
			@Override
			public SLStatus run(SLProgressMonitor monitor) {
				System.out.println("Merging to client");
				PromisesLibMerge.merge(true);
				System.out.println("Merging to fluid");
				PromisesLibMerge.merge(false);
				return SLStatus.OK_STATUS;
			}
		};
		EclipseJob.getInstance().schedule(job);
	}
}
