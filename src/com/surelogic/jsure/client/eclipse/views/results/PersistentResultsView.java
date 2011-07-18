package com.surelogic.jsure.client.eclipse.views.results;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.ui.IMemento;

import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.scans.JSureScanInfo;
import com.surelogic.jsure.core.scans.JSureScansHub;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IDropInfo;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.xml.SeaSnapshot.Info;

public class PersistentResultsView extends ResultsView implements
		JSureScansHub.Listener {
	private static final String VIEW_STATE = "view.state";

	final File f_viewState;

	public PersistentResultsView() {
		File viewState = null;
		try {
			final File jsureData = JSurePreferencesUtility
					.getJSureDataDirectory();
			if (jsureData != null) {
				viewState = new File(jsureData, VIEW_STATE + ".xml");
			} else {
				viewState = File.createTempFile(VIEW_STATE, ".xml");
			}
			// System.out.println("Using location: "+location);
		} catch (IOException e) {
			// Nothing to do
		}
		f_viewState = viewState;
	}

	@Override
	public void scansChanged(JSureScansHub.ScanStatus status) {
		seaChanged();
	}

	@Override
	public void analysisStarting() {
		// Ignore this, so we can continue to look at the old results
	}

	@Override
	public void seaChanged() {
		// load it up
		finishCreatePartControl();
	}

	@Override
	protected void finishCreatePartControl() {
		final JSureScanInfo scan = JSureScansHub.getInstance()
				.getCurrentScanInfo();
		if (scan != null) {
			// TODO restore viewer state?
			final long start = System.currentTimeMillis();
			f_provider.buildModelOfDropSea_internal();
			final long end = System.currentTimeMillis();
			setViewerVisibility(true);
			System.out.println("Loaded snapshot for " + this + ": "
					+ (end - start) + " ms");

			// Running too early?
			if (f_viewState != null && f_viewState.exists()) {
				f_viewerbook.getDisplay().asyncExec(new Runnable() {
					public void run() {
						try {
							// viewer.refresh();
							loadViewState(f_viewState);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		try {
			saveViewState(f_viewState);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private GenericResultsViewContentProvider<Info, Content> f_provider;

	@Override
	protected IResultsViewContentProvider makeContentProvider() {
		return new GenericResultsViewContentProvider<Info, Content>(
				Sea.getDefault()) {
			{
				f_provider = this;
			}

			@Override
			public IResultsViewContentProvider buildModelOfDropSea() {
				try {
					saveViewState(f_viewState);
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					return super.buildModelOfDropSea_internal();
				} finally {
					f_viewerbook.getDisplay().asyncExec(new Runnable() {
						public void run() {
							restoreViewState();
						}
					});
				}
			}

			@Override
			protected boolean dropsExist(Class<? extends Drop> type) {
				final JSureScanInfo scan = JSureScansHub.getInstance()
						.getCurrentScanInfo();
				if (scan != null) {
					return scan.dropsExist(type);
				}
				return false;
			}

			@Override
			protected <R extends IDropInfo> Collection<R> getDropsOfType(
					Class<? extends Drop> type, Class<R> rType) {
				final JSureScanInfo scan = JSureScansHub.getInstance()
						.getCurrentScanInfo();
				if (scan != null) {
					return scan.getDropsOfType(type);
				}
				return Collections.emptyList();
			}

			@Override
			protected Content makeContent(String msg) {
				return new Content(msg, Collections.<Content> emptyList(), null);
			}

			@Override
			protected Content makeContent(String msg,
					Collection<Content> contentRoot) {
				return new Content(msg, contentRoot, null);
			}

			@Override
			protected Content makeContent(String msg, Info drop) {
				return new Content(msg, Collections.<Content> emptyList(), drop);
			}

			@Override
			protected Content makeContent(String msg, ISrcRef ref) {
				return new Content(msg, ref);
			}
		};
	}

	static class Content extends AbstractContent<Info, Content> {
		Content(String msg, Collection<Content> content, Info drop) {
			super(msg, content, drop);
		}

		Content(String msg, ISrcRef ref) {
			super(msg, ref);
		}
	}
}
