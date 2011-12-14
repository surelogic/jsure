package com.surelogic.jsure.client.eclipse.dialogs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
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

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.xml.AnnotationElement;

/**
 * A dialog to edit the attributes of an single library annotation. This dialog
 * is used by the library annotation editor to allow the user to change
 * attributes of an annotation shown in the editor.
 */
public final class LibraryAnnotationDialog extends Dialog {

	/**
	 * Called to open a dialog that edits the passed attributes.
	 * 
	 * @param annotation
	 *            the annotation being edited. Not modified by this call.
	 * @param attributes
	 *            a map of the annotation's attributes. This map is not modified
	 *            by this call (any changes are returned in a new map).
	 * @return A map, if any user changes were made, containing the modified
	 *         contents of the passed attributes, {@code null} otherwise. If a
	 *         map is returned it contains only keys with modified values from
	 *         the values contained in the passed attributes map.
	 * 
	 * @throws IllegalArgumentException
	 *             if either of the passed parameters are {@code null}.
	 */
	public static Map<String, String> edit(AnnotationElement annotation,
			Map<String, String> attributes) {
		if (annotation == null)
			throw new IllegalArgumentException(I18N.err(33, "annotation"));
		if (attributes == null)
			throw new IllegalArgumentException(I18N.err(33, "attributes"));
		final LibraryAnnotationDialog dialog = new LibraryAnnotationDialog(
				annotation, attributes);
		if (dialog.open() == Dialog.OK) {
			return dialog.f_edits;
		}
		return null;
	}

	private final Map<String, String> f_attributes;
	private final Map<String, String> f_edits;
	private final AnnotationElement f_annotation;

	private Table f_projectTable;

	private LibraryAnnotationDialog(AnnotationElement annotation,
			Map<String, String> attributes) {
		super(EclipseUIUtility.getShell());
		f_annotation = annotation;
		f_attributes = Collections.unmodifiableMap(attributes);
		f_edits = new HashMap<String, String>(attributes);
	}

	@Override
	protected final void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setImage(SLImages.getImage(CommonImages.IMG_ANNOTATION));
		newShell.setText("Add/Edit Library Annotation");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite panel = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		panel.setLayout(gridLayout);

		final Label label = new Label(panel, SWT.WRAP);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		label.setText("Select the attributes for this annotation you want to be true");

		f_projectTable = new Table(panel, SWT.FULL_SELECTION);
		final GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 200;
		f_projectTable.setLayoutData(data);

		for (Map.Entry<String, String> entry : f_attributes.entrySet()) {
			TableItem item = new TableItem(f_projectTable, SWT.NONE);
			item.setText(entry.getKey());
			item.setImage(SLImages.getImage(CommonImages.IMG_PROJECT));
			item.setData(entry.getKey());
			item.setChecked("true".equals(entry.getValue()));
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
		
		/*
		 * Set the state of the OK button.
		 */
		//getButton(IDialogConstants.OK_ID).setEnabled(f_focusProject != null);
	}
}
