package com.surelogic.jsure.views.debug.oracleDiff;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.*;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.xml.Entity;
import static com.surelogic.jsure.xml.AbstractXMLReader.*;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;
import edu.cmu.cs.fluid.java.AbstractSrcRef;
import edu.cmu.cs.fluid.java.ISrcRef;
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
			public Object getEnclosingFile() {
				return e.getAttribute(PATH_ATTR);
			}
			public int getOffset() {
				return Integer.parseInt(e.getAttribute(OFFSET_ATTR));
			}
			@Override
			public String getCUName() {
				return null;
			}
			@Override
			public Long getHash() {
				return Long.getLong(e.getAttribute(HASH_ATTR));
			}
			@Override
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
		final ProjectsDrop pd = ProjectsDrop.getDrop();
		for(String name : pd.getIIRProjects().getProjectNames()) {			
			final IProject p = EclipseUtility.getProject(name);
			if (p == null || !p.exists()) {
				continue;
			}
			final File pFile = p.getLocation().toFile();
			final File file  = SeaSummary.findSummary(pFile.getAbsolutePath());			
			//IFile file = p.getFile(name + SeaSnapshot.SUFFIX);
			if (file.exists()) {
				try {
					IFile newFile = p.getFile(name + ".new" + SeaSnapshot.SUFFIX);
					File newFile2 = newFile.getLocation().toFile();
					if (!newFile2.isFile()) {
						SeaSummary.summarize(name, Sea.getDefault(), newFile2);
					}
					
					Diff d = SeaSummary.diff(name, Sea.getDefault(), file);
					f_contentProvider.setDiff(d);					
					return;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("No snapshot to diff against in "+name);
			}
		}
	}
}
