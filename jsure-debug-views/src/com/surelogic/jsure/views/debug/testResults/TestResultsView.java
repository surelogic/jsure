package com.surelogic.jsure.views.debug.testResults;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.core.Eclipse;
import com.surelogic.jsure.views.debug.testResults.model.Root;
import com.surelogic.jsure.views.debug.testResults.model.TestResultsLabelProvider;
import com.surelogic.jsure.views.debug.testResults.model.TestResultsTreeContentProvider;
import com.surelogic.test.AbstractTestOutput;
import com.surelogic.test.ITest;
import com.surelogic.test.ITestOutput;
import com.surelogic.test.ITestOutputFactory;

public final class TestResultsView extends ViewPart {
  private static final ImageDescriptor EXPORT_IMAGE_DESC =
    SLImages.getImageDescriptor(CommonImages.IMG_EXPORT);

  private TreeViewer treeViewer;
  private TestResultsTreeContentProvider contentProvider;
  private Root root;
  private Action saveAction;
  
  private final StringWriter stringWriter;
  private final PrintWriter printWriter;
  
  private boolean shouldReset;
  
  
  private final Runnable REFRESH = new Runnable() {
    public void run() {
      treeViewer.refresh();
    }
  };
  
  public TestResultsView() {
    shouldReset = false;
    stringWriter = new StringWriter();
    printWriter = new PrintWriter(stringWriter);
  }

  void reset() {
    shouldReset = false;
    stringWriter.getBuffer().setLength(0);
    root.clear();
  }
  
  @Override
  public void createPartControl(final Composite parent) {
    root = new Root();
    contentProvider = new TestResultsTreeContentProvider(root);
    treeViewer = new TreeViewer(parent);
    treeViewer.setContentProvider(contentProvider);
    treeViewer.setLabelProvider(new TestResultsLabelProvider());
    treeViewer.setSorter(new ViewerSorter());
    treeViewer.setInput(root);
    
    saveAction = new Action("Export Results") {
      @Override
      public void run() {
        exportResults();
      }
    };
    saveAction.setToolTipText("Export results to text file");
    saveAction.setImageDescriptor(EXPORT_IMAGE_DESC);
    
    getViewSite().getActionBars().getToolBarManager().add(saveAction);
    
    // Hook to the test results
    Eclipse.initialize();
    Eclipse.getDefault().addTestOutputFactory(new Factory());
  }

  private void exportResults() {
    final Shell shell = getViewSite().getShell();
    final FileDialog fileChooser = new FileDialog(shell, SWT.SAVE);
    fileChooser.setFilterExtensions(new String[] { "*.txt" });
    fileChooser.setText("Select text file output name");
    String filename = fileChooser.open();
    if (filename != null) {
      if (!filename.endsWith(".txt")) {
        filename = filename.concat(".txt");
      }
      FileWriter txtFile;
      try {
        txtFile = new FileWriter(filename);
      } catch (final IOException e) {
        MessageDialog.openError(shell, "Error exporting results",
            "Unable to create text results file: " + e.getMessage());
        return; // abort
      }
      try {
        txtFile.write(stringWriter.toString());
        txtFile.close();
      } catch (final IOException e) {
        MessageDialog.openError(shell, "Error Exporting Results", e.getMessage());
        // Try to close anyway
        try {
          txtFile.close();
        } catch (final IOException e2) {
          // punt
        }
      }
    }
  }

  @Override
  public void setFocus() {
    treeViewer.getTree().setFocus();
  }

  private void refresh() {
    EclipseUIUtility.asyncExec(REFRESH);
  }

  
  private final class TreeOutput extends AbstractTestOutput {
    protected TreeOutput(final String n) {
      super(n);
    }
    
    @Override
    public void close() {
      super.close();
      shouldReset = true;
    }

    @Override
    public ITest reportStart(final ITest o) {
      final ITest returnValue = super.reportStart(o);
      if (shouldReset) reset();
      return returnValue;
    }
    
    public void reportError(final ITest o, final Throwable ex) {
      if (report(o, ex)) {
        root.addError(o, ex);
        printWriter.print("ERROR in ");
        printWriter.println(o.getClassName());
        ex.printStackTrace(printWriter);
        refresh();
      }
    }

    public void reportFailure(final ITest o, final String msg) {
      if (report(o, msg)) {
        root.addFailure(o, msg);
        printWriter.print("FAILURE in ");
        printWriter.print(o.getClassName());
        printWriter.print(": ");
        printWriter.println(msg);
        refresh();
      }
    }

    public void reportSuccess(final ITest o, final String msg) {
      if (report(o, msg)) {
        root.addSuccess(o, msg);
        printWriter.print("SUCCESS in ");
        printWriter.print(o.getClassName());
        printWriter.print(": ");
        printWriter.println(msg);
        refresh();
      }
    }
  }
  
  
    
  public final class Factory implements ITestOutputFactory {
    public ITestOutput create(final String name) {
      return new TreeOutput(name);
    }
  }
}
