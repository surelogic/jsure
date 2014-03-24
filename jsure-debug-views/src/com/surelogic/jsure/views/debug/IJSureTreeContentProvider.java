package com.surelogic.jsure.views.debug;

import com.surelogic.common.ui.views.ITreeLabelContentProvider;

public interface IJSureTreeContentProvider extends ITreeLabelContentProvider {
	/**
	 * @return a non-null value for a valid scan
	 */
	String build();
}
