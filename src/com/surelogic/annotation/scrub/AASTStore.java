/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/AASTStore.java,v 1.8 2008/09/04 18:50:16 chance Exp $*/
package com.surelogic.annotation.scrub;

import com.surelogic.aast.*;
import com.surelogic.annotation.test.TestResult;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Stores the AASTs after parsing until they are scrubbed
 * TODO may need to have one per project
 * 
 * @author Edwin.Chan
 */
public final class AASTStore {
  protected static final Collection<IAASTRootNode> asts = new ArrayList<IAASTRootNode>();
  
  /**
   * Resulting AASTs organized by type
   */
  @SuppressWarnings("rawtypes")
  protected static final Map<Class,Collection<? extends IAASTRootNode>> byClass = 
    new HashMap<Class,Collection<? extends IAASTRootNode>>();
  
  /**
   * Map from an AAST to the @Promise drop that created it
   */
  static final ConcurrentMap<IAASTRootNode, ScopedPromiseDrop> promiseSource = 
	  new ConcurrentHashMap<IAASTRootNode, ScopedPromiseDrop>();
  
  /**
   * Map from the AAST to the comp unit that it is assumed for
   */
  static final Map<IAASTRootNode, IRNode> assumedFor =
	new HashMap<IAASTRootNode, IRNode>();
  
  static IRNode assumedCu = null;
  
  /**
   * Callbacks to be run after the AAST is scrubbed
   */
  @SuppressWarnings("rawtypes")
  protected static final Map<IAASTRootNode,List<ValidatedDropCallback>> triggers = 
    new HashMap<IAASTRootNode,List<ValidatedDropCallback>>();
  
  protected static final Map<IAASTRootNode,TestResult> results = 
    new HashMap<IAASTRootNode,TestResult>();
  
  public static synchronized <T extends IAASTRootNode> 
  void add(T ast) {
	if (assumedCu != null) {
		assumedFor.put(ast, assumedCu);
	}
	//System.out.println("Adding AAST: "+ast);
    asts.add(ast);
    addByClass(ast);
  }
  
  // RequiresLock
  private static <T extends IAASTRootNode> 
  void addByClass(T ast) {  	  
    Class<?> cls = ast.getClass();
    @SuppressWarnings("unchecked")
    Collection<T> c = (Collection<T>) byClass.get(cls);
    if (c == null) {
      c = new ArrayList<T>();
      byClass.put(cls, c);
    }
    c.add(ast);
  }
  
  public static synchronized Iterable<IAASTRootNode> getASTs() {
    return new ArrayList<IAASTRootNode>(asts);
  }
  
  public static synchronized void clearASTs() {
	//System.out.println("Clearing AASTs");
    asts.clear();
    byClass.clear();
    triggers.clear();
    results.clear();
    assumedFor.clear();
    promiseSource.clear();
  }
  
  @SuppressWarnings("unchecked")
  public static synchronized <T extends IAASTRootNode> Iterable<T> getASTsByClass(Class<T> c) {
    Iterable<T> it = (Iterable<T>) byClass.get(c);
    return (it == null) ? Collections.<T>emptyList() : it;
  }

  public static synchronized void setupAssumption(IRNode cu) {
	  assumedCu = cu;
  }
  
  public static synchronized void clearAssumption() {
	  assumedCu = null;
  }
  
  public static synchronized IRNode checkIfAssumption(IAASTRootNode root) {
	  final IRNode cu = assumedFor.get(root);
	  return cu;
  }
  
  @SuppressWarnings("rawtypes")
  public static synchronized void triggerWhenValidated(IAASTRootNode root, ValidatedDropCallback r) {
    List<ValidatedDropCallback> l = triggers.get(root);
    if (l == null) {
      l = new ArrayList<ValidatedDropCallback>();
      triggers.put(root, l);
    }
    l.add(r);
  }

  public static synchronized <A extends IAASTRootNode>
  void validate(PromiseDrop<A> pd) {  
    if (pd.getAST() == null) {
      if (pd instanceof MethodEffectsPromiseDrop) {
        // continue         
      } else {
        throw new IllegalArgumentException("No AST for "+pd);
      }
    }
    else if (pd.getAST().getStatus() != AASTStatus.VALID) {
      throw new IllegalArgumentException("AAST is not valid");
    }
    @SuppressWarnings("rawtypes")
    List<ValidatedDropCallback> l = triggers.remove(pd.getAST());
    if (l != null) {
      for(ValidatedDropCallback<PromiseDrop<A>> cb : l) {
        cb.validated(pd);
      }
    }
  }
  
  /**
   * A convenience method for rules that create AASTs, like scoped promises
   */
  public static synchronized void addDerived(IAASTRootNode clone, IAASTRootNode orig, ValidatedDropCallback<?> r) {
	  add(clone);
	  triggerWhenValidated(clone, r);
	  cloneTestResult(orig, clone);
  }
  
  public static synchronized void associateTestResult(IAASTRootNode root, TestResult result) {
    if (result == null) {
      return;
    }
    results.put(root, result);
  }
  
  public static synchronized void cloneTestResult(IAASTRootNode orig, IAASTRootNode clone) {
	TestResult result = getTestResult(orig);
	if (result == null) {
		return;
	}
	TestResult newResult = getTestResult(clone);
	if (newResult == null) {
		newResult = result.cloneResult();
		TestResult.addAAST(newResult, clone);
		associateTestResult(clone, newResult);	
	}
  }
  
  public static synchronized TestResult getTestResult(IAASTRootNode root) {
    return results.get(root);
  }
  
  public static synchronized void sync() {	  
	  //System.out.println("Syncing w/ AAST store");
  }

  public static void setPromiseSource(IAASTRootNode ast, ScopedPromiseDrop pd) {
	  promiseSource.put(ast, pd);
  }
  
  public static ScopedPromiseDrop getPromiseSource(IAASTRootNode ast) {
	  return promiseSource.get(ast);
  }
}
