package com.surelogic.jsure.client.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.dialogs.InstallTutorialProjectsDialog;

public class ImportTutorialProjectsHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final ClassLoader loader = Thread.currentThread()
				.getContextClassLoader();
		InstallTutorialProjectsDialog
				.open(EclipseUIUtility.getShell(),
						CommonImages.IMG_JSURE_LOGO,
						"/com.surelogic.jsure.client.help/ch01s03.html",
						loader.getResource("/lib/JSureTutorial_BoundedFIFO.zip"),
						loader.getResource("/lib/JSureTutorial_PlanetBaron.zip"),
						loader.getResource("/lib/JSureTutorial_util.concurrent.SynchronizedVariable.zip"),
						loader.getResource("/lib/JSureTutorial_EffectiveEffects.zip"));
		return null;
	}
}
