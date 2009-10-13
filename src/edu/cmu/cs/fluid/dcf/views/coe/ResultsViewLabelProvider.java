package edu.cmu.cs.fluid.dcf.views.coe;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.xml.results.coe.CoE_Constants;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;

public final class ResultsViewLabelProvider extends LabelProvider implements
		IResultsViewLabelProvider {
	private boolean m_showInferences = true;

	/**
	 * @param showInferences
	 *            The showInferences to set.
	 */
	public final void setShowInferences(final boolean showInferences) {
		m_showInferences = showInferences;
	}

	@Override
	public String getText(final Object obj) {
		if (obj instanceof Content) {
			Content c = (Content) obj;
			/*
			 * if (c.referencedDrop != null) { return
			 * c.referencedDrop.getClass().getSimpleName()+": "+c.getMessage();
			 * }
			 */
			return c.getMessage();
		}
		return "invalid: not of type Content";
	}

	@Override
	public Image getImage(final Object obj) {
		if (obj instanceof Content) {
			Content c = (Content) obj;
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
}