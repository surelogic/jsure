/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/UnversionedJavaBinder.java,v 1.40 2008/10/27 15:26:44 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

import org.apache.commons.collections15.MultiMap;

import com.surelogic.InRegion;
import com.surelogic.analysis.ConcurrentAnalysis;
import com.surelogic.common.concurrent.ConcurrentHashSet;
import com.surelogic.common.concurrent.ConcurrentMultiHashMap;
import com.surelogic.dropsea.ir.drops.PackageDrop;
import com.surelogic.javac.persistence.JSurePerformance;

import edu.cmu.cs.fluid.derived.*;
import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.project.JavaMemberTable;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import extra166y.Ops.Procedure;

public class UnversionedJavaBinder extends AbstractJavaBinder implements ICompUnitListener {
  private static final boolean cacheAllSourceTypes = true;
  
  /**
   * Helps to figure out what to invalidate after an AST is modified
   * 
   * TODO keep a "base" impl to clone for parameterizations?
   * TODO what about types that have no parameterizations? (waste of space)
   */
  private final MultiMap<IRNode, IJavaSourceRefType> sourceTypeParameterizations = cacheAllSourceTypes ?	  
	  new ConcurrentMultiHashMap<IRNode, IJavaSourceRefType>() {
      @Override
      protected Collection<IJavaSourceRefType> newCollection() {
    	  return new ConcurrentHashSet<IJavaSourceRefType>();
      }
  } : null;
	
  private final ConcurrentMap<IJavaSourceRefType,IJavaMemberTable> memberTableCache = cacheAllSourceTypes ? 
      new ConcurrentHashMap<IJavaSourceRefType,IJavaMemberTable>() : null;
  
  
  private final Map<IRNode,IJavaMemberTable> oldMemberTableCache = cacheAllSourceTypes ? null :
	  new ConcurrentHashMap<IRNode, IJavaMemberTable>();
  
  private final ThreadLocal<IJavaMemberTable> objectTable =
	  new ThreadLocal<IJavaMemberTable>() {
	  protected IJavaMemberTable initialValue() {
		  return JavaMemberTable.makeBatchTable(typeEnvironment.getObjectType());
	  }
  };
  
  public UnversionedJavaBinder(final ITypeEnvironment tEnv) {
    super(tEnv);
    warnAboutPkgBindings = false;
    IDE.getInstance().addCompUnitListener(this);
  }
  
  private JavaCanonicalizer.IBinderCache cache = null;
  
  void setBinderCache(JavaCanonicalizer.IBinderCache c) {
	  cache = c;
  }
  
  @Override
  public IJavaType getJavaType(IRNode n) {
	  if (cache != null) {
		  IJavaType t = cache.checkForType(n);
		  if (t != null) {
			  return t;
		  }
	  }
	  return getJavaType_internal(n);
  }
  protected IJavaType getJavaType_internal(IRNode n) {  
	  return super.getJavaType(n);  
  }
  
  @Override
  protected IBinding getIBinding_impl(IRNode node) {
	  if (cache != null) {
		  IBinding b = cache.checkForBinding(node);
		  if (b != null) {
			  return b;
		  }	
	  }
	  try {
		  return super.getIBinding_impl(node);
	  }
	  catch (SlotUndefinedException e) {
		  LOG.log(Level.SEVERE, "Unable to get binding", e);
		  return null;
	  }
  }
  
  /*
  @Override
  public IRNode getBinding(IRNode node) {	  
    Operator op = JJNode.tree.getOperator(node);
    if (NamedType.prototype.includes(op)) {
      String name = NamedType.getType(node);
      IRNode td   = typeEnvironment.findNamedType(name);
      if (td != null) {
        return td;
      } else {
        try {
          //System.out.println("Binding "+DebugUnparser.toString(node));
          return super.getBinding(node);
        }
        catch (SlotUndefinedException e) {
          LOG.log(Level.SEVERE, "Unable to get binding", e);
          return null;
        }
      }
    }
    try {
      return super.getBinding(node);
    }
    catch (SlotUndefinedException e) {
      //LOG.log(Level.SEVERE, "Unable to get binding");//, e);
      return null;
    }
  }
  */
  
  public void astsChanged() {
	//System.out.println("Cleared out state for "+this);
    clearAll(true);
    JavaTypeFactory.clearCaches();
  }
  
  private static boolean clearBindings(Map<IRNode,IGranuleBindings> bindings) {
	  boolean isDeriving = false;
	  for(IGranuleBindings b : bindings.values()) {
		  if (b.isDeriving()) {
			  isDeriving = true;
		  } else {
			  b.destroy();
		  }
	  }
	  return isDeriving;
  }
  
  private synchronized void clearAll(boolean force) {
    //System.out.println("Clearing all bindings: force="+force);
	boolean isDeriving = clearBindings(partialGranuleBindings);
	isDeriving = clearBindings(allGranuleBindings) || isDeriving;
	if (force || !isDeriving) {
		//System.out.println("Cleared ALL");
		partialGranuleBindings.clear();
		allGranuleBindings.clear();
		UnversionedJavaImportTable.clearAll();
		if (cacheAllSourceTypes) {
			sourceTypeParameterizations.clear();
			memberTableCache.clear();
		} else {
			oldMemberTableCache.clear();
		}
		JavaMemberTable.clearAll();
	}
  }
  
  @Override
  protected void reset() {
	  clearAll(true);
  }
  
  /**
   * Only to be called after canonicalizing an AST
   */
  public synchronized void astChanged(IRNode cu) {
    // If so, we need to clear all the cached data
    for(final IRNode n : JJNode.tree.topDown(cu)) {
      final Operator op = JJNode.tree.getOperator(n);
      if (!isGranule(n, op)) {
        continue;
      }
      IGranuleBindings b1 = allGranuleBindings.remove(n);
      IGranuleBindings b2 = partialGranuleBindings.remove(n);
      boolean changed = false;
      if (UnversionedJavaImportTable.clear(n) || b1 != null || b2 != null) {
        // System.out.println("Cleared "+DebugUnparser.toString(n));      
    	  if (b1 != null && !b1.isDeriving()) {
    		  b1.destroy();
    	  }
    	  // TODO what to do if deriving, yet now obsolete
    	  if (b2 != null && !b2.isDeriving()) {
    		  b2.destroy();
    	  }
    	  changed = true;
      }
      if (TypeDeclaration.prototype.includes(n)) {
    	  changed |= removeStaleTables(n);
      }
      /*
      if (changed && TypeDeclaration.prototype.includes(op)) {
    	  String qname = JavaNames.getFullTypeName(n);
    	  //if (qname.startsWith("com.surelogic")) {
    		  System.out.println("Clearing bindings for "+qname);
    	  //}
      }
      */
    }
    JavaTypeFactory.clearCaches();
  }

  /**
   * @return true if removed
   */
  private boolean removeStaleTables(final IRNode tdecl) {
	  if (cacheAllSourceTypes) {
		  boolean removed = false;
		  final Collection<IJavaSourceRefType> types = sourceTypeParameterizations.remove(tdecl);
		  if (types != null) {
			  removed = true;
			  
			  final Map<IJavaSourceRefType,IJavaMemberTable> tables = memberTableCache;
			  final Procedure<Integer> proc = new Procedure<Integer>() {
				  public void op(Integer ignore) {
					  for(final IJavaSourceRefType t : types) {
						  tables.remove(t);
					  }
				  }
			  };
			  ConcurrentAnalysis.executeOnAllThreads(proc);
		  }
		  return removed;
	  }
	  return oldMemberTableCache.remove(tdecl) != null;
  }
  
  private boolean hasNoTypeParameters(IJavaDeclaredType t) {
	  if (t.getOuterType() != null) {
		  return t.getTypeParameters().isEmpty() && hasNoTypeParameters(t.getOuterType());
	  }
	  return t.getTypeParameters().isEmpty();
  }
  
  @Override
  public IJavaMemberTable typeMemberTable(IJavaSourceRefType type) {
	if (cacheAllSourceTypes) {
		if (type == typeEnvironment.getObjectType()) {
			return objectTable.get();
		}
		final Map<IJavaSourceRefType,IJavaMemberTable> tables = memberTableCache;
		IJavaMemberTable rv = tables.get(type);
		if (rv == null) {
			// unversioned, uncached
			rv = JavaMemberTable.makeBatchTable(type);
			
			// Record that it might need to be removed		
			tables.put(type, rv);
			sourceTypeParameterizations.put(type.getDeclaration(), type);
		}
		return rv;
	}
	// Original behavior
    if (type instanceof IJavaDeclaredType) {
      if (type == typeEnvironment.getObjectType()) {
    	  return objectTable.get();
      }
      // Limited caching due to cost of finding/removing stale info
      IJavaDeclaredType jt = (IJavaDeclaredType) type;
      if (hasNoTypeParameters(jt)) {
    	IRNode decl = jt.getDeclaration();
    	IJavaMemberTable mt = oldMemberTableCache.get(decl);
    	if (mt == null) {
    		mt = JavaMemberTable.makeBatchTable(type);
    		//System.err.println("Caching member table for "+type);
    		oldMemberTableCache.put(decl, mt);    		
    	}
    	return mt;    	
      }
    }
    /* Old
    else if (type instanceof IJavaTypeFormal) {
      IJavaMemberTable mt = memberTableCache.get(type.getDeclaration());
      if (mt == null) {
        mt = JavaMemberTable.makeBatchTable(type);
        memberTableCache.put(type.getDeclaration(), mt);
      }
      return mt;
    }
    */
    // unversioned, uncached
    return JavaMemberTable.makeBatchTable(type);
  }

  @Override
  public IJavaScope getImportTable(IRNode node) {
    return UnversionedJavaImportTable.getImportTable(node,this);
  }
  
  @Override
  public IGranuleBindings ensureBindingsOK(final IRNode node) {    
	  return super.ensureBindingsOK(node);
  }
  
  @Override
  protected IGranuleBindings makeGranuleBindings(IRNode cu) {
    return new CompUnitBindings(cu);
  }
  
  /**
   * The structure used to determine whether bindings are valid.
   * It includes the slots (since they have to be rooted here), but
   * it dos not do the binding action itself (this is done in
   * {@link #BinderVisitor}.
   * @author Edwin
   */
  class CompUnitBindings extends AbstractDerivedInformation implements IGranuleBindings {
    final IRNode unit;
    @InRegion("Status")
    private boolean hasFullInfo = false;
    @InRegion("Status")
    private boolean isDestroyed = false;
    
    /**
     * binding each use of a name to the declaration that it refers to.
     */
    final SlotInfo<IBinding> useToDeclAttr;
    /**
     * binding each method declaration to a set of methods that it overrides.
     */
    final SlotInfo<List<IBinding>> methodOverridesAttr;
    
    CompUnitBindings(IRNode cu) {
      unit = cu;
      SlotFactory f = SimpleSlotFactory.prototype;
      useToDeclAttr = f.newLabeledAttribute("CompUnitBindings.useToDecl");
      methodOverridesAttr = f.newLabeledAttribute("CompUnitBindings.methodOverrides", null);
    }

    public IRNode getNode() {
    	return unit;
    }
    
    public synchronized boolean isDestroyed() {
    	return isDestroyed;
    }
    
    public synchronized void destroy() {    	
    	useToDeclAttr.destroy();
    	methodOverridesAttr.destroy();
    	isDestroyed = true;
    }
    
    @Override
    protected String getLabel() {
    	String rv = JJNode.getInfoOrNull(unit);
    	if (rv == null) {
    		rv = DebugUnparser.toString(unit);
    	}
    	return rv;
    }
    
    public SlotInfo<List<IBinding>> getMethodOverridesAttr() {
      return methodOverridesAttr;
    }

    public SlotInfo<IBinding> getUseToDeclAttr() {
      return useToDeclAttr;
    }

    @Override
    protected boolean derive() {
      deriveInfo(this, unit);
      
      // Check if this should have been destroyed
      Map<IRNode,IGranuleBindings> bindings;
      if (containsFullInfo()) {
    	  bindings = allGranuleBindings;
      } else {
    	  bindings = partialGranuleBindings;
      }
      if (this != bindings.get(unit)) {
    	  // Not available
    	  destroy();
      }
      return true;
    }

    public void ensureDerived(IRNode node) {
      if (node.valueExists(useToDeclAttr)) {
        return;
      }
      try {
    	  ensureDerived();
      } catch(DerivationException e) {    	  
    	  IRNode context = VisitUtil.getEnclosingStatement(node);
    	  if (context == null) {
    		  context = VisitUtil.getClosestClassBodyDecl(node);
    	  }
    	  final IRNode type = VisitUtil.getClosestType(node);
    	  System.out.println("DerivationException: "+DebugUnparser.toString(node));
    	  System.out.println("Node:                "+node);    	  
    	  //System.out.println("Use to decl SI:      "+useToDeclAttr);  
    	  System.out.println("Context:             "+DebugUnparser.toString(context));
    	  System.out.println("Context type:        "+JavaNames.getFullTypeName(type));
    	  System.out.println("Granule:             "+DebugUnparser.toString(getGranule(node)));
    	  System.out.println("Info:                "+(containsFullInfo() ? "Full" : "Partial"));
    	  throw e;
      }
    }
    
    public boolean containsFullInfo() {
      return hasFullInfo;
    }

    public void setContainsFullInfo(boolean full) {
      hasFullInfo = full;      
    }
  }
  
  public static void printStats(JSurePerformance perf) {
	  perf.setLongProperty("Binding.partial.time", partialTime);
	  perf.setLongProperty("Binding.full.time", fullTime);
	  perf.setLongProperty("Bindings.partial", numPartial);
	  perf.setLongProperty("Bindings.full", numFull);
	  partialTime = fullTime = 0;
	  AbstractJavaBinder.printStats();
	  BatchJavaTypeVisitor.printStats();
  }
  
  @Override
  protected BinderVisitor newBinderVisitor(IGranuleBindings cuBindings, IRNode gr) {
    return new UnversionedBinderVisitor(cuBindings, gr);
  }
  
  // This class should be removed as soon as it's functionality is clearly unneeeded
  class UnversionedBinderVisitor extends BinderVisitor {
    public UnversionedBinderVisitor(IGranuleBindings cu, IRNode gr) {
      super(cu, gr);
    }

    // XXX: This functionality either duplicates what's in BinderVisitor
    // or shoul dbe copied.
    @Override
    public Void visitDemandName(IRNode node) {
      visit(node); // bind the object
      if (isFullPass) {
        String pkg     = DemandName.getPkg(node);
        PackageDrop pd = PackageDrop.findPackage(pkg);
        IRNode p = null;
        if (pd == null) {
        	//System.out.println("unknown package: "+pkg);     
        	// Could be a type for a "static" demand name?
        } else {
        	p = pd.getPackageDeclarationNode();
        }
        bind(node, p);
      }
      return null;
    }
  }

  /*
  public void getBindings(Map<IRNode,IBinding> bMap, IRNode tree) {
	getBindings(bMap, tree, null);
  }
  
  private void getBindings(Map<IRNode,IBinding> bMap, IRNode node, IGranuleBindings granule) {
	  Operator op         = JJNode.tree.getOperator(node);
	  boolean needGranule = granule == null || isGranule(op, node);	  
	  if (needGranule) {
		  granule = ensureBindingsOK(node);  
		  System.out.println("Got new "+(granule.containsFullInfo() ? "full    " : "partial ")+
				             granule+" for "+node);
	  }
	  if (op instanceof IHasBinding) {
		  IBinding b = node.getSlotValue(granule.getUseToDeclAttr());
		  bMap.put(node, b);
	  }
	  for(IRNode c : JJNode.tree.children(node)) {
		  getBindings(bMap, c, granule);
	  }
  }
  */
}
