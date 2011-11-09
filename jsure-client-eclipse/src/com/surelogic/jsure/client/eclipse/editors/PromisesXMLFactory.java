package com.surelogic.jsure.client.eclipse.editors;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public final class PromisesXMLFactory implements IElementFactory {
	static final String PATH = "com.surelogic.jsure.client.eclipse.PromisesXMLFactory.path";
	static final String READ_ONLY = "com.surelogic.jsure.client.eclipse.PromisesXMLFactory.readOnly";
	
	@Override
	public IAdaptable createElement(IMemento memento) {
		final String path = memento.getString(PATH);
		final boolean ro = memento.getBoolean(READ_ONLY);
		if (path != null) {
			return PromisesXMLEditor.makeInput(path, ro);
		}
		return null;
	}

}
