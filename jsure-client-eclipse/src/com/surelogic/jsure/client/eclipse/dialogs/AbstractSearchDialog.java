package com.surelogic.jsure.client.eclipse.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.model.selection.SelectionManager;

/**
 * Common code used for OpenSearchDialog and DeleteSearchDialog
 * 
 * @author Edwin.Chan
 */
public abstract class AbstractSearchDialog extends Dialog {

	protected final SelectionManager f_manager = SelectionManager.getInstance();

	protected Mediator f_mediator = null;

	public AbstractSearchDialog(Shell parent) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}

	@Override
	protected final Control createContents(Composite parent) {
		final Control result = super.createContents(parent);
		f_mediator.setDialogState();
		return result;
	}

	protected Composite setupDialogArea(Composite parent, String title, int projectListFlags) {
		Composite panel = (Composite) super.createDialogArea(parent);
		final GridLayout gridLayout = new GridLayout();
		panel.setLayout(gridLayout);

		final Label l = new Label(panel, SWT.WRAP);
		GridData data = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		l.setLayoutData(data);
		l.setText(title);

		final Group projectGroup = new Group(panel, SWT.NONE);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 200;
		projectGroup.setLayoutData(data);
		projectGroup.setText("Saved Searches");
		projectGroup.setLayout(new FillLayout());

		final Table projectList = new Table(projectGroup, SWT.MULTI | SWT.FULL_SELECTION);

		for (String projectName : f_manager.getSavedSelectionNames()) {
			TableItem item = new TableItem(projectList, SWT.NONE);
			item.setText(projectName);
			item.setImage(SLImages.getImage(CommonImages.IMG_JSURE_FINDER));
		}

		createMediator(projectList);
		f_mediator.init();
		return panel;
	}

	protected abstract void createMediator(final Table projectList);

	@Override
	protected final void okPressed() {
		if (f_mediator != null)
			f_mediator.okPressed();
		super.okPressed();
	}

	public final void setOKEnabled(boolean enabled) {
		Button ok = getButton(IDialogConstants.OK_ID);
		ok.setEnabled(enabled);
	}

	protected abstract class Mediator {
		protected final Table f_searchTable;

		Mediator(Table searchTable) {
			f_searchTable = searchTable;
		}

		void init() {
			f_searchTable.addListener(SWT.Selection, new Listener() {
				@Override
        public void handleEvent(Event event) {
					setDialogState();
				}
			});
		}

		void setDialogState() {
			setOKEnabled(f_searchTable.getSelectionCount() > 0);
		}

		abstract void okPressed();
	}
}
