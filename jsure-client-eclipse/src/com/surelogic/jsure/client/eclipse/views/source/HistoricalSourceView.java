package com.surelogic.jsure.client.eclipse.views.source;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.ISourceZipFileHandles;
import com.surelogic.common.SLUtility;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.common.ui.views.AbstractHistoricalSourceView;
import com.surelogic.javac.Config;
import com.surelogic.javac.JavaSourceFile;
import com.surelogic.javac.Projects;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

public class HistoricalSourceView extends AbstractHistoricalSourceView
		implements JSureDataDirHub.CurrentScanChangeListener {

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		final JSureScan scan = JSureDataDirHub.getInstance().getCurrentScan();
		if (scan != null) {
			setSourceSnapshotTime(scan.getTimeOfScan());
		}
		JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
	}

	@Override
	public void dispose() {
		try {
			JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);
		} finally {
			super.dispose();
		}
	}

	@Override
	public void currentScanChanged(final JSureScan scan) {
		final UIJob job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				clearSourceCodeFromView();
				if (scan != null) {
					setSourceSnapshotTime(scan.getTimeOfScan());
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

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
					return info.getJSureRun().getSourceZips();
				}
			};
		}
		return zips;
	}
	
	public static void tryToOpenInEditor(final IJavaRef javaRef) {
	  if (javaRef == null) return;
	  tryToOpenInEditor(javaRef.getPackageName(),
        DeclUtil.getTypeNameDollarSignOrNull(javaRef.getDeclaration()), javaRef.getLineNumber());
	}

	public static void tryToOpenInEditor(final String pkg, final String type,
			int lineNumber) {
		tryToOpenInEditor(HistoricalSourceView.class, null, pkg, 
				type == null ? SLUtility.PACKAGE_INFO : type,
				lineNumber);
	}

	public static void tryToOpenInEditor(final String pkg, final String type,
			final String field) {
		tryToOpenInEditorUsingFieldName(HistoricalSourceView.class, null, pkg,
				type, field);
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
