package com.surelogic.jsure.client.eclipse.editors;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public final class PromisesXMLFactory implements IElementFactory {
	static final String PATH = "com.surelogic.jsure.client.eclipse.PromisesXMLFactory.path";
	
	@Override
	public IAdaptable createElement(IMemento memento) {
		final String path = memento.getString(PATH);
		if (path != null) {
			return PromisesXMLEditor.makeInput(path);
		}
		return null;
	}

}
