package com.surelogic.jsure.client.eclipse.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import com.surelogic.common.eclipse.preferences.AbstractCommonPreferencePage;
import com.surelogic.common.i18n.I18N;

public class JSurePreferencePage extends AbstractCommonPreferencePage {
	private BooleanFieldEditor f_autoOpenModelingProblemsView;

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
		diGroup.setText(I18N.msg(messagePrefix + "preference.page.group.app"));

		setupForPerspectiveSwitch(diGroup);

		f_autoOpenModelingProblemsView = new BooleanFieldEditor(
				PreferenceConstants.P_AUTO_OPEN_MODELING_PROBLEMS_VIEW,
				I18N
						.msg("jsure.eclipse.preference.page.autoOpenModelingProblemsView"),
				diGroup);
		f_autoOpenModelingProblemsView.fillIntoGrid(diGroup, 2);
		f_autoOpenModelingProblemsView.setPage(this);
		f_autoOpenModelingProblemsView.setPreferenceStore(getPreferenceStore());
		f_autoOpenModelingProblemsView.load();
		return panel;
	}

	@Override
	protected void performDefaults() {
		f_autoOpenModelingProblemsView.loadDefault();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		f_autoOpenModelingProblemsView.store();
		return super.performOk();
	}
}
