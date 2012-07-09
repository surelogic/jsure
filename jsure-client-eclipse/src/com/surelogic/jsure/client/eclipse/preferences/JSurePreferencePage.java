package com.surelogic.jsure.client.eclipse.preferences;

import java.io.File;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.ScaleFieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.MemoryUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.dialogs.ChangeDirectoryLocationDialog;
import com.surelogic.common.ui.preferences.AbstractCommonPreferencePage;
import com.surelogic.common.ui.preferences.PathPreferenceEditor;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

import edu.cmu.cs.fluid.ide.IDEPreferences;

public class JSurePreferencePage extends AbstractCommonPreferencePage {
	static private final String TOOL_MB_LABEL = "jsure.eclipse.preference.page.toolMemoryPreferenceLabel";
	static private final String THREADS_LABEL = "jsure.eclipse.preference.page.thread.msg";
	static private final String TIMEOUT_WARNING_LABEL = "jsure.eclipse.preference.page.timeoutWarning";
	static private final String TIMEOUT_LABEL = "jsure.eclipse.preference.page.timeout";

	private PathPreferenceEditor f_xmlDiffDir;
	private BooleanFieldEditor f_balloonFlag;
	private BooleanFieldEditor f_selectProjectsToScan;
	private BooleanFieldEditor f_selectProjectsToUpdateJar;
	private BooleanFieldEditor f_autoSaveDirtyEditorsBeforeVerify;
	private BooleanFieldEditor f_allowJavadocAnnos;
	private ScaleFieldEditor f_analysisThreadCount;
	private ScaleFieldEditor f_toolMemoryMB;
	private ScaleFieldEditor f_timeoutWarningSec;
	private BooleanFieldEditor f_timeoutFlag;
	private ScaleFieldEditor f_timeoutSec;
	private BooleanFieldEditor f_loadAllClassesFlag;

	public JSurePreferencePage() {
		super("jsure.eclipse.", JSurePreferencesUtility.getSwitchPreferences());
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite panel = new Composite(parent, SWT.NONE);
		GridLayout grid = new GridLayout();
		panel.setLayout(grid);

		final Group dataGroup = createGroup(panel, "preference.page.group.data");
		dataGroup.setLayout(new GridLayout());

		final Label dataDirectory = new Label(dataGroup, SWT.NONE);
		dataDirectory.setText(JSurePreferencesUtility.getJSureDataDirectory()
				.getAbsolutePath());
		dataDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));

		final Group xmlDiffDirGroup = createGroup(panel,
				"preference.page.group.xmlDiffDir");
		xmlDiffDirGroup.setLayout(new GridLayout(2, false));

		final Label xmlDiffDir = new Label(xmlDiffDirGroup, SWT.NONE);
		xmlDiffDir
				.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		f_xmlDiffDir = new PathPreferenceEditor(xmlDiffDir,
				IDEPreferences.JSURE_XML_DIFF_DIRECTORY);
		f_xmlDiffDir.show();

		final Button change = new Button(xmlDiffDirGroup, SWT.PUSH);
		change.setText(I18N
				.msg("jsure.eclipse.preference.page.changexmlDiffDir"));
		change.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT, false,
				false));
		change.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				final File existing = JSurePreferencesUtility
						.getJSureXMLDirectory();
				final ChangeDirectoryLocationDialog dialog = new ChangeDirectoryLocationDialog(
						change.getShell(),
						existing,
						I18N.msg("jsure.eclipse.change.data.directory.dialog.title"),
						SLImages.getImage(CommonImages.IMG_JSURE_LOGO),
						I18N.msg("jsure.eclipse.change.data.directory.dialog.information"));

				if (dialog.open() != Window.OK) {
					return;
				}

				if (!dialog.isValidChangeToDataDirectory()) {
					return;
				}

				final File path = dialog.getNewDataDirectory();
				f_xmlDiffDir.set(path.getAbsolutePath());
				f_xmlDiffDir.show();
			}
		});

		final Group diGroup = createGroup(panel, "preference.page.group.app");

		f_balloonFlag = new BooleanFieldEditor(
				JSurePreferencesUtility.SHOW_BALLOON_NOTIFICATIONS,
				I18N.msg("jsure.eclipse.preference.page.balloonFlag"), diGroup);
		setupEditor(diGroup, f_balloonFlag);

		setupForPerspectiveSwitch(diGroup);

		f_selectProjectsToScan = new BooleanFieldEditor(
				JSurePreferencesUtility.ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_SCAN,
				I18N.msg("jsure.eclipse.preference.page.selectProjectsToScan"),
				diGroup);
		setupEditor(diGroup, f_selectProjectsToScan);

		f_selectProjectsToUpdateJar = new BooleanFieldEditor(
				JSurePreferencesUtility.ALWAYS_ALLOW_USER_TO_SELECT_PROJECTS_TO_UPDATE_JAR,
				I18N.msg("jsure.eclipse.preference.page.selectProjectsToUpdateJar"),
				diGroup);
		setupEditor(diGroup, f_selectProjectsToUpdateJar);

		f_autoSaveDirtyEditorsBeforeVerify = new BooleanFieldEditor(
				JSurePreferencesUtility.SAVE_DIRTY_EDITORS_BEFORE_VERIFY,
				I18N.msg("jsure.eclipse.preference.page.autoSaveBeforeVerify"),
				diGroup);
		setupEditor(diGroup, f_autoSaveDirtyEditorsBeforeVerify);

		final Group annoGroup = createGroup(panel,
				"preference.page.group.annos");
		f_allowJavadocAnnos = new BooleanFieldEditor(
				IDEPreferences.ALLOW_JAVADOC_ANNOS,
				I18N.msg("jsure.eclipse.preference.page.allowJavadocAnnos"),
				annoGroup);
		setupEditor(annoGroup, f_allowJavadocAnnos);

		final Group analysisSettingsGroup = createGroup(panel,
				"preference.page.group.analysis");
		final Label description = new Label(analysisSettingsGroup, SWT.NONE);
		description.setText(I18N
				.msg("jsure.eclipse.preference.page.analysis.desc"));
		description.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true,
				true, 2, 1));
		setupThreadCount(analysisSettingsGroup);
		setupMemorySize(analysisSettingsGroup);
		setupTimeoutWarning(analysisSettingsGroup);
		f_timeoutFlag = new BooleanFieldEditor(IDEPreferences.TIMEOUT_FLAG,
				I18N.msg("jsure.eclipse.preference.page.timeoutFlag"),
				analysisSettingsGroup);
		setupEditor(analysisSettingsGroup, f_timeoutFlag);
		setupTimeout(analysisSettingsGroup);
		f_loadAllClassesFlag = new BooleanFieldEditor(
				IDEPreferences.LOAD_ALL_CLASSES,
				I18N.msg("jsure.eclipse.preference.page.loadAllClasses"),
				analysisSettingsGroup);
		setupEditor(analysisSettingsGroup, f_loadAllClassesFlag);

		return panel;
	}

	private void setupScaleEditor(Group group, final ScaleFieldEditor editor,
			int min, int max, int incr, final String label) {
		editor.fillIntoGrid(group, 2);
		editor.setMinimum(min);
		editor.setMaximum(max);
		editor.setPageIncrement(incr);
		editor.setPage(this);
		editor.setPreferenceStore(EclipseUIUtility.getPreferences());
		editor.load();
		editor.getScaleControl().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(final Event event) {
				updateScaleLabel(editor, label);
			}
		});
	}

	private void updateScaleLabel(ScaleFieldEditor editor, String msg) {
		final int param = editor.getScaleControl().getSelection();
		editor.setLabelText(I18N.msg(msg, param));
	}

	private void setupThreadCount(Group group) {
		int threads = EclipseUtility
				.getIntPreference(IDEPreferences.ANALYSIS_THREAD_COUNT);
		f_analysisThreadCount = new ScaleFieldEditor(
				IDEPreferences.ANALYSIS_THREAD_COUNT, I18N.msg(THREADS_LABEL,
						threads), group);
		int max = Runtime.getRuntime().availableProcessors();
		setupScaleEditor(group, f_analysisThreadCount, 1, max, 1, THREADS_LABEL);
	}

	private void setupMemorySize(Group group) {
		final int estimatedMax = MemoryUtility.computeMaxMemorySizeInMb();
		int mb = EclipseUtility.getIntPreference(IDEPreferences.TOOL_MEMORY_MB);
		if (mb > estimatedMax) {
			mb = estimatedMax;
			EclipseUtility.setIntPreference(IDEPreferences.TOOL_MEMORY_MB, mb);
		}
		final String label = I18N.msg(TOOL_MB_LABEL, mb);
		f_toolMemoryMB = new ScaleFieldEditor(IDEPreferences.TOOL_MEMORY_MB,
				label + "     ", group);
		setupScaleEditor(group, f_toolMemoryMB, 256, estimatedMax, 256,
				TOOL_MB_LABEL);
	}

	private void setupTimeoutWarning(Group group) {
		int timeoutWarningSec = EclipseUtility
				.getIntPreference(IDEPreferences.TIMEOUT_WARNING_SEC);

		final String label = I18N.msg(TIMEOUT_WARNING_LABEL, timeoutWarningSec);
		f_timeoutWarningSec = new ScaleFieldEditor(
				IDEPreferences.TIMEOUT_WARNING_SEC, label + "     ", group);
		setupScaleEditor(group, f_timeoutWarningSec, 5, 600, 60,
				TIMEOUT_WARNING_LABEL);
	}

	private void setupTimeout(Group group) {
		int timeoutSec = EclipseUtility
				.getIntPreference(IDEPreferences.TIMEOUT_SEC);

		final String label = I18N.msg(TIMEOUT_LABEL, timeoutSec);
		f_timeoutSec = new ScaleFieldEditor(IDEPreferences.TIMEOUT_SEC, label
				+ "     ", group);
		setupScaleEditor(group, f_timeoutSec, 5, 600, 60, TIMEOUT_LABEL);
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
		f_xmlDiffDir.loadDefault();
		f_balloonFlag.loadDefault();
		f_selectProjectsToScan.loadDefault();
		f_selectProjectsToUpdateJar.loadDefault();
		f_autoSaveDirtyEditorsBeforeVerify.loadDefault();
		f_allowJavadocAnnos.loadDefault();
		f_analysisThreadCount.loadDefault();
		f_toolMemoryMB.loadDefault();
		f_timeoutWarningSec.loadDefault();
		f_timeoutFlag.loadDefault();
		f_timeoutSec.loadDefault();
		f_loadAllClassesFlag.loadDefault();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		f_xmlDiffDir.store();
		f_balloonFlag.store();
		f_selectProjectsToScan.store();
		f_selectProjectsToUpdateJar.store();
		f_autoSaveDirtyEditorsBeforeVerify.store();
		f_allowJavadocAnnos.store();
		f_analysisThreadCount.store();
		f_toolMemoryMB.store();
		f_timeoutWarningSec.store();
		f_timeoutFlag.store();
		f_timeoutSec.store();
		f_loadAllClassesFlag.store();
		return super.performOk();
	}
}
