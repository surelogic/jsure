package com.surelogic.jsure.client.eclipse.editors;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;

import com.surelogic.xml.TestXMLParserConstants;

public class PromisesXMLEditorMatcher implements IEditorMatchingStrategy {
	@Override
	public boolean matches(final IEditorReference editorRef, final IEditorInput input) {				
		final String name = input.getName();
		if (!name.endsWith(TestXMLParserConstants.SUFFIX)) {
			return false;
		}		
		try {
			IEditorInput eInput = editorRef.getEditorInput();
			return name.equals(eInput.getName()) && input.equals(eInput);
		} catch (PartInitException e) {
			e.printStackTrace();
			return false;
		}
	}

}
