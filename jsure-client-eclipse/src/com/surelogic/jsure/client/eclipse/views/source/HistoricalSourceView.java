package com.surelogic.jsure.client.eclipse.views.source;

import java.io.File;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.NonNull;
import com.surelogic.common.ISourceZipFileHandles;
import com.surelogic.common.SLUtility;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.common.ui.views.AbstractHistoricalSourceView;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public class HistoricalSourceView extends AbstractHistoricalSourceView implements JSureDataDirHub.CurrentScanChangeListener {

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

  // Run in UI?
  @Override
  @NonNull
  protected ISourceZipFileHandles findSources(String run) {
    final JSureScanInfo info = OLD.equals(run) ? JSureDataDirHub.getInstance().getLastMatchingScanInfo() : JSureDataDirHub
        .getInstance().getCurrentScanInfo();
    final ISourceZipFileHandles zips;
    if (info == null) {
      zips = ISourceZipFileHandles.EMPTY;
    } else {
      setSourceSnapshotTime(info.getJSureRun().getTimeOfScan());
      zips = new ISourceZipFileHandles() {
        @Override
        public Iterable<File> getSourceZips() {
          return info.getJSureRun().getSourceZips();
        }

        public File getSourceZipForProject(String proj) {
          if (proj == null) {
        	return null;
          }
          final String name = proj + ".zip";
          for(File z : getSourceZips()) {
        	if (name.equals(z.getName())) {
        	  return z;
        	}
          }
          return null;
        }        
      };
    }
    return zips;
  }

  public static void tryToOpenInEditor(final IJavaRef javaRef, final boolean tryToUseOld) {
    if (javaRef == null)
      return;
    tryToOpenInEditor(javaRef.getPackageName(), DeclUtil.getTypeNameDollarSignOrNull(javaRef.getDeclaration()),
        javaRef.getLineNumber(), tryToUseOld);
  }

  public static void tryToOpenInEditor(final IDecl decl, final boolean tryToUseOld) {
    if (decl == null)
      return;
    tryToOpenInEditor(DeclUtil.getPackageNameOrEmpty(decl), DeclUtil.getTypeNameDollarSignOrNull(decl), 1, tryToUseOld);
  }

  private static void tryToOpenInEditor(final String pkg, final String type, int lineNumber, final boolean tryToUseOld) {
    tryToOpenInEditor(HistoricalSourceView.class, tryToUseOld ? OLD : null, pkg, type == null ? SLUtility.PACKAGE_INFO : type,
        lineNumber);
  }
}
