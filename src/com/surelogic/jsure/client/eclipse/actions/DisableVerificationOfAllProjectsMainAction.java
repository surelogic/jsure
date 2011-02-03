package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;

import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.jsure.client.eclipse.listeners.ClearProjectListener;

public class DisableVerificationOfAllProjectsMainAction extends AbstractMainAction {

	public void run(IAction action) {
		ClearProjectListener.clearNatureFromAllOpenProjects();
	}
}