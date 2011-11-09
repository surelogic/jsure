package com.surelogic.jsure.client.eclipse.views.results;

import org.eclipse.jface.viewers.*;

public interface IResultsViewContentProvider extends ITreeContentProvider {
	IResultsViewContentProvider buildModelOfDropSea();

	boolean isShowInferences();

	void setShowInferences(boolean toggle);
}
