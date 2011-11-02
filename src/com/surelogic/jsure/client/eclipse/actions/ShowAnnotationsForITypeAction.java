package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;

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
		if (o instanceof IType) {
			final IType t = (IType) o;			
			openInXMLEditor(t);
		}				
		else if (o instanceof IMember) {
			final IMember m = (IMember) o;
			openInXMLEditor(m.getDeclaringType());
			// TODO find member
		}		
	}

	private void openInXMLEditor(final IType t) {
		String qname = t.getFullyQualifiedName();
		int firstDollar = qname.indexOf('$');
		if (firstDollar >= 0) {
			// Eliminate any refs to nested classes
			qname = qname.substring(0, firstDollar);
		}
		PromisesXMLEditor.openInEditor(qname.replace('.', '/')+TestXMLParserConstants.SUFFIX, true);
	}
}
