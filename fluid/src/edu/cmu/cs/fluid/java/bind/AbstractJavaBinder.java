/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/AbstractJavaBinder.java,v 1.145 2008/11/21 16:40:43 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.ThreadSafe;
import com.surelogic.analysis.IIRProject;
import com.surelogic.common.XUtil;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.drops.PackageDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IJavaScope.LookupContext;
import edu.cmu.cs.fluid.java.bind.IJavaScope.Selector;
import edu.cmu.cs.fluid.java.bind.MethodBinder.CallState;
import edu.cmu.cs.fluid.java.operator.Annotation;
import edu.cmu.cs.fluid.java.operator.AnnotationDeclaration;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.Arguments;
import edu.cmu.cs.fluid.java.operator.ArrayType;
import edu.cmu.cs.fluid.java.operator.Call;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.ClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassType;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.DemandName;
import edu.cmu.cs.fluid.java.operator.ElementValuePair;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.Expression;
import edu.cmu.cs.fluid.java.operator.Extensions;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.ForEachStatement;
import edu.cmu.cs.fluid.java.operator.IAcceptor;
import edu.cmu.cs.fluid.java.operator.IllegalCode;
import edu.cmu.cs.fluid.java.operator.Implements;
import edu.cmu.cs.fluid.java.operator.ImplicitReceiver;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.Name;
import edu.cmu.cs.fluid.java.operator.NameExpression;
import edu.cmu.cs.fluid.java.operator.NameType;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.NormalEnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.OuterObjectSpecifier;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.PrimitiveType;
import edu.cmu.cs.fluid.java.operator.QualifiedName;
import edu.cmu.cs.fluid.java.operator.QualifiedSuperExpression;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.Resources;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.Statement;
import edu.cmu.cs.fluid.java.operator.StaticDemandName;
import edu.cmu.cs.fluid.java.operator.StaticImport;
import edu.cmu.cs.fluid.java.operator.SwitchStatement;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.TryResource;
import edu.cmu.cs.fluid.java.operator.Type;
import edu.cmu.cs.fluid.java.operator.TypeActuals;
import edu.cmu.cs.fluid.java.operator.TypeDeclInterface;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclarationStatement;
import edu.cmu.cs.fluid.java.operator.TypeFormals;
import edu.cmu.cs.fluid.java.operator.TypeRef;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableResource;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.java.operator.VoidType;
import edu.cmu.cs.fluid.java.util.BindUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableList;
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
@ThreadSafe
public abstract class AbstractJavaBinder extends AbstractBinder {
  protected static final Logger LOG = SLLogger.getLogger("FLUID.java.bind");
  
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
    if (XUtil.useExperimental) {
      SLLogger.getLogger().log(Level.INFO, "partial bindings = " + numPartial);
      SLLogger.getLogger().log(Level.INFO, "full bindings = " + numFull);
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
  public static boolean isGranule(IRNode node, Operator op) {
	/*
	// Check if already calculated
  	final int mods = JavaNode.getModifiers(node);
  	if (JavaNode.isSet(mods, JavaNode.NOT_GRANULE)) {
  		return false;
  	}
  	if (JavaNode.isSet(mods, JavaNode.IS_GRANULE)) {
  		return true;
  	}
    if (op == null) {
    	op = JJNode.tree.getOperator(node);
    }
  	final boolean rv = isGranule_private(node, op);
  	JavaNode.setModifiers(node, JavaNode.setModifier(mods, rv ? JavaNode.IS_GRANULE : JavaNode.NOT_GRANULE, true));
  	return rv;
  }
  
  public static boolean isGranule(IRNode node) {
	  return isGranule(node, null);
  }
  *
  private static boolean isGranule_private(IRNode node, Operator op) {
  */
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
    else if (op instanceof ClassType) {
    	IRNode parent = JJNode.tree.getParentOrNull(node);
    	return TypeDeclaration.prototype.includes(parent);
    }
    else if (op instanceof Implements || op instanceof Extensions) {
    	return true;
    }
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
	while (node != null) {
		synchronized (node) {
		    Operator op = JJNode.tree.getOperator(node);
		    if (isGranule(node, op)) {
		    	break;
		    }
		    // body of original loop below
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
	}
	/*
    Operator op;    
    while (!(isGranule(node, op = JJNode.tree.getOperator(node)))) {
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
    */
    return node;
  }
  
   /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.java.bind.IBinder#getIBinding(edu.cmu.cs.fluid.ir.IRNode)
   */
  @Override
  protected IBinding getIBinding_impl(IRNode node) {
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
	         TypeActuals.prototype.includes(op) || Annotation.prototype.includes(op);
  }
  
  protected IGranuleBindings ensureBindingsOK(final IRNode node) {    
    final IRNode gr            = getGranule(node);
    final Operator op          = JJNode.tree.getOperator(node); 
    final boolean needFullInfo = needFullInfo(node, op, gr);
    final Map<IRNode,IGranuleBindings> granuleBindings =
  	  needFullInfo ? allGranuleBindings : partialGranuleBindings; 
    // Note: don't alternate between locking on the IGranuleBinding and the Binder 
    IGranuleBindings bindings = granuleBindings.get(gr);
    if (bindings == null || bindings.isDestroyed()) {
    	bindings = makeGranuleBindings(gr, needFullInfo);	
    	granuleBindings.put(gr,bindings);    	
    }
  
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
  
  protected abstract IGranuleBindings makeGranuleBindings(IRNode cu, boolean needFullInfo);
 
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
    //LOG.finer("Found fake array decl " + fakeArrayDeclaration);
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
      else if (wt.getUpperBound() != null) {
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
      /*
      IJavaScope sc       = typeScope(ct.getWildcard());
      if (ct.getLowerBound() != null) {
    	  sc = new IJavaScope.ShadowingScope(sc, typeScope(ct.getLowerBound()));
      }
      */
      return typeScope(ct.getUpperBound());
    } else if (ty instanceof IJavaPrimitiveType) {
      // Handling non-canonicalized code
      IJavaPrimitiveType pty = (IJavaPrimitiveType) ty;
      IJavaDeclaredType dty  = JavaTypeFactory.getCorrespondingDeclType(getTypeEnvironment(), pty);
      // Same as above
      return new IJavaScope.SubstScope(javaTypeScope(dty), getTypeEnvironment(), dty);
    } else if (ty instanceof IJavaNullType) {
    	// TODO is this right?
        //return typeScope(typeEnvironment.getObjectType());
        return IJavaScope.nullScope;
    } else if (ty instanceof IJavaUnionType) {
    	IJavaUnionType uty = (IJavaUnionType) ty;
    	TypeUtils utils = new TypeUtils(typeEnvironment);
    	IJavaType glb = utils.getGreatestLowerBound(uty.getFirstType(), uty.getAlternateType());
    	return typeScope(glb);
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
  
  private static final IBinding nullBinding = IBinding.Util.makeBinding(null);
  
  static int numChildrenOrZero(IRNode node) {
      if (node == null) {
        return 0;
      }
      return JJNode.tree.numChildren(node);
  }
  
  enum NameContext {
	  TYPE(IJavaScope.Util.isPkgTypeDecl), 
	  NOT_TYPE(IJavaScope.Util.couldBeNonTypeName), 
	  EITHER(IJavaScope.Util.couldBeName);
	  
	  final Selector selector;
	  
	  NameContext(Selector s) {
		  selector = s;
	  }
	  boolean couldBeType() {
		  return this != NOT_TYPE;
	  }
	  boolean couldBeVariable() {
		  return this != TYPE;
	  }
  }
  
  /**
   * The actual work of binding and maintaining scopes.
   * This code has extra machinery in it to handle granules and incrementality.
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
    private final MethodBinder methodBinder = new MethodBinder(AbstractJavaBinder.this, debug);    
    private final IJavaScope.LookupContext lookupContext = new IJavaScope.LookupContext();
    
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
      if (isGranule(node, op)) {
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
    	return bind(node, binding, false);
    }
    
    private boolean bind(IRNode node, IBinding binding, boolean quietWarnings) {      
      if (pathToTarget != null) {
    	  //System.out.println("Throwing away binding for "+DebugUnparser.toString(node));
    	  return false; // don't bind: not in the target granule
      }      
      if (binding == null) {
        if (quietWarnings) {
        	return false;
        }
    	final String unparse = DebugUnparser.toString(node);
    	if (isBinary(node)) {
    		if (!unparse.endsWith(" . 1")) {
    			System.err.println("Cannot find a binding for " + unparse+" in "+typeEnvironment);
    			/*
    			if (unparse.endsWith("ModuleType")) {
    				IRNode eType = VisitUtil.getEnclosingType(node);
    				System.out.println("PROBLEM in "+JavaNames.getFullTypeName(eType));
    			}
                */
    		}
    	} else if (unparse.startsWith("super")) {
    		System.err.println("Cannot find a binding for " + unparse+" in "+typeEnvironment);
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
    	return bind(node, selector, false, name);
    }
    
   	protected boolean bind(IRNode node, Selector selector, boolean quietWarnings, String name) {    	
      // LOG.finer(name);
      return bind(node,scope,selector,quietWarnings,name);
    }
    
    /**
     * Bind the node given to the declaration that matches the selector.
     * @param node IRNode to get the binding
     * @param sc Scope to perform binding in
     * @param selector used to choose the binding
     */
    protected boolean bind(IRNode node, IJavaScope sc, Selector selector) {
      return bind(node,sc,selector,false,JJNode.getInfo(node));
    }
    
    /**
     * Bind the node given to the declaration that matches the selector.
     * @param node IRNode to get the binding
     * @param sc Scope to perform binding in
     * @param selector used to choose the binding
     * @param name name to perform lookup using
     * @return true if bound to non-null
     */
    protected boolean bind(IRNode node, IJavaScope sc, Selector selector, boolean quietWarnings, String name) {
      if (debug) {
        LOG.finer("Looking up " + name + " in " + sc);
      }
      if (sc != null) {
        IBinding binding = sc.lookup(lookupContext.use(name,node),selector);
        if (binding == null) {
          sc.lookup(lookupContext.use(name,node),selector);
        }
        bind(node, binding, quietWarnings);
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
      return bind(node,selector,false);
    }
    
    protected boolean bind(IRNode node, Selector selector, boolean quietWarnings) {
      return bind(node,selector,quietWarnings,JJNode.getInfo(node));
    }
	
    /**
     * @return true if bound
     */
    protected boolean bindCall(CallState call, String name, IJavaType recType) {
      return bindCall(call, name, typeScope(recType));
    }
    
    /**
     * @return true if bound
     */
    protected boolean bindCall(final CallState state, final String name, final IJavaScope sc) {
      /*
      if (pathToTarget != null) {
    	  System.out.println("Not computing binding for call: "+DebugUnparser.toString(call));
    	  return false; // skip the work   
      }
      */
      /*
      if (name.equals("toArray")) {
    	  System.out.println("Binding call: "+DebugUnparser.toString(call));
      }
      */
      if (debug) {
          StringBuilder sb = buildStringOfArgTypes(state.getArgTypes());
          LOG.finer("Looking for method: " + name + sb + getInVersionString());
      }
      final IRNode from;
      final Operator callOp = JJNode.tree.getOperator(state.call);
      final boolean needMethod;
      if (AnonClassExpression.prototype.includes(callOp)) {
    	  from = AnonClassExpression.getBody(state.call);
    	  needMethod = false;
      } else {
    	  from = state.call;
    	  needMethod = MethodCall.prototype.includes(callOp);
      }
      
      lookupContext.use(name,state.call);
      BindingInfo bestMethod = methodBinder.findBestMethod(sc, lookupContext, needMethod, from, state);
      /*
      if (bestMethod != null && AnonClassExpression.prototype.includes(call)) {
        System.out.println("Binding "+call);
      }
      */
      if (bestMethod == null) {
    	  return bind(state.call, (IBinding) null);
      }
      /*
      if ("getCurrentKey".equals(name)) {      	
      	System.out.println("Context type for getCurrentKey() = "+bestMethod.method.getContextType());
      }
      */
      return bind(state.call, bestMethod.method);
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
    protected boolean bindAllocation(CallState state) {
      final IJavaType ty = state.receiverType;
      /*doAccept(args);
      System.err.println("Found allocation site" + DebugUnparser.toString(node));*/
      if (ty instanceof IJavaDeclaredType) {  
        IJavaDeclaredType recType = (IJavaDeclaredType) ty;
        IRNode tdecl = recType.getDeclaration();
        Operator op  = JJNode.tree.getOperator(tdecl);
        final String tname;
        if (InterfaceDeclaration.prototype.includes(op) || AnnotationDeclaration.prototype.includes(op)) {
          // This can only appear in an AnonClassExpr
          recType = getTypeEnvironment().getObjectType();
          tname   = "Object";
        /*
        } else if (AnnotationDeclaration.prototype.includes(op)) {
          // This can only appear in an AnonClassExpr
          recType = (IJavaDeclaredType) getTypeEnvironment().findJavaTypeByName("java.lang.Annotation");
          tname   = "Annotation";
         */
        } else {
          // Use the type name for constructors
          tname = JJNode.getInfo(tdecl);
        }
        /*
        if (tname.contains("CheckStartedThread")) {
        	System.out.println("Binding newE: "+DebugUnparser.toString(node));
        }
        */
        boolean success = bindCall(state, tname, recType);
        if (!success) {
          // FIX hack to get things to bind for receivers of raw type     
          // (copied from visitMethodCall)
          IJavaType newType = convertRawType(ty, true);
          if (newType != ty) {
            success = bindCall(state,tname, newType);
          }
          if (!success) {
            IJavaType newType2 = convertRawType(ty, false);
            if (newType2 != ty) {
              success = bindCall(state,tname, newType2);
            }
            
            // Only skip debugging default calls          
            if (!success && pathToTarget == null /*&& 
                (!JavaCanonicalizer.isActive() || JJNode.tree.numChildren(args) > 0)*/) {
              bindCall(state,tname, recType);
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
    	/* This is wrong; it needs to get bound either way
    	 * 
    	if (!isFullPass) {
    		return null;
    	}
    	*/
    	//System.out.println("Binding "+node+" = "+DebugUnparser.toString(node));
    	
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
        		b = scope.lookup(lookupContext.use(name, node), IJavaScope.Util.isTypeDecl);            	
        	} else {
        		b = checkForNestedAnnotation(node, name, lastDot);
        	}
        	boolean success = bind(node, b);
        	if (!success) {
        		scope.lookup(lookupContext.use(name, node), IJavaScope.Util.isTypeDecl);    
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
		if (decl != null && !AnnotationDeclaration.prototype.includes(decl)) {
			// Not a type decl
			decl = null;
		}
		IBinding b = null;
		if (decl == null) {
			b = scope.lookup(lookupContext.use(qname, node), IJavaScope.Util.isTypeDecl);    
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
			return scope.lookup(lookupContext.use(id, node), IJavaScope.Util.isTypeDecl);    
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
        final IJavaScope old = scope;         
        final IRNode enclosingType = lookupContext.foundNewType(node);
        try {
          scope = sc;
          //System.out.println("Trying to bind ACE: "+DebugUnparser.toString(node));
          final CallState state = methodBinder.new CallState(node, targs, args, type);
          boolean success = bindAllocation(state);
          if (!success && pathToTarget == null) {
        	  System.out.println("Couldn't bind "+DebugUnparser.toString(node));
          }
        } finally {
          scope = old;
          lookupContext.leavingType(node, enclosingType);
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
      if (isFullPass) {
    	  return null;
      }
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
    public Void visitTryResource(IRNode node) {
        IJavaScope.NestedScope tryScope = new IJavaScope.NestedScope(scope);
        IRNode resources                = TryResource.getResources(node);
        for(IRNode res : Resources.getResourceIterator(resources)) {
        	tryScope.add(VariableResource.getVar(res));
        }
        doAcceptForChildren(node, tryScope);
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
      final IJavaScope shadowing = new IJavaScope.ShadowingScope(classScope,scope);
      if (AnonClassExpression.prototype.includes(parent)) {
    	  // Start of a different class
    	  doAcceptForChildren(node, shadowing);
      } else {
    	  IRNode gparent = JJNode.tree.getParent(parent);
    	  if (TypeDeclarationStatement.prototype.includes(gparent)) {
        	  // Start of a different class
    		  doAcceptForChildren(node, shadowing);
    	  } else {
    		  /**
    		   * Within a class C, a declaration d of a member type named n shadows the declarations
    		   * of any other types named n that are in scope at the point where d occurs.
    		   */
    		  IJavaScope combined = new IJavaScope.SelectiveShadowingScope(shadowing, onlyColocatedTypes(parent), shadowing);
    		  doAcceptForChildren(node, combined);
    	  }
      }
      return null;
    }

    private Selector onlyColocatedTypes(final IRNode tdecl) {
    	final IRNode cu = VisitUtil.findCompilationUnit(tdecl);
    	return new IJavaScope.AbstractSelector("Types co-located with "+JavaNames.getFieldDecl(tdecl)) {
//			@Override
			public boolean select(IRNode node) {
				if (!TypeDeclaration.prototype.includes(node)) {
					return false;
				}
				return cu != null && cu == VisitUtil.findCompilationUnit(node);
			}    		
    	};
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
    
    private IJavaScope computeScopeForInstanceFieldsAndInits(IRNode node, IJavaScope scope) {
    	// Skip body to get to type decl
        IRNode tdecl = JJNode.tree.getParent(JJNode.tree.getParent(node));
        if (!InterfaceDeclaration.prototype.includes(tdecl) && !AnnotationDeclaration.prototype.includes(tdecl)) {
        	IJavaScope.NestedScope sc = new IJavaScope.NestedScope(scope);
        	
        	// addReceiverDeclForType(tdecl, sc);
        	try {
        	IRNode initD = JavaPromise.getInitMethod(tdecl);
        	sc.put("this", JavaPromise.getReceiverNode(initD));
        	// sc.put("this", JavaPromise.getReceiverNode(type));        	
        	return sc;
        	} catch(SlotUndefinedException e) {
        		System.out.println("Died on: "+JavaNames.getTypeName(tdecl));
        	}
        }        
        return scope;
    }
    
    @Override
    public Void visitClassInitializer(IRNode node) {
      final IJavaScope withThis;
      if (!JavaNode.getModifier(node,JavaNode.STATIC)) {
    	  withThis = computeScopeForInstanceFieldsAndInits(node, scope);
      } else {
    	  withThis = scope;
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
        String tname = JJNode.getInfo(tdecl);
        final CallState state = methodBinder.new CallState(node, targs, call.get_Args(node));
        boolean success = bindCall(state, tname, ty);
        if (!success) {
        	bindCall(state, tname, ty);
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
    public Void visitElementValuePair(IRNode node) {
      // To visit children
      super.visitElementValuePair(node);
    	
      if (!isFullPass) {    	  
    	  return null;
      }
      final String name = ElementValuePair.getId(node);
      if (name == null) {
    	  bind(node, IBinding.NULL);
    	  return null;
      }
      final IRNode anno = VisitUtil.getEnclosingAnnotation(node);
      final IBinding b = getIBinding(anno);
      // Lookup the attribute's type
      for(IRNode decl : VisitUtil.getClassBodyMembers(b.getNode())) {
    	  final Operator op = JJNode.tree.getOperator(decl);
    	  if (AnnotationElement.prototype.includes(op)) {
    		  if (name.equals(AnnotationElement.getId(decl))) {
    			  bind(node, IBinding.Util.makeBinding(decl, typeEnvironment));
    			  return null;
    		  }    		  
    	  }
      }
      bind(node, IBinding.NULL);
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
        final CallState state = methodBinder.new CallState(node, null, args, tdecl);
        boolean success = bindCall(state, JJNode.getInfo(tdecl), state.receiverType);
        if (!success) {
            bindCall(state, JJNode.getInfo(tdecl), state.receiverType);
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
      final IJavaScope withThis;
      if (!JavaNode.getModifier(node,JavaNode.STATIC)) {
    	  withThis = computeScopeForInstanceFieldsAndInits(node, scope);
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
      final String name = MethodCall.getMethod(node);  
  
      if ("foobarbaz".equals(name)) {
      	System.out.println("getCurrentKey: "+DebugUnparser.toString(node));
      }      
     
      if (JJNode.tree.getOperator(receiver) instanceof ImplicitReceiver) {
        toUse = scope;
      } else {
        recType = getJavaType(receiver);
//        if (recType instanceof IJavaDeclaredType) {
//          System.out.println(DebugUnparser.toString(((IJavaDeclaredType) recType).getDeclaration()));
//        }
        String recName = recType.toString();
        /*
        if (recName.startsWith("org.apache.hadoop.mapreduce.Mapper") && recName.endsWith(">.Context")) {
        	System.out.println("Binding call for "+recName);
        }
        */
        if (recType != null) toUse = typeScope(recType);
      }
      if (toUse != null) {        
    	  /*
    	  if ("doPrivileged".equals(name)) {
        	String unparse = DebugUnparser.toString(node);
        	if (unparse.startsWith("AccessController.doPrivileged(new")) {
        		String args_txt = DebugUnparser.toString(args);
        		if (args_txt.contains("GetPropertyAction")) {
        			System.out.println("toArray: "+unparse);
        		}        		
        	}
        }
        */
        if (recType instanceof IJavaDeclaredType) {
          IJavaDeclaredType dt = (IJavaDeclaredType) recType;
          if (AnnotationDeclaration.prototype.includes(dt.getDeclaration())) {
            if (!JJNode.tree.hasChildren(args)) {
                return bindAnnotationElement(node, name, toUse); 
                //throw new IllegalArgumentException("Illegal call to annotation element: "+DebugUnparser.toString(node));
            }
            // Process as normal method call
          }
        }
        final CallState state = methodBinder.new CallState(node, targs, args);
        boolean success = bindCall(state,name, toUse);
        if (!success) {
          // FIX hack to get things to bind for receivers of raw type       
          IJavaType newType = convertRawType(recType, true);
          if (newType != recType) {
            success = bindCall(state,name, newType);
          }
          if (!success) {
            IJavaType newType2 = convertRawType(recType, false);
            if (newType2 != recType) {
              success = bindCall(state,name, newType2);
            }
            if (!success && pathToTarget == null) {
              System.out.println("Receiver: "+DebugUnparser.toString(receiver));
              System.out.println("Args:     "+DebugUnparser.toString(args));
              IJavaType temp = getJavaType(receiver);
              typeScope(temp);
              bindCall(state,name, toUse);
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
      boolean success = bind(node, toUse, IJavaScope.Util.isAnnoEltOrNoArgMethod);
      if (!success) {
        bind(node, toUse, IJavaScope.Util.isAnnoEltOrNoArgMethod);
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
          else if (InterfaceDeclaration.prototype.includes(op)) {
              formals = InterfaceDeclaration.getTypes(decl);    
          } 
          else {
              return t; // No type formals possible  
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
                newParams.add(JavaTypeFactory.getWildcardType(null, superRefT));
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
      if (JJNode.tree.hasChildren(tformals)) {    	  
    	  addDeclsToScope(tformals, sc);
    	  sc = new IJavaScope.NestedScope(sc); // necessary to segregate type variables from locals
      }
      IRNode formals = isConstructor ?
          ConstructorDeclaration.getParams(node) : MethodDeclaration.getParams(node);
      /*
      IRNode annos = isConstructor ?
    	  ConstructorDeclaration.getAnnos(node) : MethodDeclaration.getAnnos(node);
      if (JJNode.tree.hasChildren(annos) && isBinary(node)) {
    	  System.out.println("Found binary func w/ annos: "+JavaNames.getFullName(node));
      }
      */
      /*
      if ("of".equals(JJNode.getInfoOrNull(node))) {
    	  System.out.println("Debugging "+JavaNames.getFullName(node));
      }
      */
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
            if (!contains(overs, mdecl)) {
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
    
    private boolean contains(List<IBinding> overs, IRNode mdecl) {
    	for(IBinding b : overs) {
    		if (b.getNode().equals(mdecl)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    @Override
    public Void visitNameType(IRNode node) {
      /*
      if (DebugUnparser.toString(node).contains("Context")) {
    	  System.out.println("NameType Context");
      }
      */
      if (!isFullPass) {
    	  visit(node);
    	  bind(node,getIBinding(NameType.getName(node)));
      } else if (!isFullPass) {
    	  System.out.println("Ignoring NameType: "+DebugUnparser.toString(node));
      }
      return null;
    }
    
    @Override
    public Void visitNamedType(IRNode node) {
      if (isFullPass) {
      	  return null;
      }
      String name = JJNode.getInfo(node);
      /*
      if (name.endsWith("Reducer")) {
    	  System.out.println("Binding type Reducer");
      }
      */
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
        IBinding b = scope.lookup(lookupContext.use(name, node), IJavaScope.Util.isTypeDecl);
        /*
        if (name.equals("E")) {
          	IRNode method = VisitUtil.getEnclosingClassBodyDecl(node);
          	if (method != null && "of".equals(JJNode.getInfoOrNull(method))) {          	
          		System.out.println("Binding E in "+JavaNames.getFullName(method)+" to "+
          				JavaNames.getFullName(b.getNode()));
          		IRNode enclosing = VisitUtil.getEnclosingDecl(b.getNode());
          		System.out.println("\t = "+DebugUnparser.toString(enclosing));
          		System.out.println();
          	}
        }
        */
        // Added for debugging
        if (b == null && !isBinary(node)) {
          scope.printTrace(System.out, 0);
          classTable.getOuterClass(name,node);
          scope.lookup(lookupContext.use(name, node), IJavaScope.Util.isTypeDecl);
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
        pkg = drop == null ? null : drop.getCompilationUnitIRNode();
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
    	final CallState state = methodBinder.new CallState(node, targs, args, type);
        boolean success = bindAllocation(state);
        if (!success) {
        	bindAllocation(state);
        }
      }
      return null;
    } 
    
    @Override
    public Void visitParameterDeclaration(IRNode node) {
      /*
      final String name = JJNode.getInfo(node);
      if ("context".equals(name)) {
    	 System.out.println("Binding param: "+DebugUnparser.toString(node)); 
      }
      */
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
      final CallState state = methodBinder.new CallState(alloc, null, args, typeDecl);
      boolean success = bindAllocation(state);
      if (success && isACE) { 
    	  // bind NewE inside of ACE
    	  try {
    		  IBinding b = alloc.getSlotValue(bindings.getUseToDeclAttr());
        	  bind(AnonClassExpression.getAlloc(alloc), b);
    	  } catch (SlotUndefinedException e) {
        	  bindAllocation(state);
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
	
	private IJavaType getType(IBinding b) {
		IJavaType t = AbstractJavaBinder.this.getJavaType(b.getNode());
		return b.convertType(t);		
	}
    
    @Override
    public Void visitQualifiedName(final IRNode node) {
      visit(node); // bind where we look from
      
      final IRNode base = QualifiedName.getBase(node);
      IBinding baseBinding = getIBinding(base);
      if (baseBinding == null) {
        bind(node,(IRNode)null);
      } else {
    	bindQualifiedName(node, baseBinding);
    	/*
    	if (!success) {    		
    		// Base might be ambiguous ... try to rebind
    		if (SimpleName.prototype.includes(base)) {
    			// Hack the selector to make sure that the name ref exists    			
    			final NameContext context = computeNameContext(node);
       			final String id = JJNode.getInfo(node);
       			final Selector s = new IJavaScope.AbstractSelector("Customized to find "+id) {
					@Override
					public boolean select(IRNode n) {
						if (context.selector.select(n)) {
							// TODO how do I get an IBinding from n? (need type context)
							bindQualifiedName(node, null);
						}
						return false;
					}};
    			bind(node, s);
    		}      	
    	}
    	*/
      }
      return null;
    }
    
    /**
     * Given a binding for the base, try to bind the rest of the qualified name
     */
    private boolean bindQualifiedName(IRNode node, IBinding baseBinding) {
      Operator bbop = JJNode.tree.getOperator(baseBinding.getNode());
      IJavaScope scope;
      if (bbop instanceof TypeDeclaration) {
        IJavaType baseType = JavaTypeFactory.getMyThisType(baseBinding.getNode(), true);
        scope = typeScope(baseBinding.convertType(baseType));
      } else if (bbop instanceof VariableDeclarator || bbop instanceof ParameterDeclaration) {
        scope = typeScope(getType(baseBinding));
      } else if (bbop instanceof NamedPackageDeclaration) {
        scope = classTable.packageScope(baseBinding.getNode());
      } else if (bbop instanceof EnumConstantDeclaration) {
    	scope = typeScope(getType(baseBinding));
      } else {
        LOG.warning("Cannot process qualified name " + DebugUnparser.toString(node) +
            " base binding -> " + bbop);
        return false;
      }
      if (scope == null) {
        LOG.severe("scope is null for " + DebugUnparser.toString(node));
        return false;
      }
      NameContext context = computeNameContext(node);
      boolean success = bind(node,scope,context.selector);
      if (!success) {
    	  bind(node,scope,context.selector);
      } else {
    	  return true;
      }
      return false;
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
      //IRNode oldRV = null;
      IRNode enclosingType = null;
      if (decl != null) {
        Operator op = JJNode.tree.getOperator(decl);
        if (isParameterToAnonClassExpr(op, decl, n)) {
          // "inside" a ACE
          decl = VisitUtil.getEnclosingClassBodyDecl(decl);
          op   = JJNode.tree.getOperator(decl);
        }        
        if (SomeFunctionDeclaration.prototype.includes(op)) {
          if (contextTypeB != null) {
        	// Check if the context type is the same as the enclosing type
        	enclosingType = VisitUtil.getEnclosingType(n);
        	if (contextTypeB == enclosingType) {
        		// If so, use the receiver node instead
        		return JavaPromise.getReceiverNodeOrNull(decl);
        	}
        	if (ConstructorDeclaration.prototype.includes(op)) {
        		// Check if it's inside a ConstructorCall        		        
        		if (insideConstructorCall(n)) {
        			// Use the constructor's IPQR
        			return JavaPromise.getQualifiedReceiverNodeByName(decl, contextTypeB);        			
        		//} else {
        		//	System.out.println("In constructor, but not call: "+DebugUnparser.toString(n));
        		}
        	//} else {
        	//	System.out.println("In method: "+DebugUnparser.toString(n)+" in "+JavaNames.genMethodConstructorName(decl));
        	//	oldRV = JavaPromise.getQualifiedReceiverNodeByName(decl, contextTypeB);		
        	}
          } else {
        	  return JavaPromise.getReceiverNodeOrNull(decl);
          }
        }
      }
      // initializer or method
      if (enclosingType == null) {
    	  enclosingType = VisitUtil.getEnclosingType(n);
      }
      IRNode rv;
      //decl = JavaPromise.getInitMethodOrNull(type);
      
      if (contextTypeB != null && contextTypeB != enclosingType) {
    	// Use the type's IFQR
        rv = JavaPromise.getQualifiedReceiverNodeByName(enclosingType, contextTypeB);
      } else {
        rv = JavaPromise.getReceiverNodeOrNull(enclosingType);
      }
      if (enclosingType == null || decl == null || rv == null) {
        LOG.severe("Got nulls while binding "+DebugUnparser.toString(n));
        JavaPromise.getQualifiedReceiverNodeByName(enclosingType, contextTypeB);
      }
      /*
      if (oldRV != rv) {
    	  //getting receivers from different nodes (method vs class)
    	  System.out.println("Results differ");
      }
      */
      return rv;
    }
    
    
    private boolean insideConstructorCall(IRNode n) {
    	n = JJNode.tree.getParentOrNull(n);
    	
    	while (n != null) {
    		final Operator op = getOperator(n);
    		//System.out.println("At: "+op.name());
    		if (ConstructorCall.prototype.includes(op)) {
    			return true;
    		}
    		if (Statement.prototype.includes(op)) {
    			return false;
    		}
    		n = JJNode.tree.getParentOrNull(n);
    	}
		return false;
	}

    @Override
    public Void visitParameterizedType(IRNode node) {
      if (isFullPass) {
    	return null;
      }  	
      visit(node); // bind types      
      IJavaType ty = typeEnvironment.convertNodeTypeToIJavaType(node);
      if (ty != null) {
    	  bind(node, ((IJavaDeclaredType) ty).getDeclaration());
      }
       return null;
    }
    
    @Override
    public Void visitPrimitiveType(IRNode node) {
        if (isFullPass) {
      	  return null;
        }    	
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

    /**
     * Figure what this name could be
     */
    private NameContext computeNameContext(final IRNode node) {
    	IRNode here = node;
    	boolean checkNext = false;
    	boolean partOfQualifiedName = false;
    	
    	while (here != null) {
    		IRNode parent = JJNode.tree.getParentOrNull(here);
    		Operator pop  = JJNode.tree.getOperator(parent);
    		
    		if (checkNext) {
    			if (MethodCall.prototype.includes(pop) || FieldRef.prototype.includes(pop)) {
    				return NameContext.EITHER;
    			}    				
    			return NameContext.NOT_TYPE;
    		}
    		if (NameType.prototype.includes(pop)) {
    			return NameContext.TYPE;
    		}
    		else if (QualifiedName.prototype.includes(pop)) {
    			// this only matters if it's ambiguous
    			partOfQualifiedName = true;
    		}
    		else if (NameExpression.prototype.includes(pop)) {
    			if (partOfQualifiedName) {
    				return NameContext.EITHER;
    			}
    			checkNext = true;
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
      /*
      final String name = JJNode.getInfo(node);
      if ("TestNameResolution".equals(name)) {
    	  System.out.println("Binding 'Inner'");
      }
      */
      final NameContext context = computeNameContext(node);
      /*
      final String name = JJNode.getInfo(node);
      if ("Inner".equals(name)) {
    	  System.out.println("Binding 'Inner': "+context);
      }
      */      
      final IJavaScope.Selector isAccessible = methodBinder.makeAccessSelector(node);
      /*
      if ("Lock".equals(JJNode.getInfoOrNull(node))) {
    	  IRNode parent = JJNode.tree.getParentOrNull(node);
    	  if (parent != null) {
    		  IRNode gp =  JJNode.tree.getParentOrNull(parent);
    		  if (ParameterDeclaration.prototype.includes(gp)) {
    	    	  System.out.println("Looking for Lock");
    		  }
    	  }
      }
      */
      boolean success = false;
      if (context.couldBeVariable()) {
    	  success = bind(node, IJavaScope.Util.combineSelectors(IJavaScope.Util.couldBeNonTypeName, isAccessible), true);
      }
      if (!success && context.couldBeType()) {
    	  success = bind(node, IJavaScope.Util.combineSelectors(IJavaScope.Util.isPkgTypeDecl, isAccessible));
      }
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
    	  if (context.couldBeVariable()) {
    		  bind(node, IJavaScope.Util.combineSelectors(isAccessible, IJavaScope.Util.couldBeNonTypeName));
    	  }
    	  bind(node, IJavaScope.Util.combineSelectors(isAccessible, IJavaScope.Util.isPkgTypeDecl));
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
      /*
      if (ThisExpression.prototype.includes(node)) {
    	  System.out.println("Context for this: "+DebugUnparser.toString(VisitUtil.getEnclosingClassBodyDecl(node)));
      }
      */
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
      final IRNode enclosingType = lookupContext.foundNewType(node);
      /*
      if ("TestBindingInnerClass".equals(JJNode.getInfo(node))) {    	
    	  System.out.println("Binding type: "+JavaNames.getFullTypeName(node));
      }
      */
      IJavaScope.NestedScope sc = new IJavaScope.NestedScope(scope);
      sc.add(node);
      addDeclsToScope(tformals, sc);
      try {
    	  doAcceptForChildren(node,sc);
    	  return null;
      } finally {
    	  lookupContext.leavingType(node, enclosingType);
      }
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
      if (isFullPass) {
    	  return null;
      }
      visit(node); // bind base type
      
      IRNode base = TypeRef.getBase(node);
      IBinding baseB = getLocalIBinding(base);
      if (baseB != null) {
    	  IRNode baseDecl = baseB.getNode();
    	  IJavaScope tScope = typeScope(JavaTypeFactory.convertIRTypeDeclToIJavaType(baseDecl));
    	  boolean success = bind(node,tScope,IJavaScope.Util.isTypeDecl);
    	  if (!success) {
    		  bind(node,tScope,IJavaScope.Util.isTypeDecl);
    	  }
      } else {
    	  if (AbstractJavaBinder.isBinary(node)) {      
    		  System.err.println("No binding to bind "+DebugUnparser.toString(node));
    	  } else {
    		  LOG.severe("No binding to bind "+DebugUnparser.toString(node));
    	  }
    	  bind(node, (IBinding) null);
      }
      return null;
    }
    
    @Override
    public Void visitUnnamedPackageDeclaration(IRNode node) {
      bindToPackage(node, "");
      return null;
    }
    
    @Override
    public Void visitVarArgsType(IRNode node) {
        if (!isFullPass) {
      	  // No IRNode binding that makes sense
      	  bind(node, nullBinding); 
        }
    	return super.visitVarArgsType(node);
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
    
    @Override
    public Void visitVoidType(IRNode node) {
    	bind(node, IBinding.NULL);
    	return null;
    }
  }

  /**
   * A type environment implementation linked to this binder.
   */
  @ThreadSafe
  private class TypeEnv extends AbstractTypeEnvironment {
    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.java.bind.ITypeEnvironment#getBinder()
     */
    public IBinder getBinder() {
      return AbstractJavaBinder.this;
    }
    
    @Override
    public IRNode getArrayClassDeclaration() {
      return findNamedType(PromiseConstants.ARRAY_CLASS_QNAME);
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
  
  @Override
  public Iteratable<IBinding>  findOverriddenParentMethods(final IRNode mth) {
	  final int mods = JavaNode.getModifiers(mth);
	  if (JavaNode.isSet(mods, JavaNode.PRIVATE) || JavaNode.isSet(mods, JavaNode.STATIC)) {
		  return EmptyIterator.prototype();
	  }
	  final String name   = SomeFunctionDeclaration.getId(mth);
	  final IRNode td     = VisitUtil.getEnclosingType(mth);
	  final IJavaDeclaredType t = (IJavaDeclaredType) typeEnvironment.convertNodeTypeToIJavaType(td);
  
	  final MethodBinder mb      = new MethodBinder(this, false);
	  final List<IBinding> methods = new ArrayList<IBinding>();
	  final LookupContext context = new LookupContext();
	  context.foundNewType(td);
	  context.use(name, mth);
	  
	  /*
	  final List<IJavaType> temp = new ArrayList<IJavaType>();
	  for(IJavaType st : t.getSupertypes(typeEnvironment)) {
		  temp.add(st);
	  }
	  Set<IJavaType> temp2 = new HashSet<IJavaType>(temp);
	  if (temp.size() != temp2.size()) {
		  System.out.println("Found duplicates");
	  }	  
	  */
	  final CallState call = mb.new CallState(null, null, null) {
		  @Override
		  public IJavaType[] getArgTypes() {
			  return mb.getFormalTypes(t, mth);
		  }
	  };
	  
	  for(IJavaType superT : t.getSupertypes(typeEnvironment)) {
		  final IJavaDeclaredType st = (IJavaDeclaredType) superT;
		  // Looking at the inherited members	
		  final IJavaScope superScope = 
			  new IJavaScope.SubstScope(typeMemberTable(st).asScope(this), getTypeEnvironment(), t);	  

		  // Specialized for methods!
		  BindingInfo best = mb.findBestMethod(superScope, context, true, st.getDeclaration(), call);
		  if (best != null) {
			  methods.add(best.method);
		  }
	  }
	  final Collection<IBinding> noDups = removeDuplicateBindings(methods);
	  return IteratorUtil.makeIteratable(noDups);
  }
  
  private Collection<IBinding> removeDuplicateBindings(final List<IBinding> methods) {
	  final MultiMap<IRNode, IBinding> hashed = new MultiHashMap<IRNode, IBinding>(methods.size());
	 outer:
	  for(final IBinding m : methods) {
		  final Collection<IBinding> temp = hashed.get(m.getNode());
		  if (temp != null) {
			  // check for duplicates
			  for(final IBinding b : temp) {
				  if (equals(b.getContextType(), m.getContextType())) {
					  continue outer;
				  }
			  }
		  }
		  hashed.put(m.getNode(), m);
	  }
	  return hashed.values();
  }
  
  private boolean equals(IJavaType t1, IJavaType t2) {
	  if (t1 == t2) {
		  return true;
	  }
	  // Already checked above if both are null
	  if (t1 == null || t2 == null) {
		  return false;
	  }
	  return t1.equals(t2);
  }
}
