package com.surelogic.jsecure.client.eclipse.views.adhoc;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.ui.adhoc.views.results.AbstractQueryResultsView;
import com.surelogic.jsecure.client.eclipse.adhoc.JSecureDataSource;

public class QueryResultsView extends AbstractQueryResultsView {
	@Override
	public AdHocManager getManager() {
		return JSecureDataSource.getManager();
	}
}
