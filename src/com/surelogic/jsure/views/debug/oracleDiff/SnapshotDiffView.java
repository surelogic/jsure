package com.surelogic.jsure.views.debug.oracleDiff;

import java.io.File;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.*;

import com.surelogic.common.eclipse.EclipseUtility;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;
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
		// TODO Auto-generated method stub
		
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
			if (p == null) {
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
