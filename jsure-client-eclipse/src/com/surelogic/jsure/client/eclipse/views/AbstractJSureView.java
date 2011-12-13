package com.surelogic.jsure.client.eclipse.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.views.AbstractSLView;
import com.surelogic.jsure.client.eclipse.editors.EditorUtil;
import com.surelogic.jsure.client.eclipse.views.results.ResultsImageDescriptor;

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
	

	/**
	 * Gets a cached image with an optional conflict (warning) decorator.
	 * 
	 * @param symbolicName
	 *            a name from {@link CommonImages}.
	 * @param conflict
	 *            {@code true} if a promise conflict exists, {@code false}
	 *            otherwise.
	 * @return an image that is carefully cached. The image should
	 *         <i>not</i> be disposed by the calling code.
	 */
	public static final Image getCachedImage(String symbolicName, boolean conflict) {
		return getCachedImage(SLImages.getImageDescriptor(symbolicName),
				conflict);
	}

	/**
	 * Gets a cached image with an optional conflict (warning) decorator.
	 * 
	 * @param imageDescriptor
	 *            an image descriptor.
	 * @param conflict
	 *            {@code true} if a promise conflict exists, {@code false}
	 *            otherwise.
	 * @return an image that is carefully cached. The image should
	 *         <i>not</i> be disposed by the calling code.
	 */
	public static final Image getCachedImage(ImageDescriptor imageDescriptor,
			boolean conflict) {
		final int flag = conflict ? CoE_Constants.INFO_WARNING
				: CoE_Constants.NONE;
		ResultsImageDescriptor rid = new ResultsImageDescriptor(
				imageDescriptor, flag, new Point(22, 16));
		return rid.getCachedImage();
	}
}

