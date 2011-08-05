package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.IProofDropInfo;

public final class FilterJavaClass extends Filter {

	public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
		public Filter construct(Selection selection, Filter previous) {
			return new FilterJavaClass(selection, previous, getFilterLabel());
		}

		public String getFilterLabel() {
			return "Java Class";
		}

		@Override
		public Image getFilterImage() {
			return SLImages.getImage(CommonImages.IMG_CLASS);
		}
	};

	private FilterJavaClass(Selection selection, Filter previous,
			String filterLabel) {
		super(selection, previous, filterLabel);
	}

	@Override
	public ISelectionFilterFactory getFactory() {
		return FACTORY;
	}

	@Override
	public Image getImageFor(String value) {
		return SLImages.getImage(CommonImages.IMG_CLASS);
	}

	@Override
	protected void refreshCounts(List<IProofDropInfo> incomingResults) {
		f_counts.clear();
		int runningTotal = 0;
		for (IProofDropInfo d : incomingResults) {
			final ISrcRef sr = d.getSrcRef();
			if (sr != null) {
				final String value = sr.getCUName();
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
	protected void refreshPorousDrops(List<IProofDropInfo> incomingResults) {
		f_porousDrops.clear();
		for (IProofDropInfo d : incomingResults) {
			final ISrcRef sr = d.getSrcRef();
			if (sr != null) {
				final String value = sr.getCUName();
				if (value != null) {
					if (f_porousValues.contains(value))
						f_porousDrops.add(d);
				}
			}
		}
	}
}
