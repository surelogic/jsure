package com.surelogic.jsure.views.debug.oracleDiff;

import java.io.File;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.*;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.xml.Entity;
import com.surelogic.jsure.client.eclipse.views.AbstractDoubleCheckerView;
import com.surelogic.jsure.core.listeners.PersistentDropInfo;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.*;

import edu.cmu.cs.fluid.java.AbstractSrcRef;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.IDropInfo;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.sea.xml.*;
import edu.cmu.cs.fluid.sea.xml.SeaSummary.Diff;

public class SnapshotDiffView extends AbstractDoubleCheckerView {
	private SnapshotDiffContentProvider f_contentProvider = 
		new SnapshotDiffContentProvider();

	public SnapshotDiffView() {
		super(false); // Use tree
	}

	@Override
	protected void subscribe() {
		PersistentDropInfo.getInstance().addListener(this);
	}
	
	@Override
	protected void finishCreatePartControl() {
		if (PersistentDropInfo.getInstance().load()) {
			updateView();
			setViewerVisibility(true);
			System.out.println("Loaded snapshot for "+this);
		}
	}
	
	@Override
	protected void makeActions() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void fillContextMenu(IMenuManager manager, IStructuredSelection s) {
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
	protected void handleDoubleClick(IStructuredSelection selection) {
		if (selection.size() == 1) {
			Object o = selection.getFirstElement();
			if (o instanceof Entity) {
				Entity e = (Entity) o;			
				ISrcRef ref = makeSrcRef(e);
				highlightLineInJavaEditor(ref);
			}
		}		
	}

	private ISrcRef makeSrcRef(final Entity e) {
		for(Map.Entry<String,String> me : e.getAttributes().entrySet()) {
			System.out.println(me.getKey()+" = "+me.getValue());
		}
		return new AbstractSrcRef() {
			@Override
      public Object getEnclosingFile() {
				return e.getAttribute(PATH_ATTR);
			}
			@Override
      public int getOffset() {
				return Integer.parseInt(e.getAttribute(OFFSET_ATTR));
			}
			public String getCUName() {
				return null;
			}
			public Long getHash() {
				return Long.getLong(e.getAttribute(HASH_ATTR));
			}
			public String getPackage() {
				return null;
			}			
		};
	}

	@Override
	protected void setViewState() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void setupViewer() {
		viewer.setContentProvider(f_contentProvider);
		viewer.setLabelProvider(f_contentProvider);
		//viewer.setSorter(createSorter());
		//ColumnViewerToolTipSupport.enableFor(viewer);
	}

	@Override
	protected void updateView() {
		final Collection<? extends IDropInfo> info = PersistentDropInfo.getInstance().getRawInfo();
		if (!info.isEmpty()) {
			try {
				File file = null;
				for(String name : PersistentDropInfo.getInstance().findProjectsLabel().split(", ")) {			
					final IProject p = EclipseUtility.getProject(name);
					if (p == null || !p.exists()) {
						continue;
					}
					final File pFile = p.getLocation().toFile();
					file  = SeaSummary.findSummary(pFile.getAbsolutePath());	
				}
				Diff d = SeaSummary.diff(info, file);
				f_contentProvider.setDiff(d);					
				return;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
				System.out.println("No snapshot to diff against");
		}
	}
}
