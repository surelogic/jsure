package com.surelogic.jsure.client.eclipse.views.source;

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
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

public class JSureHistoricalSourceView extends AbstractHistoricalSourceView {

	@Override
	protected ISourceZipFileHandles findSources(String run) {
		final JSureScanInfo info = JSureDataDirHub.getInstance()
				.getCurrentScanInfo();
		final ISourceZipFileHandles zips;
		if (info == null) {
			zips = new ISourceZipFileHandles() {
				public Iterable<File> getSourceZips() {
					return Collections.emptyList();
				}
			};
		} else {
			zips = new ISourceZipFileHandles() {
				public Iterable<File> getSourceZips() {
					return Arrays.asList(new File(info.getDir(), "zips")
							.listFiles());
				}
			};
		}
		return zips;
	}

	public static void tryToOpenInEditor(final String pkg, final String type,
			int lineNumber) {
		tryToOpenInEditor(JSureHistoricalSourceView.class, null, pkg, type,
				lineNumber);
	}

	public static void tryToOpenInEditor(final String pkg, final String type,
			final String field) {
		tryToOpenInEditorUsingFieldName(JSureHistoricalSourceView.class, null,
				pkg, type, field);
	}

	public static String tryToMapPath(String path) {
		final JSureScanInfo info = JSureDataDirHub.getInstance()
				.getCurrentScanInfo();
		final Projects projects = info.getProjects();
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
