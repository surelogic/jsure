package com.surelogic.jsure.client.eclipse.views;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import com.surelogic.common.ISourceZipFileHandles;
import com.surelogic.common.ui.views.AbstractHistoricalSourceView;
import com.surelogic.javac.Config;
import com.surelogic.javac.JavaSourceFile;
import com.surelogic.javac.Projects;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

public class JSureHistoricalSourceView extends AbstractHistoricalSourceView
		implements JSureDataDirHub.CurrentScanChangeListener {
	private static Projects projects;
	private static ISourceZipFileHandles zips;
	private static boolean viewIsEnabled = true;

	public JSureHistoricalSourceView() {
		currentScanChanged();
	}

	@Override
	public void currentScanChanged(JSureScan scan) {
		currentScanChanged();
	}

	public void currentScanChanged() {
		final JSureScanInfo info = JSureDataDirHub.getInstance()
				.getCurrentScanInfo();
		if (info == null) {
			projects = null;
			zips = new ISourceZipFileHandles() {
				public Iterable<File> getSourceZips() {
					return Collections.emptyList();
				}
			};
		} else {
			projects = info.getProjects();
			zips = new ISourceZipFileHandles() {
				public Iterable<File> getSourceZips() {
					return Arrays.asList(new File(info.getDir(), "zips")
							.listFiles());
				}
			};
		}
	}

	@Override
	protected ISourceZipFileHandles findSources(String run) {
		// if (config.getRun().equals(run)) {
		return zips;
		// }
		// return null;
	}

	public static void tryToOpenInEditor(final String pkg, final String type,
			int lineNumber) {
		if (viewIsEnabled) {
			tryToOpenInEditor(JSureHistoricalSourceView.class, null, pkg, type,
					lineNumber);
		}
	}

	public static void tryToOpenInEditor(final String pkg, final String type,
			final String field) {
		if (viewIsEnabled) {
			tryToOpenInEditorUsingFieldName(JSureHistoricalSourceView.class,
					null, pkg, type, field);
		}
	}

	public static String tryToMapPath(String path) {
		if (projects == null) {
			return path;
		}
		try {
			final URI uri = new URI(path);
			for (Config config : projects.getConfigs()) {
				JavaSourceFile f = config.mapPath(uri);
				if (f != null) {
					String mapped = f.file.toURI().toString();
					if (mapped != null) {
						return mapped;
					}
				}
			}
		} catch (URISyntaxException e) {
			// Nothing to do
		}
		return path;
	}
}
