package com.surelogic.jsure.client.eclipse.views;

import com.surelogic.common.ui.views.ITableContentProvider;

public interface IResultsTableContentProvider extends ITableContentProvider {
	String[] getColumnLabels();
	boolean isIntSortedColumn(int colIdx);
	
	void build();

}
