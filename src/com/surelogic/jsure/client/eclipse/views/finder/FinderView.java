package com.surelogic.jsure.client.eclipse.views.finder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.CascadingList;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.RadioArrowMenu;
import com.surelogic.common.ui.SLImages;

public final class FinderView extends ViewPart {

	@Override
	public void createPartControl(Composite parent) {

		final CascadingList finder = new CascadingList(parent, SWT.None);
		for (int i = 0; i < 10; i++) {

			RadioArrowMenu menu = new RadioArrowMenu(finder);
			menu.addChoice("Hello 1",
					SLImages.getImage(CommonImages.IMG_ANNOTATION));
			menu.addChoice("Hello 2",
					SLImages.getImage(CommonImages.IMG_ANNOTATION));
			menu.addChoice("Hello 3",
					SLImages.getImage(CommonImages.IMG_ANNOTATION));
			menu.addChoice("Hello 4",
					SLImages.getImage(CommonImages.IMG_ANNOTATION));
		}
		finder.pack();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}
