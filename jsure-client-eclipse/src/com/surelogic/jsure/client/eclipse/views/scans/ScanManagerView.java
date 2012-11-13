package com.surelogic.jsure.client.eclipse.views.scans;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;
import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class ScanManagerView extends ViewPart implements
		JSureDataDirHub.ContentsChangeListener,
		JSureDataDirHub.CurrentScanChangeListener {

	private ScanManagerMediator f_mediator = null;

	@Override
	public void createPartControl(Composite parent) {
		final CheckboxTableViewer table = CheckboxTableViewer.newCheckList(
				parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION
						| SWT.MULTI);
		f_mediator = new ScanManagerMediator(table);
		f_mediator.init();

		final IActionBars actionBars = getViewSite().getActionBars();

		final Action refreshAction = f_mediator.getRefreshAction();
		refreshAction.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_REFRESH));
		refreshAction.setText(I18N.msg("jsure.scan.view.text.refresh"));
		refreshAction.setToolTipText(I18N
				.msg("jsure.scan.view.tooltip.refresh"));
		actionBars.getToolBarManager().add(refreshAction);
		actionBars.getMenuManager().add(refreshAction);
		actionBars.getToolBarManager().add(new Separator());
		actionBars.getMenuManager().add(new Separator());

		final Action setAsCurrentAction = f_mediator.getSetAsCurrentAction();
		setAsCurrentAction.setText(I18N.msg("jsure.scan.view.text.setCurrent"));
		setAsCurrentAction.setToolTipText(I18N
				.msg("jsure.scan.view.tooltip.setCurrent"));
		setAsCurrentAction.setEnabled(false);

		final Action rescanAction = f_mediator.getRescanAction();
		rescanAction.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_JSURE_RE_VERIFY));
		rescanAction.setText(I18N.msg("jsure.scan.view.text.rescan"));
		rescanAction.setToolTipText(I18N
				.msg("jsure.scan.view.tooltip.rescan"));
		rescanAction.setEnabled(false);
		actionBars.getToolBarManager().add(rescanAction);
		actionBars.getMenuManager().add(rescanAction);
		
		final Action deleteScanAction = f_mediator.getDeleteScanAction();
		deleteScanAction.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_RED_X));
		deleteScanAction.setText(I18N.msg("jsure.scan.view.text.delete"));
		deleteScanAction.setToolTipText(I18N
				.msg("jsure.scan.view.tooltip.delete"));
		deleteScanAction.setEnabled(false);
		actionBars.getToolBarManager().add(deleteScanAction);
		actionBars.getMenuManager().add(deleteScanAction);

		/**
		 * Add a context menu to the table viewer.
		 */
		final MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				manager.add(setAsCurrentAction);
				manager.add(new Separator());
				manager.add(rescanAction);				
				manager.add(deleteScanAction);
			}
		});
		final Menu menu = menuMgr.createContextMenu(table.getControl());
		table.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, table);

		JSureDataDirHub.getInstance().addContentsChangeListener(this);
		JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
	}

	@Override
	public void dispose() {
		try {
			JSureDataDirHub.getInstance().removeContentsChangeListener(this);
			JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);

			if (f_mediator != null)
				f_mediator.dispose();
			f_mediator = null;
		} finally {
			super.dispose();
		}
	}

	@Override
	public void setFocus() {
		final ScanManagerMediator mediator = f_mediator;
		if (mediator != null)
			mediator.setFocus();
	}

	@Override
	public void scanContentsChanged(JSureDataDir dataDir) {
		notifyMediatorInSwtThread();
	}

	@Override
	public void currentScanChanged(JSureScan scan) {
		notifyMediatorInSwtThread();

	}

	private void notifyMediatorInSwtThread() {
		final ScanManagerMediator mediator = f_mediator;
		if (mediator != null)
			mediator.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					mediator.refreshScanContents();
				}
			});
	}
}
