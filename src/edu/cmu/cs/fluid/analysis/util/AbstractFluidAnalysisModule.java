package edu.cmu.cs.fluid.analysis.util;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.JSureProperties;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.core.Eclipse;
import com.surelogic.jsure.core.EclipseCodeFile;
import com.surelogic.xml.TestXMLParserConstants;

import edu.cmu.cs.fluid.dc.AbstractAnalysisModule;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.analysis.IWarningReport;
import edu.cmu.cs.fluid.java.bind.IHasBinding;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.WarningDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.PackageDrop;
import edu.cmu.cs.fluid.sea.drops.SourceCUDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AbstractRunner;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.cmu.cs.fluid.util.IteratorUtil;
import edu.cmu.cs.fluid.util.SimpleRemovelessIterator;

/**
 * A module that contains the basics to drive most Fluid analyses using the IR.
 */
public abstract class AbstractFluidAnalysisModule<Q> extends
    AbstractAnalysisModule<Q> implements IWarningReport {
  static {
    Eclipse.initialize();
  }
  private static final String PROMISES_XML_SUFFIX = TestXMLParserConstants.SUFFIX;
  private static final Logger LOG = SLLogger
      .getLogger("AbstractFluidAnalysisModule");

  private IProject project;

  private IJavaProject jProject;

  // Not used here -- move down?
  protected ICompilationUnit javaFile;

  protected IProject getProject() {
    return project;
  }
  
  protected IJavaProject getJavaProject() {
    return jProject;
  }
  
  protected ICompilationUnit getCompUnit() {
    return javaFile;
  }
   
  public AbstractFluidAnalysisModule() {
	  super();
  }
  
  public AbstractFluidAnalysisModule(boolean inParallel, Class<Q> type) {
	  super(inParallel, type);
  }
  
  @Override
  public boolean needsAST() {
    return false; // TODO fix to take advantage of what Majordomo does
  }

  @Override
  public void preBuild(IProject p) {
    project = p;
    Eclipse.initialize();
//    Reporter.init();
    
    IDE.getInstance().clearCancelled();
    clearQueues();
    Eclipse.getDefault().setInProgress(true);
  }

  @Override
  public void postBuild(IProject project) {
    Eclipse.getDefault().setInProgress(false);
    clearQueues();
  }

  @Override
  public void analyzeBegin(IProject p) {
    project = p;
    jProject = JavaCore.create(p);
    //Eclipse.getDefault().setDefaultClassPath(p);
    PromiseFramework.getInstance().setReporter(this);
  }

  @Override
  public boolean analyzeResource(IResource resource, int kind) {
    /*
     * Need analyzeCompilationUnit to be called (by returning false) unless the
     * resource was REMOVED (I think MOVED_FROM is impossible, but I'm not
     * sure). JSR-175 makes the "package-info.java" file special -- we DO NOT at
     * this point want to generate a Fluid AST for this file -- perhaps in the
     * future this would make sense.
     */
    if (isRemoved(kind)) {
      removeResource(resource);
      return true;
    }
    return resource.getName().endsWith("package-info.java");
  }

  protected final boolean isRemoved(int kind) {
    return kind == IResourceDelta.REMOVED || kind == IResourceDelta.MOVED_FROM;
  }
  
  @Override
  public void cancel() {
    IDE.getInstance().setCancelled();
  }

  public static void doindent(PrintStream s, int i) {
    for (; i > 0; --i) {
      s.print("  ");
    }
  }

  public void dumpTree(Logger log, PrintStream s, IRNode root, int indent) {
    if (log.isLoggable(Level.FINE)) {
      doindent(s, indent);
      if (root == null) {
        s.println("null");
      } else {
        if (edu.cmu.cs.fluid.parse.JJNode.tree.opExists(root)) {
          String name = JJNode.tree.getOperator(root).name();
          s.print(name + "  =  ");
        }
        try {
          if (JJNode.getInfoOrNull(root) == null)
            s.print(" ");
          else {
            s.print(JJNode.getInfoOrNull(root));

            Operator op = JJNode.tree.getOperator(root);
            if (op instanceof IHasBinding) {
              IRNode binding = null;
              //Eclipse.getDefault().getTypeEnv(project).getBinder().getBinding(root);
              if (binding != null) {
                s.print(" binding = " + DebugUnparser.toString(binding));
              } else {
                s.print(" No binding ...");
              }
            }
          }
          s.println();
        } catch (Exception e) {
          s.print(" ");
        }
        if (JJNode.tree.hasChildren(root)) {
          java.util.Iterator<IRNode> children = JJNode.tree.children(root);
          while (children.hasNext()) {
            IRNode child = children.next();
            dumpTree(log, s, child, indent + 1);
          }
        }
      }
    }
  }

  public static Object runVersioned(AbstractRunner r) {
    return IDE.runVersioned(r);
  }

  public static Object runInVersion(AbstractRunner r) {
    return IDE.runAtMarker(r);
  }

  protected static Operator getOperator(final IRNode n) {
    return JJNode.tree.getOperator(n);
  }

  public static boolean isPromisesXML(IResource resource) {
    return (resource.getType() == IResource.FILE && resource.getName()
        .endsWith(PROMISES_XML_SUFFIX));
  }

  public static boolean isPackageInfo(IResource resource) {
    return (resource.getType() == IResource.FILE && resource.getName().equals(
        "package-info.java"));
  }

  public static boolean isPackageJava(IResource resource) {
    return (resource.getType() == IResource.FILE && resource.getName().equals(
        "package.java"));
  }
  
  public static boolean isJavaSource(IResource resource) {
	    return (resource.getType() == IResource.FILE && resource.getName().endsWith(
	        ".java"));
	  }

  public static boolean isDotProject(IResource resource) {
    return (resource.getType() == IResource.FILE && resource.getFullPath()
        .toString().equals(".project"));
  }
  
  public static boolean isDotClasspath(IResource resource) {
	    return (resource.getType() == IResource.FILE && resource.getFullPath()
	        .toString().equals(".classpath"));
  }

  public static boolean isFluidProperties(IResource resource) {
	final String name = resource.getFullPath().toString();
    return (resource.getType() == IResource.FILE && name.equals(JSureProperties.JSURE_PROPERTIES));
  }

  public boolean isOnOutputPath(IResource resource) {
    final IPath p = resource.getFullPath();
    try {
      IClasspathEntry[] entries = jProject.getResolvedClasspath(true);
      for (IClasspathEntry entry : entries) {
        IPath out = entry.getOutputLocation();
        if (out == null) {
          continue;
        }
        if (out.isPrefixOf(p)) {
          return true;
        }
      }
      IPath out = jProject.getOutputLocation();
      if (out != null && out.isPrefixOf(p)) {
        return true;
      }
    } catch (JavaModelException e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * @param resource
   *          The promises XML file
   * @return The corresponding type name extracted from the file
   */
  public static String getCorrespondingTypeName(IResource resource) {
    String promisesXML = resource.getName();
    int len = promisesXML.length() - PROMISES_XML_SUFFIX.length();
    return promisesXML.substring(0, len);
  }
  
  public static String getCorrespondingPackageName(IResource resource) {
    String name   = getCorrespondingTypeName(resource);
    String folder = resource.getLocation().removeLastSegments(1).lastSegment();
    if (name.endsWith(folder)) {
      return name;
    }
    return null;
  }

  public void reportWarning(String description, IRNode here) {
    IRReferenceDrop drop = makeWarningDrop();
    initDrop(drop, description, here, warningCategory());
  }

  public void reportProblem(String description, IRNode here) {
    IRReferenceDrop drop = makeProblemDrop();
    initDrop(drop, description, here, problemCategory());
  }

  protected void initDrop(IRReferenceDrop drop, String description,
      IRNode here, Category cat) {
    drop.setMessage(description);
    /*
     * FIX r.setMessage(description + " at " + javaFile.getHandleIdentifier() +
     * ":" + lineNo);
     */
    drop.setNodeAndCompilationUnitDependency(here);
    drop.setCategory(cat);
  }

  protected IRReferenceDrop makeWarningDrop() {
    return new WarningDrop();
  }

  protected IRReferenceDrop makeProblemDrop() {
    return new WarningDrop();
  }

  protected Category warningCategory() {
    return JavaGlobals.UNCATEGORIZED;
  }

  protected Category problemCategory() {
    return JavaGlobals.UNCATEGORIZED;
  }

  /**
   * Global queue of qualified types to be processed by later analyses
   */
  private static Set<String> typeQueue = new HashSet<String>();

  /**
   * Per-analysis set of qualified types that are finished
   */
  private Set<String> completed = new HashSet<String>();

  private void clearQueues() {
    typeQueue.clear();
    completed.clear();
  }

  protected static void addQualfiedTypeNamesToSet(Set<String> queue, IRNode cu) {
    Iterator<String> it = JavaNames.getQualifiedTypeNames(cu);
    while (it.hasNext()) {
      queue.add(it.next());
    }
  }

  /**
   * Adding types to the list that later analyses should look at
   */
  protected void queueForLaterProcessing(PackageDrop p) {
    // final boolean debug = LOG.isLoggable(Level.FINE);

    // Find the package
    IWorkspaceRoot myWorkspaceRoot = Eclipse.getDefault().getWorkspaceRoot();
    IJavaModel javaModel = JavaCore.create(myWorkspaceRoot);
    try {
      for (IJavaProject proj : javaModel.getJavaProjects()) {
    	if (!proj.equals(project)) {
    		continue;
    	}
        for (IPackageFragment pkgFrag : proj.getPackageFragments()) {
          if (p.javaOSFileName.equals(pkgFrag.getElementName())) {
            // Found a package frag that matches
            if (pkgFrag.getKind() == IPackageFragmentRoot.K_SOURCE) {
              for (ICompilationUnit cu : pkgFrag.getCompilationUnits()) {
                for (IType t : cu.getTypes()) {
                  /*
                   * if (t.isMember()) { continue; // skip this }
                   */
                  String name = t.getFullyQualifiedName();
                  queueForLaterProcessing(name);
                }
              }
            } else { // binary
              for (IClassFile cf : pkgFrag.getClassFiles()) {
                IType t = cf.getType();
                queueForLaterProcessing(t.getFullyQualifiedName());
              }
            }
          }
          // otherwise, continue searching for packages
        }
      }
    } catch (JavaModelException e) {
    	// Ignore
    } 
  }

  protected void queueForLaterProcessing(IRNode cu) {
    addQualfiedTypeNamesToSet(typeQueue, cu);
  }

  protected void queueForLaterProcessing(String qname) {
    LOG.info("Queueing " + qname);
    /*
    if (qname.indexOf('.') < 0) {
    	System.out.println("No qualified name");
    }
    */
    if (qname.indexOf('$') >= 0) {
      //System.out.println("$");
      qname = qname.replace('$', '.');
    }
    typeQueue.add(qname);
  }

  protected void doneProcessing(String qname) {
    completed.add(qname);
  }

  protected void doneProcessing(IRNode cu) {
    addQualfiedTypeNamesToSet(completed, cu);
  }

  protected boolean isProcessed(String qname) {
    return completed.contains(qname);
  }

  /**
   * Get a list of types that are still waiting to be processed
   */
  private Iteratable<String> getTypesStillWaiting() {
    if (typeQueue.size() == 0) {
      return EmptyIterator.prototype();
    }
    final Iterator<String> it = typeQueue.iterator();
    return new SimpleRemovelessIterator<String>() {
      @Override
      protected Object computeNext() {
        while (it.hasNext()) {
          String qname = it.next();
          if (completed.contains(qname)) {
            // skip since it's already done
            // LOG.info("Skipping "+qname);
            continue;
          }
          return qname;
        }
        return IteratorUtil.noElement;
      }
    };
  }

  protected void handleWaitQueue(final IQueueHandler handler) {
    runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      public void run() {
        for (String qname : getTypesStillWaiting()) {
          LOG.fine("Handling waiting: " + qname);
          if (IDE.getInstance().isCancelled()) {
            return;
          }
          handler.handle(qname);
        }
      }
    });
  }

  protected void removeResource(IResource resource) {
	if (!resource.getName().endsWith(".java")) {
		return;			
    }
    final CUDrop drop = SourceCUDrop.queryCU(new EclipseCodeFile(resource));   
    if (drop != null && drop.isValid()) {
   	  //System.out.println("Invalidating "+drop.javaOSFileName);
      drop.invalidate();
    }
  }
}
