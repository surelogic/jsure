package com.surelogic.jsure.client.eclipse.editors;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;

import com.surelogic.xml.TestXMLParserConstants;

public class PromisesXMLEditorMatcher implements IEditorMatchingStrategy {
	@Override
	public boolean matches(IEditorReference editorRef, IEditorInput input) {
		String name = input.getName();
		return name.endsWith(TestXMLParserConstants.SUFFIX);
	}

}
