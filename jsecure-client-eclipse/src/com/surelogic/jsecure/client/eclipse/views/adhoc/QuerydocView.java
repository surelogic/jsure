package com.surelogic.jsecure.client.eclipse.views.adhoc;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.ui.adhoc.views.doc.AbstractQuerydocView;
import com.surelogic.jsecure.client.eclipse.adhoc.JSecureDataSource;

public class QuerydocView extends AbstractQuerydocView {
	@Override
	public AdHocManager getManager() {
		return JSecureDataSource.getManager();
	}
}
