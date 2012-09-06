package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.views.results.ResultsImageDescriptor;

import edu.cmu.cs.fluid.sea.IProofDropInfo;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public final class FilterVerificationJudgment extends Filter {

	public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
		public Filter construct(Selection selection, Filter previous) {
			return new FilterVerificationJudgment(selection, previous,
					getFilterLabel());
		}

		public String getFilterLabel() {
			return "Verification Judgment";
		}

		@Override
		public Image getFilterImage() {
			return SLImages.getImage(CommonImages.IMG_VERIFICATION_RESULT);
		}
	};

	private FilterVerificationJudgment(Selection selection, Filter previous,
			String filterLabel) {
		super(selection, previous, filterLabel);
	}

	@Override
	public ISelectionFilterFactory getFactory() {
		return FACTORY;
	}

	public static final String CONSISTENT = "Consistent";
	public static final String CONSISTENT_REDDOT = "Consistent (Contingent)";
	public static final String INCONSISTENT_REDDOT = "Inconsistent (Contingent)";
	public static final String INCONSISTENT = "Inconsistent";

	@Override
	protected void deriveAllValues() {
		synchronized (this) {
			f_allValues.clear();
			f_allValues.add(CONSISTENT);
			f_allValues.add(CONSISTENT_REDDOT);
			f_allValues.add(INCONSISTENT_REDDOT);
			f_allValues.add(INCONSISTENT);
		}
	}

	private static final ResultsImageDescriptor ID_CONSISTENT = new ResultsImageDescriptor(
			SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION),
			CoE_Constants.CONSISTENT, new Point(22, 16));
	private static final ResultsImageDescriptor ID_CONSISTENT_REDDOT = new ResultsImageDescriptor(
			SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION),
			CoE_Constants.CONSISTENT | CoE_Constants.REDDOT, new Point(22, 16));
	private static final ResultsImageDescriptor ID_INCONSISTENT_REDDOT = new ResultsImageDescriptor(
			SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION),
			CoE_Constants.INCONSISTENT | CoE_Constants.REDDOT,
			new Point(22, 16));
	private static final ResultsImageDescriptor ID_INCONSISTENT = new ResultsImageDescriptor(
			SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION),
			CoE_Constants.INCONSISTENT, new Point(22, 16));

	@Override
	public Image getImageFor(String value) {
		if (CONSISTENT.equals(value))
			return ID_CONSISTENT.getCachedImage();
		if (CONSISTENT_REDDOT.equals(value))
			return ID_CONSISTENT_REDDOT.getCachedImage();
		if (INCONSISTENT_REDDOT.equals(value))
			return ID_INCONSISTENT_REDDOT.getCachedImage();
		if (INCONSISTENT.equals(value))
			return ID_INCONSISTENT.getCachedImage();

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
		if (d.instanceOf(PromiseDrop.class)) {
			final boolean reddot = d.proofUsesRedDot();
			final String value;
			if (d.provedConsistent())
				value = reddot ? CONSISTENT_REDDOT : CONSISTENT;
			else
				value = reddot ? INCONSISTENT_REDDOT : INCONSISTENT;
			return value;
		}
		return null;
	}
}
