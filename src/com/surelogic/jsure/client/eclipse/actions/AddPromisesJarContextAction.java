package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;

import com.surelogic.common.eclipse.actions.AbstractSingleProjectAction;

public class AddPromisesJarContextAction extends AbstractSingleProjectAction {
	public void run(IAction action) {
		AddPromisesJarMainAction.performAction(project);
	}
}
