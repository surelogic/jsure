package com.surelogic.jsure.client.eclipse.editors;

import com.surelogic.common.ui.views.ITableContentProvider;

public interface IResultsTableContentProvider extends ITableContentProvider {
	String[] getColumnLabels();

	boolean isIntSortedColumn(int colIdx);

	/**
	 * @return a label used to update the title
	 */
	String build();
}
