package com.surelogic.jsure.client.eclipse.model.selection;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

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
		// TODO Auto-generated method stub

	}

	@Override
	protected void refreshPorousDrops(List<IProofDropInfo> incomingResults) {
		// TODO Auto-generated method stub

	}
}
