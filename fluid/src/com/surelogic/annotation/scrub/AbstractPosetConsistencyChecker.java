package com.surelogic.annotation.scrub;

import java.util.ArrayList;
import java.util.Collection;

import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.Iteratable;
import edu.uwm.cs.fluid.util.Poset;

public abstract class AbstractPosetConsistencyChecker<E, P extends Poset<E>> extends AbstractPromiseScrubber<PromiseDrop<?>> {
  protected enum Source { 
    NO_PROMISE(0), ASSUMPTION(1), PROMISE(2);
      
    private final int value;
      
    private Source(final int v) { value = v; }
      
    public static Source getSource(final PromiseDrop<?> a) {
      return a.isAssumed() ? Source.ASSUMPTION : Source.PROMISE;
    }
  
    /**
     * @return true if we should check consistency between the two promises
     */
    boolean check(final Source parent) {
      // At least one promise, or two assumptions
      return value + parent.value > 1;
    }
  }

  protected final class Pair extends com.surelogic.common.Pair<E, Source> {
    public Pair(final E v, final Source s) {
      super(v, s);
    }
  }

  

  private final P poset;
  private Collection<PromiseDrop<?>> relevantDrops = new ArrayList<PromiseDrop<?>>();
  private final boolean proposePromises;
  
  
  
  public AbstractPosetConsistencyChecker(
      final P p, final String name, final String[] deps, final boolean pp) {
    super(ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY, NONE,
        name, ScrubberOrder.NORMAL, deps);
    poset = p;
    proposePromises = pp;
  }

    
  
  public final void addRelevantDrop(final PromiseDrop<?> d) {
    relevantDrops.add(d);
  }
  
  @Override
  protected final Iterable<PromiseDrop<?>> getRelevantAnnotations() {
    return relevantDrops;
  }

  // non-final in case there is more clean up that needs to be done
  @Override
  protected void finishRun() {
    relevantDrops.clear();
  }

    
    
  @Override
  protected final void processDrop(final PromiseDrop<?> a) {
    checkConsistency(
        a.getPromisedFor(), getValue(a), Source.getSource(a), false);
  }
    
  @Override
  protected final boolean processUnannotatedMethodRelatedDecl(
      final IRNode unannotatedNode) {
    return checkConsistency(
        unannotatedNode, getUnannotatedValue(unannotatedNode), Source.NO_PROMISE, true);
  }
  

  
  /**
   * Called to get the POSET value for a promise from an <em>overriding</em>
   * method.
   */
  protected abstract E getValue(PromiseDrop<?> a);

  /**
   * Called to get the POSET value for a node from an <em>overridden</em>
   * method.
   */
  protected abstract Pair getValue(IRNode n);
  
  protected final Pair getValueImpl(final E value, final PromiseDrop<?> d) {
    return new Pair(value, Source.getSource(d));
  }

  protected abstract E getUnannotatedValue(IRNode unannotatedNode);
    
    
    
  private boolean checkConsistency(
      final IRNode promisedFor, final E s,
      final Source src, final boolean generateProposal) {
    /* 3 cases, return value, parameter, receiver. */
    boolean good = true;
    
    final Operator op = JJNode.tree.getOperator(promisedFor);
    if (ParameterDeclaration.prototype.includes(op)) {
      final IRNode mdecl = JJNode.tree.getParent(JJNode.tree.getParent(promisedFor));
      for (final IBinding bc : getContext().getBinder(mdecl).findOverriddenParentMethods(mdecl)) {
        final IRNode parentMethod = bc.getNode();
        
        // find the same parameter in the original
        final IRNode params = MethodDeclaration.getParams(mdecl);
        final IRNode parentParams = MethodDeclaration.getParams(parentMethod);
        final Iteratable<IRNode> paramsIter = Parameters.getFormalIterator(params);
        final Iteratable<IRNode> parentParamsIter = Parameters.getFormalIterator(parentParams);
        while (paramsIter.hasNext()) {
          final IRNode p = paramsIter.next();
          final IRNode parentP = parentParamsIter.next();
          if (p == promisedFor) { // found the original param
            final Pair parentState = getValue(parentP);
            if (src.check(parentState.second()) && 
              !poset.lessEq(parentState.first(), s)) {
              good = false;
              if (proposePromises && generateProposal) {
                getContext().reportErrorAndProposal(
                    proposePromise(parentState.first(), null, promisedFor, parentMethod),
                    "The annotation on parameter {0} of {1} cannot be changed from {2} to {3}",
                    ParameterDeclaration.getId(p),
                    JavaNames.genRelativeFunctionName(parentMethod),
                    getAnnotationName(parentState.first()), getAnnotationName(s));
              } else {
                getContext().reportError(promisedFor,
                    "The annotation on parameter {0} of {1} cannot be changed from {2} to {3}",
                    ParameterDeclaration.getId(p),
                    JavaNames.genRelativeFunctionName(parentMethod),
                    getAnnotationName(parentState.first()), getAnnotationName(s));
              }
            }
          }
        }
      }
    } else if (ReceiverDeclaration.prototype.includes(op)) {
      final IRNode mdecl = JavaPromise.getPromisedFor(promisedFor);
      for (final IBinding bc : getContext().getBinder(mdecl).findOverriddenParentMethods(mdecl)) {
        final IRNode parentMethod = bc.getNode();
        
        // Get the receiver in the original
        final IRNode rcvr = JavaPromise.getReceiverNode(parentMethod);
        final Pair parentState = getValue(rcvr);  
        if (src.check(parentState.second()) && !poset.lessEq(parentState.first(), s)) {
          good = false;
          if (proposePromises && generateProposal) {
            getContext().reportErrorAndProposal(
                proposePromise(parentState.first(), "this", promisedFor, parentMethod),
                "The annotation on the receiver of {0} cannot be changed from {1} to {2}",
                JavaNames.genRelativeFunctionName(parentMethod),
                getAnnotationName(parentState.first()), getAnnotationName(s));
          } else {
            getContext().reportError(promisedFor,
                "The annotation on the receiver of {0} cannot be changed from {1} to {2}",
                JavaNames.genRelativeFunctionName(parentMethod),
                getAnnotationName(parentState.first()), getAnnotationName(s));
          }
        }
      }
    } else if (ReturnValueDeclaration.prototype.includes(op)) {
      final IRNode mdecl = JavaPromise.getPromisedFor(promisedFor);
      for (final IBinding bc : getContext().getBinder(mdecl).findOverriddenParentMethods(mdecl)) {
        final IRNode parentMethod = bc.getNode();

        // Get the return value in the original
        final IRNode rcvr = JavaPromise.getReturnNode(parentMethod);
        final Pair parentState = getValue(rcvr);        
        if (src.check(parentState.second()) && !poset.lessEq(s, parentState.first())) {
          good = false;
          if (proposePromises && generateProposal) {
            getContext().reportErrorAndProposal(
                proposePromise(parentState.first(), "return", promisedFor, parentMethod),
                "The annotation on the return value of {0} cannot be changed from {1} to {2}",
                JavaNames.genRelativeFunctionName(parentMethod),
                getAnnotationName(parentState.first()), getAnnotationName(s));
          } else {
            getContext().reportError(promisedFor,
                "The annotation on the return value of {0} cannot be changed from {1} to {2}",
                JavaNames.genRelativeFunctionName(parentMethod),
                getAnnotationName(parentState.first()), getAnnotationName(s));
          }
        }
      }
    }
    
    return good;
  }
  
  protected abstract String getAnnotationName(E value);
  
  protected abstract ProposedPromiseDrop proposePromise(
      E value, String valueValue, IRNode promisedFor, IRNode parentMethod);
}
