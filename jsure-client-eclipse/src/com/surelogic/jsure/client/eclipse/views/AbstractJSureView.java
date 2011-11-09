package com.surelogic.jsure.client.eclipse.views;

import com.surelogic.common.ui.views.AbstractSLView;
import com.surelogic.jsure.client.eclipse.editors.EditorUtil;

import edu.cmu.cs.fluid.java.ISrcRef;

/**
 * Various code that's proven handy in JSure views
 * 
 * @author Edwin
 */
public abstract class AbstractJSureView extends AbstractSLView {
	/**
	 * Open and highlight a line within the Java editor, if possible. Otherwise,
	 * try to open as a text file
	 * 
	 * @param srcRef
	 *            the source reference to highlight
	 */
	protected final void highlightLineInJavaEditor(ISrcRef srcRef) {
		try {
			EditorUtil.highlightLineInJavaEditor(srcRef);
		} catch (org.eclipse.core.runtime.CoreException e) {
			showMessage("CoreException was thrown");
		}
	}
}

