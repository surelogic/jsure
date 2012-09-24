package com.surelogic.jsure.client.eclipse.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.ui.SLImages;
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
		EditorUtil.highlightLineInJavaEditor(srcRef);
	}

	/**
	 * Gets a cached image with an optional conflict (warning) decorator.
	 * 
	 * @param symbolicName
	 *            a name from {@link CommonImages}.
	 * @param showWariningDecorator
	 *            {@code true} if a warning decorator should be shown,
	 *            {@code false} otherwise.
	 * @return an image that is carefully cached. The image should <i>not</i> be
	 *         disposed by the calling code.
	 */
	public static final Image getCachedImage(String symbolicName, Decorator d) {
		return getCachedImage(SLImages.getImageDescriptor(symbolicName), d);
	}

	/**
	 * Gets a cached image with an optional conflict (warning) decorator.
	 * 
	 * @param imageDescriptor
	 *            an image descriptor.
	 * @param showWarningDecorator
	 *            {@code true} if a warning decorator should be shown,
	 *            {@code false} otherwise.
	 * @return an image that is carefully cached. The image should <i>not</i> be
	 *         disposed by the calling code.
	 */
	public static final Image getCachedImage(ImageDescriptor imageDescriptor,
			Decorator d) {
		ResultsImageDescriptor rid = new ResultsImageDescriptor(
				imageDescriptor, d.flag, new Point(22, 16));
		return rid.getCachedImage();
	}

	public enum Decorator {
		NONE(CoE_Constants.NONE), WARNING(CoE_Constants.HINT_WARNING), RED_DOT(
				CoE_Constants.REDDOT);

		final int flag;

		Decorator(int flag) {
			this.flag = flag;
		}
	}
}
