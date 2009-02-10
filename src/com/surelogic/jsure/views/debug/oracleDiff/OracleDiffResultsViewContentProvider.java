package com.surelogic.jsure.views.debug.oracleDiff;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import com.surelogic.common.ByteArrayStreams;
import com.surelogic.common.SafeCloseInputStream;
import com.surelogic.tree.diff.Diff;
import com.surelogic.xml.results.coe.LightweightXMLTreeBuilder;
import com.surelogic.xml.results.coe.ResultsTreeNode;

import edu.cmu.cs.fluid.dc.Nature;
import edu.cmu.cs.fluid.dcf.views.coe.ResultsViewContentProvider;
import edu.cmu.cs.fluid.dcf.views.coe.XMLReport;
//import edu.cmu.cs.fluid.sea.drops.ProjectDrop;

public final class OracleDiffResultsViewContentProvider extends ResultsViewContentProvider {
  // XXX: Stolen from com.surelogic.jsure.tests.RegressionTest
  private static FilenameFilter oracleFilter = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      return name.startsWith("oracle") && name.endsWith(".zip");
    }
  };
  
  
  
  private final ByteArrayStreams byteStreams = new ByteArrayStreams();

  

  // XXX: Stolen from com.surelogic.jsure.tests.RegressionTest
  private String getOracleName(String projectPath, FilenameFilter filter,
      String defaultName) {
    File path = new File(projectPath);
    File[] files = path.listFiles(filter);
    File file = null;
    for (File zip : files) {
      if (file == null) {
        file = zip;
      } else if (zip.getName().length() > file.getName().length()) {
        // Intended for comparing 3.2.4 to 070221
        file = zip;
      } else if (zip.getName().length() == file.getName().length()
          && zip.getName().compareTo(file.getName()) > 0) {
        // Intended for comparing 070107 to 070221
        file = zip;
      }
    }
    return (file != null) ? file.getAbsolutePath() : projectPath
        + File.separator + defaultName;
  }

  // XXX: Based on Results(String) from edu.cmu.cs.fluid.srv.Results
  private ResultsTreeNode getResultsFromZIP(final String zipFile) {
    ResultsTreeNode root = null;
    ZipFile zip = null;
    try {
      zip = new ZipFile(zipFile);
      ZipEntry xmlFile = zip.getEntry("results.xml");
      if (xmlFile != null) {
        if (xmlFile.getSize() <= 0) {
          throw new IllegalArgumentException("Bad results.xml of length: "+xmlFile.getSize());
        }
        final InputStream is = zip.getInputStream(xmlFile); 
        try {
          root = (ResultsTreeNode) LightweightXMLTreeBuilder.parse(
              is, LightweightXMLTreeBuilder.RESULTS_FILE);
        } finally {
          try {
            is.close();
          } catch (final IOException e) {
            // Didn't close, what can I do about it?
          }
        }
      } else {
        throw new FileNotFoundException(
            "results.xml not found while reading " + zipFile);
      }
    } catch (final IOException e) {
      throw new IllegalArgumentException(e);
    } finally {
      if (zip != null) {
        try {
          zip.close();
        } catch (final IOException e) {
          // Didn't close, what can I do about it?
        }
      }
    }
    return root;
  }
  
  
  @Override
  protected Object[] getElementsInternal() {
// XXX: See bug 1563
    // Find oracle file
    IProject proj = null;
    for (final IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (p.isOpen() && Nature.hasNature(p)) {
        proj = p;
        break;
      }
    }
    if (proj != null) {
//    final String projectName = ProjectDrop.getProject();
//    if (projectName != null) {
//      final IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
      
      /* Overall, based on runAnalysis() from com.surelogic.jsure.tests.RegressionTest,
       * but we try to avoid generating/extracting entire zip files.
       */

      final String oracleName = getOracleName(
          proj.getLocation().toOSString(), oracleFilter, "oracle.zip");
      if (!(new File(oracleName).exists())) {
        // No oracle
        // TODO: Find out how to send feedback to the view so that we display a message
        return noObjects;
      }
      final ResultsTreeNode oracleRoot = getResultsFromZIP(oracleName);
      
      final PrintWriter out = new PrintWriter(byteStreams.getOutputStream());
      final XMLReport.OutState state;
      try {
        state = new XMLReport.OutState(out);
      } catch (final IllegalArgumentException e) {
        /* Happens during start up if the view is open because the sea isn't 
         * initialized yet.  Need a better way to check this ahead of time.
         */
        byteStreams.reset();
        return noObjects;
      }
      XMLReport.generateReport(state);
      out.close();
      final SafeCloseInputStream in = byteStreams.getInputStream();
      final ResultsTreeNode currentRoot =
        (ResultsTreeNode) LightweightXMLTreeBuilder.parse(
            in, LightweightXMLTreeBuilder.RESULTS_FILE);
      in.close();
      byteStreams.reset();
        
      final ResultsTreeNode results = Diff.diff(oracleRoot, currentRoot, false);
      return results.getChildrenAsCollection().toArray();
    }
    return noObjects;
  }
  
  @Override
  protected Object[] getChildrenInternal(final Object parent) {
    if (parent instanceof ResultsTreeNode) {
      final ResultsTreeNode node = (ResultsTreeNode) parent;
      final List<ResultsTreeNode> children = node.getChildrenAsCollection();
      if (m_showInferences) {
        return children.toArray();
      } else {
        return ResultsTreeNode.filterNonInfo(children);
      }
    } else {
      return noObjects;
    }
  }
}
