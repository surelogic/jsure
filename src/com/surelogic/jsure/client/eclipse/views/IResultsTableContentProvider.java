package com.surelogic.jsure.client.eclipse.views;

import com.surelogic.common.ui.views.ITableContentProvider;
import com.surelogic.fluid.javac.scans.ScanStatus;

public interface IResultsTableContentProvider extends ITableContentProvider {
	String[] getColumnLabels();
	boolean isIntSortedColumn(int colIdx);
	
	/**
	 * @return A label used to update the title
	 */
	String build(ScanStatus status);
}
