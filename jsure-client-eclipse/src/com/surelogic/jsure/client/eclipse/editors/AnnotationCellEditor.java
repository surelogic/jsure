package com.surelogic.jsure.client.eclipse.editors;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import com.surelogic.xml.AnnotationElement;

public class AnnotationCellEditor extends TextCellEditor {
	// Contents handled by the superclass
	private Text promise;
	
	AnnotationCellEditor(Composite parent) {
		super(parent);
    }
	
	@Override
	protected Control createControl(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		promise = new Text(c, SWT.SINGLE);
		super.createControl(c);
		
		GridLayout grid = new GridLayout(2, false);
		grid.horizontalSpacing = 0;
		grid.verticalSpacing = 0;
		grid.marginWidth = 0;
		grid.marginHeight = 0;		
		c.setLayout(grid);
		return c;
	}

	@Override
	protected Object doGetValue() {
		String contents = (String) super.doGetValue();
		if (contents.isEmpty()) {
			return promise.getText();
		}
		return promise.getText()+'('+contents+')';
	}

	@Override
	protected void doSetValue(Object value) {
		if (value instanceof AnnotationElement) {
			AnnotationElement a = (AnnotationElement) value;
			promise.setText(a.getPromise());
			super.doSetValue(a.getContents()+"                    ");
		} else {
			promise.setText("");
			super.doSetValue(value.toString());
		}		
		promise.getParent().layout();
		/*
		Rectangle b = promise.getBounds();
		promise.setBounds(b.x, b.y, b.width+100, b.height);		
		promise.redraw();
		*/
	}
}
