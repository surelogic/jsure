package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SLImages;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;
import edu.cmu.cs.fluid.eclipse.ui.ITableContentProvider;
import edu.cmu.cs.fluid.sea.*;

public class ProblemsView extends AbstractDoubleCheckerView {
	private final ContentProvider content = new ContentProvider();
	
	public ProblemsView() {
		super(true);
	}

	@Override
	protected void makeActions() {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected void fillContextMenu(IMenuManager manager) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void setViewState() {
		// Nothing to do
	}

	@Override
	protected void setupViewer() {
		for(String label : labels) {
			final TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.LEFT);
			column.getColumn().setText(label);			
			column.getColumn().setWidth(40*label.length());
		}		
		
		viewer.setContentProvider(content);
		viewer.setLabelProvider(content);
		//viewer.setSorter(createSorter());
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().pack();
	}

	@Override
	protected void updateView() {
		content.build();
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		PromiseWarningDrop d = (PromiseWarningDrop) selection.getFirstElement();
		highlightLineInJavaEditor(d.getSrcRef());
	}
	
	static final String[] labels = {
		"Description", "Resource", "Line"
	};
	
	class ContentProvider implements ITableContentProvider {
		final List<PromiseWarningDrop> contents = new ArrayList<PromiseWarningDrop>();
		
		public void build() {
			contents.clear();
			
			Set<? extends PromiseWarningDrop> promiseWarningDrops = Sea
					.getDefault().getDropsOfType(PromiseWarningDrop.class);
			for (PromiseWarningDrop id : promiseWarningDrops) {
				// PromiseWarningDrop id = (PromiseWarningDrop) j.next();
				// only show info drops at the main level if they are not attached
				// to a promise drop or a result drop
				contents.add(id);
			}			
			// FIX sort?
		}

		public Object[] getElements(Object inputElement) {
			return contents.toArray();
		}

		public int numColumns() {
			return labels.length;
		}
		
		public String getColumnTitle(int column) {
			return labels[column];
		}
		
		public int getColumnWeight(int column) {
			switch (column) {
			case 0:
				return 50;
			case 1:
				return 20;
			case 2:
				return 10;
			}
			return 10;
		}

		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) {
				return SLImages.getImage(CommonImages.IMG_RED_X);
			}
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			PromiseWarningDrop d = (PromiseWarningDrop) element;
			switch (columnIndex) {
			case 0:
				return d.getMessage();
			case 1:
				IFile f = (IFile) d.getSrcRef().getEnclosingFile();
				return f.getFullPath().toPortableString();
			case 2:
				return Integer.toString(d.getSrcRef().getLineNumber());
			}
			return "";
		}
		
		public void dispose() {
			contents.clear();
		}
		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			//throw new UnsupportedOperationException();
		}

		public boolean isLabelProperty(Object element, String property) {
			throw new UnsupportedOperationException();
		}

		public void addListener(ILabelProviderListener listener) {
			//throw new UnsupportedOperationException();
		}
		
		public void removeListener(ILabelProviderListener listener) {
			//throw new UnsupportedOperationException();
		}
	}
}
