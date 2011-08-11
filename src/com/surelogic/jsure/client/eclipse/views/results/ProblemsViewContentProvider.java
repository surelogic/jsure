package com.surelogic.jsure.client.eclipse.views.results;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.IDropInfo;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;

public final class ProblemsViewContentProvider extends
		AbstractResultsTableContentProvider<IDropInfo> {

	ProblemsViewContentProvider() {
		super("Description");
	}

	protected String getAndSortResults(List<IDropInfo> contents) {
		final JSureScanInfo info = JSureDataDirHub.getInstance()
				.getCurrentScanInfo();
		if (info == null) {
			return null;
		}
		Set<? extends IDropInfo> promiseWarningDrops = info
				.getDropsOfType(PromiseWarningDrop.class);
		for (IDropInfo id : promiseWarningDrops) {
			/*
			 * Only show info drops at the main level if they are not attached
			 * to a promise drop or a result drop.
			 * 
			 * Also we only want to show problems in the source code. To do this
			 * we get the CU name and see if it ends in ".java"
			 */
			final ISrcRef srcRef = id.getSrcRef();
			if (srcRef != null) {
				final String path = srcRef.getRelativePath();
				if (path != null) {
					if (path.endsWith(".java")) {
						contents.add(id);
					}
				}
			}
		}
		Collections.sort(contents, sortByLocation);
		return info.getLabel();
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return SLImages.getImage(CommonImages.IMG_ANNOTATION_ERROR);
		} else {
			return null;
		}
	}
}