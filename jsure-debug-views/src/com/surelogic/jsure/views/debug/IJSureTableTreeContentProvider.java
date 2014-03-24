package com.surelogic.jsure.views.debug;

public interface IJSureTableTreeContentProvider extends IResultsTableContentProvider, IJSureTreeContentProvider {
	boolean showAsTree();
	void setAsTree(boolean asTree);
}
