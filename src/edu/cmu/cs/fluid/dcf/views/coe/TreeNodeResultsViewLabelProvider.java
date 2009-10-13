package edu.cmu.cs.fluid.dcf.views.coe;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.tree.diff.IDiffNode.Status;
import com.surelogic.xml.results.coe.CoE_Constants;
import com.surelogic.xml.results.coe.ResultsTreeNode;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;

public final class TreeNodeResultsViewLabelProvider extends LabelProvider
		implements IResultsViewLabelProvider {
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
		if (obj instanceof ResultsTreeNode) {
			final ResultsTreeNode n = (ResultsTreeNode) obj;
			final Status status = n.getStatus();
			if (status == Status.ORIGINAL || status == Status.CHANGED
					|| status == Status.ADDED || status == Status.DELETED) {
				return status + " " + n.getMessage();
			}
			return n.getMessage();
		}
		return "invalid: not of type ResultsTreeNode";
	}

	@Override
	public Image getImage(final Object obj) {
		if (obj instanceof ResultsTreeNode) {
			final ResultsTreeNode n = (ResultsTreeNode) obj;
			int flags = Integer.valueOf(n.flags);
			// If we don't show inferences, remove them from the flags
			if (!m_showInferences) {
				flags = flags & ~CoE_Constants.INFO_WARNING
						& ~CoE_Constants.INFO;
			}
			final ImageDescriptor id = SLImages.getImageDescriptor(n.baseImage);
			final ResultsImageDescriptor rid = new ResultsImageDescriptor(id,
					flags, AbstractDoubleCheckerView.ICONSIZE);
			return rid.getCachedImage();
		}
		return SLImages.getImage(CommonImages.IMG_UNKNOWN);
	}
}
