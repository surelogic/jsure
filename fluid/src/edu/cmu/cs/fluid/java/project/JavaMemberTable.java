/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/project/JavaMemberTable.java,v 1.59 2008/09/08 20:37:36 chance Exp $
 */
package edu.cmu.cs.fluid.java.project;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.debug.DebugUtil;
import edu.cmu.cs.fluid.ir.ExplicitSlotFactory;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRSequence;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;
import edu.cmu.cs.fluid.version.*;
import static edu.cmu.cs.fluid.util.IteratorUtil.noElement;

/**
 * Table for a class or interface that nables us to map names to sequences of
 * declaratrions. The table is incremental (depends on the class) and always
 * returns the correct result for the desired version. It also records where the
 * table is used in a dynamic dependency link so that if the table changes, the
 * uses can be notified.
 * <P>
 * TODO: Move to fluid.java.bind
 * TODO: Get rid of static things -- parameterize by JavaProject.
 * @author boyland
 */
public class JavaMemberTable extends VersionedDerivedInformation implements IJavaMemberTable {

  private static final Logger LOG = SLLogger.getLogger("FLUID.java.bind");
  
  /**
   * Set of versions for which all of the tables are valid.
   * Null if a table is just being added.
   */
  private static Set<Version> allValid = new HashSet<Version>();
  private ExplicitSlotFactory factory;

  /**
   * Mainly used to find supertypes and remake bindings
   */
  public final IJavaSourceRefType type;
  public final IRNode typeDeclaration;
  public final boolean isVersioned;
  private boolean repopulated = false;

  private JavaMemberTable(IJavaSourceRefType type) {
    this(type.getDeclaration(), type, false);
  }
  
  private JavaMemberTable(IRNode tdecl, boolean isVersioned) {
    this(tdecl, null, isVersioned);
  }
  
  /**
   * Create a class member table for the given class or interface. It will be
   * made valid for the current version.
   * 
   * @param tdecl
   *          the type declaration node. Must not be null.
   */
  private JavaMemberTable(IRNode tdecl, IJavaSourceRefType type, boolean isVersioned) {
    this(tdecl, type, Version.getVersion(),isVersioned);
    if (tdecl == null) { throw new NullPointerException(
        "type declaration cannot be null"); }
    factory = VersionedSlotFactory.bidirectional(Version.getVersion());
    populateDeNovo();
  }

  private JavaMemberTable(IRNode tdecl, IJavaSourceRefType t, Version v, boolean versioned) {
    super(v);
    typeDeclaration = tdecl;
    type        = t;
    isVersioned = versioned;
  }

  protected static final HashMap<IRNode,JavaMemberTable> tables = new HashMap<IRNode,JavaMemberTable>();

  protected static final JavaMemberTable placeholder = new JavaMemberTable(
      null, null, null, false);

  /**
   * Create a versioned member table for the given declaration.
   * @param tdecl Type declaration node.
   * @return the member table for this declaration.
   */
  public static JavaMemberTable get(IRNode tdecl) {
    if (tdecl == null) { throw new NullPointerException(
        "Cannot get member table for null node"); }
    JavaMemberTable jmt;
    synchronized (tables) {
      jmt = tables.get(tdecl);
      if (jmt == null) {
        tables.put(tdecl, placeholder);
      } else {
        while (jmt == placeholder) {
          try {
            tables.wait();
          } catch (InterruptedException e) {
            LOG.severe("wait interrupted");
            throw new FluidRuntimeException("wait interrupted");
          }
          jmt = tables.get(tdecl);
        }
      }
    }
    if (jmt == null) {
      jmt = new JavaMemberTable(tdecl,true);
      synchronized (tables) {
        tables.put(tdecl, jmt);
        tables.notifyAll();
        // we only clear at the very end, when the table may be available to another
        // thread.  Up to this point, this table is essentially invisible.
        allValid.clear();
      }
    }
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("making sure we have the table derived for this version: " + Version.getVersion());
    }
    jmt.ensureDerived();
    return jmt;
  }
  
  /**
   * Create a non-cached, unversioned table for the class.
   * This is to be used only in the current version and in the current thread.
   * @param tdecl type declaration (including anon class expression)
   * @return
   */
  public static JavaMemberTable makeBatchTable(IJavaSourceRefType type) {
    return new JavaMemberTable(type);
  }

  /**
   * Make sure that all tables currently around are valid for the current
   * version.  This does not apply to any table currently being constructed,
   * or which is constructed while this method is running.
   */
  public static void ensureAllValid() {
    List<JavaMemberTable> allTables;
    Version v = Version.getVersion();
    synchronized (tables) {
      if (allValid.contains(v)) return;
      allTables = new ArrayList<JavaMemberTable>(tables.values());
    }
    for (Iterator<JavaMemberTable> it = allTables.iterator(); it.hasNext(); ) {
      JavaMemberTable jmt = it.next();
      if (jmt == placeholder) continue;
      if (!JJNode.tree.isNode(jmt.typeDeclaration)) {
          // if the node is not valid in this version, don't bother updating it.
          continue;
      }
      jmt.ensureDerived();
    }
    synchronized (tables) {
      if (allTables.size() == tables.size()) {
        allValid.add(v);
      }
      // otherwise, perhaps a new one is added
    }
  }
  
  /**
   * Discard all existing versioned tables.
   */
  public static void clearAll() {
    synchronized (tables) {
      allValid.clear();
      tables.clear();
    }
  }
  
  /**
   * Start up the table in the current version
   */
  void populateDeNovo() {
    (new UpdateTableVisitor(false,null,null)).doAccept(getClassBody());
  }

  /**
   * @return class body for type declaration
   */
  protected IRNode getClassBody() {
    Operator op;
    try {
        op = JJNode.tree.getOperator(typeDeclaration);
    } catch (RuntimeException e) {
        System.out.println("Node without op = " + typeDeclaration);
        VersionedRegion vr = VersionedRegion.getVersionedRegion(typeDeclaration);
        if (vr != null) {
            System.out.println("  in versioned region " + vr);
        }
        throw e;
    }
    // TODO: handle enums.
    IRNode body;
    if (op instanceof ClassDeclaration) {
      body = ClassDeclaration.getBody(typeDeclaration);
    } else if (op instanceof InterfaceDeclaration) {
      body = InterfaceDeclaration.getBody(typeDeclaration);
    } else if (op instanceof AnonClassExpression) {
      body = AnonClassExpression.getBody(typeDeclaration);
    } else if (op instanceof TypeFormal) {
      return null;
    } else if (op instanceof EnumDeclaration) {
      body = EnumDeclaration.getBody(typeDeclaration);
    } else if (op instanceof EnumConstantClassDeclaration) {
      body = EnumConstantClassDeclaration.getBody(typeDeclaration);
    } else if (op instanceof AnnotationDeclaration) {
      body = AnnotationDeclaration.getBody(typeDeclaration);
    } else {
      LOG.severe("Not sure what sort of type declaration this is: " + op);
      body = null;
    }
    return body;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation#deriveChild(edu.cmu.cs.fluid.version.Version, edu.cmu.cs.fluid.version.Version)
   */
  @Override
  protected void deriveChild(Version parent, Version child)
      throws UnavailableException {
    deriveRelated(parent,child);
  }
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.version.VersionedDerivedInformation#deriveParent(edu.cmu.cs.fluid.version.Version, edu.cmu.cs.fluid.version.Version)
   */
  @Override
  protected void deriveParent(Version child, Version parent)
      throws UnavailableException {
    deriveRelated(child,parent);
  }
  
  protected void deriveRelated(Version from, Version to) {
    Version changeVersion = to;
    if (from.parent() == to) changeVersion = from;
    
    Version.saveVersion(from);
    try {
      new UpdateTableVisitor(true,to,changeVersion).doAccept(getClassBody());
    } finally {
      Version.restoreVersion();
    }
    
    Version.saveVersion(to);
    try {
      new UpdateTableVisitor(false,from,changeVersion).doAccept(getClassBody());
    } finally {
      Version.restoreVersion();
    }
   }
  

  @Override
  public synchronized void clear() {
    super.clear();
    entries.clear();
  }
  
  /**
   * The incremental algorithm:
   * we go into the class body looking for named declarations.
   * If at any point the node we are looking at (a child) has a changed
   * parent, then starting here and below, we work non-incrementally.
   * If a named declaration has a changed name, then we also work
   * non-incrementally.
   * When non-incremental, if we encounter a named declaration,
   *  we add/remove it.
   *  
   * Note: since this operates purely on the IR, its work could 
   * actually be shared between different parameterizations
   */
  private class UpdateTableVisitor extends Visitor<Void> {
    private Version changeVersion; // notification version
    private Version effectiveVersion;
    private Version comparisonVersion; // not the current version
    private boolean isRemoval;
    private boolean isIncremental = true;
    
    /**
     * Create an 'add'  or removal update.  Either
     * <dl>
     * <dt>removal = false</dt><dd>adding things that appear in the current
     * version but not in the other version. Changes are effective
     * in the current version. </dd>
     * <dt>removal=true</dt><dd> removing things that appear in the current
     * version but not in the other version. Changes are effective in
     * the other version.
     * </ul>
     * Notifications will be done for
     * the given notification (change) version.
     * @param removal whether this is a removal pass
     * @param otherVersion version against which we compare nodes
     * @param notificationVersion version to notify users for
     */
    public UpdateTableVisitor(boolean removal, Version otherVersion, Version notificationVersion) {
      changeVersion = notificationVersion;
      effectiveVersion = removal ? otherVersion : Version.getVersion();
      comparisonVersion = otherVersion;
      isRemoval = removal;
      if (otherVersion == null) isIncremental = false;
    }
    
    @Override
    public Void doAccept(IRNode node) {
      if (node == null) return null;
      // this is called on a node before we've decided what sort of node it is.
      // we use incrementality to skip it if it hasn't changed
      if (!isIncremental) return super.doAccept(node);
      if (JavaIncrementalBinder.parentIsChanged(node,comparisonVersion)) {
        isIncremental = false;
      } else if (!JavaIncrementalBinder.treeChanged(node,comparisonVersion)) {
        return null; // nothing to do
      }
      super.doAccept(node);
      isIncremental = true;
      return null;
    }
    
    @Override
    public Void visitClassBody(IRNode node) {
      doAcceptForChildren(node);
      return null;
    }
    
    @Override
    public Void visitFieldDeclaration(IRNode node) {
      doAcceptForChildren(node);
      return null;
    }
    
    @Override
    public Void visitVariableDeclarators(IRNode node) {
      doAcceptForChildren(node);
      return null;
    }
    
    
    private void visitNamedDeclaration(IRNode node) {
      if (!isIncremental || JavaIncrementalBinder.infoIsChanged(node,comparisonVersion)) {
        String name = JJNode.getInfo(node);
        /*
        if (type != null && type.getName().endsWith("CDRInputStream_1_0")) {
        	System.out.println("CDRInputStream_1_0: "+node);
        	System.out.println();
        }
        */
        if (isRemoval) {
          removeDeclaration(name,node,effectiveVersion,changeVersion);
        } else {
          addDeclaration(name,node,changeVersion);
        }
      }
    }
    
    @Override
    public Void visitSomeFunctionDeclaration(IRNode node) {
      visitNamedDeclaration(node);
      return null;
    }
    @Override
    public Void visitTypeDeclaration(IRNode node) {
      visitNamedDeclaration(node);
      return null;
    }
    @Override
    public Void visitVariableDeclarator(IRNode node) {
      visitNamedDeclaration(node);
      return null;
    }
    @Override
    public Void visitEnumConstants(IRNode node) {
      doAcceptForChildren(node);
      return null;
    }
    @Override
    public Void visitEnumConstantDeclaration(IRNode node) {
      visitNamedDeclaration(node);
      return null;
    }
    @Override
    public Void visitAnnotationElement(IRNode node) {
      visitNamedDeclaration(node);
      return null;
    }
  }
  
  
  /**
   * A table from names to Entries. This table is is externally immutable
   */
  private final HashMap<String,Entry> entries = new HashMap<String,Entry>();

  /**
   * Get entry associated with the name.
   * 
   * @param name
   * @return entry associated with the given name in the table
   */
  protected Entry getEntry(String name) {
    Entry entry;
    synchronized (entries) {
      entry = entries.get(name);
      if (entry == null) {
        if (isVersioned) {
          entry = new VersionedEntry();
        } else {
          entry = new UnversionedEntry();
        }
        entries.put(name, entry);
      }
    }
    return entry;
  }

  /**
   * Add a declaration for the (possibly versioned) declaration list for this name.
   * The change will be effected for the current version, which may not be
   * the notification version.  (It is either the notification version or the parent
   * of the notification version.)
   * @param name key to use in this table
   * @param decl declaration with this name.
   * @param v version at which to inform uses.
   */
  protected void addDeclaration(String name, IRNode decl, Version v) {
    Entry entry = getEntry(name);
    entry.addDeclaration(decl,v);
  }

  /**
   * Remove a declaration from the (possible versioned) declaration list for this name.
   * The change will be effective for the given version (possibly backwards
   * since bidirectional slots are used).  The uses are notified at the notification
   * version which always looks in the forward direction (the change is
   * from the parent of the notification version to the notification version).
   * @param name key to remove declaration from
   * @param decl declaration to remove
   * @param effective version to effect change for
   * @param notification version to notify users for
   */
  protected void removeDeclaration(String name, IRNode decl, Version effective, Version notification) {
    if (!isVersioned) {
      removeDeclaration(name,decl,null);
      return;
    }
    Version.saveVersion(effective);
    try {
      removeDeclaration(name,decl,notification);
    } finally {
      Version.restoreVersion();
    }
  }
  
  protected void removeDeclaration(String name, IRNode decl, Version v) {
    Entry entry = getEntry(name);
    entry.removeDeclaration(decl,v);
  }

   /**
   * Return the declarations that have the given name in this scope. The use
   * node is left in a (currently unversioned) dynamic dependency. 
   * @param name
   *          string to use as a key
   * @param useNode
   *          use site on whose behalf lookup is done; null if no dynamic
   *          dependency needed
   * @return
   */
  public Iterator<IRNode> getDeclarationsFromUse(String name, IRNode useNode) {
	if (isVersioned) {
		ensureDerived();
	}
    Entry entry = getEntry(name);
    if (useNode != null) {
      entry.addUse(useNode);
    }
    synchronized (this) {
    	// Added to prevent comod exceptions
    	Iterator<IRNode> it = entry.getDeclarations();
    	if (!it.hasNext()) {
    	  return new EmptyIterator<IRNode>();
    	}
    	List<IRNode> l = new ArrayList<IRNode>();
    	while (it.hasNext()) {
    		l.add(it.next());
    	}
    	return l.iterator();
    }
  }

  /**
   * Notify all uses of this member table that there is
   * a major change (usually a change in superclass)
   */
  public void notifyAllUses() {
    notifyAllUses(Version.getVersion());
  }  
  /**
   * Notify all uses of this member table that there is a major change
   * (usually a change in superclass).
   * @param v version where change happened.
   */
  public void notifyAllUses(Version v) {
    synchronized (entries) {
      for (Iterator<Entry> it = entries.values().iterator(); it.hasNext();) {
        Entry entry = it.next();
        entry.notifyUses(v);
      }
    }
  }
  
  protected static interface Entry extends JavaIncrementalBinder.IVersionedDependency, Iterable<IRNode> {
    Iterator<IRNode> getDeclarations();
    void addDeclaration(IRNode decl, Version v);
    void removeDeclaration(IRNode decl, Version v);
  }
  
  protected class VersionedEntry extends JavaIncrementalBinder.Dependency
  implements Entry 
  {
    final IRSequence<IRNode> declarations = factory.newSequence(~0);


    /**
     * Return the declarations with the name for this entry
     * 
     * @return enumeration of all declarations entered for this class with the
     *         given name.
     */
    public Iterator<IRNode> getDeclarations() {
      return declarations.elements();
    }

    /**
     * Locate where a declaration exists in the declarations sequence. This
     * should be reasonably fast unless all the methods in the class have the
     * same name.
     * 
     * @param decl
     * @return
     */
    protected IRLocation findDeclaration(IRNode decl) {
      for (IRLocation loc = declarations.firstLocation(); loc != null; loc = declarations
          .nextLocation(loc)) {
        if (declarations.elementAt(loc).equals(decl)) return loc;
      }
      return null;
    }

    /**
     * Add a declaration to the list in the entry. If already there do nothing.
     * Uses are notified that there is a change at v (unless null).
     * 
     * @param decl
     * @param v version at which change should be record.  (null if no such version)
     */
    public void addDeclaration(IRNode decl, Version v) {
      if (findDeclaration(decl) != null) return;
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine(JavaMemberTable.this + ": Adding " + decl + " '" + JJNode.getInfoOrNull(decl) + "' in " + Version.getVersion() + " recorded in " + v);
      }
      declarations.appendElement(decl);
      notifyUses(v);
    }
    
    /**
     * Remove a declaration from the list and notify all uses
     * that there is a change at version v (unless null).
     * 
     * @param decl
     */
    public void removeDeclaration(IRNode decl, Version v) {
      IRLocation loc = findDeclaration(decl);
      if (loc == null) return;
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine(JavaMemberTable.this + ": Removing " + decl + " '" + JJNode.getInfoOrNull(decl) + "' in " + Version.getVersion() + " recorded in " + v);
      }
      declarations.removeElementAt(loc);
      notifyUses(v);
    }

    public Iterator<IRNode> iterator() {
      return declarations.elements();
    }
  }
  
  /**
   * Entries for unversioned tables: used for a batch process and
   * then discarded.
   */
  protected static class OldUnversionedEntry extends AbstractJavaBinder.UnversionedDependency
  implements Entry 
  {
    private Vector<IRNode> decls = new Vector<IRNode>();
    public void addDeclaration(IRNode decl, Version v) {
      decls.add(decl);
    }
    public Iterator<IRNode> getDeclarations() {
      return decls.iterator();
    }
    public void removeDeclaration(IRNode decl, Version v) {
      decls.remove(decl);
    }
    
    // no use links
    public synchronized void notifyUses(Version v) { }

    public Iterator<IRNode> iterator() {
      return decls.iterator();
    }
  }   
  
  /**
   * Designed to eliminate the Vector in most cases,
   * at the cost of keeping a firstDecl field
   */
  protected static class UnversionedEntry extends AbstractJavaBinder.UnversionedDependency
  implements Entry 
  {
    private IRNode firstDecl     = null;
    private Vector<IRNode> decls = null; //new Vector<IRNode>();
    public void addDeclaration(IRNode decl, Version v) {
      if (firstDecl == null) {
        firstDecl = decl;
        return;
      }
      if (decls == null) {
        decls = new Vector<IRNode>();
        decls.add(firstDecl);
      }
      decls.add(decl);     
    }
    public Iterator<IRNode> getDeclarations() {
      if (decls == null) {
        if (firstDecl == null) {
          return new EmptyIterator<IRNode>();
        }
        return new SingletonIterator<IRNode>(firstDecl);
      }
      return decls.iterator();
    }
    public void removeDeclaration(IRNode decl, Version v) {
      if (decls == null) {
        firstDecl = decl.equals(firstDecl) ? null : firstDecl;
      } else {
        decls.remove(decl);
        if (decls.isEmpty()) {
          firstDecl = null;
          decls = null;
        } else {
          firstDecl = decls.get(0);
        }
      }
    }
    
    // no use links
    public synchronized void notifyUses(Version v) { }

    public Iterator<IRNode> iterator() {
      return getDeclarations();
    }
  }
  
  /**
   * Return a scope object that represents the table.
   * It is valid in any version that the member table itself is valid.
   * In other words, it is versioned if the member table itself is versioned.
   *@param binder binder to use on demand to look up super types.
   */
  public IJavaScope asScope(AbstractJavaBinder binder) {
	  if (binder != cachedBinder) {
        cachedFullScope = new IJavaScope.ExtendScope(asLocalScope(binder.getTypeEnvironment()),
        		                                     new SuperScope(binder));
        cachedBinder = binder;
	  }
      return cachedFullScope;    
  }
 
  public IJavaScope asSuperScope(AbstractJavaBinder binder) {
	  if (binder != cachedBinder) {
        cachedFullScope = new SuperScope(binder);
        cachedBinder = binder;
	  }
      return cachedFullScope;    
  }
  
  /**
   * Only used in asScope() above
   */
  private volatile IJavaScope cachedFullScope = null;
  private volatile IBinder cachedBinder = null;
  
  /**
   * Return a scope that only mimics the member table, not looking at superclasses.
   * @return scope for members in this table.
   */
  public Scope asLocalScope(ITypeEnvironment tEnv) {
    return new Scope(tEnv);
  }
  
  /**
   * A class to handle binding that are inherited by this class.
   * It may look rather inefficient (re-iterating through the supertypes each time),
   * and probably is.  But to make it more efficient, one would need to cache the
   * super scopes which has major implications for versioning.  If this class is indeed
   * a bottleneck, we will have to investigate caching the versioned scopes.
   * @author boyland
   */
  private class SuperScope implements IJavaScope {
    final boolean isTypeFormal;
    final AbstractJavaBinder binder;
    final List<IJavaType> superTypes = new ArrayList<IJavaType>();
    
    public SuperScope(AbstractJavaBinder b) {
      binder       = b;
      isTypeFormal = TypeFormal.prototype.includes(typeDeclaration);
    }
    
	public boolean canContainPackages() {
		return false;
	}    
    
    private Iteratable<IJavaType> getSuperTypes_internal() {
      IJavaType thisType;
      if (isTypeFormal) {
        thisType = JavaTypeFactory.getTypeFormal(typeDeclaration);
      } 
      else if (type != null) {
        thisType = type;
      }
      else {
        thisType = JavaTypeFactory.getDeclaredType(typeDeclaration,null,null);
      }
      return thisType.getSupertypes(binder.getTypeEnvironment());
    }

    /**
     * Added caching
     */
    private synchronized Iteratable<IJavaType> getSuperTypes() {
    	if (superTypes.isEmpty()) {
    		for(IJavaType st : getSuperTypes_internal()) {
    			superTypes.add(st);
    		}
    	}
    	return new SimpleIteratable<IJavaType>(superTypes.iterator());
    }
    
    /**
     * Checks if the lookup makes no sense because:
     * 1. it's trying to use the supertype to lookup itself
     */
    private boolean isNonsensicalLookup(IRNode useSite) {
    	if (useSite == null) {
    		return false;
    	}
    	final IRNode type = VisitUtil.getEnclosingType(useSite);
    	if (!typeDeclaration.equals(type)) {
    		// The use isn't in this type itself
    		return false;
    	}		
    	for(IRNode n : VisitUtil.getSupertypeNames(type)) {
    		if (inSubtree(useSite, n)) {
    			return true;
    		}
    	}
    	return false;
	}
    
    private boolean inSubtree(final IRNode nodeToFind, IRNode here) {
    	if (nodeToFind.equals(here)) {
    		return true;
    	}
    	for(IRNode n : JJNode.tree.children(here)) {
    		if (inSubtree(nodeToFind, n)) {
    			return true;
    		}
    	}
		return false;
	}

	/* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.project.JavaScope#lookup(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.project.JavaScope.Selector)
     */
    public IBinding lookup(String name, IRNode useSite, Selector selector) {
      if (isNonsensicalLookup(useSite)) {
    	  return null;
      }
      //LOG.fine("Looking in superclasses for object/type: " + name);
      for (IJavaType st : getSuperTypes()) {
      	/*
        if (st instanceof IJavaDeclaredType) {
    		IJavaDeclaredType sdt = (IJavaDeclaredType) st;
    		if (sdt.getDeclaration().identity() == IRNode.destroyedNode) {
    			getSuperTypes();
    		}
    	}
        */
        // XXX: BUG: need to mark this useSite somehow as dependent on the lookup here.
        IJavaScope scope = binder.typeScope(st);
        if (scope == null) {
        	continue;
        }
        IBinding binding = scope.lookup(name,useSite,selector);
        if (binding != null) return binding;
      }
      return null;
    }

	/* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.project.JavaScope#lookupAll(java.lang.String, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.java.project.JavaScope.Selector)
     */
    public Iteratable<IBinding> lookupAll(String name, IRNode useSite, Selector selector) {
      Iteratable<IBinding> firstResult = null;
      List<IBinding> allResults = null;
      //LOG.fine("Looking in superclasses for method: " + name);
      for (IJavaType superType : getSuperTypes()) {
        /*
    	if (superType instanceof IJavaDeclaredType) {
    		IJavaDeclaredType sdt = (IJavaDeclaredType) superType;
    		if (sdt.getDeclaration().identity() == IRNode.destroyedNode) {
    			getSuperTypes();
    		}
    	}
        */
        // XXX: BUG: need to mark this useSite somehow as dependent on the lookup here.    	
        if (LOG.isLoggable(Level.FINEST)) {
          LOG.finest("Super of " + JJNode.getInfo(typeDeclaration) + " is " + superType);
        }
        IJavaScope scope = binder.typeScope(superType);
        Iteratable<IBinding> result = scope.lookupAll(name,useSite,selector);
        if (result.hasNext()) {
          if (firstResult == null) { // common case
            firstResult = result;
          } else {
            if (allResults == null) {
              // FIX should this be a set?
              allResults = new ArrayList<IBinding>();
              while (firstResult.hasNext()) {
                allResults.add(firstResult.next());
              }
            }
            while (result.hasNext()) {
              allResults.add(result.next());
            }
          }
        }
      }
      if (allResults != null) {
        return IteratorUtil.makeIteratable(allResults);
      } else if (firstResult != null) {
        return firstResult;
      } else {
        return IJavaScope.EMPTY_BINDINGS_ITERATOR; 
      }
    }

    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "[SuperScope for "+JavaNames.getFullTypeName(typeDeclaration)+"]");
      for (IJavaType superType : getSuperTypes()) {
        IJavaScope scope = binder.typeScope(superType);
        if (scope != null) {
        	scope.printTrace(out, indent+2);
        } else {
        	DebugUtil.println(out, indent+2, "Null for "+superType);
        }
      }
      DebugUtil.println(out, indent, "[End SuperScope for "+JavaNames.getFullTypeName(typeDeclaration)+"]");
    }
  }
  
  public class Scope implements IJavaScope {
	final ITypeEnvironment tEnv;

	protected Scope(ITypeEnvironment te) {
    	tEnv = te;
    }
    
	public boolean canContainPackages() {
		return false;
	}   
	
    public IBinding lookup(String name, final IRNode useSite, final Selector selector) {
      final boolean debug = LOG.isLoggable(Level.FINER);
      if (debug) LOG.fine("Looking for " + name + " in " + this);
      synchronized (JavaMemberTable.this) {
    	  if (isVersioned) {
    		  JavaMemberTable.this.ensureDerived();
    	  }      
    	  Iterator<IRNode> members = getDeclarationsFromUse(name,useSite);
    	  while (members.hasNext()) {
    		  IRNode n = members.next();
    		  if (debug) LOG.finer("  considering " + DebugUnparser.toString(n));
    		  if (selector.select(n) && 
    				  (TypeDeclaration.prototype.includes(n) || BindUtil.isAccessible(tEnv, n, useSite))) {
    			  return IBinding.Util.makeBinding(n, (IJavaDeclaredType) JavaMemberTable.this.type, tEnv);
    		  }
    	  }
      }
      return null;
    }
    
    private List<IRNode> copyIterator(Iterator<IRNode> it) {
    	if (!it.hasNext()) {
    		return Collections.emptyList();
    	}
    	final List<IRNode> temp = new ArrayList<IRNode>();
    	while (it.hasNext()) {
    		IRNode n = it.next();
    		temp.add(n);
    	}
    	return temp;
    }
    
    public Iteratable<IBinding> lookupAll(String name, final IRNode useSite, final Selector selector) {
      final boolean debug = LOG.isLoggable(Level.FINER);
      if (debug) {
        LOG.finer("Looking for all " + name + " in " + this);
        //new FluidError("no error").printStackTrace();
      }
      List<IRNode> tempMembers;
      synchronized (JavaMemberTable.this) {
    	  if (isVersioned) {
    		  JavaMemberTable.this.ensureDerived();      
    	  }
    	  Iterator<IRNode> members = getDeclarationsFromUse(name,useSite);
    	  if (!JJNode.versioningIsOn && !members.hasNext() && 
    			  !repopulated) {   
    		  repopulated = true;
    		  if (debug) {
    			  String context = JJNode.getInfoOrNull(typeDeclaration);
    			  if (context == null) {
    				  context = DebugUnparser.toString(typeDeclaration);
    			  }
    			  LOG.finer("Re-populating member table: "+context);
    		  }
    		  populateDeNovo();
    		  
    		  members = getDeclarationsFromUse(name,useSite);
    	  }
    	  tempMembers = copyIterator(members);
      }
      if (!tempMembers.isEmpty()) {
          /*
          if ("getCurrentKey".equals(name)) {
        	  System.out.println("Found getCurrentKey");
          }
          */
    	  return new FilterIterator<IRNode, IBinding>(tempMembers.iterator()) {
    		  @Override
    		  public Object select(IRNode n) {
    			  if (selector.select(n)) {
    				  if (debug) {
    					  LOG.finer("Selected node from " + Scope.this);
    				  }
    				  // No context type? (filled in later?)
    				  return IBinding.Util.makeBinding(n);
    			  }
    			  return noElement;
    		  }
    	  };        
      }
      return EMPTY_BINDINGS_ITERATOR;
    }
    
    @Override
    public String toString() {
      return JavaMemberTable.this + "$Scope";
    }

    public void printTrace(PrintStream out, int indent) {
      DebugUtil.println(out, indent, "["+this+"]");
      for(Map.Entry<String, Entry> e : entries.entrySet()) {
        String prefix = e.getKey()+" = ";
        boolean declared = false;
        for(IRNode decl : e.getValue()) {
          DebugUtil.println(out, indent+2, prefix+DebugUnparser.toString(decl));
          declared = true;          
        }
        if (!declared) {
          DebugUtil.println(out, indent+2, prefix+"???");
        }
      }
    }
  }
  
  @Override
  public String toString() {
    return "JavaMemberTable(" + JavaNames.getFullTypeName(typeDeclaration)+ ")";
  }
  
  @Override
  public String debugString() {
    StringBuilder sb = new StringBuilder();
    sb.append(this);
    sb.append('\n');
    for (Map.Entry<String,Entry> e : entries.entrySet()) {
      sb.append("  " + e.getKey() + ":");
      for (Iterator<IRNode> it = e.getValue().getDeclarations(); it.hasNext();) {
        IRNode n = it.next();
        sb.append(' ');
        sb.append(n);
      }
      sb.append('\n');
    }
    return sb.toString();
  }
}
