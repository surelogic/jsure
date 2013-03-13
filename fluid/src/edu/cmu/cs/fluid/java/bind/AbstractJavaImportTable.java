/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/AbstractJavaImportTable.java,v 1.25 2008/08/22 16:56:33 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.io.PrintStream;
import java.util.*;
import java.util.logging.*;

import com.surelogic.*;
import com.surelogic.common.Pair;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.debug.DebugUtil;
import edu.cmu.cs.fluid.derived.IDerivedInformation;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A table representing a Java import list mapping names to declarations.
 * We have one of these for each compilation unit.  
 * <p>
 * @author boyland
 * @author Edwin.Chan
 */
@Region("protected ImportState")
@RegionLock("StateLock is this protects ImportState")
public abstract class AbstractJavaImportTable implements IJavaScope {
  protected static final Logger LOG = SLLogger.getLogger("FLUID.java.bind");

  protected final IDerivedInformation info;
  protected final AbstractJavaBinder binder;
  protected final ITypeEnvironment tEnv;
  protected final IRNode compilationUnit;
  
  /**
   * Create an empty import table
   * 
   * Subclass constructors need to call initialize()
   */
  @Unique("return")
  protected AbstractJavaImportTable(IRNode cu, AbstractJavaBinder b) {
    compilationUnit = cu;
    binder = b;
    tEnv = binder.getTypeEnvironment();
    info = makeInformation();    
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
  //final Map<IRNode,Entry> indirect = new ListMap<IRNode,Entry>(); // for debugging

  @UniqueInRegion("ImportState")
  final Map<IRNode,Entry> indirectPackages = new HashMap<IRNode,Entry>();
  
  @RequiresLock("StateLock")
  private Iterable<Map.Entry<IRNode, Entry>> getIndirectEntries() {
	  return new AppendIterator<Map.Entry<IRNode,Entry>>(indirectPackages.entrySet().iterator(), 
			  indirect.entrySet().iterator());
	  //return indirect.entrySet();
  }  
  
  protected abstract IDerivedInformation makeInformation();
  
  @RequiresLock("StateLock")
  protected final void initialize() {
    // clear the current tables
    // very cheap, if tables are empty already!
    clearScopes(direct);
    clearScopes(indirect);
    addIndirect("java.lang",compilationUnit); // implicit "import java.lang.*;"
    Iterator<IRNode> enm = JJNode.tree.children(CompilationUnit.getImps(compilationUnit));
    while (enm.hasNext()) {
      IRNode importNode = (IRNode)enm.next();
      //System.out.println("Adding import: " + DebugUnparser.toString(importNode));
      addImport(importNode);
    }
  }
  
  @RequiresLock("StateLock")
  protected abstract void clearScopes(Map<?,Entry> map);
  
  /**
   * Add an import to the table.
   * @param importNode
   */
  protected final void addImport(IRNode importNode) {
    IRNode itemNode = ImportDeclaration.getItem(importNode);
    Pair<IJavaScope,String> pr = resolveImport(itemNode,importNode);
    IJavaScope scope = pr.first();
    String name = pr.second();
    if (scope == null) {
      LOG.info("addImport has no effect in "+tEnv+": " + DebugUnparser.toString(importNode));
      resolveImport(itemNode,importNode);
      return; // nothing to add
    }
    final Operator op = JJNode.tree.getOperator(itemNode);
    final boolean isPackageScope = scope instanceof IJavaPackageScope;
    if (op instanceof StaticImport || op instanceof StaticDemandName) {
      scope = new SelectedScope(scope,IJavaScope.Util.isStatic); // types too!
    } else {
      scope = new SelectedScope(scope,IJavaScope.Util.isTypeDecl);
    }
    synchronized (this) {
    	if (name == null) {
    		/*
      if (op instanceof ClassType) {
    	  LOG.warning("Unable to find "+itemNode);
    	  resolveImport(itemNode,importNode);
      }
    		 */
    		if (isPackageScope) {
    			addScope(indirectPackages, importNode, scope);
    		} else {
    			addScope(indirect,importNode,scope);
    		}
    	} else {
    		if (name.indexOf('.') >= 0) {
    			LOG.warning("Got qualified name: "+name);
    			resolveImport(itemNode,importNode);
    		}
    		addScope(direct,name,scope);
    	}
    }
  }

  protected final void addIndirect(String qName,IRNode useSite) {
	IJavaScope scope = resolveAsScope(qName,useSite);
	synchronized (this) {
		addScope(indirect,useSite,scope);
	}
  }
      
  @RequiresLock("StateLock")
  protected abstract <T> void addScope(Map<T,Entry> map, T key, IJavaScope scope);
  
  /**
   * Resolve an import item as a pair of scope and name.
   * The scope is a local scope (only accepting canonical names) and the name
   * is the name from the import, or null (if demand import is to be used).
   * @param node import item
   * @param useSite import declaration (parent of node)
   * @return non-null pair of scope and name
   */
  
  protected final Pair<IJavaScope,String> resolveImport(IRNode node, IRNode useSite) {
    Operator op = JJNode.tree.getOperator(node);
    if (op instanceof DemandName) {
      return new Pair<IJavaScope,String>(resolveAsScope(JJNode.getInfo(node),useSite),null);
    } else if (op instanceof StaticDemandName) {
      //System.out.println("Got "+DebugUnparser.toString(node));
      IRNode tNode = StaticDemandName.getType(node);
      return new Pair<IJavaScope,String>(resolveTypeAsScope(tNode,useSite,false),null);
    } else if (op instanceof NameType) {
      IRNode nameNode = NameType.getName(node);
      String name = JJNode.getInfo(nameNode);
      if (JJNode.tree.getOperator(nameNode) instanceof QualifiedName) {
        IRNode prefix = QualifiedName.getBase(nameNode);
        return new Pair<IJavaScope,String>(resolveNameAsScope(prefix,useSite,true),name);
      } else {
        //return new Pair<IJavaScope,String>(tEnv.getClassTable().packageScope(""),name);
    	return resolveNamedType(useSite, name);
      }
    } else if (op instanceof NamedType) {
      String qName = JJNode.getInfo(node);
      return resolveNamedType(useSite, qName);
    } else if (op instanceof TypeRef) {
      IRNode tNode = TypeRef.getBase(node);
      String name = JJNode.getInfo(node);
      IJavaScope sc = resolveTypeAsScope(tNode,useSite,true);
      if (sc == null) {
        resolveTypeAsScope(tNode,useSite,true);
      }
      return new Pair<IJavaScope,String>(sc,name);
    } else if (op instanceof StaticImport) {
      IRNode tNode = StaticImport.getType(node);
      String name = JJNode.getInfo(node);
      return new Pair<IJavaScope,String>(resolveTypeAsScope(tNode,useSite,false),name);
    } else if (op instanceof TypedDemandName) {
      IRNode tNode = TypedDemandName.getType(node);
      return new Pair<IJavaScope,String>(resolveTypeAsScope(tNode,useSite,true),null);
    } else {
      LOG.severe("Import not handled: " + op + " -- " + DebugUnparser.toString(node));
      return new Pair<IJavaScope,String>(null,null);
    }
  }

private Pair<IJavaScope, String> resolveNamedType(IRNode useSite, String qName) {
	int dot = qName.lastIndexOf('.');
      if (dot == -1) {
        return new Pair<IJavaScope,String>(tEnv.getClassTable().packageScope(""),qName);
      }
      String name = qName.substring(dot+1);
      String scopeName = qName.substring(0,dot);
      return new Pair<IJavaScope,String>(resolveAsScope(scopeName,useSite),name);
}
  
  private final IJavaScope resolveTypeAsScope(IRNode node, IRNode useSite, boolean asLocal) {
    Operator op = JJNode.tree.getOperator(node);
    if (op instanceof NameType) {
      return resolveNameAsScope(NameType.getName(node),useSite,asLocal);
    } else if (op instanceof NamedType) {
      return resolveAsScope(JJNode.getInfo(node),useSite);
    } else if (op instanceof TypeRef) {
      IJavaScope scope = resolveTypeAsScope(TypeRef.getBase(node),useSite,asLocal);
      LookupContext context = new LookupContext().use(JJNode.getInfo(node),useSite);
      IBinding tbind = IJavaScope.Util.lookupType(scope,context);
      if (tbind == null) return null;
      assert tbind.getContextType() == null;
      IJavaMemberTable table = getMemberTable(tbind);
      return asLocal ? table.asLocalScope(binder.typeEnvironment) : table.asScope(binder);
    } else {
      LOG.severe("Import from type not handled: " + DebugUnparser.toString(node));
      return null;
    }
  }
  
  private final IJavaScope resolveNameAsScope(IRNode node, IRNode useSite, boolean asLocal) {
    Operator op = JJNode.tree.getOperator(node);
    if (op instanceof QualifiedName) {
      IJavaScope scope = resolveNameAsScope(QualifiedName.getBase(node),useSite,asLocal);
      return resolveAsScope(scope,QualifiedName.getId(node),useSite,asLocal);
    } else {
      return resolveAsScope(JJNode.getInfo(node),useSite);
    }
  }

  /**
   * Resolve a nested name as a (local) scope.
   * In other words, we never look in inherited scopes.
   * @param scope scope to look in
   * @param name name of type or package in scope
   * @param useSite place on whose behalf we are looking up
   * @return scope for name (or null)
   */
  private final IJavaScope resolveAsScope(IJavaScope scope, String name, IRNode useSite, boolean asLocal) {
    LookupContext context = new LookupContext().use(name,useSite);
    IBinding pbind = IJavaScope.Util.lookupPackage(scope,context);
    if (pbind != null) {
      return tEnv.getClassTable().packageScope(pbind.getNode());
    }
    IBinding tbind = IJavaScope.Util.lookupType(scope,context);
    if (tbind == null) return null;
    IJavaMemberTable table = getMemberTable(tbind);
    return asLocal ? table.asLocalScope(binder.getTypeEnvironment()) : table.asScope(binder);
  }
  
  private IJavaMemberTable getMemberTable(IBinding tbind) {
	  IJavaType t = JavaTypeFactory.getMyThisType(tbind.getNode());
	  return binder.typeMemberTable((IJavaSourceRefType) t);
  }
  
  /**
   * Resolve a qualified name as a scope.
   * NB: looks up fully qualified name, not necessarily canonical names.
   * @param qName qualified name (may have dots)
   * @param useSite place ion whose behalf we are looking up
   * @return scope for qualified name.
   */
  private final IJavaScope resolveAsScope(final String qName, IRNode useSite) {
    // first try to resolve using the class table:
    IJavaClassTable classTable = tEnv.getClassTable();
    IRNode outerNode = classTable.getOuterClass(qName,useSite);
    // if that doesn't work, see if there are nested types involved
    if (outerNode == null) { 
      int dot = qName.lastIndexOf('.');
      if (dot == -1) return null;
      String name = qName.substring(dot+1);
      String scopeName = qName.substring(0,dot);
      IJavaScope scope = resolveAsScope(scopeName,useSite);
      if (scope == null) return null;
      return resolveAsScope(scope,name,useSite,true);
    }
    if (Util.isTypeDecl(outerNode)) { 
      IJavaType outerType = tEnv.getMyThisType(outerNode);
      return binder.typeScope(outerType);
    }
    if (IJavaScope.Util.isPackageDecl(outerNode)) { return classTable.packageScope(qName); }
    return null;
  } 
   
  protected static interface Entry extends AbstractJavaBinder.IDependency {
    IJavaScope getScope();
  }
  
  @Override
  public boolean canContainPackages() {
	  return false;
  } 
  
  @Override
  public final synchronized IBinding lookup(LookupContext context, Selector selector) {
	final String name = context.name;
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("Looking for " + name + " in import table.");
    }
    // Only if it's a simple name
    if (name.indexOf('.') < 0) {
      // first try direct import    
      {
    	  Entry entry = direct.get(name);
    	  if (entry != null) {
    		  IJavaScope scope = entry.getScope();
    		  if (scope != null) {
    			  IBinding b = scope.lookup(context, selector);
    			  if (b != null) {
    				  /*
        	  if (!selector.select(b.getNode())) {
        		  System.out.println("Didn't use selector on "+name);
        	  }
    				   */
    				  entry.addUse(context.useSite);
    				  return b;
    			  } else {
    				  //System.out.println("Didn't find "+name+" yet");
    			  }
    			  // Otherwise, keep looking
    		  }
    	  }    
      }
      
      // next try package    
      IRNode pdecl = CompilationUnit.getPkg(compilationUnit);
      String pName = "";
      if (NamedPackageDeclaration.prototype.includes(pdecl)) {
    	  pName = JJNode.getInfo(pdecl);
      }
      IJavaScope sc = tEnv.getClassTable().packageScope(pName);
      IBinding result = sc.lookup(context,selector);
      if (result != null) return result;
      
      // try all indirect (demand) imports    
      for (Map.Entry<IRNode,Entry> e : getIndirectEntries()) {
    	//IRNode key  = e.getKey();
    	Entry entry = e.getValue();
        IJavaScope scope = entry.getScope();
        if (scope == null) continue;
        // LOG.finer("Looking for " + name + " indirectly through " + scope);
        IBinding x = scope.lookup(context, selector);
        if (x != null) {
          final IRNode useSite = context.useSite;
          entry.addUse(useSite);
          if (BindUtil.isAccessible(binder.getTypeEnvironment(), x.getNode(),useSite)) {
        	  return x;
          }
        }
      }
    } else {
    	//System.out.println("Got qname: "+name);
    }
    
    // try top-level package, but not class (class in anonymous package)
    IRNode outer = tEnv.getClassTable().getOuterClass(name, context.useSite);
    if (outer != null && 
        JJNode.tree.getOperator(outer) instanceof NamedPackageDeclaration &&
        selector.select(outer)) {
      return IBinding.Util.makeBinding(outer);
    }
    
    // found nothing:
    return null;
  }
  @Override
  public final synchronized Iteratable<IBinding> lookupAll(LookupContext context, Selector selector) {
	final String name = context.name;	  
    final IRNode useSite = context.useSite;
	if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("Looking for " + name + " in import table.");
    }
    Iteratable<IBinding> rv = null;
    
    // first try direct import
    {
      Entry entry = direct.get(name);
      if (entry != null) {
        IJavaScope scope = entry.getScope();
        if (scope != null) {
          rv = scope.lookupAll(context, selector);
          if (rv != null && rv.hasNext()) {
              entry.addUse(useSite);
          } 
        }
      }
    }
    // No package level methods/constructors
    
    // try all indirect (demand) imports
    {
      for (Map.Entry<IRNode,Entry> e : getIndirectEntries()) {
       	//IRNode key  = e.getKey();
    	//System.out.println("Looking at import "+DebugUnparser.toString(e.getKey()));
       	Entry entry = e.getValue();
        IJavaScope scope = entry.getScope();
        if (scope == null) continue;
        // LOG.finer("Looking for " + name + " indirectly through " + scope);
        Iteratable<IBinding> x = scope.lookupAll(context, selector);
        if (x != null && x.hasNext()) {
          entry.addUse(useSite);
          if (rv != null) {
        	  rv = new AppendIterator<IBinding>(rv, x);
          } else {
        	  rv = x;
          }
        }
      }
    }
    // No outer methods and constructors
    
    // found nothing:
    if (rv == null) {
    	return EmptyIterator.prototype();
    }
    return rv;
  }
  
  @Override
  @Vouch("debug code")
  public void printTrace(PrintStream out, int indent) {
    DebugUtil.println(out, indent,"[Import table: "+this.compilationUnit+"]");
    for(String s : direct.keySet()) {
      DebugUtil.println(out, indent+2, s);
    }
    for(IRNode n : indirect.keySet()) {
      DebugUtil.println(out, indent+2, DebugUnparser.toString(n));
    }
  }
}
