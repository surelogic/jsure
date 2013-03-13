/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/project/JavaIncrementalBinder.java,v 1.80 2008/09/04 15:14:39 chance Exp $
 */
package edu.cmu.cs.fluid.java.project;

import java.util.*;
import java.util.logging.*;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.tree.PropagateUpTree;
import edu.cmu.cs.fluid.tree.Tree;
import edu.cmu.cs.fluid.util.*;
import edu.cmu.cs.fluid.version.*;


/**
 * Perform bindings on Java files incrementally.
 * The incrementality occurs in two dimensions:
 * <ol>
 * <li> Incrementality by versions: we compute the bindings for one version
 *      using the information for a connected version. The granularity of incrementality
 *      is the method.  (If any bindings need to change for anything in a method,
 *      we re-bind the method.)
 * <li> Incrementality over the files of a project.  Binding information is computed
 *      on demand.  The granularity of incrementality here is the compilation unit:
 *      if any bindings are desired, we compute all the bindings for the compilation
 *      unit.
 * </ol>
 * This class only implements the <em>non</em>-deprecated methods of the
 * binding interfaces.  (And currently doesn't even implement all of these.)
 * <p>
 * TODO move to fluid.java.bind
 * TODO parameterize by IJavaClassTable or some such.
 * @author boyland
 */
public class JavaIncrementalBinder extends AbstractJavaBinder {

  private static Logger LOG = SLLogger.getLogger("FLUID.java.bind");
  
  final static VersionedChangeRecord treeChanged = 
	  JJNode.versioningIsOn ? (VersionedChangeRecord) JJNode.treeChanged : null;
  
  /**
   * transient information concerning which bindings may have to be looked up again.
   * XXX this shouldn't be static.
   */
  static VersionedChangeRecord bindingsChanged = new VersionedChangeRecord();
  static {
    PropagateUpTree.attach(bindingsChanged,(Tree) JJNode.tree);
  }
  
  // new and experimental:
  // XXX: so new and experimental, it isn't used.  
  // XXX: The purpose is to do dynamic dependency tracking while 
  // looking into superclasses in order to fix the bug found by
  // Brandon Konop.  See bug pointers in JavaMemberTable.SuperScope.
  static ThreadGlobal<IRNode> useSite = new ThreadGlobal<IRNode>(null);
  
  // new and experimental.
  // also should not be static:
  /*
  public static void addLink(IRNode node, IRNode link) {
    bindingsChanged.addLink(node,link);
  }*/
  
  static boolean canDumpTree(IRNode node) {
    if (node == null) return false;
    VersionedRegion vr = VersionedRegion.getVersionedRegion(node);
    if (vr == null) return false;
    Bundle b1 = JJNode.getBundle();
    Bundle b2 = JavaNode.getBundle();
    VersionedChunk vc1 = VersionedChunk.get(vr, b1);
    VersionedChunk vc2 = VersionedChunk.get(vr, b2);
    return Version.isCurrentlyLoaded(vc1) && Version.isCurrentlyLoaded(vc2);
  }
  
  /**
   * Bindings need to be updated for this use for this version.
   * This marks the use so that it will be noticed in the
   * incremental binder traversal.  It is an O(1) (approx.) operation.
   * @param useNode
   */
  public static void needsTraversal(IRNode useNode, Version v) {
    Version.saveVersion(v);
    try {
      /* Must be careful not to dump something in an unloaded version */
      if (LOG.isLoggable(Level.FINER) && JJNode.tree.isNode(useNode) && canDumpTree(useNode)) {
        LOG.finer(v + ": needs traversal: " + DebugUnparser.toString(useNode));
      }
      bindingsChanged.setChanged(useNode);
    } finally {
      Version.restoreVersion();
    }
  }

  public static void dumpChangedTree(IRNode root, int ind, Version v1, Version v2) {
    for (int i=0; i < ind; ++i) System.out.print("  ");
    if (root == null) {
      System.out.println("<null>");
      return;
    }
    Operator op = JJNode.tree.getOperator(root);
    System.out.print(op.name());
    String i = JJNode.getInfoOrNull(root);
    if (i != null) System.out.print(" "+i);
    boolean changed1 = treeChanged.changed(root,v1,v2);
    boolean changed2 = bindingsChanged.changed(root,v1,v2);
    if (changed1) System.out.print(" *");
    if (changed2) System.out.print(" #");
    if (parentIsChanged(root,v1)) System.out.print(" !");
    if (changed1 || changed2) {
      System.out.println();
      for (IRNode ch : JJNode.tree.children(root)) {
        dumpChangedTree(ch,ind+1,v1,v2);
      }
    } else {
      System.out.println("...");
    }
  }
  
  public interface IVersionedDependency extends IDependency {
    void notifyUses(Version v);
  }
  
  /**
   * A class to record uses of things that might change (that is, be
   * different in a different version).
   * We record the uses unversioned for simplicity.
   * TODO: We should record the initial version at which information is recorded, 
   * and not notify any changes for that version.
   * @author boyland
   */
  static abstract class Dependency implements IVersionedDependency {
    /**
     * A monotonically non-decreasing set of versions where changes happen.
     * We start with null because we may not ever notice any changes.
     */
    Set<Version> changed = null;
    
    /**
     * A monotonically non-decreasing set of IRNodes.
     * We start with a null set in case nothing is used.
     * (Also, see {@link ClassMemberTable.UnversionedEntry} which doesn't
     * use uses.)
     */
    Set<IRNode> uses = null;

    /**
     * Mark all the use sites of this table entry (a name lookup in a scope) as
     * requiring new lookup for this version. Since we associate uses with entries (all the
     * declarations of the same name), this is a bit conservative, but should be
     * reasonable unless everything in a class has the same name.
     * This method only notifies uses once per version.  If uses are added,
     * they will be informed of all previous notifications (retroactively).
     * @see #addUse
     * @param v version at which to notify uses (or null if no notification needed)
     */
    @Override
    public synchronized void notifyUses(Version v) {
      if (v == null) return;
      if (changed != null && changed.contains(v)) return;
      if (changed == null) {
        changed = new HashSet<Version>();
      }
      changed.add(v);
      if (uses == null) return;
      for (Iterator<IRNode> it = uses.iterator(); it.hasNext(); ) {
        IRNode use = it.next();
        JavaIncrementalBinder.needsTraversal(use,v);
      }
    }
       
    /**
     * Add a new use to the dependency.  It is informed of all changes thus far.
     * @param use useSite to add.
     */
    @Override
    public synchronized void addUse(IRNode use) {
      if (use == null) return;
      if (uses == null) uses = new HashSet<IRNode>();
      if (uses.contains(use)) return;
      uses.add(use);
      if (changed == null) return;
      for (Iterator<Version> it = changed.iterator(); it.hasNext();) {
        Version v = it.next();
        JavaIncrementalBinder.needsTraversal(use,v);
      }
    }
  }
  
  public static boolean infoIsChanged(IRNode node, Version other) {
    String currentInfo = JJNode.getInfo(node);
    Version.saveVersion(other);
    try {
      return !currentInfo.equals(JJNode.getInfoOrNull(node));
    } finally {
      Version.restoreVersion();
    }
  }
  
  /**
   * Return true if the "parent" slot for this node is different than it was in
   * the other version.
   * @param node any Java FAST node.
   * @param other some other version
   * @return true if parent is different
   */
  public static boolean parentIsChanged(IRNode node, Version other) {
    IRNode currentParent = JJNode.tree.getParentOrNull(node);
    Version.saveVersion(other);
    try {
      IRNode otherParent = JJNode.tree.getParentOrNull(node);
      if (currentParent == null) return otherParent != null;
      return !currentParent.equals(otherParent);
    } finally {
      Version.restoreVersion();
    }
  }
  
  public static boolean treeChanged(IRNode node, Version other) {
	  if (treeChanged != null) {
		  return treeChanged.changed(node, other);
	  }
	  return JJNode.treeChanged.equals(node);
  }
  
  /**
   * Create an incremental binder using the given class table.
   */
  public JavaIncrementalBinder(IJavaClassTable table) {
    super(table);
    // System.out.println("LOG level for " + LOG + " is " + LOG.getLevel());
  }

  /**
   * The structure used to determine whether bindings are valid.
   * It includes the slots (since they have to be rooted here), but
   * it dos not do the binding action itself (this is done in
   * {@link #BinderVisitor}.
   * @author boyland
   */
  class GranuleBindings extends VersionedDerivedInformation 
  implements IGranuleBindings {
    final IRNode unit;
    final Version rootVersion;
    /**
     * binding each use of a name to the declaration that it refers to.
     */
    final SlotInfo<IBinding> useToDeclAttr;
    /**
     * binding each method declaration to a set of methods that it overrides.
     */
    final SlotInfo<List<IBinding>> methodOverridesAttr;
    
    private boolean isDestroyed = false;
    
    GranuleBindings(IRNode gr) {
      unit = gr;
      rootVersion = Version.getVersion();
      SlotFactory f = VersionedSlotFactory.bidirectional(rootVersion);
      useToDeclAttr = f.newAttribute();
      methodOverridesAttr = f.newAttribute(null);
    }

    @Override
    public IRNode getNode() {
    	return unit;
    }
    
    @Override
    public synchronized boolean isDestroyed() {
    	return isDestroyed;
    }
    
    @Override
    public synchronized void destroy() {
    	useToDeclAttr.destroy();
    	methodOverridesAttr.destroy();
    	isDestroyed = true;
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation#deriveChild(edu.cmu.cs.fluid.version.Version, edu.cmu.cs.fluid.version.Version)
     */
    @Override
    protected void deriveChild(Version parent, Version child)
        throws UnavailableException {
      deriveRelated(parent, child);
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation#deriveParent(edu.cmu.cs.fluid.version.Version, edu.cmu.cs.fluid.version.Version)
     */
    @Override
    protected void deriveParent(Version child, Version parent)
        throws UnavailableException {
      deriveRelated(child, parent);
    }
    
    protected void deriveRelated(Version from, Version to) {
      //! TODO: change this to use the deriveInfo from AbstractJavaBinder
      // XXX: Not sure how to communicate the two versions
      Version.saveVersion(to);
      try {
        if (LOG.isLoggable(Level.FINE)) {
          System.out.flush();
          //LOG.getHandlers()[0].flush();
          LOG.fine(" see dump next ----------------------------------------------------" + to);
          for (Logger log = LOG; log != null; log=log.getParent()) {
            for (Handler h : log.getHandlers()) {
              h.flush();
            }
          }
          System.err.flush();
          dumpChangedTree(unit,0,from,to);
          System.out.flush();
        }
        // we need to do two passes, here and in deriveVersion
        // because we need to bind all types before we try to handle
        // method calls because a method return type will need to be bound
        // before we access its binding.
        IncrementalBinderVisitor incrementalBinderVisitor = new IncrementalBinderVisitor(this,unit,from,to);
        incrementalBinderVisitor.start();
        incrementalBinderVisitor.setFullPass();
        incrementalBinderVisitor.start();
      } finally {
        Version.restoreVersion();
      }
    }
    
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation#deriveVersion(edu.cmu.cs.fluid.version.Version)
     */
    @Override
    protected void deriveVersion(Version v) throws UnavailableException {
      assert (v == rootVersion);
      Version.saveVersion(v);
      try {
        // call out to the batch code.
        deriveInfo(this, unit);
      } finally {
        Version.restoreVersion();
      }
    }

    @Override
    public SlotInfo<List<IBinding>> getMethodOverridesAttr() {
      return methodOverridesAttr;
    }

    @Override
    public SlotInfo<IBinding> getUseToDeclAttr() {
      return useToDeclAttr;
    }

    @Override
    public void ensureDerived(IRNode node) {
      super.ensureDerived();
    }

    @Override
    public boolean containsFullInfo() {
      return true;
    }
  }
  
  @Override
  protected GranuleBindings ensureBindingsOK(IRNode node) {
    JavaMemberTable.ensureAllValid();
    GranuleBindings bindings = 
      (GranuleBindings) super.ensureBindingsOK(node);
    return bindings;
  }
  
  @Override
  protected final GranuleBindings makeGranuleBindings(IRNode cu, boolean needFullInfo) {
    return new GranuleBindings(cu); // TODO
  }

  @Override
  public JavaMemberTable typeMemberTable(IJavaSourceRefType type) {
    // Because of teh new granularity, we always need an incremental table
    return JavaMemberTable.get(type.getDeclaration());
    /*
    if (JavaIncrementalBinder.boundIncrementally(tdecl)) {  
      LOG.finer("getting incremental table");
      return JavaMemberTable.get(tdecl);
    } else {
      LOG.finer("getting batch scope");
      return JavaMemberTable.makeBatchTable(tdecl);
    }*/
  }
  
  /**
   * Return a member table from a type.
   * @param ty
   * @return member table
   * @deprecated use {@link #typeScope(IJavaType)}
   */
  @Deprecated
  public JavaMemberTable typeMemberTable(IJavaType ty) {
    if (ty instanceof IJavaDeclaredType) {
      IJavaDeclaredType dty = (IJavaDeclaredType)ty;
      IRNode tdecl = dty.getDeclaration();
      IJavaTypeSubstitution subst = JavaTypeSubstitution.create(getTypeEnvironment(), dty);
      if (subst != null && LOG.isLoggable(Level.INFO)) {
        LOG.fine("Getting unsubstituted member table for " + ty);
        LOG.fine("  tdecl is " + DebugUnparser.toString(tdecl));
      }
      return typeMemberTable(dty);
    } else if (ty instanceof IJavaArrayType) {
      return typeMemberTable(asDeclaredType((IJavaArrayType)ty));
    } else {
      LOG.warning("non-class type! " + ty);
    }
    return null;
  }
  
  class IncrementalBinderVisitor extends BinderVisitor {
    final Version from, to;
    public IncrementalBinderVisitor(GranuleBindings cu, IRNode gr, Version fromV, Version toV) {
      super(cu,gr);
      from = fromV;
      to = toV;
      isBatch = false;
    }
    
    @Override
    protected boolean nodeHasChanged(IRNode node) {
      return treeChanged.changed(node,from) ||       
             bindingsChanged.changed(node,from);
    }

    @Override
    protected boolean nodeHasNewParent(IRNode node) {
      return parentIsChanged(node,from);
    }
  }
  
  // IBinder and ITypeEnvironment methods  
  @Override
  public Iteratable<IBinding> findOverriddenParentMethods(IRNode methodDeclaration) {
    GranuleBindings bindings = ensureBindingsOK(methodDeclaration);
    Collection<IBinding> col = methodDeclaration.getSlotValue(bindings.methodOverridesAttr);
    if (col == null)
      return new EmptyIterator<IBinding>();
    // The filter transforms the IBinding into an IRNode
    return IteratorUtil.makeIteratable(col);
  }
   
  public void debug() {
    for (Map.Entry<IRNode,IGranuleBindings> e : allGranuleBindings.entrySet()) {
      IRNode cu = e.getKey();
      GranuleBindings vdi = (GranuleBindings) e.getValue();
      System.out.println("CU: " + DebugUnparser.toString(cu));
      System.out.println(vdi.debugString() + "\n");
    }
  }

  @Override
  public IJavaScope getImportTable(IRNode node) {
    return JavaImportTable.getImportTable(node,this);
  }

  @Override
  public String getInVersionString() {
    return " in " + Version.getVersion();
  }
  
  @Override 
  protected boolean okToAccess(IRNode node) {
    return canDumpTree(node);
  }
}
