package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

import edu.cmu.cs.fluid.sea.IProofDropInfo;
import edu.cmu.cs.fluid.sea.ResultDrop;

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
	public static final String TIMEOUT = "Timeout";

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
		f_counts.clear();
		int runningTotal = 0;
		for (IProofDropInfo d : incomingResults) {
			final String value = getFilterValueOrNull(d);
			if (value != null) {
				Integer count = f_counts.get(value);
				if (count == null) {
					f_counts.put(value, 1);
				} else {
					f_counts.put(value, count + 1);
				}
				runningTotal++;
			}
		}
		f_countTotal = runningTotal;
	}

	@Override
	protected void refreshPorousDrops(List<IProofDropInfo> incomingResults) {
		f_porousDrops.clear();
		for (IProofDropInfo d : incomingResults) {
			final String value = getFilterValueOrNull(d);
			if (value != null) {
				if (f_porousValues.contains(value))
					f_porousDrops.add(d);
			}
		}
	}

	private String getFilterValueOrNull(IProofDropInfo d) {
		if (d.isInstance(ResultDrop.class)) {
			final String value;
			if (d.isVouched())
				value = VOUCHED;
			else if (d.isTimeout())
				value = TIMEOUT;
			else if (d.isConsistent())
				value = CONSISTENT;
			else
				value = INCONSISTENT;
			return value;
		}
		return null;
	}
}
