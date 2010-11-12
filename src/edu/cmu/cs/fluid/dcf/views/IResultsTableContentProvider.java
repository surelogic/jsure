package edu.cmu.cs.fluid.dcf.views;

import com.surelogic.common.eclipse.views.ITableContentProvider;

public interface IResultsTableContentProvider extends ITableContentProvider {
	String[] getColumnLabels();
	boolean isIntSortedColumn(int colIdx);
	
	void build();

}
