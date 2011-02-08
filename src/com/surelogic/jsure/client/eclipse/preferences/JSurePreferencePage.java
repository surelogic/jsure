package com.surelogic.jsure.client.eclipse.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.preferences.AbstractCommonPreferencePage;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JSurePreferencePage extends AbstractCommonPreferencePage {
	private BooleanFieldEditor f_balloonFlag;
	private BooleanFieldEditor f_autoOpenProposedPromiseView;
	private BooleanFieldEditor f_autoOpenModelingProblemsView;
	private BooleanFieldEditor f_selectProjectsToScan;
	private BooleanFieldEditor f_allowJavadocAnnos;
	private IntegerFieldEditor f_analysisThreadCount;
	private BooleanFieldEditor f_regionModelCap;
	private BooleanFieldEditor f_regionModelCommonString;
	private StringFieldEditor f_regionModelSuffix;
	private BooleanFieldEditor f_lockModelCap;
	private StringFieldEditor f_lockModelSuffix;

	public JSurePreferencePage() {
		super("jsure.eclipse.", JSurePreferencesUtility.getSwitchPreferences());
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite panel = new Composite(parent, SWT.NONE);
		GridLayout grid = new GridLayout();
		panel.setLayout(grid);

		final Group diGroup = createGroup(panel, "preference.page.group.app");

		f_balloonFlag = new BooleanFieldEditor(
				JSurePreferencesUtility.SHOW_BALLOON_NOTIFICATIONS,
				I18N.msg("jsure.eclipse.preference.page.balloonFlag"), diGroup);
		setupEditor(diGroup, f_balloonFlag);
		
		setupForPerspectiveSwitch(diGroup);

		f_autoOpenProposedPromiseView = new BooleanFieldEditor(
				JSurePreferencesUtility.AUTO_OPEN_PROPOSED_PROMISE_VIEW,
				I18N.msg("jsure.eclipse.preference.page.autoOpenProposedPromiseView"),
				diGroup);
		setupEditor(diGroup, f_autoOpenProposedPromiseView);

		f_autoOpenModelingProblemsView = new BooleanFieldEditor(
				JSurePreferencesUtility.AUTO_OPEN_MODELING_PROBLEMS_VIEW,
				I18N.msg("jsure.eclipse.preference.page.autoOpenModelingProblemsView"),
				diGroup);
		setupEditor(diGroup, f_autoOpenModelingProblemsView);

		f_selectProjectsToScan = new BooleanFieldEditor(
				JSurePreferencesUtility.ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_SCAN,
				I18N.msg("jsure.eclipse.preference.page.selectProjectsToScan"),
				diGroup);

		final Group annoGroup = createGroup(panel,
				"preference.page.group.annos");
		f_allowJavadocAnnos = new BooleanFieldEditor(
				IDEPreferences.ALLOW_JAVADOC_ANNOS,
				I18N.msg("jsure.eclipse.preference.page.allowJavadocAnnos"),
				annoGroup);
		setupEditor(annoGroup, f_allowJavadocAnnos);

		final Group threadGroup = createGroup(panel,
				"preference.page.group.thread");
		f_analysisThreadCount = new IntegerFieldEditor(
				IDEPreferences.ANALYSIS_THREAD_COUNT,
				I18N.msg("jsure.eclipse.preference.page.thread.msg"),
				threadGroup);
		f_analysisThreadCount.setValidRange(1, 128);
		setupEditor(threadGroup, f_analysisThreadCount);

		if (EclipseUtility.bundleExists(SLUtility.FLASHLIGHT_ID)) {
			final Group modelNamingGroup = createGroup(panel,
					"preference.page.group.modelNaming");
			modelNamingGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP,
					true, false));

			f_regionModelCap = new BooleanFieldEditor(
					JSurePreferencesUtility.REGION_MODEL_NAME_CAP,
					I18N.msg("jsure.eclipse.preference.page.regionModelNameCap"),
					modelNamingGroup);
			setupEditor(modelNamingGroup, f_regionModelCap);
			f_regionModelCap.fillIntoGrid(modelNamingGroup, 2);
			f_regionModelCommonString = new BooleanFieldEditor(
					JSurePreferencesUtility.REGION_MODEL_NAME_COMMON_STRING,
					I18N.msg("jsure.eclipse.preference.page.regionModelNameCommonString"),
					modelNamingGroup);
			setupEditor(modelNamingGroup, f_regionModelCommonString);
			f_regionModelCommonString.fillIntoGrid(modelNamingGroup, 2);
			f_regionModelSuffix = new StringFieldEditor(
					JSurePreferencesUtility.REGION_MODEL_NAME_SUFFIX,
					I18N.msg("jsure.eclipse.preference.page.regionModelNameSuffix"),
					modelNamingGroup);
			setupEditor(modelNamingGroup, f_regionModelSuffix);
			f_regionModelSuffix.fillIntoGrid(modelNamingGroup, 2);
			f_lockModelCap = new BooleanFieldEditor(
					JSurePreferencesUtility.LOCK_MODEL_NAME_CAP,
					I18N.msg("jsure.eclipse.preference.page.lockModelNameCap"),
					modelNamingGroup);
			setupEditor(modelNamingGroup, f_lockModelCap);
			f_lockModelCap.fillIntoGrid(modelNamingGroup, 2);
			f_lockModelSuffix = new StringFieldEditor(
					JSurePreferencesUtility.LOCK_MODEL_NAME_SUFFIX,
					I18N.msg("jsure.eclipse.preference.page.lockModelNameSuffix"),
					modelNamingGroup);
			setupEditor(modelNamingGroup, f_lockModelSuffix);
			f_lockModelSuffix.fillIntoGrid(modelNamingGroup, 2);

			modelNamingGroup.setLayout(new GridLayout(2, false));
		}

		return panel;
	}

	private Group createGroup(Composite panel, String suffix) {
		final Group group = new Group(panel, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		group.setText(I18N.msg(messagePrefix + suffix));
		return group;
	}

	private void setupEditor(Group group, FieldEditor e) {
		e.fillIntoGrid(group, 2);
		e.setPage(this);
		e.setPreferenceStore(EclipseUIUtility.getPreferences());
		e.load();
	}

	@Override
	protected void performDefaults() {
		f_balloonFlag.loadDefault();
		f_autoOpenProposedPromiseView.loadDefault();
		f_autoOpenModelingProblemsView.loadDefault();
		f_selectProjectsToScan.loadDefault();
		f_allowJavadocAnnos.loadDefault();
		f_analysisThreadCount.loadDefault();
		if (XUtil.useExperimental()) {
			f_regionModelCap.loadDefault();
			f_regionModelCommonString.loadDefault();
			f_regionModelSuffix.loadDefault();
			f_lockModelCap.loadDefault();
			f_lockModelSuffix.loadDefault();
		}
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		f_balloonFlag.store();
		f_autoOpenProposedPromiseView.store();
		f_autoOpenModelingProblemsView.store();
		f_selectProjectsToScan.store();
		f_allowJavadocAnnos.store();
		f_analysisThreadCount.store();
		if (XUtil.useExperimental()) {
			f_regionModelCap.store();
			f_regionModelCommonString.store();
			f_regionModelSuffix.store();
			f_lockModelCap.store();
			f_lockModelSuffix.store();
		}
		return super.performOk();
	}
}
