package com.surelogic.jsure.client.eclipse.views.finder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.CascadingList;
import com.surelogic.common.ui.RadioArrowMenu;
import com.surelogic.common.ui.SLImages;

public final class FinderView extends ViewPart {

	private FinderMediator f_mediator = null;

	@Override
	public void createPartControl(Composite parent) {
		GridLayout gl = new GridLayout();
		gl.horizontalSpacing = gl.verticalSpacing = gl.marginHeight = gl.marginWidth = 0;
		parent.setLayout(gl);

		final Composite breadcrumbsPanel = new Composite(parent, SWT.NONE);
		breadcrumbsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				false));
		gl = new GridLayout();
		gl.numColumns = 2;
		gl.horizontalSpacing = gl.verticalSpacing = gl.marginHeight = gl.marginWidth = 0;
		breadcrumbsPanel.setLayout(gl);

		final Link breadcrumbs = new Link(breadcrumbsPanel, SWT.NORMAL);
		breadcrumbs.setText("");
		breadcrumbs
				.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		final ToolBar clearSelectionBar = new ToolBar(breadcrumbsPanel,
				SWT.HORIZONTAL | SWT.FLAT);
		clearSelectionBar.setLayoutData(new GridData(SWT.DEFAULT, SWT.CENTER,
				false, false));
		final ToolItem clearSelectionItem = new ToolItem(clearSelectionBar,
				SWT.PUSH);
		clearSelectionItem.setImage(SLImages.getImage(CommonImages.IMG_GRAY_X));
		clearSelectionItem.setToolTipText("Clear Current Search");

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
		finder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		f_mediator = new FinderMediator(parent, finder, breadcrumbs,
				clearSelectionItem);
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
		if (f_mediator != null)
			f_mediator.setFocus();
	}

}
