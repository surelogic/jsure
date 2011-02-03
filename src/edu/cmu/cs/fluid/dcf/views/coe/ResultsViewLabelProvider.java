package edu.cmu.cs.fluid.dcf.views.coe;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.xml.results.coe.CoE_Constants;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;

public final class ResultsViewLabelProvider extends ColumnLabelProvider implements
		IResultsViewLabelProvider {
	//private static final boolean showCustomToolTips = false;
	private boolean m_showInferences = true;

	/**
	 * @param showInferences
	 *            The showInferences to set.
	 */
	public final void setShowInferences(final boolean showInferences) {
		m_showInferences = showInferences;
	}

	@SuppressWarnings("unchecked")
	public String getText(final Object obj) {
		if (obj instanceof AbstractContent) {
			AbstractContent c = (AbstractContent) obj;
			/*
			 * if (c.referencedDrop != null) { return
			 * c.referencedDrop.getClass().getSimpleName()+": "+c.getMessage();
			 * }
			 */
			return c.getMessage();
		}
		return "invalid: not of type AbstractContent";
	}

	@SuppressWarnings("unchecked")
	public Image getImage(final Object obj) {
		if (obj instanceof AbstractContent) {
			AbstractContent c = (AbstractContent) obj;
			int flags = c.getImageFlags();
			if (m_showInferences) {
				if (c.f_isInfoWarningDecorate) {
					flags |= CoE_Constants.INFO_WARNING;
				} else if (c.f_isInfoDecorated) {
					if (!CommonImages.IMG_INFO.equals(c.getBaseImageName()))
						flags |= CoE_Constants.INFO;
				}
			}
			ImageDescriptor id = SLImages.getImageDescriptor(c
					.getBaseImageName());
			ResultsImageDescriptor rid = new ResultsImageDescriptor(id, flags,
					AbstractDoubleCheckerView.ICONSIZE);
			return rid.getCachedImage();
		}
		return SLImages.getImage(CommonImages.IMG_UNKNOWN);
	}

	public Image getToolTipImage(Object element) {
		return null;
		//return showCustomToolTips ? getImage(element) : null;
	}
	
	//@Override
	public String getToolTipText(Object element) {
		return null;
		//return showCustomToolTips ? "Tooltip\n (" + element + ")" : null;
	}

	//@Override
	public Point getToolTipShift(Object object) {
		return new Point(5, 5);
	}

	//@Override
	public int getToolTipDisplayDelayTime(Object object) {
		return 200;
	}

	//@Override
	public int getToolTipTimeDisplayed(Object object) {
		return 5000;
	}

	//@Override
	public void update(ViewerCell cell) {
		final Object element = cell.getElement();
		cell.setText(getText(element));
		cell.setImage(getImage(element));
	}
}