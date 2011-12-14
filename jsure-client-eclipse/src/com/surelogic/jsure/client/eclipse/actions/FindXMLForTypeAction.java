package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jdt.core.*;
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

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.JDTUIUtility;
import com.surelogic.common.ui.actions.AbstractMainAction;

public class FindXMLForTypeAction extends AbstractMainAction {
	private IWorkbenchWindow f_window;
	private final TypeSelectionExtension f_extension = new Extension();
	
	public void init(IWorkbenchWindow window) {
		f_window = window;
	}
	
	@Override
	public void run(IAction action) {
		try {
			final SelectionDialog dialog = 
				JavaUI.createTypeDialog(EclipseUIUtility.getShell(), f_window, 
						SearchEngine.createWorkspaceScope(), 
						IJavaElementSearchConstants.CONSIDER_ALL_TYPES, 
						false, "", 
						f_extension);
			int result= dialog.open();
			if (result != IDialogConstants.OK_ID) {
				return;
			}
			Object[] types= dialog.getResult();
			if (types == null || types.length == 0) {
				return;
			}
			// Open the corresponding XML!
			IType t = (IType) types[0];
			ICompilationUnit cu = t.getCompilationUnit();
			if (cu != null) {
				JDTUIUtility.openInEditor(t, true, true);
			} else {
				ShowAnnotationsForITypeAction.openInXMLEditor(t);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
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
