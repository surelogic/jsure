package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IProofDrop;

import edu.cmu.cs.fluid.java.ISrcRef;

public final class FilterProject extends Filter {

	public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
		public Filter construct(Selection selection, Filter previous) {
			return new FilterProject(selection, previous, getFilterLabel());
		}

		public String getFilterLabel() {
			return "Project";
		}

		@Override
		public Image getFilterImage() {
			return SLImages.getImage(CommonImages.IMG_PROJECT);
		}
	};

	private FilterProject(Selection selection, Filter previous,
			String filterLabel) {
		super(selection, previous, filterLabel);
	}

	@Override
	public ISelectionFilterFactory getFactory() {
		return FACTORY;
	}

	@Override
	public Image getImageFor(String value) {
		return SLImages.getImage(CommonImages.IMG_PROJECT);
	}

	@Override
	protected void refreshCounts(List<IProofDrop> incomingResults) {
		f_counts.clear();
		int runningTotal = 0;
		for (IProofDrop d : incomingResults) {
			final ISrcRef sr = d.getSrcRef();
			if (sr != null) {
				final String value = getProject(sr);
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
		}
		f_countTotal = runningTotal;
	}

	@Override
	protected void refreshPorousDrops(List<IProofDrop> incomingResults) {
		f_porousDrops.clear();
		for (IProofDrop d : incomingResults) {
			final ISrcRef sr = d.getSrcRef();
			if (sr != null) {
				final String value = getProject(sr);
				if (value != null) {
					if (f_porousValues.contains(value))
						f_porousDrops.add(d);
				}
			}
		}
	}

	private String getProject(ISrcRef sr) {
		String result = sr.getProject();
		if (result != null) {
			if (result.contains("JRE_CONTAINER")) {
				result = "Java Standard Library";
			}
		}
		return result;
	}
}
