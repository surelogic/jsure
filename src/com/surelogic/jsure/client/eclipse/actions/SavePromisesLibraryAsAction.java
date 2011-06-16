package com.surelogic.jsure.client.eclipse.actions;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.surelogic.common.FileUtility;
import com.surelogic.common.LibResources;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;

public class SavePromisesLibraryAsAction implements
		IWorkbenchWindowActionDelegate {

	public void run(IAction action) {
		DirectoryDialog dialog = new DirectoryDialog(
				EclipseUIUtility.getShell());
		dialog.setText(I18N.msg("jsure.eclipse.dialog.promises.saveAs.title"));
		dialog.setMessage(I18N.msg("jsure.eclipse.dialog.promises.saveAs.msg",
				LibResources.PROMISES_JAR));
		final String result = dialog.open();
		if (result != null) {
			final File jarFile = new File(result, LibResources.PROMISES_JAR);
			try {
				FileUtility.copy(LibResources.PROMISES_JAR,
						LibResources.getPromisesJar(), jarFile);
			} catch (IOException e) {
				SLLogger.getLogger().log(
						Level.SEVERE,
						I18N.err(224, LibResources.PROMISES_JAR,
								jarFile.getAbsolutePath()), e);
			}
		}
	}

	public void init(IWorkbenchWindow window) {
		// Nothing to do
	}

	public void dispose() {
		// Nothing to do
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// Nothing to do
	}
}
