package com.surelogic.jsure.client.eclipse.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.surelogic.common.i18n.I18N;

public class ProblemsFilterPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private CheckboxTreeViewer checktree;

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
		label1.setText(I18N
				.msg("jsure.eclipse.preference.page.problemfilter.msg"));
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;
		label1.setLayoutData(data);
		Label label2 = new Label(composite, SWT.NONE);
		label2.setText(I18N
				.msg("jsure.eclipse.preference.page.problemfilter.table.msg"));
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		data.grabExcessHorizontalSpace = true;
		label2.setLayoutData(data);
		checktree = new CheckboxTreeViewer(composite, SWT.BORDER | SWT.H_SCROLL
				| SWT.V_SCROLL);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.grabExcessHorizontalSpace = true;
		data.verticalAlignment = GridData.FILL;
		data.grabExcessVerticalSpace = true;
		checktree.getControl().setLayoutData(data);
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
			}
		});
		Button buttonRemove = new Button(buttonHolder, SWT.PUSH);
		buttonRemove.setText("&Remove");
		buttonRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		new Label(buttonHolder, SWT.NONE);
		Button buttonSelectAll = new Button(buttonHolder, SWT.PUSH);
		buttonSelectAll.setText("&Select All");
		buttonSelectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// analysisModuleContentProvider.setAll(true);
			}
		});
		Button buttonDeselectAll = new Button(buttonHolder, SWT.PUSH);
		buttonDeselectAll.setText("D&eselect All");
		buttonDeselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// analysisModuleContentProvider.setAll(false);
			}
		});
		return composite;
	}
}
