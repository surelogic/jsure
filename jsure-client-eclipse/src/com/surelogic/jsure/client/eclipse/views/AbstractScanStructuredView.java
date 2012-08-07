package com.surelogic.jsure.client.eclipse.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.surelogic.common.i18n.I18N;
import com.surelogic.jsure.client.eclipse.refactor.ProposedPromisesRefactoringAction;

import edu.cmu.cs.fluid.sea.IDropInfo;
import edu.cmu.cs.fluid.sea.IProposedPromiseDropInfo;

/**
 * Uses a StructuredViewer
 */
public abstract class AbstractScanStructuredView<T> extends
		AbstractJSureScanView {

	private final int f_extraStyle;
	private StructuredViewer[] f_viewers;
	private final Class<T> f_class;

	protected final Action f_annotate = new ProposedPromisesRefactoringAction() {
		@Override
		protected List<? extends IProposedPromiseDropInfo> getProposedDrops() {
			return getSelectedProposals();
		}

		@Override
		protected String getDialogTitle() {
			return I18N.msg("jsure.eclipse.proposed.promise.edit");
		}
	};

	/**
	 * Gets the list of selected proposed promises. Must never return
	 * {@code null}.
	 * 
	 * @return the non-{@code null} list of proposed promises.
	 */
	protected List<? extends IProposedPromiseDropInfo> getSelectedProposals() {
		return Collections.emptyList();
	}

	protected AbstractScanStructuredView(Class<T> c) {
		this(SWT.NONE, c);
	}

	protected AbstractScanStructuredView(int style, Class<T> c) {
		f_extraStyle = style;
		f_class = c;
	}

	@Override
	protected StructuredViewer getViewer() {
		return f_viewers[getViewIndex()];
	}

	protected int getViewIndex() {
		return 0;
	}

	@Override
	protected Control buildViewer(Composite parent) {
		f_viewers = newViewers(parent, f_extraStyle);
		// To make sure only one is showing
		for (StructuredViewer v : f_viewers) {
			setupViewer(v);
			v.getControl().setVisible(false);
		}

		if (f_viewers.length == 1) {
			return f_viewers[0].getControl();
		}
		return null; // need to call getViewer() to get the control
	}

	protected abstract StructuredViewer[] newViewers(Composite parent,
			int extraStyle);

	@Override
	protected void setupViewer(StructuredViewer viewer) {
		super.setupViewer(viewer);

		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				final ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection) {
					handleDoubleClick((IStructuredSelection) selection);
				}
			}
		});
	}

	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
	}

	/********************* Methods to handle selections ******************************/

	protected final List<? extends T> getSelectedRows() {
		final IStructuredSelection selection = (IStructuredSelection) getViewer()
				.getSelection();
		final List<T> result = new ArrayList<T>();
		for (final Object element : selection.toList()) {
			if (f_class.isInstance(element)) {
				result.add(f_class.cast(element));
			}
		}
		return result;
	}

	private final void handleDoubleClick(final IStructuredSelection selection) {
		final Object d = selection.getFirstElement();
		if (d instanceof IDropInfo) {
			IDropInfo di = (IDropInfo) d;
			highlightLineInJavaEditor(di.getSrcRef());
		} else {
			if (d != null)
				handleDoubleClick(d);
		}
	}

	protected void handleDoubleClick(Object d) {
		// default is to do nothing -- subtypes can override.
	}

	protected final Action makeCopyAction(String label, String tooltip) {
		Action a = new Action() {
			@Override
			public void run() {
				final Clipboard clipboard = new Clipboard(getSite().getShell()
						.getDisplay());
				try {
					clipboard.setContents(new Object[] { getSelectedText() },
							new Transfer[] { TextTransfer.getInstance() });
				} finally {
					clipboard.dispose();
				}
			}
		};
		a.setText(label);
		a.setToolTipText(tooltip);
		return a;
	}

	protected final String getSelectedText() {
		IStructuredSelection selection = (IStructuredSelection) getViewer()
				.getSelection();
		StringBuilder sb = new StringBuilder();
		for (Object elt : selection.toList()) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			appendText(sb, elt);
		}
		return sb.toString();
	}

	protected abstract void appendText(StringBuilder sb, Object elt);
}
