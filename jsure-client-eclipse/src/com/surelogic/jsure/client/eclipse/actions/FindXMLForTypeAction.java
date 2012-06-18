package com.surelogic.jsure.client.eclipse.actions;

import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.dialogs.ITypeInfoFilterExtension;
import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.SelectionDialog;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.actions.AbstractMainAction;

public class FindXMLForTypeAction extends AbstractMainAction {
	private IWorkbenchWindow f_window;
	private final TypeSelectionExtension f_extension = new Extension();

	public void init(IWorkbenchWindow window) {
		f_window = window;
	}

	@Override
	public void run(IAction action) {
		run();
	}

	public void run() {
		try {
			final SelectionDialog dialog = JavaUI.createTypeDialog(
					EclipseUIUtility.getShell(), f_window,
					SearchEngine.createWorkspaceScope(),
					IJavaElementSearchConstants.CONSIDER_ALL_TYPES, false, "",
					f_extension);
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
				ShowAnnotationsForITypeAction.openInXMLEditor(t);
			}
		} catch (final CoreException e) {
			SLLogger.getLogger().log(
					Level.WARNING,
					I18N.err(161, e.getClass().getName(),
							FindXMLForTypeAction.class.getName()), e);
		}
	}

	class Extension extends TypeSelectionExtension {
		@Override
		public ITypeInfoFilterExtension getFilterExtension() {
			return new FilterExtension();
		}
	}

	class FilterExtension implements ITypeInfoFilterExtension {
		@Override
		public boolean select(ITypeInfoRequestor req) {
			// TODO no way to keep only binaries?
			return true;
		}
	}
}
