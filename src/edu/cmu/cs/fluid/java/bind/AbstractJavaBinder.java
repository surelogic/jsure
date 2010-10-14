/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/AbstractJavaBinder.java,v 1.145 2008/11/21 16:40:43 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

import com.surelogic.analysis.IIRProject;
import com.surelogic.common.XUtil;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IJavaScope.Selector;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.PackageDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;
import edu.cmu.cs.fluid.util.Stack;
import edu.cmu.cs.fluid.version.Version;

/**
 * General code to compute binding information on Java trees.
 * The basic idea is that we find the granule of binding work associated with the
 * node and then if this work hasn't been done yet, we do all the bindings for that granule
 * and then return the information.
 * <p>
 * If, while doing binding, we find we need the binding of another node, we may end up with a
 * recursion problem.  If the granularity of work were the single node, we would simply
 * catch this and flag an error.  Since we use larger granules of work, it is a little more
 * complex.  We permit using of binding information that is already computed, but
 * if it is not computed yet, it counts are a recursive call, and hence an error.
 * Recently, we had to move from CU granularity to class granularity to avoid a
 * circularity of this nature.
 * <p>
 * This basic story is complicated by incrementality.  We obviously don't
 * want to do the work over again if nothing changed.  On the one hand,
 * it seems rather coarse to invalidate all work after any change in any Java 
 * tree anywhere (i.e. if requested in a new version, if versioning is on).
 * On the other hand, it's not correct to only invalidate the binding
 * information if the tree for the granule has changed: bindings go out
 * of the granule.
 * <p>
 * The solution is to keep dependencies: when one refers to an entity, we add
 * a back pointer to the use.  Then if a declaration changes, we invalidate
 * the work done in the granule.  This is inter-granule incrementality.
 * <p>
 * To this picture can be added intra-granule incrementality: in which we
 * attempt to avoid redoing all the binding work of a granule.  This requires
 * keeping change bits on nodes which can be expensive (in space).  The
 * dependencies would then be used to invalidate only the particular use.
 * <p>
 * This class implements granularity and inter-granule incrementality.
 * The granularity is currently set at the "class" level,
 * although perhaps the method body level would give better results
 * and would obviate the need for intra-granule incrementality.
 * Each granule needs a hash table.
 * @author Edwin.Chan
 * @author John Boyland
 */
public abstract class AbstractJavaBinder extends AbstractBinder {
  protected static final Logger LOG = SLLogger.getLogger("FLUID.java.bind");
  private static final IJavaType[] noTypes = new IJavaType[0];
  
  public static volatile boolean foundIssue = false;  
  public static final AtomicInteger issueCount = new AtomicInteger(0);
  protected static final boolean storeNullBindings = true;
  protected boolean warnAboutPkgBindings = false;

  public static final ThreadLocal<Stack<IGranuleBindings>> bindingStack = new ThreadLocal<Stack<IGranuleBindings>>() {
	  @Override
	  protected Stack<IGranuleBindings> initialValue() {
		  return new Stack<IGranuleBindings>();
	  }
  };
  
  public static int numPartial;
  public static int numFull;
  public static long partialTime, fullTime;
  
  public static void printStats() {
	  if (XUtil.useDeveloperMode()) {
		  SLLogger.getLogger().log(Level.INFO,"partial = "+numPartial);
		  SLLogger.getLogger().log(Level.INFO,"full = "+numFull);
	  }
  }
  
  protected final IJavaClassTable classTable;
  protected final ITypeEnvironment typeEnvironment;
  protected final Map<IRNode,IGranuleBindings> allGranuleBindings = 
    new ConcurrentHashMap<IRNode,IGranuleBindings>();
  
  /**
   * Granule bindings to track if the first visitor pass has been done
   */
  protected final Map<IRNode,IGranuleBindings> partialGranuleBindings = 
    new ConcurrentHashMap<IRNode,IGranuleBindings>();
  
  protected AbstractJavaBinder(IJavaClassTable table) {
    classTable      = table;
    typeEnvironment = new TypeEnv();
  }
  
  protected AbstractJavaBinder(ITypeEnvironment tEnv) {
    typeEnvironment = tEnv;
    classTable      = tEnv.getClassTable();
  }

  protected static Operator getOperator(IRNode node) {
    return JJNode.tree.getOperator(node);
  }
  
  @Override
  public final ITypeEnvironment getTypeEnvironment() {
    return typeEnvironment;
  }
  
  /**
   * Whether this node can be printed for debugging information.
   * Because of versioning, it may be dangerous to try to print a node.
   * @param node
   * @return true if this node can be printed.
   */
  protected boolean okToAccess(IRNode node) {
    return true;
  }
  
  /**
   * Check if this is a granule.
   * This code is <em>definitive</em>.  It should be the
   * only place that knows what the granularity is.
   * Currently it is true for classes (and the compilation unit).
   * @param op
   * @return if this operator marked a granule.
   */
  public static boolean isGranule(Operator op, IRNode node) {
    if (op instanceof TypeDeclInterface) {
      /*
      if (op instanceof AnonClassExpression) {
        return !OuterObjectSpecifier.prototype.includes(JJNode.tree.getParent(node));
      }
      */
      /*
      if (op instanceof TypeFormal) {
    	return false;
      }
      */
      return true;
    }
    /*
    else if (op instanceof OuterObjectSpecifier) {
      IRNode alloc = OuterObjectSpecifier.getCall(node);
      if (AnonClassExpression.prototype.includes(alloc)) {
        return true;
      }
    }
    */
    else if (op instanceof NewExpression) {
    	// This is necessary because the type of the NewE may depend on binding the OOS
        return getOOSParent(node) != null;
    }
    else if (op instanceof OuterObjectSpecifier) {
    	return true;
    }
    else if (op instanceof Expression) {    	    	
    	// Check if it's the top-level Expression and contains an OOS 
    	IRNode parent = JJNode.tree.getParent(node);
    	if (Statement.prototype.includes(parent)) {
        	// Check if already calculated
        	int mods = JavaNode.getModifiers(node);
        	if (JavaNode.isSet(mods, JavaNode.IS_GRANULE)) {
        		return true;
        	}
        	if (JavaNode.isSet(mods, JavaNode.NOT_GRANULE)) {
        		return false;
        	}
        	
    		for(IRNode n : JJNode.tree.topDown(node)) {
    			if (OuterObjectSpecifier.prototype.includes(n)) {    				
    				//System.out.println("Granule: "+DebugUnparser.toString(node));
    				JavaNode.setModifiers(node, JavaNode.setModifier(mods, JavaNode.IS_GRANULE, true));
    				return true;
    			}
    		}
    		JavaNode.setModifiers(node, JavaNode.setModifier(mods, JavaNode.NOT_GRANULE, true));
    		return false;
    	}
    	return false;
    }
    return op instanceof CompilationUnit;
  }
  
  /**
   * @return non-null if node is the call for an OOS
   */
  static IRNode getOOSParent(IRNode node) {
  	final IRNode parent = JJNode.tree.getParent(node);
  	if (OuterObjectSpecifier.prototype.includes(parent) && node.equals(OuterObjectSpecifier.getCall(parent))) {
  		return parent;
  	}
  	return null;
  }
  
  /**
   * Return the granule of binding associated with this node, or throw
   * an exception if we don't find one (while going to the root).
   * @param node node we wish binding information for
   * @return granule which this node is associated with
   */
  public IRNode getGranule(IRNode node) {
    Operator op;
    while (!(isGranule(op = JJNode.tree.getOperator(node), node))) {
      IRNode p;
      try {
    	p = JJNode.tree.getParent(node);        
      } catch (SlotUndefinedException e) {
        p = null;
      }
      if (p == null) {
        // for debugging
        throw new NullPointerException("Has a null/undefined parent: " + node + " with op = " + op);
      }
      node = p;
    }
    return node;
  }
  
   /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#getIBinding(edu.cmu.cs.fluid.ir.IRNode)
   */
  public IBinding getIBinding(IRNode node) {
    IGranuleBindings bindings = ensureBindingsOK(node);    
    /*
    if (!JJNode.versioningIsOn && !node.valueExists(bindings.getUseToDeclAttr())) {
    	// Maybe canonicalizing the AST
    	System.out.println("Re-deriving "+bindings.hashCode()+" for "+node.hashCode());
    	bindings.clear();
    	bindings.ensureDerived();
    }
    */
    // LOG.finer("getting binding for " + DebugUnparser.toString(node));
    try {
    	synchronized (bindings) {
    		// TODO how to protect against destroyed bindings?
    		if (bindings.isDestroyed()) {
    			return getIBinding(node);
    		} else {    		
    			IBinding binding = node.getSlotValue(bindings.getUseToDeclAttr());
    			if (binding != null && binding.getNode() != null && binding.getNode().identity() == IRNode.destroyedNode) {
    				System.out.println("Destroyed binding for "+DebugUnparser.toString(node));
    				bindings.destroy();
    				return getIBinding(node);
    			}
    			return binding;
    		}
    	}
    } catch (SlotUndefinedException e) {
    	// TODO cache erroreous nodes?
    
      // debugging
      String msg;
      if (bindings.containsFullInfo()) {
          msg = "no full binding for ";
      } else {
          msg = "no partial binding for ";
      }
      IRNode granule = getGranule(node);
      if (granule == node) {
    	  IRNode parent = JJNode.tree.getParentOrNull(node);
    	  LOG.severe(msg + node + " = " + DebugUnparser.toString(node)+" (parent: "+JJNode.tree.getOperator(parent).name()+")");
    			  //" in version " + Version.getVersion());
      } else {
    	  LOG.severe(msg + node + " = " + DebugUnparser.toString(node) + 
    			  //" in version " + Version.getVersion());
    			  "\n\tin granule " + DebugUnparser.toString(granule));
    	  needFullInfo(node, JJNode.tree.getOperator(node), granule);
    	  ensureBindingsOK(node);  
      }
//      System.out.println("Operator is " + JJNode.tree.getOperator(node));
//      System.out.println(DebugUnparser.toString(node));
//      if (handlePromises) {
//        System.out.println("grant parent is " + DebugUnparser.toString(JavaPromise.getParentOrPromisedFor(JavaPromise.getParentOrPromisedFor(node))));
//      } else {
//        System.out.println("grant parent is " + DebugUnparser.toString(JJNode.tree.getParent(JJNode.tree.getParent(node))));
//      }
      
      ensureBindingsOK(node);
      throw e;
    }
  }

  protected static boolean needFullInfo(IRNode node, Operator op, IRNode gr) {
      boolean needFullInfo = !JavaNode.getModifier(gr, JavaNode.AS_BINARY) || Call.prototype.includes(op);
      if (needFullInfo && !JJNode.versioningIsOn) {
        IRNode here = node;
        while (here != null && Name.prototype.includes(op)) {
        	here = JJNode.tree.getParent(here);
        	op   = JJNode.tree.getOperator(here); 
        }
        needFullInfo = !isType(op);
        /*
        if (!needFullInfo && here != null) {
        	// here is a Type, so skip it
        	final IRNode parent = JJNode.tree.getParent(here);
        	needFullInfo = isSpecialTypeCase(parent);
        }
        */
      }
      return needFullInfo;
  }
  
  private static boolean isType(Operator op) {
	  return (Type.prototype.includes(op) && !TypeDeclaration.prototype.includes(op)) ||
	         TypeActuals.prototype.includes(op);
  }
  
  // Check for case of OOS around an AllocationCallExpression (or pair)
  static boolean isSpecialTypeCase(IRNode here) {
	  /*
	  if (foundIssue) {
		  System.out.println("Checking isSpecialTypeCase for "+DebugUnparser.toString(here));
	  }
	  Operator op = JJNode.tree.getOperator(here);
	  // Skip Type nodes
	  while (isType(op)) {
		  here = JJNode.tree.getParent(here);
		  op   = JJNode.tree.getOperator(here); 
	  }
	  if (AllocationCallExpression.prototype.includes(here)) {    	 
		  IRNode parent = JJNode.tree.getParent(here);
		  Operator pop = JJNode.tree.getOperator(parent);
		  if (OuterObjectSpecifier.prototype.includes(pop)) {
			  return true;
		  }
		  if (AnonClassExpression.prototype.includes(pop)) {
			  IRNode gparent = JJNode.tree.getParent(parent);
			  return OuterObjectSpecifier.prototype.includes(gparent);
		  }
	  }
	  */
	  return false;
  }   
  
  @SuppressWarnings("deprecation")
  protected IGranuleBindings ensureBindingsOK(final IRNode node) {    
    final IRNode gr            = getGranule(node);
    final Operator op          = JJNode.tree.getOperator(node); 
    final boolean needFullInfo = needFullInfo(node, op, gr);
    final Map<IRNode,IGranuleBindings> granuleBindings =
  	  needFullInfo ? allGranuleBindings : partialGranuleBindings;
    IGranuleBindings bindings;
    // FIX deadlock 
    // Issue: we alternate between locking on the IGranuleBinding and the Binder 
    bindings = granuleBindings.get(gr);
    
    if (bindings == null || bindings.isDestroyed()) {
    	bindings = makeGranuleBindings(gr);
    	bindings.setContainsFullInfo(needFullInfo);    	
    	granuleBindings.put(gr,bindings);    	
    }
    
    // FIX hack to be able to use just derived info (e.g. a binding for a NamedType)
    // to compute other info (e.g. a binding for a ParameterizedType containing the
    // NamedType)
    final Stack<IGranuleBindings> stack = bindingStack.get();
    stack.push(bindings);    

    final int size = stack.size();
    try {
    	try {
    		BindingsThread t = null;
    		//if (size > 10) {
    		//System.out.println("Binding stack for "+JJNode.getInfoOrNull(node)+": "+size);
    		if (size > 50) {
    			// Finish derivation in a separate thread to avoid StackOverflowError
    			//System.out.println("Over 50");
    			t = new BindingsThread(bindings, node);
    		}
    		//}
    		if (t == null) {
    			bindings = deriveBindings(bindings, node);
    		} else {
    			t.start();
    			try {
    				t.join();
    			} catch (InterruptedException e) {
    				LOG.log(Level.SEVERE, "Interrupted while joining with deriving thread", e);
    			}
    			bindings = t.bindings;
    		}
    	} catch(StackOverflowError e) {
    		if (stack.size() == 1) { // Last one, try to restart
    			System.err.println("Retry after StackOverflow");
    			reset();
    			return ensureBindingsOK(node);
    		}
    		throw e;
    	}
    } finally {
    	stack.pop();
    }
    return bindings;
  }
  
  protected void reset() {
	  // Nothing to do yet
  }
    
  class BindingsThread extends Thread {
	  IGranuleBindings bindings;
	  final IRNode node;
	  final Version v;
	  
	  BindingsThread(IGranuleBindings bindings, IRNode node) {
		this.bindings = bindings;
		this.node = node;
		v = Version.getVersion();
	  }

	  @SuppressWarnings("deprecation")
	  @Override
	  public void run() {
		  Version.setVersion(v);
		  bindings = deriveBindings(bindings, node);
	  }
  }
  
  IGranuleBindings deriveBindings(IGranuleBindings bindings, IRNode node) {
	  try {
	  // To prevent it from being destroyed while deriving
		  synchronized (bindings) {
			  IGranuleBindings toDerive = bindings;
			  while (toDerive.isDestroyed()) {
				  // Retry
				  toDerive = ensureBindingsOK(node);
			  }
			  if (toDerive == bindings) {
				  toDerive.ensureDerived(node);
			  } else {
				  // Otherwise, already derived
			  }
			  bindings = toDerive;
		  }
      } catch (StackOverflowError e) {
    	  System.out.println("StackOverflow: "+DebugUnparser.toString(node)+" for "+this);
    	  e.printStackTrace();
    	  throw e;    	  
      }
	  return bindings;
  }
  
  protected abstract IGranuleBindings makeGranuleBindings(IRNode cu);
 
  protected final void deriveInfo(IGranuleBindings bindings, IRNode unit) {
    //System.out.println("Deriving info for "+DebugUnparser.toString(unit));
	/*
	if (TypeDeclaration.prototype.includes(unit)) {
		String qname = JavaNames.getFullTypeName(unit);
		System.out.println("Deriving info for "+qname+": "+
				(bindings.containsFullInfo() ? "full" : "partial"));
		if (qname.endsWith("TestMultipleTextOutputFormat") ||
			qname.endsWith("TestMultiFileInputFormat")) {
			System.err.println(DebugUnparser.childrenToString(VisitUtil.getClassBody(unit)));
		}
	}
	*/
	  
    // we need to do two passes
    // because we need to bind all types before we try to handle
    // method calls because a method return type will need to be bound
    // before we access its binding.
    
    // Two passes will not be necessary if we do method body granularity.
    BinderVisitor binderVisitor = newBinderVisitor(bindings, unit);
    
    //System.out.println(DebugUnparser.toString(unit));
    //binderVisitor.start();
    /*
    if (bindings.containsFullInfo() && AnonClassExpression.prototype.includes(unit)) {
      System.out.println("Deriving ACE");
    }
    */
    if (bindings.containsFullInfo()) {
      binderVisitor.setFullPass();
      long start = System.currentTimeMillis();
      
      //System.out.println("Full:\t"+DebugUnparser.toString(unit));
      /*
      if (AnonClassExpression.prototype.includes(unit)) {
    	  System.out.println("Binding ACE: "+DebugUnparser.toString(unit));
      }
      */
      binderVisitor.start();

      long end = System.currentTimeMillis();
      fullTime += (end-start);
      numFull  += bindings.getUseToDeclAttr().size();
    } else {
      long start = System.currentTimeMillis();

      //System.out.println("Part:\t"+DebugUnparser.toString(unit));
      binderVisitor.start();

      long end = System.currentTimeMillis();
      partialTime += (end-start);
      numPartial += bindings.getUseToDeclAttr().size();
    }
  }
  
  protected BinderVisitor newBinderVisitor(IGranuleBindings bindings, IRNode gr) {
    return new BinderVisitor(bindings, gr); 
  }
  
  public final IJavaClassTable getClassTable() {
    return classTable;
  }

  /**
   * Return a member table from a type.
   * @param tdecl
   * @return member table
   */
  public abstract IJavaMemberTable typeMemberTable(IJavaSourceRefType tdecl);
  
  protected final IJavaDeclaredType asDeclaredType(IJavaArrayType ty) {
    IRNode fakeArrayDeclaration = getTypeEnvironment().getArrayClassDeclaration();
    LOG.finer("Found fake array decl " + fakeArrayDeclaration);
    return JavaTypeFactory.getDeclaredType(fakeArrayDeclaration,new ImmutableList<IJavaType>(ty.getElementType()),null);
  }
  
  public final IJavaScope typeScope(IJavaType ty) {
    if (ty instanceof IJavaDeclaredType) {
      IJavaDeclaredType dty = (IJavaDeclaredType)ty;
      return new IJavaScope.SubstScope(javaTypeScope(dty), getTypeEnvironment(), dty);
    } else if (ty instanceof IJavaArrayType) {
      return typeScope(asDeclaredType((IJavaArrayType)ty));
    } else if (ty instanceof IJavaTypeFormal) {
      IJavaTypeFormal tf = (IJavaTypeFormal) ty;
      return javaTypeScope(tf);
      /*
       * FIX doesn't handle interface bounds
       * 
      IJavaType sc       = tf.getSuperclass(getTypeEnvironment());
      if (sc != null) {
        
        return typeScope(sc);
      }      
      // No bounds
      return typeScope(getTypeEnvironment().getObjectType());
      */
    } else if (ty instanceof IJavaWildcardType) {
      IJavaWildcardType wt = (IJavaWildcardType) ty;
      if (wt.getUpperBound() != null) {
        return typeScope(wt.getUpperBound());  
      }
      /* // Could be any supertype of T, including Object
      else if (wt.getLowerBound() != null) {
        LOG.warning("What type scope do I use for "+wt+"?");
      }
      */
      return typeScope(typeEnvironment.getObjectType());
    } else if (ty instanceof IJavaIntersectionType) {
      IJavaIntersectionType it = (IJavaIntersectionType) ty;
      return new IJavaScope.ShadowingScope(typeScope(it.getPrimarySupertype()), 
                                           typeScope(it.getSecondarySupertype()));
    } else if (ty instanceof IJavaCaptureType) {
      IJavaCaptureType ct = (IJavaCaptureType) ty;
      IJavaScope sc       = typeScope(ct.getWildcard());
      for(IJavaReferenceType b : ct.getTypeBounds()) {
        sc = new IJavaScope.ShadowingScope(sc, typeScope(b));
      }
      return sc;
    } else if (ty instanceof IJavaPrimitiveType) {
      // Handling non-canonicalized code
      IJavaPrimitiveType pty = (IJavaPrimitiveType) ty;
      IJavaDeclaredType dty  = JavaTypeFactory.getCorrespondingDeclType(getTypeEnvironment(), pty);
      // Same as above
      return new IJavaScope.SubstScope(javaTypeScope(dty), getTypeEnvironment(), dty);
    } else {
      LOG.warning("non-class type! " + ty);      
    }
    return null;
  }
  
  /**
   * Get the correct kind of member table scope.
   * @param tdecl Type declaration node
   * @return scope for the class and its supeclasses
   */
  public final IJavaScope javaTypeScope(IJavaSourceRefType tdecl) {
    return typeMemberTable(tdecl).asScope(this);
  }
  
  /**
   * Returns a table representing a Java import list mapping names 
   * to declarations.
   */
  public abstract IJavaScope getImportTable(IRNode cu);
  
  public String getInVersionString() {
    return "";
  }
  
  static class BindingInfo {
  	final IBinding method;
  	final int numBoxed;
  	final boolean usedVarArgs;
  	
  	BindingInfo(IBinding m, int boxed, boolean var) {
  		method = m;
  		numBoxed = boxed;
  		usedVarArgs = var;
  	}
  }
  
  private static final IBinding nullBinding = IBinding.Util.makeBinding(null);
  
  /**
   * The actual work of binding and maintaining scopes.
   * This code has extra machinery in it too handle granules and incrementality.
   * <P>
   * The binding process has several distinct modes
   * going down the tree:
   * <ul>
   * <li> To start with, we start at the compilation unit and work down
   *      until we reach the granule.  As we go, the scope is adjusted to
   *      handle where we are.  These scopes need not keep dependency information
   *      as long as any change to the information used invalidates the
   *      {@link IGranuleBindings} instance later on.  In this process,
   *      we do not store any binding information (wrong granule).
   * <li> Once we reach the granule, we (optionally) start an intra-granule
   *      incremental visitor that only visits changed places or places
   *      where bindings have been invalidated.
   * <li> Or we use a batch visitor.  But in this or the last case, we
   *      need to (1) store binding information and (2) avoid going into a
   *      nested granule.
   * </ul>
   * We don't bother to use try-finally because we don't catch errors,
   * and the visitor is intended for single use only.  (It should be discarded
   * afterwads.)
   * @author boyland
   */
  protected class BinderVisitor extends Visitor<Void> {	  
    protected IJavaScope scope;
    protected final IGranuleBindings bindings;
    protected Collection<IRNode> pathToTarget; // if non-null only visit these nodes (in reverse order)
    protected final IRNode targetGranule;
    protected boolean isBatch = true; // by default we have a batch binder
    protected boolean isFullPass = false; // by default we start in the preliminary pass
    protected final boolean debug = LOG.isLoggable(Level.FINER);        
    private final Hashtable2<IJavaType,IJavaType,Boolean> callCompatCache = 
    	new Hashtable2<IJavaType,IJavaType,Boolean>();
    
    /**
     * No change in functionality, but changed to avoid
     * StackOverflow issues
     */
    @Override
    public void doAcceptForChildren(IRNode node) {
    	for(IRNode n : JJNode.tree.children(node)) {
    		doAccept(n);
    	}
    }
    
    protected final boolean isCallCompatible(IJavaType t1, IJavaType t2) {
    	if (t1 == null || t2 == null) {    	
    		return false;
    	}
    	Boolean result = callCompatCache.get(t1, t2);
    	if (result == null) {
    		result = typeEnvironment.isCallCompatible(t1, t2);
    		callCompatCache.put(t1, t2, result);
    	}
    	return result;
    }
    
    public BinderVisitor(IGranuleBindings cu, IRNode gr) {
      bindings = cu;
      scope = null;
      targetGranule = gr;
      pathToTarget = new HashSet<IRNode>();
    }
    
    /**
     * Start doing the binding.
     */
    public void start() {
      /*
      if (bindings.containsFullInfo() && 
    	  "new Super { private int g #; { #; } { #; } }".equals(DebugUnparser.toString(targetGranule))) {
    	  foundIssue = true;
      }
      */
      /*
      if (foundIssue) {
    	  System.out.println("Starting to bind granule "+DebugUnparser.toString(targetGranule)+": "+isFullPass);
      }
      */
      if (debug) {
        LOG.finer("Starting to bind granule "
            + DebugUnparser.toString(targetGranule));
      }      
      //boolean debug = bindings.containsFullInfo() && AnonClassExpression.prototype.includes(targetGranule);
      IRNode n = targetGranule;
      for (;;) {
        IRNode p = JJNode.tree.getParentOrNull(n);
        if (p == null) break;
        n = p;
        pathToTarget.add(n);
        /*
        if (debug) {
          System.out.println("Path to target: "+n);
        }
        */
      }
      doAccept(n);
      /*
      if (foundIssue) {
    	  System.out.println("Finishing granule "+DebugUnparser.toString(targetGranule)+": "+isFullPass);
      }
      */
    }
    
    public void setFullPass() {
      isFullPass = true;
    }
    
    /**
     * For intra-granule incrementality, we only look
     * if there was a change here or if we are in batch mode
     * for one of many reasons.
     * @param node
     */
    protected void doAcceptIfChanged(IRNode node) {
      if (isBatch) {
        /*
        if (bindings.containsFullInfo() && AnonClassExpression.prototype.includes(targetGranule)) {
          System.out.println("Accepting "+node);
        }
        */
        super.doAccept(node);
      } else if (nodeHasNewParent(node)) {
        isBatch = true;
        super.doAccept(node);
        isBatch = false;
      } else if (pathToTarget != null || nodeHasChanged(node)) {
        super.doAccept(node);
      }
    }
    
    /**
     * Return true if this node or a descendant node has changes,
     * or invalidated bindings.  This means it will not be skipped in
     * the intra-granule traversal.
     * @param node
     * @return true if should visit this node.
     */
    protected boolean nodeHasChanged(IRNode node) {
      return true; // never called if batch
    }
    
    /**
     * Return true if this node has a new parent: that is the whole 
     * subtree came from elsewhere.  In this case, we can't rely on
     * change information below this point: we must be batch.
     * @param node
     * @return true if the node has a different parent.
     */
    protected boolean nodeHasNewParent(IRNode node) {
      return true; // never called if batch
    }
    
    /**
     * Return if this node must be visitied even if not on path to target or not
     * changed since the last visit.  This is because this
     * node contains a declaration that must be added to the local scope, even
     * if we skip the looking at its children.
     * @param node
     * @return if this node must be visited.
     */
    protected boolean mustVisit(IRNode node, Operator op) {
      return (op instanceof DeclStatement || op instanceof TypeDeclarationStatement);
    }
    
    @Override
    public Void doAccept(IRNode node) {
      if (node == null) {
    	if (debug) {
    		LOG.finer("Skipping null");
    	}
        return null;
      }
      /*
      if (foundIssue) {
    	  System.out.println("  doAccept on "+JJNode.tree.getOperator(node).name()+" -- "+DebugUnparser.toString(node));
      }
      */
      if (pathToTarget != null) {
        // trying to find our target granule
        if (node == targetGranule) {
          Collection<IRNode> saved = pathToTarget;
          pathToTarget = null;
          doAcceptIfChanged(node);
          pathToTarget = saved;
          return null;
        }
        final int size = pathToTarget.size();
        /*
        if (size > 0) {
        	System.out.println("Comparing last path: "+DebugUnparser.toString(pathToTarget.get(size-1)));
        	System.out.println("            to node: "+DebugUnparser.toString(node));
        }
        */
        if (size > 0 && pathToTarget.contains(node)) {
          pathToTarget.remove(node);
          doAcceptIfChanged(node);
          return null;
        }
        final Operator op = JJNode.tree.getOperator(node);
        if (mustVisit(node, op)) {
          /*
          if (bindings.containsFullInfo() && AnonClassExpression.prototype.includes(targetGranule)) {
            System.out.println("Accepting "+node);
          }
          */
          //return super.doAccept(node);          
          return ((IAcceptor) op).accept(node, this);
        } else if (debug) {
            LOG.finer("Skipping node not on path -- " + DebugUnparser.toString(node));
        }

        return null;
      }
      final Operator op = JJNode.tree.getOperator(node);
      if (isGranule(op, node)) {
    	//System.out.println("Skipping granule "+DebugUnparser.toString(node));
        return null; // skip granule nested in this one
      }
      if (debug && LOG.isLoggable(Level.FINEST) && okToAccess(node)) {
        LOG.finest(this + " visit" + (isFullPass ? "(full) " : "(initial) ") + op + DebugUnparser.toString(node));
      }
      /*
      if (handlePromises) {
        super.doAccept(node);
        bindPromises(node);
        
        // Methods and constructors need to be handled specially to deal
        // with the scoping
        if (!SomeFunctionDeclaration.prototype.includes(op)) {
          final IJavaScope saved = scope;
          try {
            if (ClassDeclaration.prototype.includes(op) || 
                EnumDeclaration.prototype.includes(op)) {     
              IJavaScope.NestedScope sc = new IJavaScope.NestedScope(scope);
              addReceiverDeclForType(node, sc);
              scope = sc;
            }
            PromiseFramework.getInstance().processPromises(node, promiseProcessor);        
          } finally {
            scope = saved;
          }
        }
        return null;
      }
      */
      //return super.doAccept(node);
      return ((IAcceptor) op).accept(node, this);
    }

    private void addReceiverDeclForType(IRNode type, IJavaScope.NestedScope sc) {
      /*
      IRNode initD = JavaPromise.getInitMethod(type);
      sc.put("this", JavaPromise.getReceiverNode(initD));
      */
      sc.put("this", JavaPromise.getReceiverNode(type));
    }

    /*
    private void bindPromises(IRNode node) {
      PromiseFramework frame = PromiseFramework.getInstance();
      Operator op = JJNode.tree.getOperator(node);
      if (op instanceof IHasCustomBinding) {
        IRNode b = frame.getBinding(op, node);
        if (b != null) {
//          System.out.println("Bound "+node+":"+op.name()+" to "+DebugUnparser.toString(b));
          bind(node, b);
        } else {
//          System.out.println("Bound to null: "+DebugUnparser.toString(node)+" - "+op.name());
          frame.getBinding(op, node);
        }
      }
    }
    */
    
    /**
     * We do the accept of a node
     * using a new scope
     * @param node the node to visit
     * @param newScope the new scope to use for its children
     */
    protected void doAccept(IRNode node, IJavaScope newScope) {
      IJavaScope saved = scope;
      scope = newScope;
      doAccept(node);
      scope = saved;
    }
    
    /**
     * We do the accept of a node using a new scope
     * @param node the node to visit
     * @param newScope the new scope to use for its children
     */
    protected void doAcceptForChildren(IRNode node, IJavaScope newScope) {
      IJavaScope saved = scope;
      boolean wasBatch = isBatch;
      scope = newScope;
      //doAcceptForChildren(node);
      for(IRNode n : JJNode.tree.children(node)) {
    	  doAccept(n);
      }
      scope = saved;
      isBatch = wasBatch;
    }
    
    /**
     * Visit the children of this node with a new scope, ignoring
     * incrementality.  
     * @param node node to visit children for
     * @param newScope new scope to use
     */
    protected void doBatchAcceptForChildren(IRNode node, IJavaScope newScope) {
      if (isBatch) {
        doAcceptForChildren(node,newScope);
      } else {
        isBatch = true;
        doAcceptForChildren(node,newScope);
        isBatch = false;
      }
    }
    
    /**
     * By default, we visit all children.
     * NB: Code in this class should never explicitly call "super.visit(node)"
     * which is a NOP!
     */
    @Override
    public Void visit(IRNode node) {
      //doAcceptForChildren(node);
      for(IRNode n : JJNode.tree.children(node)) {
    	doAccept(n);
      }
      /*
      // This needs to be done after the rest of the children
      // (particularly the ParameterDecls)
      PromiseFramework frame = PromiseFramework.getInstance();
      Operator op = JJNode.tree.getOperator(node);
      
      if (SomeFunctionDeclaration.prototype.includes(op)) {
        frame.processPromises(node, promiseProcessor);        
      }
      */
      return null;
    }
    
    /**
     * Add the declaration children to the current scope.
     * Also force the context to be batch if any of these things are changed.
     * @param decls
     * @param sc
     */
    private void addDeclsToScope(IRNode decls, IJavaScope.NestedScope sc) {
      for (IRNode d : JJNode.tree.children(decls)) {
        // always insert all type formals first before visiting children for two reasons:
        // 1. type formals can refer to each other
        // 2. we don't want incremental binders to skip adding a formal to
        // a scope even if it never changes.
        //System.out.println("Adding decl " + JavaNode.getInfo(d) + " to scope: " + sc);
        if (!isBatch && nodeHasChanged(d)) isBatch = true;
        sc.add(d);
      }
    }

    private IBinding getLocalIBinding(IRNode node) {
    	if (!node.valueExists(bindings.getUseToDeclAttr())) {
    		return getIBinding(node);
    	}
    	return node.getSlotValue(bindings.getUseToDeclAttr());
    }
    
    /**
     * Set the binding of a node.  trivial except for logging.
     * @param node node at which to set the binding
     * @param binding binding to bind to.
     */
    private boolean bind(IRNode node, IBinding binding) {      
      if (pathToTarget != null) {
    	  //System.out.println("Throwing away binding for "+DebugUnparser.toString(node));
    	  return false; // don't bind: not in the target granule
      }      
      if (binding == null) {
    	final String unparse = DebugUnparser.toString(node);
    	if (isBinary(node)) {
    		if (!unparse.endsWith(" . 1")) {
    			System.err.println("Cannot find a binding for " + unparse+" in "+typeEnvironment);
    		}
    	} else {
    		LOG.warning("Cannot find a binding for " + unparse+" in "+typeEnvironment);
    	}
        if (storeNullBindings) {
          node.setSlotValue(bindings.getUseToDeclAttr(), null);
        }
        /*
        if (!SimpleName.prototype.includes(node) && 
            !QualifiedName.prototype.includes(node)) {
        	System.out.println();
        }
        */
        return false;
      } else if (debug){
        LOG.finer("Establishing binding for " + node + " = " + DebugUnparser.toString(node) + " to " + 
            DebugUnparser.toString(binding.getNode()) + getInVersionString());
      }
      node.setSlotValue(bindings.getUseToDeclAttr(),binding);
      /*
      if (!node.valueExists(bindings.getUseToDeclAttr())) {
    	  System.out.println("Didn't successfully create binding");
    	  node.setSlotValue(bindings.getUseToDeclAttr(),binding);
      }
      */
      return true;
    }

    protected boolean bind(IRNode node, IRNode binding) {
      if (binding == null && !storeNullBindings) {
        if (LOG.isLoggable(Level.WARNING)) {
          String unparse = DebugUnparser.toString(node);
          LOG.warning("null binding for "+JJNode.tree.getOperator(node).name()+": "+unparse);
        }
        return false;
      }
      return bind(node,IBinding.Util.makeBinding(binding, getTypeEnvironment()));
    }
    
    /**
     * Bind the node given to the declaration that matches the selector.
     * @param node IRNode to get the binding
     * @param selector used to choose the binding
     * @param name name to perform lookup using
     */
    protected boolean bind(IRNode node, Selector selector, String name) {
      // LOG.finer(name);
      return bind(node,scope,selector,name);
    }
    
    /**
     * Bind the node given to the declaration that matches the selector.
     * @param node IRNode to get the binding
     * @param sc Scope to perform binding in
     * @param selector used to choose the binding
     */
    protected boolean bind(IRNode node, IJavaScope sc, Selector selector) {
      return bind(node,sc,selector,JJNode.getInfo(node));
    }
    
    /**
     * Bind the node given to the declaration that matches the selector.
     * @param node IRNode to get the binding
     * @param sc Scope to perform binding in
     * @param selector used to choose the binding
     * @param name name to perform lookup using
     * @return true if bound to non-null
     */
    protected boolean bind(IRNode node, IJavaScope sc, Selector selector, String name) {
      if (debug) {
        LOG.finer("Looking up " + name + " in " + sc);
      }
      if (sc != null) {
        IBinding binding = sc.lookup(name,node,selector);
        if (binding == null) {
          sc.lookup(name,node,selector);
        }
        bind(node, binding);
        return binding != null;
      } else {
        bind(node, (IRNode) null);
        return false;
      }
    }
    
    /**
     * Bind a node using its own info as the name to lookup in the scope
     * @param node usesite with info
     * @param selector choose which declaration
     */
    protected boolean bind(IRNode node, Selector selector) {
      return bind(node,selector,JJNode.getInfo(node));
    }
    
    private int numChildrenOrZero(IRNode node) {
      if (node == null) {
        return 0;
      }
      return JJNode.tree.numChildren(node);
    }

    // Convert the args to IJavaTypes

	private IJavaType[] getArgTypes(IRNode args) {
	  if (args == null) {
		  return noTypes;
	  }
      final int n = JJNode.tree.numChildren(args);
      IJavaType[] argTypes = new IJavaType[n];     
      Iterator<IRNode> argse = JJNode.tree.children(args); 
      for (int i= 0; i < n; ++i) {
        IRNode arg = argse.next();
        argTypes[i] = getJavaType(arg);
      }
      return argTypes;
    }
    
    /**
     * @return true if bound
     */
    protected boolean bindCall(IRNode call, IRNode targs, IRNode args, String name, IJavaType recType) {
      return bindCall(call, targs, args, name, typeScope(recType));
    }
    
    /**
     * @return true if bound
     */
    protected boolean bindCall(final IRNode call, IRNode targs, IRNode args, String name, IJavaScope sc) {
      /*
      if (pathToTarget != null) {
    	  System.out.println("Not computing binding for call: "+DebugUnparser.toString(call));
    	  return false; // skip the work   
      }
      */
      final int numTypeArgs = numChildrenOrZero(targs);
      final IJavaType[] argTypes = getArgTypes(args);
      if (debug) {
        StringBuilder sb = buildStringOfArgTypes(argTypes);
        LOG.finer("Looking for method: " + name + sb + getInVersionString());
      }
      
      BindingInfo bestMethod = null;
      IJavaType bestClass = null; // type of containing class
      IJavaType[] bestArgs = new IJavaType[argTypes.length];
      IJavaType[] tmpTypes = new IJavaType[argTypes.length];
      
      final IRNode from;
      Operator callOp = JJNode.tree.getOperator(call);
      if (AnonClassExpression.prototype.includes(callOp)) {
    	  from = AnonClassExpression.getBody(call);
      } else {
    	  from = call;
      }
      IJavaScope.Selector isAccessible = new IJavaScope.AbstractSelector("Is accessible") {
		public boolean select(IRNode mdecl) {
			boolean ok = BindUtil.isAccessible(typeEnvironment, mdecl, from);
			return ok;
		}    	  
      };
      Iterator<IBinding> methods = IJavaScope.Util.lookupCallable(sc,name,call,isAccessible);
      findMethod: while (methods.hasNext()) {
        final IBinding mbind = methods.next();
        final IRNode mdecl = mbind.getNode();
        if (debug) {
          LOG.finer("Considering method binding: " + mdecl + " : " + DebugUnparser.toString(mdecl)
              + getInVersionString());
        }
        final IRNode typeFormals = SomeFunctionDeclaration.getTypes(mbind.getNode());
        final int numTypeFormals = numChildrenOrZero(typeFormals);
        IJavaTypeSubstitution methodTypeSubst = null;
        if (numTypeArgs != 0) {
          if (numTypeArgs != numTypeFormals) {
        	  continue findMethod;
          } else {
        	  methodTypeSubst = FunctionParameterSubstitution.create(AbstractJavaBinder.this, 
          			                                                 mbind, targs);
          }
        }    
        
        final BindingInfo match = matchMethod(targs, args, argTypes, mbind, tmpTypes, methodTypeSubst);
        if (match == null) {
          continue findMethod;
        }
        
        IRNode tdecl = JJNode.tree.getParent(JJNode.tree.getParent(match.method.getNode()));
        IJavaType tmpClass = typeEnvironment.convertNodeTypeToIJavaType(tdecl);
        // we don't detect the case that there is no best method.
        if (bestMethod == null ||
            (typeEnvironment.isAssignmentCompatible(bestArgs,tmpTypes) && 
             typeEnvironment.isSubType(tmpClass,bestClass)) &&
             useMatch(bestMethod, match) ||
            bestMethod.numBoxed > match.numBoxed) { 
          // BUG: this algorithm does the wrong
          // thing in the case of non-overridden multiple inheritance
          // But there's no right thing to do, so...
          IJavaType[] t = bestArgs;
          bestArgs = tmpTypes;
          tmpTypes = t;
          bestMethod = match;
          bestClass = tmpClass;      
        }
      }
      /*
      if (bestMethod != null && AnonClassExpression.prototype.includes(call)) {
        System.out.println("Binding "+call);
      }
      */
      if (bestMethod == null) {
    	  return bind(call, (IBinding) null);
      }
      return bind(call, bestMethod.method);
    }

    /**
     * Only ruling out the case that the match used varargs,
     * but the best did not.
     */
    private boolean useMatch(BindingInfo best, BindingInfo match) {    	
    	return !match.usedVarArgs || best.usedVarArgs;
	}

	/**
     * @param tmpTypes The types matched against
     * @return non-null if mbind matched the arguments
     */
    private BindingInfo matchMethod(IRNode targs, IRNode args, IJavaType[] argTypes, 
                                  IBinding mbind, IJavaType[] tmpTypes,
                                  IJavaTypeSubstitution mSubst) {
      final int numTypeArgs = numChildrenOrZero(targs);
      IRNode mdecl = mbind.getNode();
      Operator op  = JJNode.tree.getOperator(mdecl);      
      IRNode formals;
      IRNode typeFormals;
      if (op instanceof MethodDeclaration) {
        formals = MethodDeclaration.getParams(mdecl);
        typeFormals = MethodDeclaration.getTypes(mdecl);
        /*
        if ("toArray".equals(MethodDeclaration.getId(mdecl))) {
        	System.out.println(DebugUnparser.toString(mdecl));
        	System.out.println();
        }
        */
      } else {
        formals = ConstructorDeclaration.getParams(mdecl);
        typeFormals = ConstructorDeclaration.getTypes(mdecl);
      }
      int numTypeFormals = JJNode.tree.numChildren(typeFormals);
      Map<IJavaType,IJavaType> map;
      if (numTypeFormals != 0) {
        map = new HashMap<IJavaType,IJavaType>();
        for(IRNode tf : JJNode.tree.children(typeFormals)) {
    		IJavaTypeFormal jtf = JavaTypeFactory.getTypeFormal(tf);
    		map.put(jtf, numTypeArgs == 0 ? jtf : mSubst.get(jtf)); // FIX slow lookup
        }
      } else {
        map = Collections.emptyMap();
      }
      BindingInfo matched = matchedParameters(targs, args, argTypes, mbind, formals, tmpTypes, 
    		                               map, mSubst);
      if (matched == null) {
        matched = matchedParameters(targs, args, argTypes, mbind, formals, tmpTypes, null, mSubst);
      }
      return matched;
    }
    
    private BindingInfo matchedParameters(IRNode targs, IRNode args, IJavaType[] argTypes, 
                                       IBinding mbind, IRNode formals, IJavaType[] tmpTypes, 
                                       Map<IJavaType,IJavaType> map,
                                       IJavaTypeSubstitution mSubst) {
    	// Get the last parameter 
    	final IRNode varType;
    	IRLocation lastLoc = JJNode.tree.lastChildLocation(formals);
    	if (lastLoc != null) {
    		IRNode lastParam = JJNode.tree.getChild(formals,lastLoc);
    		IRNode ptype = ParameterDeclaration.getType(lastParam);
    		if (VarArgsType.prototype.includes(ptype)) {
    			if (debug) {
    				LOG.finer("Handling variable numbers of parameters.");
    			}
    			varType = ptype;
    		} else {
    			varType = null;
    		}
    	} else {
    		varType = null;
    	}
    	final int numFormals = JJNode.tree.numChildren(formals);
    	if (varType != null) {
    		if (argTypes.length < numFormals - 1) {
    			if (debug) {
    				LOG.finer("Wrong number of parameters.");
    			}
        		return null;
    		}
    	} 
    	else if (numFormals != argTypes.length) {
    		if (debug) {
    			LOG.finer("Wrong number of parameters.");
    		}
    		return null;
    	}    	    	    	
    	
    	// First, capture type variables
    	// (expanding varargs to fill in what would be null)
    	final Iterator<IRNode> fe = JJNode.tree.children(formals);
    	IJavaType varArgBase = null;
    	for (int i=0; i < argTypes.length; ++i) {
    		IJavaType fty;
    		if (!fe.hasNext()) {
    			if (varType == null) {
    				LOG.severe("Not enough parameters to continue");
    				return null;
    			}
    			else if (varArgBase == null) {
    				LOG.severe("No varargs type to copy");
    				return null;
    			}
    			// Expanded from 
    			fty = varArgBase;
    		} else {
    			IRNode ptype  = ParameterDeclaration.getType(fe.next());    		
    			fty = typeEnvironment.convertNodeTypeToIJavaType(ptype);
    			//fty = JavaTypeFactory.convertNodeTypeToIJavaType(ptype,AbstractJavaBinder.this);
    			fty = mbind.convertType(fty);
    			if (ptype == varType && 
    			    (i < argTypes.length-1 || 
    			     (i==argTypes.length-1 && !(argTypes[i] instanceof IJavaArrayType)))) {
    				// FIX what's the right way to convert if the number of args match
    				IJavaArrayType at = (IJavaArrayType) fty;
    				varArgBase = at.getElementType();     		
    				fty = varArgBase;
    			}
    		}
    		tmpTypes[i] = fty;    		
    		if (map != null) {
    			capture(map, fty, argTypes[i]);
    		}
    	}
    	
      // Then, substitute and check if compatible
      final boolean isVarArgs = varType != null;
      int numBoxed = 0;    	
      for (int i=0; i < argTypes.length; ++i) {       
        IJavaType fty      = tmpTypes[i];
        IJavaType captured = map == null ? typeEnvironment.computeErasure(fty) : substitute(map, fty);          
        if (!isCallCompatible(captured,argTypes[i])) {        	
          // Check if need (un)boxing
          if (onlyNeedsBoxing(captured, argTypes[i])) {
        	  numBoxed++;
        	  continue;
          }
          if (isVarArgs && i == argTypes.length-1 && captured instanceof IJavaArrayType &&
              argTypes[i] instanceof IJavaArrayType) {
        	  // issue w/ the last/varargs parameter
        	  final IJavaArrayType at = (IJavaArrayType) captured;
        	  final IJavaType eltType = at.getElementType();
        	  final IRNode varArg = Arguments.getArg(args, i); 
        	  if (VarArgsExpression.prototype.includes(varArg)) {
        	    inner:
        		  for(IRNode arg : VarArgsExpression.getArgIterator(varArg)) {
        			  final IJavaType argType = getJavaType(arg);
        			  if (!isCallCompatible(eltType, argType)) {        	
        				  // Check if need (un)boxing
        				  if (onlyNeedsBoxing(eltType, argType)) {
        					  numBoxed++;
        					  continue inner;
        				  }
        				  return null;
        			  }
        		  }
        	      continue;
        	  }
          }
          if (debug) {
            LOG.finer("... but " + argTypes[i] + " !<= " + captured);
          }
          return null;
        }
      }
      if (map != null) {
    	if (mSubst == null) {
    	  mSubst = 
    	    FunctionParameterSubstitution.create(AbstractJavaBinder.this, mbind.getNode(), map);
    	}
        if (mSubst != IBinding.NULL) {
          return new BindingInfo(IBinding.Util.makeMethodBinding(mbind, mSubst), numBoxed, isVarArgs);
        }
      }
      return new BindingInfo(mbind, numBoxed, isVarArgs);
    }
    
    private boolean onlyNeedsBoxing(IJavaType formal, IJavaType arg) {
    	 if (formal instanceof IJavaPrimitiveType && arg instanceof IJavaDeclaredType) {    
    		 // Could unbox arg?
    		 IJavaDeclaredType argD = (IJavaDeclaredType) arg;
    		 IJavaType unboxed = JavaTypeFactory.getCorrespondingPrimType(argD);
    		 return unboxed != null && isCallCompatible(formal, unboxed);  
    	 }
    	 else if (formal instanceof IJavaDeclaredType && arg instanceof IJavaPrimitiveType) {
    		 // Could box arg?
    		 IJavaPrimitiveType argP = (IJavaPrimitiveType) arg;
    		 IJavaType boxed         = JavaTypeFactory.getCorrespondingDeclType(getTypeEnvironment(), argP);
    		 return boxed != null && isCallCompatible(formal, boxed);    		 
         }
    	 return false;
    }
    	
    private IJavaType substitute(Map<IJavaType, IJavaType> map, IJavaType fty) {
      if (fty == null) {
        return null;
      }
      if (map.isEmpty()) {
        return fty;
      }
      if (fty instanceof IJavaTypeFormal) {
        IJavaType rv = map.get(fty);  
        if (rv != null) {
          return rv;
        }
      }
      else if (fty instanceof IJavaDeclaredType) {
        IJavaDeclaredType dt = (IJavaDeclaredType) fty;
        // copied from captureDeclaredType
        final int size   = dt.getTypeParameters().size(); 
        boolean captured = false;
        List<IJavaType> params = new ArrayList<IJavaType>(size);
        for(int i=0; i<size; i++) {
          IJavaType oldT = dt.getTypeParameters().get(i);            
          IJavaType newT = substitute(map, oldT); 
          params.add(newT);
          if (!oldT.equals(newT)) {
            captured = true;                
          }
        }
        if (captured) {
          return JavaTypeFactory.getDeclaredType(dt.getDeclaration(), params, 
                                                 dt.getOuterType());
        }
      }
      else if (fty instanceof IJavaWildcardType) {
        return substituteWildcardType(map, fty);
      }
      else if (fty instanceof IJavaArrayType) {
    	  IJavaArrayType at = (IJavaArrayType) fty;
    	  IJavaType baseT   = at.getBaseType();
    	  IJavaType newBase = substitute(map, baseT);
    	  if (newBase != baseT) {
    		  return JavaTypeFactory.getArrayType(newBase, at.getDimensions());
    	  }
      }
      return fty;
    }

    private IJavaReferenceType substituteWildcardType(
        Map<IJavaType, IJavaType> map, IJavaType fty) {
      IJavaWildcardType wt     = (IJavaWildcardType) fty;
      if (wt.getUpperBound() != null) {
        IJavaReferenceType upper = (IJavaReferenceType) substitute(map, wt.getUpperBound());
        if (!upper.equals(wt.getUpperBound())) {
          return JavaTypeFactory.getWildcardType(upper, null);
        }
      }
      else if (wt.getLowerBound() != null) {
        IJavaReferenceType lower = (IJavaReferenceType) substitute(map, wt.getLowerBound());
        if (!lower.equals(wt.getLowerBound())) {
          return JavaTypeFactory.getWildcardType(null, lower);
        }
      }
      return wt;
    }
    
    /**
     * For now, try to handle the simple cases 
     * 1. T appears as the type of a parameter
     * 2. T appears as the base type of an array parameter (T[])
     * 3. T appears as a parameter of a parameterized type (Foo<T,T>)
     * 4. T appears as a bound on a wildcard (? extends T)
     * 
     * This no longer does any substitution
     */
    private void capture(Map<IJavaType, IJavaType> map,
                              IJavaType fty, IJavaType argType) {
      if (fty == null) {
        return;
      }
      if (map.isEmpty()) {
        return;
      }
      
      // Check case 1
      if (fty instanceof IJavaTypeFormal) {
        IJavaType rv = map.get(fty);        
        if (rv == fty) { // no substitution yet
          // Add mapping temporarily to do substitution on extends bound
          IJavaType extendsT = fty.getSuperclass(typeEnvironment);          
          map.put(fty, argType);
          
          if (typeEnvironment.isSubType(argType, substitute(map, extendsT))) {                      
            return;
          } else {
//            System.out.println("Couldn't quite match "+fty+", "+argType);
            // restore previous mapping
            map.put(fty, fty);
          }
        } else {
          // FIX What if it's not the identity mapping?
        }
      }
      // Check case 2
      else if (fty instanceof IJavaArrayType) {
        IJavaArrayType fat = (IJavaArrayType) fty;
        if (argType instanceof IJavaArrayType) {
          IJavaArrayType aat = (IJavaArrayType) argType;
          if (fat.getDimensions() == aat.getDimensions()) {  
        	capture(map, fat.getBaseType(), aat.getBaseType());  
          }
          else if (fat.getDimensions() < aat.getDimensions()) {          
        	final int diff = aat.getDimensions() - fat.getDimensions();
            capture(map, fat.getBaseType(), JavaTypeFactory.getArrayType(aat.getBaseType(), diff));          
          }
        }
      }
      // Check case 3
      else if (fty instanceof IJavaDeclaredType) {
        IJavaDeclaredType fdt = (IJavaDeclaredType) fty;
        final int size        = fdt.getTypeParameters().size();
        
        if (size == 0) {
          return; // No type parameters to look at   
        }
        if (argType instanceof IJavaDeclaredType) {
          IJavaDeclaredType adt = (IJavaDeclaredType) argType;
          captureDeclaredType(map, fdt, adt);
        }
      }
      // FIX This may require binding the return type first
      else if (fty instanceof IJavaWildcardType) {
        // nothing to capture
      }      
    }

    private void captureDeclaredType(final Map<IJavaType, IJavaType> map,
                                          final IJavaDeclaredType fdt, 
                                          final IJavaDeclaredType adt) {
      final int size = fdt.getTypeParameters().size();
      // Check if it's the same (parameterized) type
      if (fdt.getDeclaration().equals(adt.getDeclaration())) {
        if (size == adt.getTypeParameters().size()) {
          for(int i=0; i<size; i++) {
            IJavaType oldT = fdt.getTypeParameters().get(i);            
            capture(map, oldT, adt.getTypeParameters().get(i)); 
          }
        }
        // Looks to be a raw type of the same kind
        else if (size > 0 && adt.getTypeParameters().isEmpty()) { 
          captureMissingTypeParameters(map, fdt, adt);
        }
      }
      // Look at supertypes for type variables?
      if (ClassDeclaration.prototype.includes(fdt.getDeclaration())) {
        IJavaDeclaredType superT = adt.getSuperclass(typeEnvironment);
        if (superT != null) {
          captureDeclaredType(map, fdt, superT);
        }
      } else {
        // FIX is this right?
        for(IJavaType superT : adt.getSupertypes(typeEnvironment)) {
          capture(map, fdt, superT);
        }
      }
    }
    
    /**
     * Match up the parameters in fdt with the missing ones in adt
     */
    private void captureMissingTypeParameters(Map<IJavaType, IJavaType> map,
                                              final IJavaDeclaredType fdt, 
                                              final IJavaDeclaredType adt) {
      final Operator op = JJNode.tree.getOperator(adt.getDeclaration());            
      final IRNode formals;
      if (ClassDeclaration.prototype.includes(op)) {        
        formals = ClassDeclaration.getTypes(adt.getDeclaration());
      }
      else if (InterfaceDeclaration.prototype.includes(op)) {
        formals = InterfaceDeclaration.getTypes(adt.getDeclaration());
      }
      else {
        return; // nothing to do        
      }
      Iterator<IJavaType> fdtParams = fdt.getTypeParameters().iterator(); 
      for(IRNode tf : TypeFormals.getTypeIterator(formals)) {
        IJavaType fT = JavaTypeFactory.getTypeFormal(tf);
        capture(map, fdtParams.next(), fT.getSuperclass(typeEnvironment));
      }
    }

    private StringBuilder buildStringOfArgTypes(IJavaType[] argTypes) {
      StringBuilder sb = new StringBuilder();
      sb.append('(');
      for (int i=0; i < argTypes.length; ++i) {
        if (i != 0) {
          sb.append(',');
        }
        sb.append(argTypes[i]);
      }
      sb.append(')');
      return sb;
    }
    
    /**
     * Bind a call to an allocation (New expression)
     * @param node new expression to do binding on
     * @param type type of class to look up constructor for
     * @param targs TODO
     * @param args actual parameters
     */
    protected boolean bindAllocation(IRNode node, IJavaType ty, IRNode targs, IRNode args) {
      /*doAccept(args);
      System.err.println("Found allocation site" + DebugUnparser.toString(node));*/
      //IJavaType ty = typeEnvironment.convertNodeTypeToIJavaType(type);
      if (ty instanceof IJavaDeclaredType) {  
        IJavaDeclaredType recType = (IJavaDeclaredType) ty;
        IRNode tdecl = recType.getDeclaration();
        Operator op  = JJNode.tree.getOperator(tdecl);
        final String tname;
        if (InterfaceDeclaration.prototype.includes(op)) {
          // This can only appear in an AnonClassExpr
          recType = getTypeEnvironment().getObjectType();
          tname   = "Object";
        } else {
          // Use the type name for constructors
          tname = JJNode.getInfo(tdecl);
        }
        boolean success = bindCall(node,targs,args,tname, recType);
        if (!success) {
          // FIX hack to get things to bind for receivers of raw type     
          // (copied from visitMethodCall)
          IJavaType newType = convertRawType(ty, true);
          if (newType != ty) {
            success = bindCall(node,targs,args,tname, newType);
          }
          if (!success) {
            IJavaType newType2 = convertRawType(ty, false);
            if (newType2 != ty) {
              success = bindCall(node,targs,args,tname, newType2);
            }
            
            // Only skip debugging default calls          
            if (!success && pathToTarget == null /*&& 
                (!JavaCanonicalizer.isActive() || JJNode.tree.numChildren(args) > 0)*/) {
              bindCall(node,targs,args,tname, recType);
            }
          }
        }
        return success;
      } else {
        LOG.warning("Strange new call: type is " + ty);
      }
      return false;
    }
    
    // the various visit methods for "interesting" operators
    
    @Override
    public Void visitAnnotation(IRNode node) {
    	visit(node);
    	if (!isFullPass) {
    		return null;
    	}
    	// Copied from NamedType
    	String name = Annotation.getId(node);
    	/*
    	if (foundIssue) {
    		if ("Override".equals(name)) {
    			IRNode parent = JJNode.tree.getParent(node);
    			IRNode gparent = JJNode.tree.getParent(parent);
    			System.out.println("Binding anno: "+DebugUnparser.toString(gparent));
    		}
    	}
*/
        IRNode decl = classTable.getOuterClass(name,node);
        if (decl == null) { // probably because it's not fully qualified
        	final int lastDot = name.lastIndexOf('.');
        	IBinding b;
        	if (lastDot < 0) {
        		b = scope.lookup(name, node, IJavaScope.Util.isTypeDecl);            	
        	} else {
        		b = checkForNestedAnnotation(node, name, lastDot);
        	}
        	boolean success = bind(node, b);
        	if (!success) {
        		scope.lookup(name, node, IJavaScope.Util.isTypeDecl);    
        	}
        } else {
        	bind(node, decl);
        }
    	return null;
    }
    
    private IBinding checkForNestedAnnotation(final IRNode node, final String name, int lastDot) {
   		final String qname = name.substring(0, lastDot);
		final String id = name.substring(lastDot+1);
		IRNode decl = classTable.getOuterClass(qname, node);
		IBinding b = null;
		if (decl == null) {
			b = scope.lookup(qname, node, IJavaScope.Util.isTypeDecl);    
			if (b == null) {
				// Check for more nesting
				lastDot = qname.lastIndexOf('.');
				if (lastDot >= 0) { 
					b = checkForNestedAnnotation(node, qname, lastDot);
				}
			}
		}
		if (b != null) {
			decl = b.getNode();
		}
		if (decl != null) {
			IJavaScope scope = typeScope(JavaTypeFactory.convertIRTypeDeclToIJavaType(decl));
			return scope.lookup(id, node, IJavaScope.Util.isTypeDecl);    
		}
		return null;
    }
    
    @Override
    public Void visitAnonClassExpression(IRNode node) {
      /*
      if ("new Super { private int g #; { #; } { #; } }".equals(DebugUnparser.toString(node))) {
    	  System.out.println("Got ACE");
      }
      */
      String name    = JJNode.getInfoOrNull(node);
      IJavaScope sc;
      if (name != null) {
        IJavaScope.NestedScope nsc = new IJavaScope.NestedScope(scope);
        nsc.put(name, node);
        sc = nsc;
      } else {
        sc = scope;
      }
      // TODO: check if new structure for anonymous classes
      // causes trouble, or better would enable us to simplify this code.
      IRNode type  = AnonClassExpression.getType(node);
      IRNode args  = AnonClassExpression.getArgs(node);
      IRNode targs = null;
      IRNode parent = getOOSParent(node);
      if (parent != null) {
    	  handleTypeForOOS(OuterObjectSpecifier.getObject(parent), type);
      } else {
    	  doAccept(type, sc);
      }
      doAccept(args, sc);
      if (isFullPass && pathToTarget == null) {
        // FIX? never called if used with OuterObjectSpecifier
        doAccept(AnonClassExpression.getAlloc(node), sc);
        IJavaScope old = scope; 
        try {
          scope = sc;
          //System.out.println("Trying to bind ACE: "+DebugUnparser.toString(node));
          IJavaType ty = typeEnvironment.convertNodeTypeToIJavaType(type);
          boolean success = bindAllocation(node, ty, targs, args);
          if (!success && pathToTarget == null) {
        	  System.out.println("Couldn't bind "+DebugUnparser.toString(node));
          }
        } finally {
          scope = old;
        }        
        /*
        if (!node.valueExists(bindings.getUseToDeclAttr())) {
          System.out.println("no binding");
        } else {
          System.out.println(bindings+" bound ACE: "+node);
        }        
      } else {
        System.out.println(bindings+" unbound ACE: "+node);
        */
      }
      //System.out.println("hasFullInfo = "+bindings.containsFullInfo());
      doAccept(AnonClassExpression.getBody(node), sc);
      return null;
    }
    
    @Override
    public Void visitArrayType(IRNode node) {
      // No IRNode binding that makes sense
      bind(node, nullBinding); 
      return super.visitArrayType(node);      
    }
    
    @Override
    public Void visitBlockStatement(IRNode node) {
      doAcceptForChildren(node,new IJavaScope.NestedScope(scope));
      return null;
    }
    
    @Override
    public Void visitCatchClause(IRNode node) {
      IJavaScope.NestedScope catchScope = new IJavaScope.NestedScope(scope);
      catchScope.add(CatchClause.getParam(node));
      doAcceptForChildren(node, catchScope);
      return null;
    }
    
    @Override
    public Void visitClassBody(IRNode node) {
      if (debug) {
    	  LOG.finer("visiting getClassBody(IRNode)"); 
      }
      IRNode parent = JJNode.tree.getParent(node);
      IJavaType type = typeEnvironment.getMyThisType(parent);
      IJavaScope classScope = typeScope(type);
      doAcceptForChildren(node, new IJavaScope.ShadowingScope(classScope,scope));
      return null;
    }

    @Override
    public Void visitClassDeclaration(IRNode node) {
      return visitTypeDeclaration(node,ClassDeclaration.getTypes(node));
    }

    @Override
    public Void visitClassExpression(IRNode n) {
      if (!isFullPass) {
        return visit(n);
      }
      IRNode result;
      IRNode t = ClassExpression.getType(n);
      Operator top = JJNode.tree.getOperator(t);
      if (top instanceof ThisExpression) {
        // find the enclosing class
        result = VisitUtil.getEnclosingType(n);
      } else if (top instanceof NamedType || top instanceof NameType) {
        result = getBinding(t);
      } else if (top instanceof TypeRef) {
        result = getBinding(t);
      } else if (top instanceof PrimitiveType) {
        result = null; // XXX no IRNode for prim types?
      } else if (top instanceof ArrayType) {
        result = null; // XXX no IRNode for array types
      } else if (top instanceof VoidType) {
        result = null; // XXX no IRNode for void type
      } else {
        throw new IllegalArgumentException("Got "+top.name()+" as base for ClassExpr");
      }
      bind(n, result);
      return null;
    }
    
    @Override
    public Void visitClassInitializer(IRNode node) {
      IJavaScope withThis = scope;
      if (!JavaNode.getModifier(node,JavaNode.STATIC)) {
        IRNode tdecl = JJNode.tree.getParent(JJNode.tree.getParent(node));
        if (!InterfaceDeclaration.prototype.includes(tdecl)) {
        	IJavaScope.NestedScope sc = new IJavaScope.NestedScope(scope);
        	addReceiverDeclForType(tdecl, sc);
        	withThis = sc;
        }
      }
      doBatchAcceptForChildren(node,withThis);
      return null;
    }
    
    @Override
    public Void visitCompilationUnit(IRNode node) {
      scope = getImportTable(node);
      /*
      String unparse = DebugUnparser.toString(node);
      if (!unparse.contains("class") && !unparse.contains("interface") && !unparse.contains("enum")) {
    	  System.out.println(unparse);
      }
      */
      doAcceptForChildren(node);
      return null;
    }
    
    @Override
    public Void visitConstructorCall(IRNode node) {
      visit(node); // bind the arguments etc
      if (!isFullPass || pathToTarget != null) return null;
      
      ConstructorCall call = (ConstructorCall) getOperator(node);     
      IRNode object = call.get_Object(node);
      /*
      if (SuperExpression.prototype.includes(object)) {
    	  System.out.println("Binding super(): "+node);
      }
      */
      IJavaType ty = getJavaType(object);
      if (ty instanceof IJavaDeclaredType) {
        IRNode tdecl = ((IJavaDeclaredType)ty).getDeclaration(); 
        IRNode targs = call.get_TypeArgs(node);
        boolean success = bindCall(node,targs,call.get_Args(node),
                                   JJNode.getInfo(tdecl), ty);
        if (!success) {
        	bindCall(node,targs,call.get_Args(node), JJNode.getInfo(tdecl), ty);
        }
      } else {
        LOG.warning("super/this has non-class type! " + DebugUnparser.toString(node));
      }
      return null;
    }
    
    @Override
    public Void visitConstructorDeclaration(IRNode node) {
      return processMethodDeclaration(node, true);
    }    
    
    @Override
    public Void visitDeclStatement(IRNode node) {
      addDeclsToScope(DeclStatement.getVars(node),(IJavaScope.NestedScope)scope);
      return visit(node);
    }

    @Override
    public Void visitDemandName(IRNode node) {
      String pkg = DemandName.getPkg(node);
      bindToPackage(node, pkg);
      return null;
    }
    
    @Override
    public Void visitEnumDeclaration(IRNode node) {
        /*
        if (!isFullPass) {
            System.out.println("Partial pass on "+JJNode.getInfoOrNull(node));
        }
        */
        return super.visitEnumDeclaration(node);
    }
    
    @Override
    public Void visitEnumConstantClassDeclaration(IRNode node) {
        final String name = JJNode.getInfoOrNull(node);
        final IJavaScope sc;
        if (name != null) {
          IJavaScope.NestedScope nsc = new IJavaScope.NestedScope(scope);
          nsc.put(name, node);
          sc = nsc;
        } else {
          sc = scope;
        }
        try {
        	doAcceptForChildren(node, sc);        
        } catch (Exception e) {
        	e.printStackTrace();        	
        }
        return bindEnumConstantDeclaration(node, EnumConstantClassDeclaration.getArgs(node));
    }
    
    @Override
    public Void visitNormalEnumConstantDeclaration(IRNode node) {
        visit(node); // bind the arguments etc
        return bindEnumConstantDeclaration(node, NormalEnumConstantDeclaration.getArgs(node));
    }

    private Void bindEnumConstantDeclaration(IRNode node, IRNode args) {
        if (!isFullPass || pathToTarget != null) return null;
    	
    	IRNode tdecl = VisitUtil.getEnclosingType(node);
    	IJavaType ty = typeEnvironment.convertNodeTypeToIJavaType(tdecl);
        boolean success = bindCall(node, null, args, JJNode.getInfo(tdecl), ty);
        if (!success) {
            bindCall(node, null, args, JJNode.getInfo(tdecl), ty);
        }
        return null;
    }
    
    @Override
    public Void visitSimpleEnumConstantDeclaration(IRNode node) {
        visit(node); // bind the arguments etc
        /*
        if (isFullPass) {
            System.out.println("Full binding constant: "+JJNode.getInfoOrNull(node));     
        } else {
            System.out.println("Part Binding constant: "+JJNode.getInfoOrNull(node));     
        } 
        */  
        return bindEnumConstantDeclaration(node, null);
    }
    
    // Copied from ClassInitializer
    @Override
    public Void visitFieldDeclaration(IRNode node) {
      IJavaScope withThis;
      if (!JavaNode.getModifier(node,JavaNode.STATIC)) {
        IRNode tdecl = JJNode.tree.getParent(JJNode.tree.getParent(node));
        if (!InterfaceDeclaration.prototype.includes(tdecl)) {
          IJavaScope.NestedScope sc = new IJavaScope.NestedScope(scope);
          addReceiverDeclForType(tdecl, sc);        
          withThis = sc;
        } else {
          withThis = scope;
        }
      } else {
        withThis = scope;
      }
      // At this point we are visiting if on the way to a nested class (unlikely)
      // or if batch already, or if incremental and this has changed
      // (in which case we want batch).
      // Simplest is to ALWAYS use batch.
      doBatchAcceptForChildren(node,withThis);
      return null;
    }
    
    @Override
    public Void visitFieldRef(IRNode node) {
      visit(node); // bind the object
      if (isFullPass) {
        IJavaType ty = getJavaType(FieldRef.getObject(node));
        boolean success = bind(node, typeScope(ty), IJavaScope.Util.isValueDecl);
        if (!success) {
          bind(node, typeScope(ty), IJavaScope.Util.isValueDecl);
        }
      }
      return null; 
    }
    
    @Override
    public Void visitForStatement(IRNode node) {
      doAcceptForChildren(node,new IJavaScope.NestedScope(scope));
      return null;
    }
    
    @Override
    public Void visitForEachStatement(IRNode node) {
      IJavaScope.NestedScope foreachScope = new IJavaScope.NestedScope(scope);
      foreachScope.add(ForEachStatement.getVar(node));
      doAcceptForChildren(node, foreachScope);
      return null;
    }
    
    @Override
    public Void visitInterfaceDeclaration(IRNode node) {
      return visitTypeDeclaration(node,InterfaceDeclaration.getTypes(node));
    }

    @Override
    public Void visitLabeledBreakStatement(IRNode node) {
      bind(node,IJavaScope.Util.isLabeledStatement);
      return null;
    }
    @Override
    public Void visitLabeledContinueStatement(IRNode node) {
      visitLabeledBreakStatement(node);
      return null;
    }
    @Override
    public Void visitLabeledStatement(IRNode node) {
      IJavaScope.NestedScope sc = new IJavaScope.NestedScope(scope);
      sc.add(node);
      doAcceptForChildren(node,sc);
      return null;
    }
    
    @Override
    public Void visitMethodCall(IRNode node) {
      visit(node); // bind the arguments etc
      if (!isFullPass || pathToTarget != null) return null;
      MethodCall call   = (MethodCall) getOperator(node);
      IRNode receiver   = call.get_Object(node);
      IRNode args       = call.get_Args(node);
      IRNode targs      = call.get_TypeArgs(node);
      IJavaScope toUse  = null;
      IJavaType recType = null;
      if (JJNode.tree.getOperator(receiver) instanceof ImplicitReceiver) {
        toUse = scope;
      } else {
        recType = getJavaType(receiver);
//        if (recType instanceof IJavaDeclaredType) {
//          System.out.println(DebugUnparser.toString(((IJavaDeclaredType) recType).getDeclaration()));
//        }
        if (recType != null) toUse = typeScope(recType);
      }
      if (toUse != null) {
        String name = MethodCall.getMethod(node);    
        /*
        if ("clone".equals(name) && 
            recType != null && "sun.font.AttributeValues".equals(recType.getName())) {
        	System.out.println(node);
        }
        */
        if (recType instanceof IJavaDeclaredType) {
          IJavaDeclaredType dt = (IJavaDeclaredType) recType;
          if (AnnotationDeclaration.prototype.includes(dt.getDeclaration())) {
            if (JJNode.tree.hasChildren(args)) {
              throw new IllegalArgumentException("Illegal call to annotation element: "+DebugUnparser.toString(node));
            }
            return bindAnnotationElement(node, name, toUse); 
          }
        }
        boolean success = bindCall(node,targs,args,name, toUse);
        if (!success) {
          // FIX hack to get things to bind for receivers of raw type       
          IJavaType newType = convertRawType(recType, true);
          if (newType != recType) {
            success = bindCall(node,targs,args,name, newType);
          }
          if (!success) {
            IJavaType newType2 = convertRawType(recType, false);
            if (newType2 != recType) {
              success = bindCall(node,targs,args,name, newType2);
            }
            if (!success && pathToTarget == null) {
              IJavaType temp = getJavaType(receiver);
              bindCall(node,targs,args,name, toUse);
            }
          }
        }
      } else {
        LOG.severe("Nothing to look for a method name in. Skipping call to " + 
        		   JavaNames.genMethodConstructorName(node)+" in"+
        		   DebugUnparser.toString(VisitUtil.getEnclosingClassBodyDecl(node)));
        bind(node,(IRNode)null);
      }
      return null;
    }
    
    private Void bindAnnotationElement(IRNode node, String name, IJavaScope toUse) {
      boolean success = bind(node, toUse, IJavaScope.Util.isAnnotationElt);
      if (!success) {
        bind(node, toUse, IJavaScope.Util.isAnnotationElt);
      }      
      return null;
    }

    IJavaType convertRawType(IJavaType t, boolean asWildcard) {
      if (t instanceof IJavaDeclaredType) {
        IJavaDeclaredType dt   = (IJavaDeclaredType) t;
        List<IJavaType> params = dt.getTypeParameters();
        if (params == null || params.size() == 0) {
          IRNode decl = dt.getDeclaration();
          Operator op = JJNode.tree.getOperator(decl);
          IRNode formals;
     
          if (ClassDeclaration.prototype.includes(op)) {
            formals = ClassDeclaration.getTypes(decl);
          }
          else if (EnumDeclaration.prototype.includes(op) || 
                   AnnotationDeclaration.prototype.includes(op)) {
            return t; // No type formals possible
          }
          else {
            formals = InterfaceDeclaration.getTypes(decl);                     
          }       
          
          Iteratable<IRNode> tf = TypeFormals.getTypeIterator(formals);
          List<IJavaType> newParams;
          if (tf.hasNext()) {
            newParams = new ArrayList<IJavaType>();
            for(IRNode formal : tf) {
              IJavaTypeFormal f = JavaTypeFactory.getTypeFormal(formal);
              IJavaType superT  = f.getSuperclass(getTypeEnvironment());
              if (asWildcard && superT instanceof IJavaReferenceType) {
                IJavaReferenceType superRefT = (IJavaReferenceType) superT;
                newParams.add(JavaTypeFactory.getWildcardType(superRefT, null));
              } else {
                newParams.add(superT);
              }
            }
          } else {
            newParams = Collections.emptyList(); 
          }
          return JavaTypeFactory.getDeclaredType(decl, newParams, dt.getOuterType());
        }
      }
      return t;
    }
    
    @Override
    public Void visitMethodDeclaration(IRNode node) {
      return processMethodDeclaration(node, false);
    }

    private Void processMethodDeclaration(IRNode node, boolean isConstructor) {
      IJavaScope.NestedScope sc = new IJavaScope.NestedScope(scope);
      boolean isStatic          = JavaNode.getModifier(node,JavaNode.STATIC);
      if (!isStatic) {
        IRNode receiverNode = JavaPromise.getReceiverNodeOrNull(node); 
        if (receiverNode == null) {
          LOG.severe("No receiver for " + DebugUnparser.toString(node));
        } else {
          sc.put("this",receiverNode);
        }
      }
      IRNode returnNode = JavaPromise.getReturnNodeOrNull(node);
      if (returnNode != null) {
        sc.put("return", returnNode);
      }
      IRNode tformals = isConstructor? 
          ConstructorDeclaration.getTypes(node) : MethodDeclaration.getTypes(node);
      IRNode formals = isConstructor ?
          ConstructorDeclaration.getParams(node) : MethodDeclaration.getParams(node);
      addDeclsToScope(tformals, sc);
      addDeclsToScope(formals, sc);
      
      doAcceptForChildren(node,sc);
      
      if (!isFullPass || pathToTarget != null) return null;
      if (!isStatic) {
        // overriding
        IRNode tdecl = VisitUtil.getEnclosingType(node);
        if (tdecl == null) {
          LOG.severe("Surrounding tdecl is null?: " + DebugUnparser.toString(node));
        }
        IJavaType thisType = JavaTypeFactory.getDeclaredType(tdecl,null,null);
        List<IBinding> overrides = new ArrayList<IBinding>();
        List<IJavaType> paramTypes = new ArrayList<IJavaType>();
        for (IRNode p : JJNode.tree.children(SomeFunctionDeclaration.getParams(node))) {
          paramTypes.add(getJavaType(p));
        }      
        findOverrides(node,overrides,typeEnvironment.getSuperTypes(thisType),paramTypes);
        Object oldOverrides = node.getSlotValue(bindings.getMethodOverridesAttr());
        if (oldOverrides == null || !oldOverrides.equals(overrides)) {
          // we depend on the (semantically problematic) definition for equals:
          // that two lists are equal if they have the same elements.
          node.setSlotValue(bindings.getMethodOverridesAttr(),overrides);
        }
      }
      return null;
    }
    
    protected Void findOverrides(IRNode overrider, List<IBinding> overs, Iterator<IJavaType> types, List<IJavaType> paramTypes) {
      checkType: while (types.hasNext()) {
        IJavaType type = types.next();
        IJavaDeclaredType jt = (IJavaDeclaredType)(type);
        IJavaTypeSubstitution subst = JavaTypeSubstitution.create(getTypeEnvironment(), jt);
        IJavaMemberTable jmt = typeMemberTable(jt);
        Iterator<IRNode> enm = jmt.getDeclarationsFromUse(JJNode.getInfo(overrider),overrider);
        checkCandidates: while (enm.hasNext()) {
          IRNode mdecl = enm.next();
          if (JJNode.tree.getOperator(mdecl) instanceof MethodDeclaration &&
              JJNode.tree.numChildren(MethodDeclaration.getParams(mdecl)) == paramTypes.size()) {
            Iterator<IRNode> params = JJNode.tree.children(MethodDeclaration.getParams(mdecl));
            Iterator<IJavaType> match = paramTypes.iterator();
            if (debug) {
              LOG.finer("Considering override: " + mdecl + "=" + DebugUnparser.toString(mdecl) + " for " + overrider);
            }
            while (params.hasNext()) {
              IJavaType ty = getJavaType(params.next());
              if (ty != null) {
            	  ty = ty.subst(subst);
              }
              IJavaType oty = match.next();
              if (ty != oty) {
            	if (debug) {
            		LOG.finer("Rejected because " + ty + "!=" + oty);
            	}
                continue checkCandidates;
              }
            }
            // if not accessible, then stop looking in this class or its superclasses.
            if (!BindUtil.isAccessible(typeEnvironment, mdecl,overrider)) {
              if (debug) {
            	  LOG.finer("Rejected because inaccessible");
              }
              continue checkType;
            }
            // a full match!
            if (!overs.contains(mdecl)) {
              overs.add(IBinding.Util.makeBinding(mdecl,jt, getTypeEnvironment())); // FIX
            }
            // whether or not already in, we stop looking
            continue checkType;
          }
        }
        // nothing worked: try our supertypes:
        findOverrides(overrider, overs, typeEnvironment.getSuperTypes(type),paramTypes);
      }
      return null;
    } 
    
    private boolean bindForType(IRNode node) {
        final boolean isSpecialCase = isSpecialTypeCase(node);
        if (isFullPass) {
        	return isSpecialCase;
        } else {
        	return !isSpecialCase;
        }
    }
    
    @Override
    public Void visitNameType(IRNode node) {
      /*
      if ("String".equals(DebugUnparser.toString(node))) {
    	  System.out.println("NameType String");
      }
      */
      if (bindForType(node)) {
    	  visit(node);
    	  bind(node,getBinding(NameType.getName(node)));
      } else if (!isFullPass) {
    	  System.out.println("Ignoring NameType: "+DebugUnparser.toString(node));
      }
      return null;
    }
    
    @Override
    public Void visitNamedType(IRNode node) {
      if (!bindForType(node)) {
      	  return null;
      }
      String name = JJNode.getInfo(node);
      if (debug) {    	  
    	  LOG.finer("Got a named type " + name);
      }
      // Ignore if it's really for scoped promises
      if (name.contains("*")) {
        return null;
      }
      /*
      if (handlePromises && "".equals(name)) {
        // ignore these -- not meant to be bound
        return null;
      }
      */
      IRNode decl = classTable.getOuterClass(name,node);
      boolean success;
      
      // TODO Clean up this code!
      if (decl == null) { // probably because it's not fully qualified
        /*
        if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
          System.out.println(name);
          if (name.equals("Z")) {
            System.out.println(name);
          }
        }
        */
        IBinding b = scope.lookup(name, node, IJavaScope.Util.isTypeDecl);
        // Added for debugging
        if (b == null && !isBinary(node)) {
          scope.printTrace(System.out, 0);
          classTable.getOuterClass(name,node);
          scope.lookup(name, node, IJavaScope.Util.isTypeDecl);
        }
        success = bind(node, b);
        /*
        if (success) {
          System.out.println("Got a local class for " + name);
        }
        */
      } else {
        success = bind(node, decl);
        //System.out.println("Got an outer class for " + name);
        if (!success) {
          bind(node, decl);
        }
      }
      if (!success) {
        //System.out.println("failure to bind NT");
//      } else {
//        System.out.println("Bound NT: "+node);
      }
      return null; 
    }
    
    @Override
    public Void visitNamedPackageDeclaration(IRNode node) {
      String pkgName = NamedPackageDeclaration.getId(node);
      bindToPackage(node, pkgName);
      doAcceptForChildren(node);
      return null;
    }

    private void bindToPackage(IRNode node, String pkgName) {
      IRNode pkg;
      if (pkgName.length() == 0) {
        PackageDrop drop = PackageDrop.findPackage("");
        pkg = drop == null ? null : drop.cu;
      } else  {
        pkg = getClassTable().getOuterClass(pkgName, node);
      }
      bind(node, pkg);
    }
    
    @Override
    public Void visitNameExpression(IRNode node) {
      if (isFullPass) {
        visit(node); // don't visit names in expressions yet.
        IBinding b = getIBinding(NameExpression.getName(node)); 
        /*
        if ("commands".equals(DebugUnparser.toString(node)) &&
        	VariableDeclarator.prototype.includes(b.getNode())) {  
        	IRNode parent = JJNode.tree.getParentOrNull(b.getNode());
        	IRNode gparent = JJNode.tree.getParentOrNull(parent);
        	String unparse = DebugUnparser.toString(gparent);
        	if (unparse.contains("HashSet")) {
        		visit(node);
        	}
        }
        */
        bind(node,b);
      }
      return null;
    }
    
    @Override
    public Void visitNewExpression(IRNode node) {
      NewExpression newE = (NewExpression) getOperator(node);
      IRNode type = newE.get_Type(node);
      IRNode parent = getOOSParent(node);
      if (parent != null) {
    	  handleTypeForOOS(OuterObjectSpecifier.getObject(parent), type);
      } else {
    	  doAccept(type);
      }
      IRNode targs = newE.get_TypeArgs(node);
      doAccept(targs);
      // HACK no longer needed due to changes in bindAllocation?
      /*
      if (targs == null && ParameterizedType.prototype.includes(type)) {
        targs = ParameterizedType.getArgs(type);
      }
      */
      
      IRNode args  = newE.get_Args(node);
      doAccept(args);
      if (isFullPass && pathToTarget == null) {
        //System.out.println(DebugUnparser.toString(node));
    	IJavaType ty = typeEnvironment.convertNodeTypeToIJavaType(type);
        bindAllocation(node,ty,targs, args);
      }
      return null;
    } 
    
    @Override
    public Void visitParameterDeclaration(IRNode node) {
      IJavaScope.NestedScope sc = (IJavaScope.NestedScope)scope;
      sc.add(node); 
      if (!isBatch && nodeHasChanged(node)) isBatch = true;
      if (debug) {
        LOG.finer("Added param " + JJNode.getInfo(node) + " to local scope.");
      }
      visit(node);
      return null;
    }
    
    @Override
    public Void visitOuterObjectSpecifier(IRNode node) {
      if (true) {
        return visit(node);
      }    	
      if (!isFullPass || pathToTarget != null) {
        return visit(node);
      }
      /*
      if (foundIssue) {
    	  System.out.println("Binding OOS");
      }
*/
      // All of the below is in another granule now
      IRNode qual = OuterObjectSpecifier.getObject(node);
      IRNode alloc = OuterObjectSpecifier.getCall(node);
      IRNode type;
      IRNode args;
      IRNode targs;
      IRNode body;
      Operator aop  = JJNode.tree.getOperator(alloc);
      final boolean isACE;
      if (aop instanceof NewExpression) {
        NewExpression newE = (NewExpression) aop;
        type  = newE.get_Type(alloc);
        args  = newE.get_Args(alloc);
        targs = newE.get_TypeArgs(alloc);
        body  = null;
        isACE = false;
      } else if (aop instanceof AnonClassExpression) {
        AnonClassExpression anon = (AnonClassExpression) aop;
        type  = anon.get_Type(alloc);
        args  = anon.get_Args(alloc);
        targs = anon.get_TypeArgs(alloc);
        body  = anon.get_Body(alloc);
        isACE = true;
      } else if (aop instanceof ConstructorCall) {
        return visit(node);
      } else {
        LOG.warning("Unknown allocation expression: " + aop +" -- "+ DebugUnparser.toString(node));
        return null;
      }
      doAccept(qual); // bind the qualifier

      /* This is required for expressions like this:
       * 
       * class C {
       *   class Inner {
       *     class Innermost {}
       *   }
       *   Object foo() {
       *     Inner i = new Inner();
       *     return i.new Innermost();
       *   }
       * }
       */
      handleTypeForOOS(qual, type);      
      doAccept(targs);
      doAccept(args);

      IRNode typeDecl = getLocalIBinding(type).getNode();
      IJavaType ty = JavaTypeFactory.convertIRTypeDeclToIJavaType(typeDecl);
      boolean success = bindAllocation(alloc,ty,((IRNode)null), args);
      if (success && isACE) { 
    	  // bind NewE inside of ACE
    	  try {
    		  IBinding b = alloc.getSlotValue(bindings.getUseToDeclAttr());
        	  bind(AnonClassExpression.getAlloc(alloc), b);
    	  } catch (SlotUndefinedException e) {
        	  bindAllocation(alloc,ty,((IRNode)null), args);
    		  alloc.getSlotValue(bindings.getUseToDeclAttr());
    	  }
      /*
      } else if (!success && isACE) {
    	  System.out.println("ACE not bound: "+DebugUnparser.toString(node));
      */
      }    
      if (body != null) {
        doAccept(body);
      }
      return null;
    }

	private void handleTypeForOOS(IRNode qual, IRNode type) {
		IJavaScope qscope = typeScope(getJavaType(qual));
		doAccept(type, new IJavaScope.ShadowingScope(qscope, scope));
	}
    
    @Override
    public Void visitQualifiedName(IRNode node) {
      visit(node); // bind where we look from
      IRNode baseBinding = getBinding(QualifiedName.getBase(node));
      if (baseBinding == null) {
        bind(node,(IRNode)null);
        return null;
      }
      Operator bbop = JJNode.tree.getOperator(baseBinding);
      IJavaScope scope;
      if (bbop instanceof TypeDeclaration) {
        IJavaType baseType = JavaTypeFactory.getMyThisType(baseBinding);
        scope = typeScope(baseType);
      } else if (bbop instanceof VariableDeclarator || bbop instanceof ParameterDeclaration) {
        scope = typeScope(getJavaType(baseBinding));
      } else if (bbop instanceof NamedPackageDeclaration) {
        scope = classTable.packageScope(baseBinding);
      } else if (bbop instanceof EnumConstantDeclaration) {
    	scope = typeScope(getJavaType(baseBinding));
      } else {
        LOG.warning("Cannot process qualified name " + DebugUnparser.toString(node) +
            " base binding -> " + bbop);
        return null;
      }
      if (scope == null) {
        LOG.severe("scope is null for " + DebugUnparser.toString(node));
        return null;
      }
      boolean isType  = isNameType(node); // or Expression
      Selector select = isType ? IJavaScope.Util.isPkgTypeDecl : IJavaScope.Util.isntCallable;
      boolean success = bind(node,scope,select);
      if (!success) {
    	  bind(node,scope,select);
      }
      return null;
    }
    
    @Override
    public Void visitQualifiedSuperExpression(IRNode node) {
      if (!isFullPass) {
        return visit(node);
      }
      // super is bound to the same thing as this
      IRNode result = handleThisExpr(node, QualifiedSuperExpression.getType(node));
      bind(node, result);
      return null;
    }

    @Override
    public Void visitQualifiedThisExpression(IRNode node) {
      if (!isFullPass) {
        return visit(node);
      }
      /*
      // Get the type for Foo.this
      IRNode nt = QualifiedThisExpression.getType(node);
      IRNode td = getBinding(nt);
      
      // Figure out where to get the QualifiedReceiverDecl from
      IRNode bd   = VisitUtil.getEnclosingClassBodyDecl(node);
      Operator op = JJNode.tree.getOperator(bd);
      IRNode result;
      if (op instanceof MethodDeclaration || op instanceof ConstructorDeclaration) {
        result = JavaPromise.getQualifiedReceiverNodeByName(bd, td);
      }
      else {
        IRNode enclosingT = VisitUtil.getEnclosingType(node);
        IRNode initD      = JavaPromise.getInitMethod(enclosingT);
        result = JavaPromise.getQualifiedReceiverNodeByName(initD, td);
      }
      */
      IRNode result = handleThisExpr(node, QualifiedThisExpression.getType(node));
      bind(node, result);

      /*
      // Brute force: walk to the root, looking for the thing
      String lookingFor = JavaNode.getInfo(node);
      Iterator<IRNode> rootWalk = JJNode.tree.rootWalk(node);
      while (rootWalk.hasNext()) {
        IRNode p = rootWalk.next();
        Operator op = JJNode.tree.getOperator(p);
        IRNode rec = null;
        String className = null;
        if (op instanceof MethodDeclaration || op instanceof ConstructorDeclaration) {
          rec = ReceiverDeclaration.getReceiverNode(p);
          IRNode ggp = JJNode.tree.getParent(JJNode.tree.getParent(p));
          if (JJNode.tree.getOperator(ggp) instanceof TypeDeclaration) {
            className = JavaNode.getInfo(ggp);
          }
        } else if (op instanceof TypeDeclaration) {
          rec = InitDeclaration.getInitMethod(p); //TODO: Shouldn't it be the receiver of this method?
          className = JavaNode.getInfo(p);
        }
        if (rec == null) continue; // move on
        if (className == null) continue; // TODO: what about anonymous classes
        if (className.equals(lookingFor)) {
          bind(node,rec);
        } else {
          LOG.warning("Cannot find qualified this/super for " + lookingFor);
        }
      }
      */
      return null;
    }
    
    // Copied from EclipseBinder
    private boolean isParameterToAnonClassExpr(Operator op, IRNode decl, IRNode thisE) {
      if (AnonClassExpression.prototype.includes(op)) {      
        IRNode parent = JJNode.tree.getParent(thisE);
        if (Arguments.prototype.includes(parent)) {
          IRNode gparent = JJNode.tree.getParent(parent);
          IRNode alloc   = AnonClassExpression.getAlloc(decl);
          if (alloc.equals(gparent)) {
            return true;
          }
        }
      }
      return false;
    }    
    
    private IRNode handleThisExpr(final IRNode n, final IRNode contextType) {
      IRNode contextTypeB = null;
      if (contextType != null) {
        contextTypeB = getBinding(contextType);
      }
      // Could be part of a field or initializer
      // Receiver defined on initializer (for now)
      IRNode decl = VisitUtil.getEnclosingClassBodyDecl(n);
      if (decl != null) {
        Operator op = JJNode.tree.getOperator(decl);
        if (isParameterToAnonClassExpr(op, decl, n)) {
          // "inside" a ACE
          decl = VisitUtil.getEnclosingClassBodyDecl(decl);
          op   = JJNode.tree.getOperator(decl);
        }
        if (MethodDeclaration.prototype.includes(op) ||
            ConstructorDeclaration.prototype.includes(op)) {
          if (contextTypeB != null) {
            return JavaPromise.getQualifiedReceiverNodeByName(decl, contextTypeB);
          }
          return JavaPromise.getReceiverNodeOrNull(decl);
        }
      }
      // initializer or method
      IRNode type = VisitUtil.getEnclosingType(n);
      IRNode rv;
      //decl = JavaPromise.getInitMethodOrNull(type);
      
      if (contextTypeB != null && contextTypeB != type) {
        rv = JavaPromise.getQualifiedReceiverNodeByName(type, contextTypeB);
      } else {
        rv = JavaPromise.getReceiverNodeOrNull(type);
      }
      if (type == null || decl == null || rv == null) {
        LOG.severe("Got nulls while binding "+DebugUnparser.toString(n));
        JavaPromise.getQualifiedReceiverNodeByName(type, contextTypeB);
      }
      return rv;
    }
    
    @Override
    public Void visitParameterizedType(IRNode node) {
      if (!bindForType(node)) {
    	return null;
      }  	
      visit(node); // bind types      
      IJavaType ty = typeEnvironment.convertNodeTypeToIJavaType(node);
      bind(node, ((IJavaDeclaredType) ty).getDeclaration());
       return null;
    }
    
    @Override
    public Void visitPrimitiveType(IRNode node) {
        // No IRNode binding that makes sense
        bind(node, nullBinding); 
        return super.visitPrimitiveType(node);
    }
    
    @Override
    public Void visitReturnStatement(IRNode node) {
      visit(node); // bind expression
      bind(node,IJavaScope.Util.isReturnValue,"return");
      return null;
    }

    private boolean isNameType(final IRNode node) {
    	IRNode here = node;
    	while (here != null) {
    		IRNode parent = JJNode.tree.getParentOrNull(here);
    		Operator pop  = JJNode.tree.getOperator(parent);
    		if (NameType.prototype.includes(pop)) {
    			return true;
    		}
    		else if (NameExpression.prototype.includes(pop)) {
    			return false;
    		}
    		else if (!(pop instanceof IllegalCode)) {
    			throw new IllegalArgumentException("Bad parent: "+pop.name()+" for "+parent);
    		}
    		here = parent;
    	}
    	throw new IllegalArgumentException("What is this? "+node);
    }
    
    @Override
    public Void visitSimpleName(IRNode node) {
      boolean isType  = isNameType(node); // or Expression
      Selector select = isType ? IJavaScope.Util.isPkgTypeDecl : IJavaScope.Util.isntCallable;
      boolean success = bind(node, select);
      /*
      String unparse = DebugUnparser.toString(node);
      if (unparse.contains("lattice")) {
    	  IRNode eT  = VisitUtil.getEnclosingType(node);
    	  if ("MustHoldTransfer".equals(JavaNames.getTypeName(eT))) {
    		  IBinding b = getIBinding(node); 
    		  IRNode bn  = b.getNode();
    		  if (VariableDeclarator.prototype.includes(bn)) {
    			  IRNode decl  = VisitUtil.getEnclosingClassBodyDecl(bn);
    			  IJavaType bt = getJavaType(bn);    		 
    			  if (bt instanceof IJavaTypeFormal) {
    				  System.out.println("MustHoldTransfer -- "+node+": "+b);
    				  //bind(node,IJavaScope.Util.isntCallable);
    			  }
    		  }
    	  }
      }
      */
      if (!success) {
    	  bind(node, select);
    	  /*
      } else if ("String".equals(SimpleName.getId(node))) {
    	  System.out.println("isFullPass("+this.isFullPass+") for "+node);
    	  System.out.println("Use to decl SI: "+this.bindings.getUseToDeclAttr());    	  
    	  */
      }
      return null;
    }
    
    @Override
    public Void visitStaticImport(IRNode node) {      
      visit(node); // bind children
      if (isFullPass) {
        // Partially copied from FieldRef
        IJavaType ty     = getJavaType(StaticImport.getType(node));
        IJavaScope scope = typeScope(ty);
        boolean success = bind(node, scope, IJavaScope.Util.isDecl);        		            		   
        if (!success) {
        	bind(node, scope, IJavaScope.Util.isDecl);
        }
      }
      return null;
    }
    
    @Override
    public Void visitStaticDemandName(IRNode node) {
      visit(node);
      if (isFullPass) {  
        IJavaType ty = getJavaType(StaticDemandName.getType(node));
        if (ty instanceof IJavaDeclaredType) {
          IJavaDeclaredType dt = (IJavaDeclaredType) ty;
          bind(node, dt.getDeclaration());
        }
      }
      return null;
    }
    
    @Override
    public Void visitSuperExpression(IRNode node) {
      // super is bound to the same thing as this
      visitThisExpression(node);
      return null;
    }
    
    @Override
    public Void visitSwitchStatement(IRNode node) {
      if (isFullPass) {
    	  doAccept(SwitchStatement.getExpr(node));
    	  //IJavaType enumType   = getTypeEnvironment().findJavaTypeByName("java.lang.Enum");
    	  IJavaType switchType = getJavaType(SwitchStatement.getExpr(node));
    	  //if (getTypeEnvironment().isSubType(switchType, enumType)) {
    	  if (switchType instanceof IJavaDeclaredType) {
    		  // Assume to be an enum type
    		  IJavaScope.NestedScope switchScope = new IJavaScope.NestedScope(scope);
    		  IJavaDeclaredType switchDT         = (IJavaDeclaredType) switchType;
    		  for(IRNode n : VisitUtil.getClassBodyMembers(switchDT.getDeclaration())) {

    			  if (EnumConstantDeclaration.prototype.includes(n)) {
        			  //System.out.println("Adding:   "+DebugUnparser.toString(n));
    				  switchScope.add(n);
    			  } else {
        			  //System.out.println("Rejected: "+DebugUnparser.toString(n));
    			  }
    		  }
        	  doAccept(SwitchStatement.getBlock(node), switchScope);
    		  return null;
    	  } else {
    		  doAccept(SwitchStatement.getBlock(node));
    	  }
    	  return null;
      } 
      return super.visitSwitchStatement(node);      
    }
    
    @Override
    public Void visitThisExpression(IRNode node) {
      bind(node, IJavaScope.Util.isReceiverDecl, "this");
      return null;
    }

    @Override
    public Void visitType(IRNode node) {
      if (isFullPass) {
    	return null;
      }
      return super.visitType(node);
    }
    
    /**
     * Visit a type declaration that has a type formals node.
     * @param node type declaration node
     * @param tformals sequence of type formals
     * @return null always
     */
    public Void visitTypeDeclaration(IRNode node, IRNode tformals) {
      /*
      if ("TestBindingInnerClass".equals(JJNode.getInfo(node))) {    	
    	  System.out.println("Binding type: "+JavaNames.getFullTypeName(node));
      }
      */
      IJavaScope.NestedScope sc = new IJavaScope.NestedScope(scope);
      sc.add(node);
      addDeclsToScope(tformals, sc);
      doAcceptForChildren(node,sc);
      return null;
    }

    @Override
    public Void visitTypeDeclarationStatement(IRNode node) {
      // add ourselves, and then recurse
      // TODO: BUG: we assume that there are no locals with the same name...
      // (NestedScope doesn't handle overloading.)
      IRNode tdecl = TypeDeclarationStatement.getTypedec(node);
      ((IJavaScope.NestedScope)scope).add(tdecl);
      if (!isBatch && nodeHasChanged(tdecl)) isBatch = true;
      return visit(node);
    }

    @Override
    public Void visitTypedDemandName(IRNode node) {
      visit(node);
      bind(node, (IRNode) null);
      return null;
    }
    
    @Override
    public Void visitTypeRef(IRNode node) {
      if (!bindForType(node)) {
    	  return null;
      }
      visit(node); // bind base type
      
      IRNode base = TypeRef.getBase(node);
      IRNode baseDecl = getLocalIBinding(base).getNode();
      IJavaScope tScope = typeScope(JavaTypeFactory.convertIRTypeDeclToIJavaType(baseDecl));
      boolean success = bind(node,tScope,IJavaScope.Util.isTypeDecl);
      if (!success) {
        bind(node,tScope,IJavaScope.Util.isTypeDecl);
      }
      return null;
    }
    
    @Override
    public Void visitUnnamedPackageDeclaration(IRNode node) {
      bindToPackage(node, "");
      return null;
    }
    
    @Override
    public Void visitVariableUseExpression(IRNode node) {
      if (isFullPass) {
    	boolean bound = bind(node,IJavaScope.Util.isValueDecl);
    	if (!bound) {
    	  bind(node,IJavaScope.Util.isValueDecl);
    	}
      }
      return null;
    }
      
    @Override
    public Void visitVoidReturnStatement(IRNode node) {
      visit(node);
      return null;
    }
  }

  /**
   * A type environment implementation linked to this binder.
   */
  private class TypeEnv extends AbstractTypeEnvironment {
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.bind.ITypeEnvironment#getBinder()
     */
    public IBinder getBinder() {
      return AbstractJavaBinder.this;
    }
    
    @Override
    public IRNode getArrayClassDeclaration() {
      return findNamedType("java.lang.[]");
    }

    @Override
    public IJavaClassTable getClassTable() {
      return classTable;
    }
    
	public IIRProject getProject() {
	  throw new UnsupportedOperationException();
	}
  }
  
  public interface IDependency {
    /**
     * Add a new use to the dependency.  It is informed of all changes thus far.
     * @param use useSite to add.
     */
    void addUse(IRNode use);
  }
  
  /**
   * An Unversioned Dependency.
   * TODO: Move this to UnversionedBinder
   * XXX: bug!  Cannot drop dependencies: must record to invalidate bindings
   * @author boyland
   */
  public static abstract class UnversionedDependency implements IDependency {
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.bind.AbstractJavaBinder.IDependency#addUse(edu.cmu.cs.fluid.ir.IRNode)
     * XXX: bug can't drop dependencies.
     */
    public final void addUse(IRNode use) {
    	// FIX need to do something here
    }
  }
  
  static boolean isBinary(IRNode n) {
  	IRNode cu = VisitUtil.getEnclosingCompilationUnit(n);
  	return JavaNode.getModifier(cu, JavaNode.AS_BINARY);
  }
}
