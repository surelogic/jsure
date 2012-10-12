/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/project/JavaProjectClassTable.java,v 1.16 2008/06/24 19:13:17 thallora Exp $
 */
package edu.cmu.cs.fluid.java.project;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.Pair;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.ExplicitSlotFactory;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.Slot;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IJavaClassTable;
import edu.cmu.cs.fluid.java.bind.AbstractJavaClassTable;
import edu.cmu.cs.fluid.project.Component;
import edu.cmu.cs.fluid.project.Project;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedDerivedInformation;
import edu.cmu.cs.fluid.version.VersionedDerivedSlot;
import edu.cmu.cs.fluid.version.VersionedSlotFactory;


/**
 * A table mapping fully-qualified class names to IR Nodes for the class
 * or interface.  This class can only be used to find packages or outer classes.
 * @author boyland
 */
public class JavaProjectClassTable extends AbstractJavaClassTable implements IJavaClassTable {
  private static Logger LOG = SLLogger.getLogger("FLUID.java.bind");
  
  protected FileLocator fileLocator;
  protected Project theProject;
  protected ExplicitSlotFactory factory; // set when we first request a version of this
  
  /**
   * Create a class table with the given parts:
   * @param floc file locator (for finding files)
   * @param proj the project structure.
   */
  public JavaProjectClassTable(FileLocator floc, Project proj) {
    fileLocator = floc;
    theProject = proj;
  }
  
  /**
   * map fully qualified strings to Entry objects.
   */
  private final Map<String,Entry> rep = new HashMap<String,Entry>();
  
  class Entry extends JavaIncrementalBinder.Dependency {
    VersionedDerivedSlot<IRNode> compNodeSlot;
    {
      Slot<IRNode> s = factory.predefinedSlot(null);
      compNodeSlot = (VersionedDerivedSlot<IRNode>) s;
    }
    public Entry(Version v) {
    }
    public Entry(Version v, IRNode compNode) {
      compNodeSlot = compNodeSlot.setValue(compNode,v);
    }
    
    public IRNode getCompNode(IRNode use) {
      addUse(use);
      return compNodeSlot.getValue();
    }
    
    public void setCompNode(Version v, IRNode compNode) {
      if (compNode.equals(getCompNode(null))) return;
      compNodeSlot = compNodeSlot.setValue(compNode,v);
      notifyUses(v);
    }
  }
  
  protected Entry getEntry(Version v, String qName) {
    synchronized (rep) {
      Entry e = rep.get(qName); 
      if (e == null) {
        LOG.finer("Creating new entry for " + qName);
        if (LOG.isLoggable(Level.FINEST)) {
          StringBuilder sb = new StringBuilder();
          for (String k : rep.keySet()) {
            sb.append(k);
            sb.append(' ');
          }
          LOG.finest("previous keys: " + sb);
        }
        e = new Entry(v);
        rep.put(qName,e);
      }
      return e;
    }
  }
  
  public class VersionSupport extends VersionedDerivedInformation {
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.version.VersionedHashMap#deriveChild(edu.cmu.cs.fluid.version.Version,
     *      edu.cmu.cs.fluid.version.Version)
     */
    @Override
    protected void deriveChild(Version parent, Version child) {
      deriveRelated(parent, child);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.version.VersionedHashMap#deriveParent(edu.cmu.cs.fluid.version.Version,
     *      edu.cmu.cs.fluid.version.Version)
     */
    @Override
    protected void deriveParent(Version child, Version parent) {
      deriveRelated(child, parent);
    }
    
    protected void deriveRelated(Version oldV, Version newV) {
      theProject.ensureLoaded(newV, fileLocator);
      removeOldClasses(Project.changeIterator(
          theProject.getRoot(), newV, oldV), oldV, newV);
      addNewClasses(Project.changeIterator(theProject.getRoot(),
          oldV, newV), oldV, newV);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see edu.cmu.cs.fluid.version.VersionedHashMap#deriveVersion(edu.cmu.cs.fluid.version.Version)
     */
    @Override
    protected void deriveVersion(Version v) throws UnavailableException {
      factory = VersionedSlotFactory.bidirectional(Version.getVersion());
      theProject.ensureLoaded(v, fileLocator);
      addNewClasses(Project.getTree().topDown(theProject.getRoot()), null, v);
    }
  }
  
  private final VersionSupport versionSupport = new VersionSupport();
  
  public final synchronized void clear() {
    versionSupport.clear();
    rep.clear();
    factory = null;
  }
  
  /**
   * @param enum
   * @param oldV
   * @param newV
   */
  private void removeOldClasses(Iterator enm, Version oldV, Version newV) {
    while (enm.hasNext()) {
      IRNode compNode = (IRNode) enm.next();
      //String name = theProject.getComponentName(compNode);
      Component comp = theProject.getComponent(compNode);
      if (comp instanceof JavaComponent) {
        if (oldV != null && theProject.nameChanged(compNode,oldV, newV)) {
          Iterator under = Project.getTree().topDown(compNode);
          removeOldClasses(under, null, newV); // avoid O(n^2) by passing
                                               // oldV==null
        } else if (oldV == null) {
          getEntry(newV, getQualifiedName(compNode)).setCompNode(newV, null);
        }
      }
    }
  }

  /**
   * @param enum
   * @param oldV
   * @param newV
   */
  private void addNewClasses(Iterator<IRNode> enm, Version oldV, Version newV) {
    final boolean debug = LOG.isLoggable(Level.FINE);
    while (enm.hasNext()) {
      IRNode compNode = enm.next();
      Component comp = theProject.getComponent(compNode);
      if (comp instanceof JavaComponent) {
        if (oldV != null && theProject.nameChanged(compNode,oldV, newV)) {
          Iterator<IRNode> under = Project.getTree().topDown(compNode);
          IRNode discard = under.next(); // discard first node
          assert discard.equals(compNode);
          addNewClasses(under, null, newV); // avoid O(n^2) by passing
                                            // oldV==null
        }
        String qualifiedName = getQualifiedName(compNode);
        if (debug) {
          LOG.fine("Found class/package: " + qualifiedName);
        }
        getEntry(newV, qualifiedName).setCompNode(newV, compNode);
      }
    }
  }
  
  // two private things used to compute qualified names
  private List<String> tempList = new ArrayList<String>();
  private StringBuilder tempBuffer = new StringBuilder();
  /**
   * Compute the qualified name for a Java component class or package in
   * a project.  This is done <em>without</em> loading the compilation unit tree.
   * The method is synchronized so it can reuse the temporary lists and string buffers.
   * @param compNode node within the project tree
   * @return fully qualified name
   */
  protected synchronized String getQualifiedName(IRNode compNode) {
    tempList.clear();
    tempBuffer.setLength(0);
    while (theProject.getComponent(compNode) instanceof JavaComponent) {
      tempList.add(theProject.getComponentName(compNode));
      compNode = Project.getTree().getParent(compNode);
    }
    for (ListIterator<String> it = tempList.listIterator(tempList.size()-1); it.hasPrevious(); ) {
      tempBuffer.append(it.previous());
      tempBuffer.append(".");
    }
    if (tempBuffer.length() > 0) {
      tempBuffer.setLength(tempBuffer.length()-1);
    }
    return tempBuffer.toString();
  }
    
  // ComponentMap compMap;
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.project.IJavaClassTable#getOuterClass(java.lang.String, edu.cmu.cs.fluid.ir.IRNode)
   */
  public IRNode getOuterClass(String qName, IRNode useSite) {
    //System.out.println("getOuterClass " + qName + " Called");
    versionSupport.ensureDerived();
    LOG.finer("getOuterClass(" + qName + "," + useSite + ") starting work.");
    Entry entry = getEntry(Version.getVersion(),qName);
    IRNode cn = entry.getCompNode(useSite);
    if (cn == null) {
      //System.out.println("No component node for this qName.");
      if (LOG.isLoggable(Level.FINER)) {
        LOG.finer("getOuterClass(" + qName + ") returning NULL");
      }
      return null;
    }
    JavaComponent comp = (JavaComponent) theProject.getComponent(cn);
    comp.ensureLoaded(Version.getVersion(),fileLocator);
    IRNode root = comp.getRoot();
    if (LOG.isLoggable(Level.FINER)) {
      String use = useSite == null ? "<null>" : "#<not loaded>";
      if (JavaIncrementalBinder.canDumpTree(useSite)) {
        use = DebugUnparser.toString(useSite);
      }
      String rootString = root == null ? "<null>" : "#<not loaded>";
      if (JavaIncrementalBinder.canDumpTree(root)) {
        rootString = DebugUnparser.toString(root);
      }
      LOG.finer("getOuterClass(" + qName + "," + use + ") returning "
          + rootString);
      
    }
    return root;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.project.IJavaClassTable#allNames()
   */
  public Set<String> allNames() {
    versionSupport.ensureDerived();
    return new HashSet<String>(rep.keySet());
  }

  public Iterable<Pair<String, IRNode>> allPackages() {
	  throw new UnsupportedOperationException();
  }
}
