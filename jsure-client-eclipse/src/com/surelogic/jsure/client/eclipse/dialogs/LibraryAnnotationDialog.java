package com.surelogic.jsure.client.eclipse.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
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
import org.eclipse.swt.widgets.Text;

import com.surelogic.annotation.rules.AnnotationRules.Attribute;
import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.xml.AnnotationElement;

/**
 * A dialog to edit the attributes of an single library annotation. This dialog
 * is used by the library annotation editor to allow the user to change
 * attributes of an annotation shown in the editor.
 */
public final class LibraryAnnotationDialog extends TitleAreaDialog {

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
	public static Map<Attribute, String> edit(AnnotationElement annotation,
			Map<Attribute, String> attributes) {
		if (annotation == null)
			throw new IllegalArgumentException(I18N.err(33, "annotation"));
		if (attributes == null)
			throw new IllegalArgumentException(I18N.err(33, "attributes"));
		final LibraryAnnotationDialog dialog = new LibraryAnnotationDialog(
				annotation, attributes);
		if (dialog.open() == Dialog.OK) {
			return null; // TODO
		}
		return null;
	}

	/*
	 * Immutable input data
	 */
	private final Map<Attribute, String> f_attributes;
	private final AnnotationElement f_annotation;

	private final String T = "true";
	private final String F = "false";

	/*
	 * Attribute working data.
	 */
	private final Map<Attribute, String> f_booleanAttributes = new HashMap<Attribute, String>();
	private final Map<Attribute, String> f_stringAttributes = new HashMap<Attribute, String>();

	private Table f_projectTable;

	private LibraryAnnotationDialog(AnnotationElement annotation,
			Map<Attribute, String> attributes) {
		super(EclipseUIUtility.getShell());
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		f_annotation = annotation;
		f_attributes = Collections.unmodifiableMap(attributes);
		for (Map.Entry<Attribute, String> entry : f_attributes.entrySet()) {
			if (boolean.class.equals(entry.getKey().getType())) {
				f_booleanAttributes.put(entry.getKey(), entry.getValue());
			} else if (String.class.equals(entry.getKey().getType())) {
				f_stringAttributes.put(entry.getKey(), entry.getValue());
			} else {
				/*
				 * The type is not supported by this dialog, log this as a
				 * problem.
				 */
				SLLogger.getLogger().warning(
						I18N.err(236, f_annotation.getLabel(), entry.getKey()
								.getType().getName()));
			}
		}
	}

	@Override
	protected final void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setImage(SLImages.getImage(CommonImages.IMG_ANNOTATION));
		newShell.setText(I18N.msg("jsure.dialog.library.xml.title"));
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite panel = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout();
		panel.setLayout(gridLayout);

		List<Attribute> attributes;

		if (!f_stringAttributes.isEmpty()) {

			/*
			 * String typed attribute editor
			 */

			final Label label = new Label(panel, SWT.WRAP);
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			label.setText(I18N.msg("jsure.dialog.library.xml.string.label"));

			final Composite stringPanel = new Composite(panel, SWT.NONE);
			stringPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					true));
			final GridLayout stringPanelLayout = new GridLayout();
			stringPanelLayout.numColumns = 2;
			stringPanel.setLayout(stringPanelLayout);

			attributes = new ArrayList<Attribute>(f_stringAttributes.keySet());
			Collections.sort(attributes);

			for (Attribute a : attributes) {
				final Label variableLabel = new Label(stringPanel, SWT.NONE);
				variableLabel.setText(a.getName());
				variableLabel.setForeground(getShell().getDisplay()
						.getSystemColor(SWT.COLOR_BLUE));
				variableLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER,
						false, false));
				final Text variableValue = new Text(stringPanel, SWT.SINGLE);
				variableValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
						true, true));
				variableValue.setText(f_stringAttributes.get(a));
			}
		}

		if (!f_booleanAttributes.isEmpty()) {

			/*
			 * boolean typed attributed editor
			 */

			final Label label = new Label(panel, SWT.WRAP);
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			label.setText(I18N.msg("jsure.dialog.library.xml.boolean.label"));

			f_projectTable = new Table(panel, SWT.FULL_SELECTION | SWT.CHECK);
			final GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.heightHint = 100;
			f_projectTable.setLayoutData(data);

			attributes = new ArrayList<Attribute>(f_booleanAttributes.keySet());
			Collections.sort(attributes);

			for (Attribute a : attributes) {
				TableItem item = new TableItem(f_projectTable, SWT.NONE);
				item.setText(a.getName());
				item.setImage(SLImages.getImage(CommonImages.IMG_GREEN_DOT));
				item.setChecked(T.equals(f_stringAttributes.get(a)));
			}

			f_projectTable.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					// TODO
				}
			});
		}

		setTitle(I18N.msg("jsure.dialog.library.xml.title"));
		setMessage(I18N.msg("jsure.dialog.library.xml.msg"),
				IMessageProvider.INFORMATION);
		Dialog.applyDialogFont(panel);

		return panel;
	}
}
