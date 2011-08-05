package com.surelogic.jsure.client.eclipse.views.finder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.CascadingList;
import com.surelogic.common.ui.RadioArrowMenu;
import com.surelogic.common.ui.SLImages;

public final class FinderView extends ViewPart {

	private FinderMediator f_mediator = null;

	@Override
	public void createPartControl(Composite parent) {

		final CascadingList finder = new CascadingList(parent, SWT.None);

		finder.addScrolledColumn(new CascadingList.IColumn() {
			@Override
			public Composite createContents(Composite panel) {
				RadioArrowMenu menu = new RadioArrowMenu(panel);
				menu.addChoice("Analysis Results",
						SLImages.getImage(CommonImages.IMG_ANALYSIS_RESULT));
				menu.addChoice("Annotation",
						SLImages.getImage(CommonImages.IMG_ANNOTATION));
				menu.addChoice("Java Class",
						SLImages.getImage(CommonImages.IMG_CLASS));
				menu.addChoice("Java Package",
						SLImages.getImage(CommonImages.IMG_PACKAGE));
				menu.addChoice("Project",
						SLImages.getImage(CommonImages.IMG_PROJECT));
				menu.addChoice("Verification Results",
						SLImages.getImage(CommonImages.IMG_VERIFICATION_RESULT));
				return menu.getPanel();
			}
		}, false);

		f_mediator = new FinderMediator(finder);
		f_mediator.init();
	}

	@Override
	public void dispose() {
		try {
			if (f_mediator != null) {
				f_mediator.dispose();
				f_mediator = null;
			}
		} finally {
			super.dispose();
		}
	}

	@Override
	public void setFocus() {
	}

}
