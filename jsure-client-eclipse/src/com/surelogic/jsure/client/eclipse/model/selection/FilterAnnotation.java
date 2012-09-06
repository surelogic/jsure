package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

import edu.cmu.cs.fluid.sea.IProofDrop;
import edu.cmu.cs.fluid.sea.Sea;

public final class FilterAnnotation extends Filter {

	public static final ISelectionFilterFactory FACTORY = new AbstractFilterFactory() {
		public Filter construct(Selection selection, Filter previous) {
			return new FilterAnnotation(selection, previous, getFilterLabel());
		}

		public String getFilterLabel() {
			return "Annotation";
		}

		@Override
		public Image getFilterImage() {
			return SLImages.getImage(CommonImages.IMG_ANNOTATION);
		}
	};

	private FilterAnnotation(Selection selection, Filter previous,
			String filterLabel) {
		super(selection, previous, filterLabel);
	}

	@Override
	public ISelectionFilterFactory getFactory() {
		return FACTORY;
	}

	@Override
	public Image getImageFor(String value) {
		return SLImages.getImage(CommonImages.IMG_ANNOTATION);
	}

	@Override
	protected void refreshCounts(List<IProofDrop> incomingResults) {
		f_counts.clear();
		int runningTotal = 0;
		for (IProofDrop d : incomingResults) {
			final String value = Sea.getAnnotationName(d);
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
	protected void refreshPorousDrops(List<IProofDrop> incomingResults) {
		f_porousDrops.clear();
		for (IProofDrop d : incomingResults) {
			final String value = Sea.getAnnotationName(d);
			if (value != null) {
				if (f_porousValues.contains(value))
					f_porousDrops.add(d);
			}
		}
	}
}
