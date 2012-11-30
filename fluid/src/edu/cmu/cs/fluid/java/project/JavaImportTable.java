/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/project/JavaImportTable.java,v 1.22 2008/09/08 17:55:23 chance Exp $
 */
package edu.cmu.cs.fluid.java.project;

import java.util.*;

import com.surelogic.*;
import com.surelogic.common.Pair;

import edu.cmu.cs.fluid.derived.IDerivedInformation;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.version.*;


/**
 * A table representing a Java import list mapping names to declarations.
 * We have one of these for each compilation unit.  
 * <p>
 * TODO: Move to fluid.java.bind ?
 * @author boyland
 */
public final class JavaImportTable extends AbstractJavaImportTable {
  protected final ExplicitSlotFactory factory;
  
  /**
   * Create an empty import table
   */
  @Unique("return")
  private JavaImportTable(IRNode cu, JavaIncrementalBinder b) {
    super(cu, b);
    final Version v = Version.getVersion();
    factory = VersionedSlotFactory.bidirectional(v);
    initialize();
  }
  
  @Override
  protected IDerivedInformation makeInformation() {
    return new VersionedInfo(Version.getVersion());
  }
  
  private static final Map<IRNode,JavaImportTable> importTables = new HashMap<IRNode,JavaImportTable>();
  public static JavaImportTable getImportTable(IRNode cu, JavaIncrementalBinder b) {
    JavaImportTable jit;
	synchronized (JavaImportTable.class) {
		jit = importTables.get(cu);
		if (jit == null) {
			jit = new JavaImportTable(cu,b);
			importTables.put(cu,jit);
		}
	}
    jit.info.ensureDerived();
    return jit;
  }

  public static synchronized void clearAll() {
    importTables.clear();
  }
  
  /**
   * The classes etc thare are imported by name.
   * A map of strings to Entry objects that hold the scope that
   * the name should be looked up in (again)
   */
  @UniqueInRegion("ImportState")
  final Map<String,Entry> direct = new HashMap<String,Entry>();
  
  /**
   * The scopes that are imported.  These implement the
   * imports that end in ".*".
   * A map from the import to an Entry object.
   */
  @UniqueInRegion("ImportState")
  final Map<IRNode,Entry> indirect = new HashMap<IRNode,Entry>();
  
  private class VersionedInfo extends VersionedDerivedInformation {
    private VersionedInfo(Version v) {
      super(v);
    }
    @Override
    public synchronized void clear() {
      // TODO Is this synchronization right
      super.clear();
      direct.clear();
      indirect.clear();
      // factory = null;
    }

    @Override
    protected void deriveChild(Version parent, Version child) {
      deriveRelated(parent, child);
    }
    @Override
    protected void deriveParent(Version child, Version parent) {
      deriveRelated(child, parent);
    }

    protected Iterator<IRNode> changeIterator(IRNode node, Version v1, Version v2) {
      return TreeChangedIterator.iterator(JavaIncrementalBinder.treeChanged,JJNode.tree,node,v1,v2);
    }
    
    @RequiresLock("StateLock")
	protected void deriveRelated(Version oldV, Version newV) {
      Version.saveVersion(newV);
      try {
        IRNode imports = CompilationUnit.getImps(compilationUnit);
        // if a whole new node, start from scratch:
        if (JavaIncrementalBinder.parentIsChanged(imports,oldV)) {
          initialize();
          return;
        }
        // otherwise, find all removed nodes, and clear them:
        Iterator changedOld = changeIterator(imports,newV,oldV);
        Version.saveVersion(oldV);
        try {
          while (changedOld.hasNext()) {
            IRNode importNode = (IRNode) changedOld.next();
            removeImport(importNode,newV);
          }
        } finally {
          Version.restoreVersion();
        }
        Iterator changedNew = changeIterator(imports,oldV,newV);
        while (changedNew.hasNext()) {
          IRNode importNode = (IRNode)changedOld.next();
          addImport(importNode);
        }
      } finally {
        Version.restoreVersion();
      }
    }
  }
  
  @Override
  protected void clearScopes(Map<?,Entry> map) {
    Version v = Version.getVersion();
    for (Iterator<Entry> it = map.values().iterator(); it.hasNext();) {
      VersionedEntry entry = (VersionedEntry)it.next();
      entry.setScope(null,v);
    }
  }
  
  private void removeImport(IRNode importNode, Version v) {
    IRNode itemNode = ImportDeclaration.getItem(importNode);
    Pair<IJavaScope,String> pr = resolveImport(itemNode,importNode);
    IJavaScope scope = pr.first();
    String name = pr.second();
    if (scope == null) {
      LOG.info("removeImport has no effect: " + DebugUnparser.toString(importNode));
      return; // nothing to remove
    }
    synchronized (this) {
    	if (name == null) {
    		removeScope(indirect,importNode,v);
    	} else {
    		removeScope(direct,name,v);
    	}
    }
  }
      
  @Override
  protected <T> void addScope(Map<T,Entry> map, T key, IJavaScope scope) {
    VersionedEntry entry = (VersionedEntry) map.get(key);
    if (entry == null) {
      entry = new VersionedEntry(scope);
      map.put(key,entry);
      return;
    }
    IJavaScope currentScope = entry.getScope();
    if (currentScope == null) {
        entry.setScope(scope, Version.getVersion());
    } else {
    	// Added to handle multiple static imports of the same identifier
    	entry.setScope(new IJavaScope.ExtendScope(currentScope, scope), Version.getVersion());
    }
  }
  
  @RequiresLock("StateLock")
  private <T> void removeScope(Map<T,Entry> map, T key, Version v) {
    VersionedEntry entry = (VersionedEntry) map.get(key);
    entry.setScope(null,v);
  }

  class VersionedEntry extends JavaIncrementalBinder.Dependency implements Entry {
    VersionedDerivedSlot<IJavaScope> scopeSlot;
    {
      Slot<IJavaScope> s = factory.undefinedSlot();
      scopeSlot = (VersionedDerivedSlot<IJavaScope>) s;
    }
    VersionedEntry(IJavaScope scope, Version v) {
      scopeSlot = scopeSlot.setValue(scope,v);
    }
    VersionedEntry(IJavaScope scope) {
      this(scope,Version.getVersion());
    }
    
    void setScope(IJavaScope sc, Version v) {
      scopeSlot = scopeSlot.setValue(sc,v);
    }
    
    public IJavaScope getScope() {
      return scopeSlot.getValue();
    }
  }
}
