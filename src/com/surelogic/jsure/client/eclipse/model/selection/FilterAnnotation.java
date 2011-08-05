package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

import edu.cmu.cs.fluid.sea.IProofDropInfo;

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
	protected void refreshCountsAndPorousDrops(
			List<IProofDropInfo> incomingResults) {
		// TODO Auto-generated method stub

	}
}
