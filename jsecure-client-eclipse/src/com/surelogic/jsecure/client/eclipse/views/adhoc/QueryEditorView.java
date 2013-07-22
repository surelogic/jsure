package com.surelogic.jsecure.client.eclipse.views.adhoc;

import com.surelogic.common.adhoc.AdHocManager;
import com.surelogic.common.ui.adhoc.views.editor.AbstractQueryEditorView;
import com.surelogic.jsecure.client.eclipse.adhoc.JSecureDataSource;

public class QueryEditorView extends AbstractQueryEditorView {
	@Override
	public AdHocManager getManager() {
		return JSecureDataSource.getManager();
	}
}
