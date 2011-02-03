package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.*;

import org.eclipse.swt.graphics.Image;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;

import edu.cmu.cs.fluid.sea.*;

public final class ProblemsViewContentProvider extends AbstractResultsTableContentProvider<IDropInfo> {
	ProblemsViewContentProvider() {
		super("Description");
	}
	
	protected void getAndSortResults(List<IDropInfo> contents) {
		Set<? extends IDropInfo> promiseWarningDrops = PersistentDropInfo
				.getInstance().getDropsOfType(PromiseWarningDrop.class);
		for (IDropInfo id : promiseWarningDrops) {
			// only show info drops at the main level if they are not
			// attached
			// to a promise drop or a result drop
			contents.add(id);
		}
		Collections.sort(contents, sortByLocation);
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0) {
			return SLImages.getImage(CommonImages.IMG_ANNOTATION_ERROR);
		} else {
			return null;
		}
	}
}