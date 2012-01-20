package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.common.ui.dialogs.ManageLicensesDialog;

public final class ManageLicensesAction extends AbstractMainAction {
	public void run(IAction action) {
		ManageLicensesDialog.open(EclipseUIUtility.getShell());
	}
}
