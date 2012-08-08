package com.surelogic.jsure.client.eclipse.handlers;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.swt.widgets.DirectoryDialog;

import com.surelogic.common.FileUtility;
import com.surelogic.common.LibResources;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;

public class SavePromisesLibraryAsHandler extends AbstractHandler implements
		IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
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
		return null;
	}
}
