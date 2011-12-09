package com.surelogic.jsure.client.eclipse.views;

public interface IJSureTableTreeContentProvider extends IResultsTableContentProvider, IJSureTreeContentProvider {
	boolean showAsTree();
	void setAsTree(boolean asTree);
}
