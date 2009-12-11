package com.surelogic.jsure.client.eclipse.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
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
  private BooleanFieldEditor f_allowJavadocAnnos;
  private IntegerFieldEditor f_analysisThreadCount;

  public JSurePreferencePage() {
    super("jsure.eclipse.", PreferenceConstants.prototype);
  }

  @Override
  protected Control createContents(Composite parent) {
    final Composite panel = new Composite(parent, SWT.NONE);
    GridLayout grid = new GridLayout();
    panel.setLayout(grid);

    final Group diGroup = createGroup(panel, "preference.page.group.app");

    setupForPerspectiveSwitch(diGroup);

    f_autoOpenModelingProblemsView = new BooleanFieldEditor(
        PreferenceConstants.P_AUTO_OPEN_MODELING_PROBLEMS_VIEW, I18N
            .msg("jsure.eclipse.preference.page.autoOpenModelingProblemsView"),
        diGroup);
    setupEditor(diGroup, f_autoOpenModelingProblemsView,
        PreferenceConstants.prototype.getPreferenceStore());

    final Group annoGroup = createGroup(panel, "preference.page.group.annos");
    f_allowJavadocAnnos = new BooleanFieldEditor(
        com.surelogic.fluid.eclipse.preferences.PreferenceConstants.P_ALLOW_JAVADOC_ANNOS,
        I18N.msg("jsure.eclipse.preference.page.allowJavadocAnnos"), annoGroup);
    setupEditor(annoGroup, f_allowJavadocAnnos,
        com.surelogic.fluid.eclipse.preferences.PreferenceConstants
            .getPreferenceStore());

    final Group threadGroup = createGroup(panel, "preference.page.group.thread");
    f_analysisThreadCount = new IntegerFieldEditor(
        com.surelogic.fluid.eclipse.preferences.PreferenceConstants.P_ANALYSIS_THREAD_COUNT,
        I18N.msg("jsure.eclipse.preference.page.thread.msg"), threadGroup);
    f_analysisThreadCount.setValidRange(1, 128);
    setupEditor(threadGroup, f_analysisThreadCount,
        com.surelogic.fluid.eclipse.preferences.PreferenceConstants
            .getPreferenceStore());

    return panel;
  }

  private Group createGroup(Composite panel, String suffix) {
    final Group group = new Group(panel, SWT.NONE);
    group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    group.setText(I18N.msg(messagePrefix + suffix));
    return group;
  }

  private void setupEditor(Group group, FieldEditor e, IPreferenceStore store) {
    e.fillIntoGrid(group, 2);
    e.setPage(this);
    e.setPreferenceStore(store);
    e.load();
  }

  @Override
  protected void performDefaults() {
    f_autoOpenModelingProblemsView.loadDefault();
    f_allowJavadocAnnos.loadDefault();
    f_analysisThreadCount.loadDefault();
    super.performDefaults();
  }

  @Override
  public boolean performOk() {
    f_autoOpenModelingProblemsView.store();
    f_allowJavadocAnnos.store();
    f_analysisThreadCount.store();
    return super.performOk();
  }
}
