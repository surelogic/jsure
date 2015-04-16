package com.surelogic.jsure.client.eclipse.handlers;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.surelogic.common.ui.dialogs.DialogUtility;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.client.eclipse.LibResources;

public class SaveAntTaskHandler extends AbstractHandler {
	private static final String ANT_ZIP_FORMAT = "jsure-ant-%s.zip";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final String target = String.format(ANT_ZIP_FORMAT, Activator.getVersion());
		final DialogUtility.ZipResourceFactory source = new DialogUtility.ZipResourceFactory() {
			public InputStream getInputStream() throws IOException {
				return LibResources.getStreamFor(LibResources.ANT_TASK_ZIP_PATHNAME);
			}
		};
		DialogUtility.copyZipResourceToUsersDiskDialogInteractionHelper(source, target, LibResources.ANT_TASK_ZIP,
				"jsure.eclipse.dialog.ant");
		return null;
	}
}
