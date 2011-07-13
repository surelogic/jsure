package com.surelogic.jsure.client.eclipse.views;

import com.surelogic.common.ui.views.ITreeLabelContentProvider;
import com.surelogic.jsure.core.scans.ScanStatus;

public interface IJSureTreeContentProvider extends ITreeLabelContentProvider {
	String build(ScanStatus s);
}
