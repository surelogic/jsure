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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
	 * @return a map, if any user changes were made, containing the modified
	 *         contents of the passed attributes, an empty map otherwise. If an
	 *         unempty map is returned it contains only keys with modified
	 *         values from the values contained in the passed attributes map.
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
			System.out.println(dialog.f_scratch);
			System.out.println(dialog.getModifiedAttributes());
			return dialog.getModifiedAttributes();
		}
		return Collections.emptyMap();
	}

	/*
	 * Immutable input data
	 */
	private final Map<Attribute, String> f_attributes;
	private final String f_annotation;

	/*
	 * Boolean true and false strings in XML
	 */
	private final String T = "true";
	private final String F = "false";

	/*
	 * Attribute working data.
	 */
	private final Map<Attribute, String> f_scratch;

	private LibraryAnnotationDialog(final AnnotationElement annotation,
			final Map<Attribute, String> attributes) {
		super(EclipseUIUtility.getShell());
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		f_annotation = annotation.getPromise();
		f_attributes = Collections.unmodifiableMap(attributes);
		f_scratch = new HashMap<Attribute, String>(attributes);
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

		final int editorHeight = 100;
		GridData data;

		final List<Attribute> stringAttributes = getSortedAttributesOfType(String.class);
		if (!stringAttributes.isEmpty()) {

			/*
			 * String attribute editor
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

			for (final Attribute a : stringAttributes) {
				final Label name = new Label(stringPanel, SWT.NONE);
				name.setText(a.getName());
				name.setForeground(getShell().getDisplay().getSystemColor(
						SWT.COLOR_BLUE));
				name.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
						false));
				final Text value = new Text(stringPanel, SWT.SINGLE);
				value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
						false));
				value.setText(f_attributes.get(a));
				value.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(ModifyEvent e) {
						f_scratch.put(a, value.getText());
						updatePreviewAnnotationInDialog();
					}
				});
			}
		}

		final List<Attribute> booleanAttributes = getSortedAttributesOfType(boolean.class);
		if (!booleanAttributes.isEmpty()) {

			/*
			 * boolean attribute editor
			 */

			final Label label = new Label(panel, SWT.WRAP);
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			label.setText(I18N.msg("jsure.dialog.library.xml.boolean.label"));

			final Table table = new Table(panel, SWT.FULL_SELECTION | SWT.CHECK);
			data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.heightHint = editorHeight;
			table.setLayoutData(data);

			for (Attribute a : booleanAttributes) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setData(a);
				item.setText(a.getName());
				item.setImage(SLImages.getImage(CommonImages.IMG_GREEN_DOT));
				item.setChecked(T.equals(f_attributes.get(a)));
			}

			table.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					for (final TableItem item : table.getItems()) {
						if (item.getData() instanceof Attribute) {
							final Attribute a = (Attribute) item.getData();
							String value = item.getChecked() ? T : F;
							f_scratch.put(a, value);
							updatePreviewAnnotationInDialog();
						}
					}
				}
			});
		}

		updatePreviewAnnotationInDialog();
		setMessage(I18N.msg("jsure.dialog.library.xml.msg"),
				IMessageProvider.INFORMATION);
		Dialog.applyDialogFont(panel);

		return panel;
	}

	/**
	 * Filters {@link #f_attributes} down to a particular type and sorts the
	 * list returned.
	 * 
	 * @param type
	 *            the type to filter by.
	 * @return a list of {@link Attribute} objects that is sorted by name.
	 */
	private List<Attribute> getSortedAttributesOfType(Class<?> type) {
		final List<Attribute> result = new ArrayList<Attribute>();
		for (Attribute a : f_attributes.keySet()) {
			if (a.getType().equals(type)) {
				result.add(a);
			}
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * Gets the changes made to the attributes by the user in the dialog.
	 * 
	 * @return a map, if any user changes were made, containing the modified
	 *         contents of the passed attributes, an empty map otherwise. If an
	 *         unempty map is returned it contains only keys with modified
	 *         values from the values contained in the passed attributes map.
	 */
	private Map<Attribute, String> getModifiedAttributes() {
		final Map<Attribute, String> result = new HashMap<Attribute, String>();
		for (Map.Entry<Attribute, String> e : f_scratch.entrySet()) {
			final String scratchValue = e.getValue();
			final String originalValue = f_attributes.get(e.getKey());
			if (!scratchValue.equals(originalValue)) {
				result.put(e.getKey(), scratchValue);
			}
		}
		return result;
	}

	private void updatePreviewAnnotationInDialog() {
		setTitle(getAnnotation());
	}

	private String getAnnotation() {
		final StringBuilder b = new StringBuilder();
		b.append('@').append(f_annotation).append('(');
		final Attribute value = getValue();
		boolean showComma = false;
		if (value != null) {
			/*
			 * Show the value attribute first without value=
			 */
			final String s = f_scratch.get(value);
			if (s != null && s.length() > 0) { // should be non-null
				b.append('\"');
				b.append(f_scratch.get(value));
				b.append('\"');
				showComma = true;
			}
		}
		/*
		 * Show the rest of the attributes only if they are different than their
		 * default values.
		 */
		for (Map.Entry<Attribute, String> e : f_scratch.entrySet()) {
			if (e.getKey().equals(value))
				continue;
			if (!e.getValue().equals(e.getKey().getDefaultValueOrNull())) {
				if (showComma)
					b.append(',');
				else
					showComma = true;
				b.append(e.getKey().getName());
				b.append('=');
				if (e.getKey().isTypeString())
					b.append('\"');
				b.append(e.getValue());
				if (e.getKey().isTypeString())
					b.append('\"');
			}
		}
		b.append(')');
		return b.toString();
	}

	private Attribute getValue() {
		for (Attribute a : f_scratch.keySet()) {
			if ("value".equals(a.getName()))
				return a;
		}
		return null;
	}
}
