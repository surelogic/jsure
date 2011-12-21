package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;

import com.surelogic.common.ui.JDTUIUtility;
import com.surelogic.jsure.client.eclipse.editors.PromisesXMLEditor;
import com.surelogic.xml.TestXMLParserConstants;

public class ShowAnnotationsForITypeAction implements IObjectActionDelegate {
	IStructuredSelection selection;
	
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// Ignore
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = null;
		
		if (selection.isEmpty()) {
			return;
		}
		if (selection instanceof IStructuredSelection) {
			this.selection = (IStructuredSelection) selection;
		}
	}
	
	@Override
	public void run(IAction action) {
		final Object o = selection.getFirstElement();
		if (o instanceof IMember) {
			IMember m = (IMember) o;
			ICompilationUnit cu = m.getCompilationUnit();
			if (cu != null) {
				JDTUIUtility.openInEditor(m, true, true);
				return;
			}	
			if (o instanceof IType) {
				final IType t = (IType) o;			
				openInXMLEditor(t);
			} else {
				final IEditorPart e = openInXMLEditor(m.getDeclaringType());			
				if (m instanceof IMethod && e instanceof PromisesXMLEditor) {
					final IMethod m2 = (IMethod) m;
					final PromisesXMLEditor xe = (PromisesXMLEditor) e;
					xe.focusOnMethod(m2.getElementName(), null);
				}
				// TODO find member
			}	
		}	
		else if (o instanceof ICompilationUnit) {
			ICompilationUnit cu = (ICompilationUnit) o;
			if (cu != null) {
				JDTUIUtility.openInEditor(cu, true, true);
				return;
			}	
		}
		else if (o instanceof IClassFile) {
			IClassFile c = (IClassFile) o;
			openInXMLEditor(c.getType());
		}
	}

	public static IEditorPart openInXMLEditor(final IType t) {
		String qname = t.getFullyQualifiedName();
		int firstDollar = qname.indexOf('$');
		if (firstDollar >= 0) {
			// Eliminate any refs to nested classes
			qname = qname.substring(0, firstDollar);
			// TODO find nested classes
		}
		return PromisesXMLEditor.openInEditor(qname.replace('.', '/')+TestXMLParserConstants.SUFFIX, false);
	}
}
