/*
 * Created on Mar 1, 2005
 */
package edu.cmu.cs.fluid.dcf.views.coe;

import static com.surelogic.xml.results.coe.CoE_Constants.ANALYSES_TAG;
import static com.surelogic.xml.results.coe.CoE_Constants.ANALYSIS_FORMAT;
import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_BASE_IMAGE;
import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_CVSREVISION;
import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_CVSSOURCE;
import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_FLAGS;
import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_LINE_NUM;
import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_MESSAGE;
import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_REDDOTS;
import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_SOURCE;
import static com.surelogic.xml.results.coe.CoE_Constants.REDDOTS_TAG;
import static com.surelogic.xml.results.coe.CoE_Constants.REDDOT_FORMAT;
import static com.surelogic.xml.results.coe.CoE_Constants.RESULT_TAG;
import static com.surelogic.xml.results.coe.CoE_Constants.ROOT_TAG;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.surelogic.common.eclipse.SourceZip;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.XMLUtil;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.xml.results.coe.CoE_Constants;

/**
 * Generate an XML representation of the ResultsView output
 * for use by the standalone results viewer.
 */
public class XMLReport {
  static final Logger LOG = SLLogger.getLogger("XMLReport");
  
  public static void exportResults(final FileOutputStream xmlFile, TreeViewer resultsViewer) {
    final TreeViewer viewer = resultsViewer;
    IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
    try {
	    progressService.busyCursorWhile(new IRunnableWithProgress(){
	       public void run(IProgressMonitor monitor) {
	         try {
	           XMLReport.generateReport(new PrintWriter(xmlFile), viewer, false);
	           xmlFile.close();
	         } catch (IOException e) {
	           LOG.log(Level.SEVERE, "Error writing XML results file ", e);
	           return; // bail out
	         }
	       }
	    });
    } catch(InvocationTargetException e) {
		SLLogger.getLogger().log(Level.SEVERE, "Problem exporting XML results", e);
    } catch(InterruptedException e) {
		SLLogger.getLogger().log(Level.SEVERE, "Problem exporting XML results", e);
    }
  }

  public static void exportResultsWithSource(final FileOutputStream zipFile,
			TreeViewer resultsViewer) {
    final TreeViewer viewer = resultsViewer;
		IProgressService progressService = PlatformUI.getWorkbench()
				.getProgressService();
		try {
			progressService.busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					ZipOutputStream out = new ZipOutputStream(zipFile);
          OutState state = new OutState(new PrintWriter(out), viewer, true);
					generateResultsZip(out, state);
				}
			});
		} catch (InvocationTargetException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Problem exporting XML results", e);
		} catch (InterruptedException e) {
			SLLogger.getLogger().log(Level.SEVERE, "Problem exporting XML results", e);
		}
	}
  
  /**
   * No progress service
   * @param zipFile
   */
  public static void exportResultsWithSource(final FileOutputStream zipFile) {
    ZipOutputStream out = new ZipOutputStream(zipFile);
    //OutState state = new OutState(new PrintWriter(out));
	try {
		Writer w = new BufferedWriter(new OutputStreamWriter(out, CoE_Constants.ENCODING));
	    OutState state = new OutState(new PrintWriter(w));
	    generateResultsZip(out, state);
	} catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	}
  }
  
  /* XXX: Made public on 2009-01-22 because otherwise the (already) public 
   * generateReport() method cannot be invoked from outside this package.
   */
  public static class OutState {
    final PrintWriter out;
    final IResultsViewContentProvider content;
    final ResultsView.ContentNameSorter sorter;
    final boolean includesSource;
    
    private OutState(PrintWriter out, TreeViewer viewer, boolean includesSource) {
      this(out, (AbstractResultsViewContentProvider) viewer.getContentProvider(), 
           (ResultsView.ContentNameSorter) viewer.getSorter(), includesSource); 
    }
    private OutState(PrintWriter pw, IResultsViewContentProvider contentProvider, 
        ResultsView.ContentNameSorter ns, boolean includesSource) {
      out = pw;
      content = contentProvider;   
      Object[] elts = content.getElements(null);
      if (elts.length == 0) {
        throw new IllegalArgumentException("No contents for content provider");
      }
      sorter = ns;
      this.includesSource = includesSource;
    }
    /* XXX: Made public on 2009-01-22 because otherwise the (already) public 
     * generateReport() method cannot be invoked from outside this package.
     */
    public OutState(PrintWriter pw) {
      this(pw, new ResultsViewContentProvider().buildModelOfDropSea(), new ResultsView.ContentNameSorter(), true);
    }
  }
  
  private static void generateReport(PrintWriter out, TreeViewer resultsViewer, boolean includesSource){
    generateReport(new OutState(out, resultsViewer, includesSource));
  }
  
  public static void generateReport(OutState state) {
    state.out.print(XMLUtil.oneAttrOpen(ROOT_TAG, ATTR_MESSAGE, "Assurance Results"));

    Object [] root = state.content.getElements(null);
    state.sorter.sort(null, root);
    if(root.length == 0){
      state.out.println("  "+XMLUtil.oneAttrNode(RESULT_TAG, ATTR_MESSAGE, "No results to report.")+
                  XMLUtil.closeNode(ROOT_TAG));
      return;
    }
    for(int i=0; i < root.length; i++){
      Content c;
      if(root[i] instanceof Content) c = (Content)root[i];
      else { LOG.severe("Root node not instance of Content"); continue; }
      printTree(state, c, 1);
    }
    state.out.print(XMLUtil.closeNode(ROOT_TAG));
    state.out.flush();
  }

  private static Set<Content> pathToRoot = new HashSet<Content>();
  
  private static void printTree(OutState state, Content c, int tabLevel){
    final StringBuilder sb = new StringBuilder();
    for(int i=0; i<tabLevel; i++) {
      sb.append("  ");
    }
    final String indent = sb.toString();
    sb.append(XMLUtil.startNode(RESULT_TAG));
    
    if(pathToRoot.contains(c)){
      state.out.print(sb);
      state.out.print(XMLUtil.oneAttrNode(ROOT_TAG, ATTR_MESSAGE, "(Skipped reference to parent node to avoid loop)"));
      return;    
    }
    sb.append(XMLUtil.setAttr(ATTR_MESSAGE, XMLUtil.escape(c.getMessage())));
    
    final Map<String, String> locAttrs = c.getLocationAttributes(new HashMap<String, String>(), !state.includesSource);
    if (locAttrs != null) {
    	if(state.includesSource) {
        sb.append(" ").append(XMLUtil.setAttr(ATTR_SOURCE, locAttrs.get(ATTR_SOURCE)));
      }	else {
        sb.append(" ").append(XMLUtil.setAttr(ATTR_CVSSOURCE, locAttrs.get(ATTR_CVSSOURCE)));
        final String cvsRevision = locAttrs.get(ATTR_CVSREVISION);
        if (cvsRevision != null) {
          sb.append(" ").append(XMLUtil.setAttr(ATTR_CVSREVISION, cvsRevision));
 			  }
    	}
      sb.append(" ").append(XMLUtil.setAttr(ATTR_LINE_NUM, locAttrs.get(ATTR_LINE_NUM)));
    }
    sb.append(" ").append(XMLUtil.setAttr(ATTR_BASE_IMAGE, c.getBaseImageName()));

    sb.append(" ").append(XMLUtil.setAttr(ATTR_FLAGS, Integer.toString(c.getFlags())));
    
    Object [] children = c.getChildren();
    if (c.isRedDot()) {
      sb.append(" ").append(XMLUtil.setAttr(ATTR_REDDOTS, "1"));
    }
//    if(children.length == 0) {
//      reddots = " reddots=\"" + genRandDots() + "\"";
//    }
    sb.append(children.length > 0 ? ">" : " />");
    state.out.println(sb);

    pathToRoot.add(c);
    state.sorter.sort(null, children);
    for(int i=0; i < children.length; i++){
      if(children[i] instanceof Content){
        printTree(state, (Content)children[i], tabLevel+1);          
      }
      else {
        LOG.severe("Child node not instance of Content");
      }
    }
    pathToRoot.remove(c);
    
    if(children.length > 0) state.out.println(indent + XMLUtil.closeNode(RESULT_TAG));
  }
  
  /*
  private static int genRandDots() {
    int result = 0;
    for(int bit = 1; bit < 64; bit *= 2) {
      if(new Random().nextInt(32) <= 1) result |= bit;
    }
    return result;
  }
  */
    
  private static void generateRedDotList(PrintWriter pw) {
    pw.println(XMLUtil.openNode(REDDOTS_TAG));
    pw.format(REDDOT_FORMAT, "1", "Red Dot", "reddot.gif");
    pw.format(REDDOT_FORMAT, "2", "Orange Dot", "orangedot.gif");
    pw.format(REDDOT_FORMAT, "4", "Yellow Dot", "yellowdot.gif");
    pw.format(REDDOT_FORMAT, "8", "Green Dot", "greendot.gif");
    pw.println(XMLUtil.closeNode(REDDOTS_TAG));
    pw.flush();
  }

  private static void generateAnalysisList(PrintWriter pw) {
    pw.println(XMLUtil.openNode(ANALYSES_TAG));
    for(String id : edu.cmu.cs.fluid.dc.Plugin.getDefault().getIncludedExtensions()) {
      pw.format(ANALYSIS_FORMAT, id);
    }
    pw.println(XMLUtil.closeNode(ANALYSES_TAG));
    pw.flush();
  }
  
  private static void generateResultsZip(ZipOutputStream out, OutState state) {
    try {
    	// Add results.xml file to zip
    	out.putNextEntry(new ZipEntry("results.xml"));
    	generateReport(state);
    	out.closeEntry();

    	new SourceZip(Activator.getWorkspace().getRoot()).generateSourceZipContents(out);

    	out.putNextEntry(new ZipEntry("redDots.xml"));
    	generateRedDotList(new PrintWriter(out));
    	out.closeEntry();

    	out.putNextEntry(new ZipEntry("analyses.xml"));
    	generateAnalysisList(new PrintWriter(out));
    	out.closeEntry();
    	
    	out.flush(); // unnecessary?
    	out.close();
    } catch (IOException e) {
    	LOG.log(Level.SEVERE, "Error writing ZIP file ", e);
    	return; // bail out
    }
  }
}