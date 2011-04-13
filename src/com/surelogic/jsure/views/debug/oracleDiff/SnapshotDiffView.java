package com.surelogic.jsure.views.debug.oracleDiff;

import java.util.*;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;

import com.surelogic.common.xml.Entity;
import com.surelogic.jsure.client.eclipse.views.AbstractScanTreeView;


import static com.surelogic.common.jsure.xml.AbstractXMLReader.*;

import edu.cmu.cs.fluid.java.AbstractSrcRef;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.IDropInfo;
//import edu.cmu.cs.fluid.sea.Sea;
//import edu.cmu.cs.fluid.sea.drops.*;


public class SnapshotDiffView extends AbstractScanTreeView<Object> {
	public SnapshotDiffView() {
		super(SWT.NONE, Object.class, new SnapshotDiffContentProvider());
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
	protected void handleDoubleClick(Object d) {
	    if (d instanceof Entity) {
	        Entity e = (Entity) d;
	        ISrcRef r = makeSrcRef(e);
	        highlightLineInJavaEditor(r);
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
}
