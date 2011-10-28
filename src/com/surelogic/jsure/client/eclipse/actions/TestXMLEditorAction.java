package com.surelogic.jsure.client.eclipse.actions;

import org.eclipse.jface.action.IAction;

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.jsure.client.eclipse.editors.PromisesXMLEditor;

public class TestXMLEditorAction extends AbstractMainAction {
	@Override
	public void run(IAction action) {
		// In jsure demo
		String path = "test"+'/'+"XMLTest.promises.xml";
		EclipseUIUtility.openInEditor(PromisesXMLEditor.makeInput(path, false), PromisesXMLEditor.class.getName());
	}
}
