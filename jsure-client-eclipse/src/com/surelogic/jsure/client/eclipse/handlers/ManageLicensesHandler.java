package com.surelogic.jsure.client.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.dialogs.ManageLicensesDialog;

public class ManageLicensesHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ManageLicensesDialog.open(EclipseUIUtility.getShell());
		return null;
	}
}
