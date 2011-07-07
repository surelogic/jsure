/*
 * Created on Mar 1, 2005
 */
package com.surelogic.fluid.javac.coe;

import static com.surelogic.common.jsure.xml.CoE_Constants.*;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.surelogic.common.AbstractJavaZip;
import com.surelogic.common.CommonImages;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.xml.XMLUtil;

//import edu.cmu.cs.fluid.java.ISrcRef;

/**
 * Generate an XML representation of the ResultsView output
 * for use by the standalone results viewer.
 */
public class XMLReport {
  static final Logger LOG = SLLogger.getLogger("XMLReport");
  
  /**
   * No progress service
   * @param zipFile
   */
  public static <T> void exportResultsWithSource(final FileOutputStream zipFile,
		                                         final IZipContext<T> context) {
    ZipOutputStream out = new ZipOutputStream(zipFile);
    OutState state = new OutState(new PrintWriter(out), context.getNameSorter());
    generateResultsZip(out, state, context);
  }
  
  static class OutState {
    final PrintWriter out;
    final ResultsViewContentProvider content;
    final NameSorter sorter;
    final boolean includesSource;
    
    private OutState(PrintWriter pw, ResultsViewContentProvider contentProvider, 
        NameSorter ns, boolean includesSource) {
      out = pw;
      content = contentProvider;   
      Object[] elts = content.getElements(null);
      if (elts.length == 0) {
        throw new IllegalArgumentException("No contents for content provider");
      }
      sorter = ns;
      this.includesSource = includesSource;
    }
    private OutState(PrintWriter pw, NameSorter ns) {
      this(pw, new ResultsViewContentProvider().buildModelOfDropSea(), ns, true);
    }
  }
  
  public static void generateReport(OutState state) {
    state.out.print(XMLUtil.oneAttrOpen(ROOT_TAG, ATTR_MESSAGE, "Assurance Results"));

    Object [] root = state.content.getElements(null);
    state.sorter.sort(root);
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
    //ISrcRef srcRef = c.getSrcRef();
    /* FIX
    if(srcRef != null && srcRef.getEnclosingFile() instanceof IResource){
    	IResource srcFile = (IResource)srcRef.getEnclosingFile();
    	ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor((IFile) srcRef.getEnclosingFile());
    	if(state.includesSource) {
        sb.append(" ").append(XMLUtil.setAttr(ATTR_SOURCE, srcFile.getFullPath() + ".html"));
      }
      sb.append(" ").append(XMLUtil.setAttr(ATTR_LINE_NUM, Integer.toString(srcRef.getLineNumber())));
    }
    */
    sb.append(" ").append(XMLUtil.setAttr(ATTR_BASE_IMAGE, c.getBaseImageName()));

    int flagInt = c.getImageFlags();
    if (c.isInfoWarningDecorate) {
      flagInt |= INFO_WARNING;
    } else if (c.isInfoDecorated) {
      if (c.getBaseImageName() != CommonImages.IMG_INFO)
        flagInt |= INFO;
    }
    sb.append(" ").append(XMLUtil.setAttr(ATTR_FLAGS, Integer.toString(flagInt)));
    
    Object [] children = c.getChildren();
    if((c.getImageFlags() & REDDOT) > 0) {
      sb.append(" ").append(XMLUtil.setAttr(ATTR_REDDOTS, "1"));
    }
//    if(children.length == 0) {
//      reddots = " reddots=\"" + genRandDots() + "\"";
//    }
    sb.append(children.length > 0 ? ">" : " />");
    state.out.println(sb);

    pathToRoot.add(c);
    state.sorter.sort(children);
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

  private static void generateAnalysisList(PrintWriter pw, String[] analyses) {
    pw.println(XMLUtil.openNode(ANALYSES_TAG));
    for(String id : analyses) {
      pw.format(ANALYSIS_FORMAT, id);
    }
    pw.println(XMLUtil.closeNode(ANALYSES_TAG));
    pw.flush();
  }
  
  private static <T> void generateResultsZip(ZipOutputStream out, OutState state, 
		                                     final IZipContext<T> context) {
	final AbstractJavaZip<T> zip = context.getZip();
    try {
    	// Add results.xml file to zip
    	out.putNextEntry(new ZipEntry("results.xml"));
    	generateReport(state);
    	out.closeEntry();

    	zip.generateSourceZipContents(out);

    	out.putNextEntry(new ZipEntry("redDots.xml"));
    	generateRedDotList(new PrintWriter(out));
    	out.closeEntry();

    	out.putNextEntry(new ZipEntry("analyses.xml"));
    	generateAnalysisList(new PrintWriter(out), zip.getAnalyses());
    	out.closeEntry();
    	
    	out.flush(); // unnecessary?
    	out.close();
    } catch (IOException e) {
    	LOG.severe("Error writing ZIP file ");
    	e.printStackTrace();
    	return; // bail out
    }
  }
}