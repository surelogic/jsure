package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.common.ui.dialogs.InstallTutorialProjectsDialog;

public class ImportTutorialProjectsAction extends AbstractMainAction {

	public void run(final IAction action) {
		final ClassLoader loader = Thread.currentThread()
				.getContextClassLoader();
		InstallTutorialProjectsDialog.open(EclipseUIUtility.getShell(),
				CommonImages.IMG_JSURE_LOGO,
				"/com.surelogic.jsure.client.help/ch01s03.html",
				loader.getResource("/lib/PlanetBaronJSure.zip"),
				loader.getResource("/lib/BoundedFIFOJSure.zip"),
				loader.getResource("/lib/oswego.util.concurrent.zip"),
        loader.getResource("/lib/SimpleVariable.zip"),
        loader.getResource("/lib/UniqueAndEffects.zip"),
        loader.getResource("/lib/AdvancedEffectExamples.zip"));
	}
}
