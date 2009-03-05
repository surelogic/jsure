package com.surelogic.jsure.client.eclipse.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import com.surelogic.common.eclipse.preferences.AbstractCommonPreferencePage;
import com.surelogic.common.i18n.I18N;

public class JSurePreferencePage extends AbstractCommonPreferencePage {
	public JSurePreferencePage() {
		super("jsure.eclipse.", PreferenceConstants.prototype);
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite panel = new Composite(parent, SWT.NONE);
		GridLayout grid = new GridLayout();
		panel.setLayout(grid);
		
		final Group diGroup = new Group(panel, SWT.NONE);
		diGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		diGroup.setText(I18N.msg(messagePrefix+"preference.page.group.app"));
		
		setupForPerspectiveSwitch(diGroup);
		return panel;
	}
}
