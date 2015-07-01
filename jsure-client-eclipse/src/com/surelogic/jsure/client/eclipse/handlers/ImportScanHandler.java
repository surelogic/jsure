package com.surelogic.jsure.client.eclipse.handlers;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.common.ui.jobs.UnzipIntoDirJob;
import com.surelogic.javac.jobs.JSureConstants;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public class ImportScanHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    final FileDialog fd = new FileDialog(EclipseUIUtility.getShell(), SWT.OPEN);
    fd.setText(I18N.msg("jsure.dialog.importscan.title"));
    fd.setFilterExtensions(new String[] { "*" + JSureConstants.JSURE_SCAN_TASK_SUFFIX, "*.*" });
    fd.setFilterNames(
        new String[] { "Compressed JSure Scan Documents (*" + JSureConstants.JSURE_SCAN_TASK_SUFFIX + ")", "All Files (*.*)" });
    final String name = fd.open();
    if (name != null) {
      final File zipFile = new File(name);
      // determine name of new scan directory from zip file name
      final String simpleName = zipFile.getName();
      if (!simpleName.toLowerCase().endsWith(".zip")) {
        SLUIJob job = new SLUIJob() {
          @Override
          public IStatus runInUIThread(IProgressMonitor monitor) {
            MessageDialog.openInformation(EclipseUIUtility.getShell(), I18N.msg("jsure.dialog.importscan.error.title"),
                I18N.msg("jsure.dialog.importscan.error.msg", name));
            return Status.OK_STATUS;
          }
        };
        job.schedule();
        return null;
      }
      final String dirName = simpleName.substring(0, simpleName.length() - JSureConstants.JSURE_SCAN_TASK_SUFFIX.length());
      final File newScanDir = new File(EclipseUtility.getJSureDataDirectory(), dirName);
      final Job job = new UnzipIntoDirJob("jsure.dialog.importscan", zipFile, newScanDir, new Runnable() {
        public void run() {
          JSureDataDirHub.getInstance().scanDirectoryAdded(newScanDir);
        }
      });
      job.schedule();
    }
    return null;
  }
}
