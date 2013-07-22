package com.surelogic.jsecure.client.eclipse.views.adhoc;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.ui.adhoc.views.explorer.AbstractQueryResultExplorerView;
import com.surelogic.jsecure.client.eclipse.adhoc.JSecureDataSource;

public class QueryResultExplorerView extends AbstractQueryResultExplorerView {
	@Override
	public AdHocManager getManager() {
		return JSecureDataSource.getManager();
	}
}
