package com.surelogic.jsure.views.debug.oracleDiff;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.HASH_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OFFSET_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PATH_ATTR;

import org.eclipse.swt.SWT;

import com.surelogic.common.xml.Entity;
import com.surelogic.jsure.client.eclipse.editors.EditorUtil;
import com.surelogic.jsure.client.eclipse.views.AbstractScanTreeView;

import edu.cmu.cs.fluid.java.AbstractSrcRef;
import edu.cmu.cs.fluid.java.ISrcRef;

public class SnapshotDiffView extends AbstractScanTreeView<Object> {
	public SnapshotDiffView() {
		super(SWT.NONE, Object.class, new SnapshotDiffContentProvider());
	}

	@Override
	protected void handleDoubleClick(Object d) {
		if (d instanceof Entity) {
			Entity e = (Entity) d;
			ISrcRef r = makeSrcRef(e);
			EditorUtil.highlightLineInJavaEditor(r);
		}
	}

	private ISrcRef makeSrcRef(final Entity e) {
		return new AbstractSrcRef() {
			@Override
			public String getEnclosingFile() {
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

			@Override
			public String getProject() {
				return null;
			}
		};
	}
}
