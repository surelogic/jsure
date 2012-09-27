package com.surelogic.jsure.client.eclipse.views.finder;

import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.CascadingList;
import com.surelogic.common.ui.ISearchBoxObserver;
import com.surelogic.common.ui.SearchBox;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.jsure.client.eclipse.model.selection.Filter;
import com.surelogic.jsure.client.eclipse.model.selection.IFilterObserver;
import com.surelogic.jsure.client.eclipse.model.selection.Selection;

public final class MFilterSelectionColumn extends MColumn implements
		IFilterObserver {

	private final Filter f_filter;

	Filter getFilter() {
		return f_filter;
	}

	private Composite f_panel = null;
	private Table f_reportContents = null;
	private Label f_totalCount = null;
	private Composite f_bottomSection = null;
	private SearchBox f_searchBox = null;
	private Label f_porousCount = null;
	private Group f_reportGroup = null;
	private TableColumn f_valueColumn = null;
	private TableColumn f_graphColumn = null;
	private Color f_barColorDark = null;
	private Color f_barColorLight = null;

	private Menu f_menu = null;
	private MenuItem f_selectAllMenuItem = null;
	private MenuItem f_deselectAllMenuItem = null;
	private MenuItem f_sortByCountMenuItem = null;
	private List<String> valueList;
	private String f_mouseOverLine = "";

	private boolean f_sortByCount = false;

	private static final int GRAPH_WIDTH = 75;

	MFilterSelectionColumn(CascadingList cascadingList, Selection selection,
			MColumn previousColumn, Filter filter) {
		super(cascadingList, selection, previousColumn);
		assert filter != null;
		f_filter = filter;
	}

	@Override
	void init() {
		CascadingList.IColumn c = new CascadingList.IColumn() {
			public Composite createContents(Composite panel) {
				f_panel = new Composite(panel, SWT.NONE);
				f_panel.setLayout(new FillLayout());

				f_reportGroup = new Group(f_panel, SWT.NONE);
				f_reportGroup.setText(f_filter.getFactory().getFilterLabel());
				GridLayout gridLayout = new GridLayout();
				gridLayout.verticalSpacing = gridLayout.horizontalSpacing = 3;
				gridLayout.marginHeight = gridLayout.marginWidth = 0;
				f_reportGroup.setLayout(gridLayout);

				f_totalCount = new Label(f_reportGroup, SWT.RIGHT);
				f_totalCount.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT,
						true, false));

				f_reportContents = new Table(f_reportGroup, SWT.VIRTUAL
						| SWT.CHECK | SWT.FULL_SELECTION | SWT.V_SCROLL);
				GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
				f_reportContents.setLayoutData(data);

				f_valueColumn = new TableColumn(f_reportContents, SWT.BORDER);
				f_valueColumn.setText("Value");
				f_valueColumn.setToolTipText("");
				f_graphColumn = new TableColumn(f_reportContents, SWT.BORDER);
				f_graphColumn.setText("#");
				f_graphColumn.setWidth(75);
				f_graphColumn
						.setToolTipText("# of applicable results with the given value");
				f_reportContents.setBackground(f_reportGroup.getBackground());
				f_reportContents.addFocusListener(new FocusListener() {
					public void focusGained(FocusEvent e) {
						if (valueList == null || valueList.isEmpty()) {
							Color focused = f_reportContents.getDisplay()
									.getSystemColor(SWT.COLOR_LIST_SELECTION);
							Color focusedText = f_reportContents.getDisplay()
									.getSystemColor(
											SWT.COLOR_LIST_SELECTION_TEXT);
							f_totalCount.setBackground(focused);
							f_totalCount.setForeground(focusedText);
						}
					}

					public void focusLost(FocusEvent e) {
						f_totalCount.setBackground(null);
						f_totalCount.setForeground(null);
					}
				});
				f_reportContents.addKeyListener(new KeyListener() {
					public void keyPressed(KeyEvent e) {
						MColumn column = null;
						if (e.keyCode == SWT.ARROW_LEFT) {
							column = getPreviousColumn();
						} else if (e.keyCode == SWT.ARROW_RIGHT) {
							column = getNextColumn();
						} else {
							handleChar(e);
							return;
						}
						if (column != null) {
							focusOnColumn(column);
							e.doit = false; // Handled
						}
					}

					private void handleChar(KeyEvent e) {
						/*
						 * Handle space and return by ourselves (e.g. override
						 * default OS behavior)
						 */
						if (e.character == ' ' || e.character == SWT.CR) {
							// Called after the table toggles the item
							int selected = f_reportContents.getSelectionIndex();
							if (selected >= 0) {
								TableItem item = f_reportContents
										.getItem(selected);
								item.setChecked(!item.getChecked());
								selectionChanged(item);
								e.doit = false;
							}
							return;
						}
					}

					public void keyReleased(KeyEvent e) {
						// Nothing to do
					}
				});
				f_reportContents.addListener(SWT.Traverse, new Listener() {
					public void handleEvent(Event e) {
						switch (e.detail) {
						case SWT.TRAVERSE_TAB_NEXT:
							setCustomTabTraversal(e);
							focusOnColumn(getNextColumn());
							break;
						case SWT.TRAVERSE_TAB_PREVIOUS:
							setCustomTabTraversal(e);
							focusOnColumn(getPreviousColumn());
							break;
						case SWT.TRAVERSE_ESCAPE:
							setCustomTabTraversal(e);
							MColumn column = getPreviousColumn();
							if (column instanceof MRadioMenuColumn) {
								MRadioMenuColumn radio = (MRadioMenuColumn) column;
								radio.escape(null);
							}
							break;
						}
					}
				});
				f_reportContents.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (e.item instanceof TableItem) {
							final TableItem item = (TableItem) e.item;
							if ((e.detail & SWT.CHECK) != 0) {
								/*
								 * A check/uncheck event.
								 */
								selectionChanged(item);
							}
						}

					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						// Nothing to do for a double-click
					}
				});
				f_reportContents.addListener(SWT.MouseMove, new Listener() {
					public void handleEvent(Event e) {
						Point p = new Point(e.x, e.y);
						TableItem item = f_reportContents.getItem(p);
						if (item != null) {
							f_mouseOverLine = (String) item.getData();
							f_reportContents.redraw();
						}
					}
				});
				f_reportContents.addListener(SWT.MouseExit, new Listener() {
					public void handleEvent(Event e) {
						f_mouseOverLine = "";
						f_reportContents.redraw();
					}
				});

				f_barColorDark = new Color(f_reportContents.getDisplay(), 255,
						102, 51);
				f_barColorLight = new Color(f_reportContents.getDisplay(), 238,
						216, 198);
				/**
				 * See http://publicobject.com/glazedlists/documentation/
				 * swt_virtual_tables.html
				 */
				f_reportContents.addListener(SWT.SetData, new Listener() {
					// Only called the first time the TableItem is shown
					// Intended to initialize the item
					public void handleEvent(Event event) {
						final TableItem item = (TableItem) event.item;
						final int index = event.index;
						updateData(item, index);
					}
				});

				/**
				 * Note that the next three listeners implement the bar graph
				 * See http://www.eclipse.org/articles/article.php?file=Article-
				 * CustomDrawingTableAndTreeItems/index.html
				 */
				f_reportContents.addListener(SWT.MeasureItem, new Listener() {
					/**
					 * The first custom draw event that is sent. This event
					 * gives a client the opportunity to specify the width
					 * and/or height of a cell's content
					 */
					public void handleEvent(Event event) {
						if (event.index == 1) {
							event.width = GRAPH_WIDTH;
						}
					}
				});
				f_reportContents.addListener(SWT.EraseItem, new Listener() {
					/**
					 * Sent just before the background of a cell is about to be
					 * drawn. The background consists of the cell's background
					 * color or the selection background if the item is
					 * selected. This event allows a client to custom draw one
					 * or both of these. Also, this event allows the client to
					 * indicate whether the cell's default foreground should be
					 * drawn following the drawing of the background.
					 */
					public void handleEvent(Event event) {
						if (event.index == 1) {
							/*
							 * Specifies that we will handle all but the focus
							 * rectangle
							 */
							event.detail &= ~(1 << 5); // SWT.HOT;
							event.detail &= ~SWT.SELECTED;
							event.detail &= ~SWT.BACKGROUND;
							event.detail &= ~SWT.FOREGROUND;
						}
					}
				});
				f_reportContents.addListener(SWT.PaintItem, new Listener() {
					/**
					 * Sent for a cell just after its default foreground
					 * contents have been drawn. This event allows a client to
					 * augment the cell, or to completely draw the cell's
					 * content.
					 */
					public void handleEvent(Event event) {
						if (event.index == 1) {
							/*
							 * We're drawing everything
							 */
							TableItem item = (TableItem) event.item;
							String value = (String) item.getData();
							int count = f_filter.getSummaryCountFor(value);
							int percent = computeBarGraphPercent(count);
							Display display = f_reportContents.getDisplay();
							GC gc = event.gc;
							boolean checked = item.getChecked();
							final int width = computeBarGraphWidth(item,
									GRAPH_WIDTH);

							// Save old colors
							Color oldForeground = gc.getForeground();
							Color oldBackground = gc.getBackground();
							// Draw background fill
							final int height = event.height - 2;
							final boolean mouseOverGraph = f_mouseOverLine == null ? value == null
									: f_mouseOverLine.equals(value);
							if (mouseOverGraph) {
								gc.setBackground(display
										.getSystemColor(SWT.COLOR_WHITE));
								gc.fillRectangle(event.x, event.y, GRAPH_WIDTH,
										height);
								gc.setBackground(f_barColorLight);
								gc.fillRectangle(event.x, event.y, width,
										height);
							} else {
								gc.setBackground(f_reportGroup.getBackground());
								gc.fillRectangle(event.x, event.y, GRAPH_WIDTH,
										height);
								gc.setBackground(f_barColorDark);
								gc.fillRectangle(event.x, event.y, width,
										height);
							}

							// Draw bounding rectangle and quartile bars
							Rectangle rect2 = new Rectangle(event.x, event.y,
									GRAPH_WIDTH - 1, height);
							gc.setForeground(display
									.getSystemColor(SWT.COLOR_GRAY));
							gc.drawRectangle(rect2);
							if (percent > 25) {
								int p = (GRAPH_WIDTH - 1) * 25 / 100;
								gc.drawLine(event.x + p, event.y, event.x + p,
										event.y + height);
							}
							if (percent > 50) {
								int p = (GRAPH_WIDTH - 1) * 50 / 100;
								gc.drawLine(event.x + p, event.y, event.x + p,
										event.y + height);
							}
							if (percent > 75) {
								int p = (GRAPH_WIDTH - 1) * 75 / 100;
								gc.drawLine(event.x + p, event.y, event.x + p,
										event.y + height);
							}
							String text = SLUtility.toStringHumanWithCommas(count);
							Point size = gc.textExtent(text);
							int offset = Math.max(0, (height - size.y) / 2);
							int rightJ = GRAPH_WIDTH - 2 - size.x;
							if (mouseOverGraph || checked) {
								gc.setForeground(display
										.getSystemColor(SWT.COLOR_BLACK));
							}
							gc.drawText(text, event.x + rightJ, event.y
									+ offset, true);

							// Restore old colors
							gc.setForeground(oldForeground);
							gc.setBackground(oldBackground);
						}
					}
				});

				f_bottomSection = new Composite(f_reportGroup, SWT.NONE);
				f_bottomSection.setLayoutData(new GridData(SWT.FILL,
						SWT.DEFAULT, true, false));
				GridLayout bottomGrid = new GridLayout();
				bottomGrid.numColumns = 2;
				bottomGrid.marginHeight = bottomGrid.marginWidth = 0;
				f_bottomSection.setLayout(bottomGrid);

				final ISearchBoxObserver observer = new ISearchBoxObserver() {

					public void searchTextChangedTo(String text) {
						f_filter.setFilterExpression(text);
						getSelection().refresh();
					}

					public void searchTextCleared() {
						f_filter.clearFilterExpression();
						getSelection().refresh();
					}
				};
				f_searchBox = new SearchBox(f_bottomSection,
						"filter the list above",
						"Clear the current filter expression", observer);
				if (!f_filter.isFilterExpressionClear()) {
					final String filterExpression = f_filter
							.getFilterExpression();
					f_searchBox.setText(filterExpression);
					getSelection().refresh();
				}
				final GridData searchGridData = new GridData(SWT.FILL,
						SWT.CENTER, true, false);
				// Makes gray-X line up a bit better
				searchGridData.horizontalIndent = 4;
				f_searchBox.getComposite().setLayoutData(searchGridData);

				f_porousCount = new Label(f_bottomSection, SWT.RIGHT);
				final GridData porousCountGridData = new GridData(SWT.FILL,
						SWT.CENTER, false, false);
				// Make a slight space between the search box
				porousCountGridData.horizontalIndent = 4;
				f_porousCount.setLayoutData(porousCountGridData);

				f_menu = new Menu(f_reportGroup.getShell(), SWT.POP_UP);
				f_menu.addListener(SWT.Show, new Listener() {
					public void handleEvent(Event event) {
						final boolean valuesExist = f_filter.hasValues();
						f_selectAllMenuItem.setEnabled(valuesExist);
						f_deselectAllMenuItem.setEnabled(valuesExist);
						f_sortByCountMenuItem.setSelection(f_sortByCount);
					}
				});

				f_selectAllMenuItem = new MenuItem(f_menu, SWT.PUSH);
				f_selectAllMenuItem.setText("Select All");
				f_selectAllMenuItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						selectAllItems();
					}
				});
				f_deselectAllMenuItem = new MenuItem(f_menu, SWT.PUSH);
				f_deselectAllMenuItem.setText("Deselect All");
				f_deselectAllMenuItem.addListener(SWT.Selection,
						new Listener() {
							public void handleEvent(Event event) {
								f_filter.setPorousNone();
								for (TableItem item : f_reportContents
										.getItems()) {
									item.setChecked(false);
								}
							}
						});
				new MenuItem(f_menu, SWT.SEPARATOR);
				f_sortByCountMenuItem = new MenuItem(f_menu, SWT.CHECK);
				f_sortByCountMenuItem.setText("Sort By Result Count");
				f_sortByCountMenuItem.addListener(SWT.Selection,
						new Listener() {
							public void handleEvent(Event event) {
								f_sortByCount = !f_sortByCount;
								updateReport();
							}
						});

				f_reportContents.setMenu(f_menu);
				f_reportGroup.setMenu(f_menu);
				f_totalCount.setMenu(f_menu);
				f_porousCount.setMenu(f_menu);

				updateReport();
				return f_panel;
			}
		};
		getCascadingList().addColumnAfter(c,
				getPreviousColumn().getColumnIndex(), false);
		f_filter.addObserver(this);
		initOfNextColumnComplete();
	}

	int computeBarGraphPercent(int count) {
		int total = f_filter.getResultCountTotal();
		int percent = (int) (((double) count / (double) total) * 100);
		return percent;
	}

	int computeBarGraphWidth(TableItem item, int totalWidth) {
		String value = (String) item.getData();
		int count = f_filter.getSummaryCountFor(value);
		int percent = computeBarGraphPercent(count);
		int width = (totalWidth - 1) * percent / 100;
		if (width < 2 && count > 0)
			width = 2;
		return width;
	}

	@Override
	void dispose() {
		super.dispose();
		f_filter.removeObserver(this);
		final int column = getColumnIndex();
		if (column != -1)
			getCascadingList().emptyFrom(column);

		/*
		 * if (lineFactory != null) { lineFactory.dispose(); }
		 */
	}

	@Override
	int getColumnIndex() {
		if (f_panel.isDisposed())
			return -1;
		else
			return getCascadingList().getColumnIndexOf(f_panel);
	}

	/**
	 * Must be called from the UI thread.
	 */
	private void updateReport() {
		if (f_panel.isDisposed())
			return;

		f_panel.setRedraw(false);

		/*
		 * Fix total count at the top.
		 */
		final int total = f_filter.getResultCountTotal();
		f_totalCount.setText(SLUtility.toStringHumanWithCommas(total)
				+ (total == 1 ? " Result" : " Results"));

		/*
		 * Fix the value lines.
		 */
		final List<String> valueList = f_sortByCount ? f_filter
				.getValuesOrderedBySummaryCount() : f_filter.getAllValues();
		this.valueList = valueList;

		final int currentRows = f_reportContents.getItemCount();
		if (currentRows != valueList.size()) {
			f_reportContents.setItemCount(valueList.size());
			// Update all the items
			TableItem[] items = f_reportContents.getItems();
			int i = 0;
			for (String row : valueList) {
				updateData(items[i], row);
				i++;
			}
		} else {
			TableItem[] items = f_reportContents.getItems();
			int i = 0;
			for (String row : valueList) {
				TableItem item = items[i];

				final boolean itemDiffers = row == null ? item.getData() != null
						: !row.equals(item.getData());
				if (itemDiffers) {
					updateData(item, row);
				}
				i++;
			}
		}

		final int porousCount = f_filter.getResultCountPorous();
		if (f_porousCount != null && !f_porousCount.isDisposed()) {
			if (f_reportContents.getItemCount() > 0) {
				final String porousCountString = SLUtility
						.toStringHumanWithCommas(porousCount);
				f_porousCount.setText(porousCountString);
				String msg = (porousCount == 0 ? "No" : porousCountString)
						+ (porousCount != 1 ? " results" : " result")
						+ " selected";
				f_totalCount.setToolTipText(msg);
				f_porousCount.setToolTipText(msg);
			} else {
				f_porousCount.setText("");
			}
		}
		f_valueColumn.setWidth(computeValueWidth());
		f_graphColumn.setWidth(GRAPH_WIDTH + 5);
		f_bottomSection.layout();
		f_panel.layout();
		f_panel.pack();
		f_panel.setRedraw(true);
	}

	int computeValueWidth() {
		Image temp = new Image(null, 100, 100);
		GC gc = new GC(temp);
		int longest = 0;
		int longestIndex = -1;
		int i = 0;
		gc.setFont(f_reportContents.getFont());
		for (String value : valueList) {
			String label = f_filter.getLabel(value);
			Image image = f_filter.getImageFor(value);
			Point size = gc.textExtent(label);
			int current = size.x;
			if (image != null) {
				current += image.getBounds().width + 2;
			}
			if (current > longest) {
				longest = current;
				longestIndex = i;
			}
			i++;
		}
		gc.dispose();
		temp.dispose();

		if (longestIndex >= 0) {
			updateData(f_reportContents.getItem(longestIndex), longestIndex);
		}
		if (longest < 25) {
			return 50;
		}
		/*
		 * Check if on Windows (really only Vista and 7 but extra space on NT is
		 * okay)
		 */
		if (SystemUtils.IS_OS_WINDOWS) {
			return longest + 35; // extra space to avoid "..."
		}
		return longest + 25;
	}

	void updateData(final TableItem item, int i) {
		final String value = valueList.get(i);
		updateData(item, value);
	}

	void updateData(final TableItem item, String value) {
		final String label = f_filter.getLabel(value);
		item.setText(label);
		item.setText(0, label);

		final Image image = f_filter.getImageFor(value);
		item.setImage(image);

		item.setData(value);
		item.setChecked(f_filter.isPorous(value));
	}

	public void filterChanged(Filter filter) {
		if (f_panel.isDisposed())
			return;
		final UIJob job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				updateReport();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	public void filterQueryFailure(Filter filter, Exception e) {
		SLLogger.getLogger().log(
				Level.SEVERE,
				"query for " + this.getClass().getName() + " failed on "
						+ filter, e);
	}

	public void filterDisposed(Filter filter) {
		dispose();
	}

	public void selectionChanged(TableItem item) {
		/*
		 * The selection changed on a line.
		 */
		f_filter.setPorous((String) item.getData(), item.getChecked());
		updateReport();
	}

	@Override
	public void forceFocus() {
		f_reportContents.forceFocus();
		getCascadingList().show(index);
	}

	private void focusOnColumn(MColumn column) {
		if (column != null) {
			column.forceFocus();
		}
	}

	@Override
	void selectAll() {
		if (f_reportContents.isFocusControl()) {
			selectAllItems();
		} else {
			super.selectAll();
		}
	}

	private void selectAllItems() {
		f_filter.setPorousAll();
		for (TableItem item : f_reportContents.getItems()) {
			item.setChecked(true);
		}
	}
}
