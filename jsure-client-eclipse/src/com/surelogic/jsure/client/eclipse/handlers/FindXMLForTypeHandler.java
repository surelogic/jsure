package com.surelogic.jsure.client.eclipse.handlers;

import java.util.logging.Level;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.jsure.client.eclipse.editors.PromisesXMLEditor;
import com.surelogic.jsure.client.eclipse.views.xml.XMLExplorerView;

public final class FindXMLForTypeHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IWorkbenchWindow window = HandlerUtil
				.getActiveWorkbenchWindow(event);
		if (window == null)
			return null;

		promptAndOpenEditor(window);

		return null;
	}

	private static void promptAndOpenEditor(final IWorkbenchWindow window) {
		try {
			final SelectionDialog dialog = JavaUI.createTypeDialog(
					EclipseUIUtility.getShell(), window,
					SearchEngine.createWorkspaceScope(),
					IJavaElementSearchConstants.CONSIDER_ALL_TYPES, false, "",
					new Extension());
			dialog.setTitle("Open Library Annotations");

			int result = dialog.open();
			if (result != IDialogConstants.OK_ID) {
				return;
			}
			Object[] types = dialog.getResult();
			if (types == null || types.length == 0) {
				return;
			}
			// Open the corresponding XML!
			IType t = (IType) types[0];
			ICompilationUnit cu = t.getCompilationUnit();
			if (cu != null) {
				JavaUI.openInEditor(t, true, true);
			} else {
				PromisesXMLEditor.openInXMLEditor(t);
			}
		} catch (final CoreException e) {
			SLLogger.getLogger().log(
					Level.WARNING,
					I18N.err(161, e.getClass().getName(),
							FindXMLForTypeHandler.class.getName()), e);
		}
	}

	/**
	 * Needed by the {@link XMLExplorerView} for now.
	 * 
	 * @return old style action.
	 */
	public static Action adaptToAction() {
		return new Action() {
			@Override
			public void run() {

				final IWorkbenchWindow window = EclipseUIUtility
						.getIWorkbenchWindow();
				if (window == null)
					return;

				promptAndOpenEditor(window);
			}
		};
	}

	private static class Extension extends TypeSelectionExtension {
		@Override
		public ITypeInfoFilterExtension getFilterExtension() {
			return new FilterExtension();
		}
	}

	private static class FilterExtension implements ITypeInfoFilterExtension {
		@Override
		public boolean select(ITypeInfoRequestor req) {
			// TODO no way to keep only binaries?
			return true;
		}
	}
}
