package com.surelogic.jsure.client.eclipse.dialogs;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;

import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.CommonImages;

import edu.cmu.cs.fluid.dc.Nature;

public final class JavaProjectSelectionDialog extends Dialog {

	private final String f_label;
	private final String f_shellTitle;
	private final Image f_shellImage;

	private final List<IJavaProject> f_openJavaProjects;
	private Table f_projectTable;
	/**
	 * Aliased and visible to the static call
	 * {@link #getProject(String, String, Image)}.
	 */
	private IJavaProject f_focusProject = null;

	/**
	 * Run in the SWT thread.
	 */
	public static IJavaProject getProject(final String label,
			final String shellTitle, final Image shellImage) {

		final List<IJavaProject> openJavaProjects = JDTUtility
				.getJavaProjects();

		final Shell shell = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getShell();

		if (openJavaProjects.isEmpty()) {
			final String msg = I18N.msg("jsure.eclipse.noJavaProjectsOpen");
			final MessageDialog dialog = new MessageDialog(shell,
					"No Projects Open", shellImage, msg,
					MessageDialog.INFORMATION, new String[] { "OK" }, 0);
			dialog.open();
			return null;
		} else {
			final JavaProjectSelectionDialog dialog = new JavaProjectSelectionDialog(
					shell, label, shellTitle, shellImage, openJavaProjects);

			if (dialog.open() == Window.CANCEL) {
				return null;
			} else {
				return dialog.f_focusProject;
			}
		}
	}

	private JavaProjectSelectionDialog(Shell parentShell, String label,
			String shellTitle, Image shellImage,
			final List<IJavaProject> openJavaProjects) {
		super(parentShell);
		this.f_label = label;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		f_shellTitle = shellTitle;
		f_shellImage = shellImage;
		f_openJavaProjects = openJavaProjects;
	}

	@Override
	protected final void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setImage(f_shellImage);
		newShell.setText(f_shellTitle);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite panel = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		panel.setLayout(gridLayout);

		final Label label = new Label(panel, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		label.setText(f_label);

		f_projectTable = new Table(panel, SWT.FULL_SELECTION);
		final GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 200;
		f_projectTable.setLayoutData(data);

		int index = 0;
		for (IJavaProject jp : f_openJavaProjects) {
			TableItem item = new TableItem(f_projectTable, SWT.NONE);
			item.setText(jp.getElementName());
			item.setImage(SLImages.getImage(CommonImages.IMG_PROJECT));
			item.setData(jp);
			if (Nature.hasNature(jp.getProject())) {
				f_projectTable.select(index);
				f_focusProject = jp;
			}
			index++;
		}

		f_projectTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				setOKState();
			}
		});
		f_projectTable.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event event) {
				if (getButton(IDialogConstants.OK_ID).isEnabled()) {
					okPressed();
				}
			}
		});

		return panel;
	}

	@Override
	protected final Control createContents(Composite parent) {
		final Control contents = super.createContents(parent);
		setOKState();
		return contents;
	}

	private final void setOKState() {
		int index = f_projectTable.getSelectionIndex();
		if (index == -1) {
			// no selection
			f_focusProject = null;
		} else {
			TableItem item = f_projectTable.getItem(index);
			if (item == null) {
				f_focusProject = null;
			} else {
				f_focusProject = (IJavaProject) item.getData();
			}
		}
		/*
		 * Set the state of the OK button.
		 */
		getButton(IDialogConstants.OK_ID).setEnabled(f_focusProject != null);
	}
}
