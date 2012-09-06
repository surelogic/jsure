package com.surelogic.jsure.client.eclipse.views.results;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IWorkbenchPage;

import com.surelogic.Utility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.ui.EclipseUIUtility;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.IDrop;
import edu.cmu.cs.fluid.sea.IProofDropInfo;

@Utility
public final class DropInfoUtility {
	public static void showDrop(IProofDropInfo d) {
		final ResultsView view = (ResultsView) EclipseUIUtility.showView(
				ResultsView.class.getName(), null, IWorkbenchPage.VIEW_VISIBLE);
		view.showDrop(d);
	}

	public static <T extends IDrop> String getResource(T d) {
		ISrcRef ref = d.getSrcRef();
		if (ref == null) {
			return "";
		}
		if (ref.getEnclosingURI() != null) {
			String path = ref.getRelativePath();
			if (path != null) {
				return path;
			}
		}
		Object o = ref.getEnclosingFile();
		if (o instanceof IFile) {
			IFile f = (IFile) o;
			return f.getFullPath().toPortableString();
		} else if (o instanceof String) {
			String name = (String) o;
			if (name.indexOf('/') < 0) {
				// probably not a file
				return name;
			}
			if (name.endsWith(".class")) {
				return name;
			}
			final int bang = name.lastIndexOf('!');
			if (bang >= 0) {
				return name.substring(bang + 1);
			}
			IFile f = EclipseUtility.resolveIFile(name);
			if (f == null) {
				return "";
			}
			return f.getFullPath().toPortableString();
		} else if (o != null) {
			return o.toString();
		}
		return "";
	}

	public static <T extends IDrop> int getLine(T d) {
		ISrcRef ref = d.getSrcRef();
		if (ref != null) {
			return ref.getLineNumber();
		}
		return Integer.MAX_VALUE;
	}
}
