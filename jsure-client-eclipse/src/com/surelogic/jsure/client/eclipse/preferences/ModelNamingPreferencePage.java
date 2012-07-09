package com.surelogic.jsure.client.eclipse.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

public class ModelNamingPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private BooleanFieldEditor f_regionModelCap;
	private BooleanFieldEditor f_regionModelCommonString;
	private StringFieldEditor f_regionModelSuffix;
	private BooleanFieldEditor f_lockModelCap;
	private StringFieldEditor f_lockModelSuffix;

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(EclipseUIUtility.getPreferences());
		setDescription(I18N.msg("jsure.eclipse.preference.page.mn.title.msg"));
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));

		f_regionModelCap = new BooleanFieldEditor(
				JSurePreferencesUtility.REGION_MODEL_NAME_CAP,
				I18N.msg("jsure.eclipse.preference.page.mn.regionModelNameCap"),
				panel);
		setupEditor(panel, f_regionModelCap);
		f_regionModelCap.fillIntoGrid(panel, 2);
		f_regionModelCommonString = new BooleanFieldEditor(
				JSurePreferencesUtility.REGION_MODEL_NAME_COMMON_STRING,
				I18N.msg("jsure.eclipse.preference.page.mn.regionModelNameCommonString"),
				panel);
		setupEditor(panel, f_regionModelCommonString);
		f_regionModelCommonString.fillIntoGrid(panel, 2);
		f_regionModelSuffix = new StringFieldEditor(
				JSurePreferencesUtility.REGION_MODEL_NAME_SUFFIX,
				I18N.msg("jsure.eclipse.preference.page.mn.regionModelNameSuffix"),
				panel);
		setupEditor(panel, f_regionModelSuffix);
		f_regionModelSuffix.fillIntoGrid(panel, 2);
		f_lockModelCap = new BooleanFieldEditor(
				JSurePreferencesUtility.LOCK_MODEL_NAME_CAP,
				I18N.msg("jsure.eclipse.preference.page.mn.lockModelNameCap"),
				panel);
		setupEditor(panel, f_lockModelCap);
		f_lockModelCap.fillIntoGrid(panel, 2);
		f_lockModelSuffix = new StringFieldEditor(
				JSurePreferencesUtility.LOCK_MODEL_NAME_SUFFIX,
				I18N.msg("jsure.eclipse.preference.page.mn.lockModelNameSuffix"),
				panel);
		setupEditor(panel, f_lockModelSuffix);
		f_lockModelSuffix.fillIntoGrid(panel, 2);

		return panel;
	}

	private void setupEditor(Composite group, FieldEditor e) {
		e.fillIntoGrid(group, 2);
		e.setPage(this);
		e.setPreferenceStore(EclipseUIUtility.getPreferences());
		e.load();
	}

	@Override
	protected void performDefaults() {
		f_regionModelCap.loadDefault();
		f_regionModelCommonString.loadDefault();
		f_regionModelSuffix.loadDefault();
		f_lockModelCap.loadDefault();
		f_lockModelSuffix.loadDefault();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		f_regionModelCap.store();
		f_regionModelCommonString.store();
		f_regionModelSuffix.store();
		f_lockModelCap.store();
		f_lockModelSuffix.store();
		return super.performOk();
	}
}
