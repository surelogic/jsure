package com.surelogic.jsure.client.eclipse.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.XUtil;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.dialogs.AddUninterestingPackageFilterDialog;
import com.surelogic.jsure.core.preferences.UninterestingPackageFilterUtility;

public class UninterestingPackageFilterPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  private Table f_filterTable;
  private Button f_buttonRemove;

  @Override
  public void init(IWorkbench workbench) {
    // do nothing
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 2;
    composite.setLayout(gridLayout);
    Label label1 = new Label(composite, SWT.NONE);
    label1.setText(I18N.msg("jsure.eclipse.preference.page.uninteresting.filter.msg"));
    GridData data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.horizontalSpan = 2;
    data.grabExcessHorizontalSpace = true;
    label1.setLayoutData(data);
    Label label2 = new Label(composite, SWT.NONE);
    label2.setText(I18N.msg("jsure.eclipse.preference.page.uninteresting.filter.table.msg"));
    data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.horizontalSpan = 2;
    data.grabExcessHorizontalSpace = true;
    label2.setLayoutData(data);
    f_filterTable = new Table(composite, SWT.FULL_SELECTION);
    data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    data.verticalAlignment = GridData.FILL;
    data.grabExcessVerticalSpace = true;
    f_filterTable.setLayoutData(data);

    setTableContents(UninterestingPackageFilterUtility.getPreference());

    f_filterTable.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectionMayHaveChanged();
      }
    });

    Composite buttonHolder = new Composite(composite, SWT.NONE);
    RowLayout rowLayout = new RowLayout();
    rowLayout.type = SWT.VERTICAL;
    rowLayout.pack = false;
    buttonHolder.setLayout(rowLayout);
    data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.verticalAlignment = SWT.TOP;
    buttonHolder.setLayoutData(data);
    Button buttonAddFilter = new Button(buttonHolder, SWT.PUSH);
    buttonAddFilter.setText("&Add Filter...");
    buttonAddFilter.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        addFilter();
        selectionMayHaveChanged();
      }
    });
    f_buttonRemove = new Button(buttonHolder, SWT.PUSH);
    f_buttonRemove.setText("&Remove");
    f_buttonRemove.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        deleteSelectedFilter();
        selectionMayHaveChanged();
      }
    });
    selectionMayHaveChanged();
    return composite;
  }

  void selectionMayHaveChanged() {
    if (!f_filterTable.isDisposed()) {
      TableItem[] selected = f_filterTable.getSelection();
      f_buttonRemove.setEnabled(selected.length > 0);
    }
  }

  void addFilter() {
    final AddUninterestingPackageFilterDialog dialog = new AddUninterestingPackageFilterDialog(getTableContents());
    if (Window.OK == dialog.open()) {
      final String regex = dialog.getFilter();
      if (regex != null && !"".equals(regex)) {
        addRowToTable(regex);
      }
    }
  }

  void deleteSelectedFilter() {
    if (!f_filterTable.isDisposed()) {
      TableItem[] selected = f_filterTable.getSelection();
      for (TableItem ti : selected) {
        ti.dispose();
      }
    }
  }

  void setTableContents(List<String> filters) {
    if (!f_filterTable.isDisposed()) {
      f_filterTable.removeAll();
      for (final String regex : filters) {
        addRowToTable(regex);
      }
    }
  }

  void addRowToTable(String regex) {
    final TableItem item = new TableItem(f_filterTable, SWT.NULL);
    item.setText(regex);
    item.setImage(SLImages.getImage(CommonImages.IMG_PACKAGE));
  }

  private List<String> getTableContents() {
    final List<String> result = new ArrayList<>();
    for (TableItem item : f_filterTable.getItems()) {
      result.add(item.getText());
    }
    return result;
  }

  @Override
  protected void performDefaults() {
    setTableContents(UninterestingPackageFilterUtility.DEFAULT);
    super.performDefaults();
    selectionMayHaveChanged();
  }

  @Override
  public boolean performOk() {
    /*
     * Save the filters.
     */
    final List<String> filters = getTableContents();
    UninterestingPackageFilterUtility.setPreference(filters, true);

    if (XUtil.useExperimental) {
      /*
       * We dump the list to the console so the default value can be cut an
       * pasted into the jsure-core-eclipse project. The value is defined as
       * DEFAULT in ModelingProblemFilterUtility in the
       * com.surelogic.jsure.core.preferences package.
       */
      StringBuilder b = new StringBuilder();
      b.append("-- Cut/Paste below to DEFAULT at line 23 of UninterestingPackageFilterUtility");
      b.append(" in the com.surelogic.jsure.core.preferences package");
      b.append('\n');
      b.append("   Arrays.asList(");
      boolean first = true;
      for (String regex : filters) {
        if (first)
          first = false;
        else
          b.append(',');
        b.append('\"');
        b.append(SLUtility.escapeJavaStringForQuoting(regex));
        b.append('\"');
      }
      b.append(")\n");
      System.out.println(b.toString());
    }
    return true;
  }
}
