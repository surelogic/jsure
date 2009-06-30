/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/AASTStore.java,v 1.8 2008/09/04 18:50:16 chance Exp $*/
package com.surelogic.annotation.scrub;

import com.surelogic.aast.*;
import com.surelogic.annotation.test.TestResult;

import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;

import java.util.*;

/**
 * Stores the AASTs after parsing until they are scrubbed
 * TODO may need to have one per project
 * 
 * @author Edwin.Chan
 */
public class AASTStore {
  protected static final Collection<IAASTRootNode> asts = new ArrayList<IAASTRootNode>();
  
  @SuppressWarnings("unchecked")
  protected static final Map<Class,Collection<? extends IAASTRootNode>> byClass = 
    new HashMap<Class,Collection<? extends IAASTRootNode>>();
  
  @SuppressWarnings("unchecked")
  protected static final Map<IAASTRootNode,List<ValidatedDropCallback>> triggers = 
    new HashMap<IAASTRootNode,List<ValidatedDropCallback>>();
  
  protected static final Map<IAASTRootNode,TestResult> results = 
    new HashMap<IAASTRootNode,TestResult>();
  
  public static synchronized <T extends IAASTRootNode> 
  void add(T ast) {
    asts.add(ast);
    addByClass(ast);
  }
  
  private static synchronized <T extends IAASTRootNode> 
  void addByClass(T ast) {
    @SuppressWarnings("unchecked")
    Collection<T> c = (Collection<T>) byClass.get(ast.getClass());
    if (c == null) {
      c = new ArrayList<T>();
      byClass.put(ast.getClass(), c);
    }
    c.add(ast);
  }

  public static synchronized Iterable<IAASTRootNode> getASTs() {
    return asts;
  }
  
  public static synchronized void clearASTs() {
    asts.clear();
    byClass.clear();
    triggers.clear();
    results.clear();
  }
  
  @SuppressWarnings("unchecked")
  public static synchronized <T extends IAASTRootNode> Iterable<T> getASTsByClass(Class<T> c) {
    Iterable<T> it = (Iterable<T>) byClass.get(c);
    return (it == null) ? Collections.<T>emptyList() : it;
  }

  @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    List<ValidatedDropCallback> l = triggers.remove(pd.getAST());
    if (l != null) {
      for(ValidatedDropCallback<PromiseDrop<A>> cb : l) {
        cb.validated(pd);
      }
    }
  }
  

  public static synchronized void associateTestResult(IAASTRootNode root, TestResult result) {
    if (result == null) {
      return;
    }
    results.put(root, result);
  }
  
  public static synchronized TestResult getTestResult(IAASTRootNode root) {
    return results.get(root);
  }
}
