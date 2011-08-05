package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

import edu.cmu.cs.fluid.sea.IProofDropInfo;

public final class FilterAnalysisResults extends Filter {

	public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
		public Filter construct(Selection selection, Filter previous) {
			return new FilterAnalysisResults(selection, previous,
					getFilterLabel());
		}

		public String getFilterLabel() {
			return "Analysis Results";
		}

		@Override
		public Image getFilterImage() {
			return SLImages.getImage(CommonImages.IMG_ANALYSIS_RESULT);
		}
	};

	private FilterAnalysisResults(Selection selection, Filter previous,
			String filterLabel) {
		super(selection, previous, filterLabel);
	}

	@Override
	public ISelectionFilterFactory getFactory() {
		return FACTORY;
	}

	public static final String CONSISTENT = "Consistent";
	public static final String VOUCHED = "Vouched";
	public static final String INCONSISTENT = "Inconsistent";
	public static final String TIMEOUT = "Analysis Timeout";

	@Override
	protected void deriveAllValues() {
		synchronized (this) {
			f_allValues.clear();
			f_allValues.add(CONSISTENT);
			f_allValues.add(VOUCHED);
			f_allValues.add(INCONSISTENT);
			f_allValues.add(TIMEOUT);
		}
	}

	@Override
	public Image getImageFor(String value) {
		if (CONSISTENT.equals(value))
			return SLImages.getImage(CommonImages.IMG_PLUS);
		if (VOUCHED.equals(value))
			return SLImages.getImage(CommonImages.IMG_PLUS_VOUCH);
		if (INCONSISTENT.equals(value))
			return SLImages.getImage(CommonImages.IMG_RED_X);
		if (TIMEOUT.equals(value))
			return SLImages.getImage(CommonImages.IMG_TIMEOUT_X);

		return SLImages.getImage(CommonImages.IMG_EMPTY);
	}

	@Override
	protected void refreshCounts(List<IProofDropInfo> incomingResults) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void refreshPorousDrops(List<IProofDropInfo> incomingResults) {
		// TODO Auto-generated method stub

	}
}
