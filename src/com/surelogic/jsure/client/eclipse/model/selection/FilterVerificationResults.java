package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.views.results.ResultsImageDescriptor;

import edu.cmu.cs.fluid.sea.IProofDropInfo;

public final class FilterVerificationResults extends Filter {

	public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
		public Filter construct(Selection selection, Filter previous) {
			return new FilterVerificationResults(selection, previous,
					getFilterLabel());
		}

		public String getFilterLabel() {
			return "Verification Results";
		}

		@Override
		public Image getFilterImage() {
			return SLImages.getImage(CommonImages.IMG_VERIFICATION_RESULT);
		}
	};

	private FilterVerificationResults(Selection selection, Filter previous,
			String filterLabel) {
		super(selection, previous, filterLabel);
	}

	@Override
	public ISelectionFilterFactory getFactory() {
		return FACTORY;
	}

	public static final String CONSISTENT = "Consistent";
	public static final String INCONSISTENT = "Inconsistent";
	public static final String REDDOT = "Contingent";

	@Override
	protected void deriveAllValues() {
		synchronized (this) {
			f_allValues.clear();
			f_allValues.add(CONSISTENT);
			f_allValues.add(INCONSISTENT);
			f_allValues.add(REDDOT);
		}
	}

	private static final ResultsImageDescriptor ID_CONSISTENT = new ResultsImageDescriptor(
			SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION),
			CoE_Constants.CONSISTENT, new Point(22, 16));
	private static final ResultsImageDescriptor ID_INCONSISTENT = new ResultsImageDescriptor(
			SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION),
			CoE_Constants.INCONSISTENT, new Point(22, 16));
	private static final ResultsImageDescriptor ID_REDDOT = new ResultsImageDescriptor(
			SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION),
			CoE_Constants.REDDOT, new Point(22, 16));

	@Override
	public Image getImageFor(String value) {
		if (CONSISTENT.equals(value))
			return ID_CONSISTENT.getCachedImage();
		if (INCONSISTENT.equals(value))
			return ID_INCONSISTENT.getCachedImage();
		if (REDDOT.equals(value))
			return ID_REDDOT.getCachedImage();

		return SLImages.getImage(CommonImages.IMG_EMPTY);
	}

	@Override
	protected void refreshCountsAndPorousDrops(
			List<IProofDropInfo> incomingResults) {
		// TODO Auto-generated method stub

	}
}
