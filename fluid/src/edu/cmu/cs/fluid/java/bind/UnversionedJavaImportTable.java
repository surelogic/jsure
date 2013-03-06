/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/UnversionedJavaImportTable.java,v 1.7 2008/09/08 18:59:29 chance Exp $
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import edu.cmu.cs.fluid.derived.*;
import edu.cmu.cs.fluid.ir.*;
import com.surelogic.Unique;
import com.surelogic.RequiresLock;

/**
 * A table representing a Java import list mapping names to declarations.
 * We have one of these for each compilation unit.  
 * <p>
 * @author Edwin.Chan
 */
public final class UnversionedJavaImportTable extends AbstractJavaImportTable {  
  /**
   * Create an empty import table
   */
  @Unique("return")
  private UnversionedJavaImportTable(IRNode cu, UnversionedJavaBinder b) {
    super(cu, b);
    initialize();
  }
  
  @Override
  protected IDerivedInformation makeInformation() {
    return new UnversionedInfo();
  }
  
  private static final Map<IRNode,UnversionedJavaImportTable> importTables = 
    new ConcurrentHashMap<IRNode,UnversionedJavaImportTable>();
  
  public static UnversionedJavaImportTable getImportTable(IRNode cu, UnversionedJavaBinder b) {
	  UnversionedJavaImportTable jit = importTables.get(cu);
	  if (jit == null || jit.binder != b) {
		  jit = new UnversionedJavaImportTable(cu,b);
		  importTables.put(cu,jit);		  
	  } 
	  jit.info.ensureDerived();
	  return jit;
  }

  public static boolean clear(IRNode cu) {
    return (importTables.remove(cu) != null);
  }
  
  public static void clearAll() {
    importTables.clear();
  }

  private class UnversionedInfo extends AbstractDerivedInformation {
    @Override
    public void clear() {
    	synchronized (UnversionedJavaImportTable.this) {
    		super.clear();
    		direct.clear();
    		indirect.clear();
    	}
    }
    
	@Override
    protected boolean derive() {
	  synchronized (UnversionedJavaImportTable.this) {
		  initialize();
	  }
      return true;
    }    
  }
  
  @Override
  protected void clearScopes(Map<?,Entry> map) {
    map.clear();
  }

  @Override
  protected <T> void addScope(Map<T,Entry> map, T key, IJavaScope scope) {
    UnversionedEntry entry = (UnversionedEntry) map.get(key);
    if (entry == null) {
      entry = new UnversionedEntry(scope);
      map.put(key,entry);
      return;
    }
    IJavaScope currentScope = entry.getScope();
    if (currentScope == null) {
    	entry.setScope(scope);
    } else {
    	// Added to handle multiple static imports of the same identifier
    	entry.setScope(new IJavaScope.ExtendScope(currentScope, scope));
    }
  }

  class UnversionedEntry extends AbstractJavaBinder.UnversionedDependency implements Entry {
    IJavaScope scope;

    UnversionedEntry(IJavaScope sc) {
      scope = sc;
    }
    
    void setScope(IJavaScope sc) {
      scope = sc;
    }
    
    @Override
    public IJavaScope getScope() {
      return scope;
    }
  }
}
