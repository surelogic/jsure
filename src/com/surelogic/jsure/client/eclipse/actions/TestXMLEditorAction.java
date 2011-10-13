package com.surelogic.jsure.client.eclipse.actions;

import java.io.File;

import org.eclipse.jface.action.IAction;

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.actions.AbstractMainAction;
import com.surelogic.jsure.core.driver.JavacEclipse;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class TestXMLEditorAction extends AbstractMainAction {
	@Override
	public void run(IAction action) {
		final String root = JavacEclipse.getDefault().getStringPreference(IDEPreferences.JSURE_XML_DIRECTORY);
		final char slash = File.separatorChar;
		// In jsure demo
		String path = root+slash+"test"+slash+"XMLTest.promises.xml";
		EclipseUIUtility.openInEditor(path);
	}
}
