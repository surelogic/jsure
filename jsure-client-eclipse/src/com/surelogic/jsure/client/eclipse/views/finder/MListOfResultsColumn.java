package com.surelogic.jsure.client.eclipse.views.finder;

import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.CommonImages;
import com.surelogic.common.jsure.xml.CoE_Constants;
import com.surelogic.common.ui.CascadingList;
import com.surelogic.common.ui.CascadingList.IColumn;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.TableUtility;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.jsure.client.eclipse.editors.EditorUtil;
import com.surelogic.jsure.client.eclipse.model.selection.ISelectionObserver;
import com.surelogic.jsure.client.eclipse.model.selection.Selection;
import com.surelogic.jsure.client.eclipse.views.results.DropInfoUtility;
import com.surelogic.jsure.client.eclipse.views.results.ResultsImageDescriptor;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;

public final class MListOfResultsColumn extends MColumn implements
		ISelectionObserver {

	/**
	 * The table used to display the results.
	 */
	private Table f_table = null;

	MListOfResultsColumn(final CascadingList cascadingList,
			final Selection selection, final MColumn previousColumn) {
		super(cascadingList, selection, previousColumn);
	}

	@Override
	void init() {
		getSelection().setShowingResults(true);
		getSelection().addObserver(this);
		changed();
	}

	@Override
	void initOfNextColumnComplete() {
		final UIJob job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {
				MListOfResultsColumn.super.initOfNextColumnComplete();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	@Override
	void dispose() {
		super.dispose();
		getSelection().setShowingResults(false);
		getSelection().removeObserver(this);

		final int column = getColumnIndex();
		if (column != -1) {
			getCascadingList().emptyFrom(column);
		}
	}

	@Override
	int getColumnIndex() {
		if (f_table.isDisposed()) {
			return -1;
		} else {
			return getCascadingList().getColumnIndexOf(f_table);
		}
	}

	@Override
	public void forceFocus() {
		if (f_table != null) {
			f_table.forceFocus();
			getCascadingList().show(index);
		}
	}

	public void selectionChanged(final Selection selecton) {
		changed();
	}

	private void changed() {
		final UIJob job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {
				if (f_table != null && f_table.isDisposed()) {
					getSelection().removeObserver(MListOfResultsColumn.this);
				} else {
					try {
						refreshDisplay();
					} finally {
						initOfNextColumnComplete();
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private final KeyListener f_keyListener = new KeyListener() {
		public void keyPressed(final KeyEvent e) {
			if (e.character == 0x01 && f_table != null) {
				f_table.selectAll();
				e.doit = false; // Handled
			}
		}

		public void keyReleased(final KeyEvent e) {
			// Nothing to do
		}
	};

	private final Listener f_rowSelection = new Listener() {
		public void handleEvent(final Event event) {
			final IProofDrop info = getSelectedItem();
			if (info != null) {
				DropInfoUtility.showDrop(info);
			}
		}
	};

	private final Listener f_doubleClick = new Listener() {
		public void handleEvent(final Event event) {
			final IProofDrop info = getSelectedItem();
			if (info != null) {
				/*
				 * Highlight this line in the editor if possible.
				 */
				final ISrcRef src = info.getSrcRef();
				if (src != null) {
					highlightLineInJavaEditor(src);
				}
			}
		}
	};

	private final IColumn f_iColumn = new IColumn() {
		public Composite createContents(final Composite panel) {
			f_table = new Table(panel, SWT.FULL_SELECTION);
			f_table.setLinesVisible(true);
			f_table.addListener(SWT.MouseDoubleClick, f_doubleClick);
			f_table.addListener(SWT.Selection, f_rowSelection);
			f_table.addKeyListener(f_keyListener);
			f_table.setItemCount(0);
			// TODO createTableColumns();

			f_table.addListener(SWT.Traverse, new Listener() {
				public void handleEvent(final Event e) {
					switch (e.detail) {
					case SWT.TRAVERSE_ESCAPE:
						setCustomTabTraversal(e);
						if (getPreviousColumn() instanceof MRadioMenuColumn) {
							final MRadioMenuColumn column = (MRadioMenuColumn) getPreviousColumn();
							column.escape(null);
							/*
							 * column.clearSelection(); column.emptyAfter(); //
							 * e.g. eliminate myself column.forceFocus();
							 */
						}
						break;
					case SWT.TRAVERSE_TAB_NEXT:
						// Cycle back to the first columns
						setCustomTabTraversal(e);
						getFirstColumn().forceFocus();
						break;
					case SWT.TRAVERSE_TAB_PREVIOUS:
						setCustomTabTraversal(e);
						getPreviousColumn().forceFocus();
						break;
					case SWT.TRAVERSE_RETURN:
						setCustomTabTraversal(e);
						f_rowSelection.handleEvent(null);
						break;
					}
				}
			});

			final Menu menu = new Menu(f_table.getShell(), SWT.POP_UP);
			f_table.setMenu(menu);

			// TODO setupMenu(menu);

			updateTableContents();
			return f_table;
		}
	};

	private void updateTableContents() {
		if (f_table.isDisposed()) {
			return;
		}

		f_table.setRedraw(false);

		final List<IProofDrop> rows = getSelection().getPorousDrops();

		final IProofDrop selected = getSelectedItem();
		f_table.removeAll();

		IProofDrop lastSelected = null;
		int i = 0;
		for (final IProofDrop data : rows) {
			final boolean rowSelected = data == selected;
			final TableItem item = new TableItem(f_table, SWT.NONE);
			setTableItemInfo(item, data);
			if (rowSelected) {
				selectItem(i, data);
				lastSelected = data;
			}
		}
		TableUtility.packColumns(f_table);
		f_table.setRedraw(true);
		/*
		 * Fix to bug 1115 (an XP specific problem) where the table was redrawn
		 * with lines through the row text. Aaron Silinskas found that a second
		 * call seemed to fix the problem (with a bit of flicker).
		 */
		if (SystemUtils.IS_OS_WINDOWS_XP) {
			f_table.setRedraw(true);
		}
		if (lastSelected != null) {
			final UIJob job = new SLUIJob() {
				@Override
				public IStatus runInUIThread(final IProgressMonitor monitor) {
					f_table.showSelection();
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}

	private void selectItem(int i, IProofDrop data) {
		if (i != -1) {
			f_table.select(i);
		}
	}

	private IProofDrop getSelectedItem() {
		final TableItem[] selected = f_table.getSelection();
		if (selected.length > 0) {
			final Object data = selected[0].getData();
			if (data instanceof IProofDrop)
				return (IProofDrop) data;
		}
		return null;
	}

	private void refreshDisplay() {
		final UIJob job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(final IProgressMonitor monitor) {
				if (f_table == null) {
					final int addAfterColumn = getPreviousColumn()
							.getColumnIndex();
					// create the display table
					getCascadingList().addColumnAfter(f_iColumn,
							addAfterColumn, false);
				} else {
					// update the table's contents
					updateTableContents();
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void setTableItemInfo(TableItem item, IProofDrop data) {
		int flags = 0;
		final ImageDescriptor img;
		if (data.instanceOf(ResultDrop.class)) {
			IResultDrop rd = (IResultDrop) data;
			if (rd.isVouched()) {
				img = SLImages.getImageDescriptor(CommonImages.IMG_PLUS_VOUCH);
			} else if (rd.isConsistent()) {
				img = SLImages.getImageDescriptor(CommonImages.IMG_PLUS);
			} else if (rd.isTimeout()) {
				img = SLImages.getImageDescriptor(CommonImages.IMG_TIMEOUT_X);
			} else {
				img = SLImages.getImageDescriptor(CommonImages.IMG_RED_X);
			}
		} else if (data.instanceOf(PromiseDrop.class)) {
			IPromiseDrop pd = (IPromiseDrop) data;
			img = SLImages.getImageDescriptor(CommonImages.IMG_ANNOTATION);
			if (data.provedConsistent())
				flags |= CoE_Constants.CONSISTENT;
			else
				flags |= CoE_Constants.INCONSISTENT;
			if (data.proofUsesRedDot())
				flags |= CoE_Constants.REDDOT;
			if (pd.isAssumed())
				flags |= CoE_Constants.ASSUME;
			if (pd.isVirtual())
				flags |= CoE_Constants.VIRTUAL;
			if (!pd.isCheckedByAnalysis())
				flags |= CoE_Constants.TRUSTED;
		} else {
			img = SLImages.getImageDescriptor(CommonImages.IMG_FOLDER);
		}
		ResultsImageDescriptor rid = new ResultsImageDescriptor(img, flags,
				new Point(22, 16));

		item.setText(data.getMessage());
		item.setImage(rid.getCachedImage());
		item.setData(data);
	}

	@Override
	void selectAll() {
		if (f_table.isFocusControl()) {
			f_table.selectAll();
		} else {
			super.selectAll();
		}
	}

	/**
	 * Open and highlight a line within the Java editor, if possible. Otherwise,
	 * try to open as a text file
	 * 
	 * @param srcRef
	 *            the source reference to highlight
	 */
	protected void highlightLineInJavaEditor(ISrcRef srcRef) {
		EditorUtil.highlightLineInJavaEditor(srcRef);
	}
}
