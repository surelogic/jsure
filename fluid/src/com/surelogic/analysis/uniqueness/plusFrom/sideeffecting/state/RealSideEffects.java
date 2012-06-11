package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.Messages;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.UniquenessAnalysis;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.Store;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.UniquenessControlFlowDrop;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.util.Triple;

public final class RealSideEffects implements ISideEffects {
  /**
   * If we are creating drops, should we temporarily suppress creation of the
   * drops.  This is used internally, and set when along the abrupt termination
   * path should not be created because they are going to duplicate results
   * reported along the normal termination path.
   */
  private boolean suppressDrops = false;
  
  /** Are the results we are creating for the abrupt termination path */
  private boolean abruptDrops = false;

  
  
  /** Need the analysis object to create the drops. */
  private final AbstractWholeIRAnalysis<UniquenessAnalysis, ?> analysis;
  
  /**
   * Method control flow drops.  Map from method/constructor declaration
   * nodes to drops.
   */
  private final UniquenessControlFlowDrop controlFlowDrop;
  
  /**
   * Whether any results were added to the control flow drop.   Checked in
   * {@link #makeResultDrops()}: any promise that doesn't have any results is
   * given a generic "invariants respected" result.
   */
  private boolean hasControlFlowResults = false;

  /**
   * Records where variables with buried references are read. Built by
   * {@link #opGet(Store, IRNode, Object)}. After analysis, this is cross
   * referenced with {@link #buriedLocals} to determine where the variable was
   * buried.
   */
  private final Set<BuriedRead> buriedReads = new HashSet<BuriedRead>();  
  
  /**
   * Records which local variables are buried by reads of unique/borrowed fields. Built
   * by {@link #opLoad(Store, IRNode, IRNode)} and
   * {@link #opLoadReachable(Store, IRNode)}. Two level map: Local Variable ->
   * Field Declaration -> set of srcOps
   */
  private final Map<Object, Map<IRNode, Set<IRNode>>> buryingLoads =
    new HashMap<Object, Map<IRNode, Set<IRNode>>>();

  /**
   * Records where an UNDEFINED value is assigned to a local variable.  Can
   * happen when a variable is assigned a method return value, the 
   * return value is the SHARED object, and the effects of the method call
   * cause the SHARED object to be invalidated.  Map from
   * local variable stack position to set of IRNodes representing invalidating
   * assignments.
   */
  private final Map<Object, Set<IRNode>> badSets = new HashMap<Object, Set<IRNode>>();

  /**
   * Track which stack locations are buried by unique field reads and where.  This is a map
   * from stack indices to sets of IRNodes indicating source locations where
   * the stack location may have been made compromised.  Built by
   * {@link #opLoad(Store, IRNode)}.  Used when handling "undefined" errors
   * to out why the stack position is undefined.
   */
  private final Map<Integer, Set<IRNode>> stackBuriedAt =
    new HashMap<Integer, Set<IRNode>>();

  /**
   * Track which stack locations are buried by unique field reads as a method side-effect.  This is a map
   * from stack indices to sets of IRNodes indicating source locations where
   * the stack location may have been made compromised.  Built by
   * {@link #opLoadReachable(Store, IRNode)}.  Used when handling "undefined" errors
   * to out why the stack position is undefined.
   */
  private final Map<Integer, Set<Triple<Set<IRNode>, IRNode, RegionEffectsPromiseDrop>>> stackIndirectlyBuriedAt =
    new HashMap<Integer, Set<Triple<Set<IRNode>, IRNode, RegionEffectsPromiseDrop>>>();

  
  
  
  /**
   * All the result drops we are going to have created.  Saved up here in case
   * we need to cancel them if the analysis times out.
   */
  private final Set<ResultDropBuilder> drops = new HashSet<ResultDropBuilder>();

  
  
  // ==================================================================
  // === Constructor 
  // ==================================================================

  public RealSideEffects(final UniquenessControlFlowDrop cfd,
      final AbstractWholeIRAnalysis<UniquenessAnalysis, ?> a) {
    controlFlowDrop = cfd;
    analysis = a;
  }
  
  
  
  // ==================================================================
  // === Control side effect production 
  // ==================================================================
  
  public void setSuppressDrops(final boolean value) {
    suppressDrops = value;
  }
  
  public void setAbruptResults(final boolean value) {
    abruptDrops = value;
  }
 


  // ==================================================================
  // === Set Helpers
  // ==================================================================
  
  private static <K, V> void addToMappedSet(
      final Map<K, Set<V>> map, final K key, final V value) {
    // Get/make the set first
    Set<V> s = map.get(key);
    if (s == null) {
      s = new HashSet<V>();
      map.put(key, s);
    }
    
    // add the value
    s.add(value);
  }

  
  
  
  

  // ==================================================================
  // == Alias burying
  // ==================================================================
  
  public void recordBuriedRead(final IRNode srcOp, final Object local) {
    if (!suppressDrops) {
      buriedReads.add(new BuriedRead(local, srcOp, abruptDrops));
    }
  }
  
  private void recordBuryingLoad(final IRNode fieldDecl,
      final Set<Object> affectedVars, final IRNode srcOp) {
    for (final Object lv : affectedVars) {
      Map<IRNode, Set<IRNode>> fieldMap = buryingLoads.get(lv);
      if (fieldMap == null) {
        fieldMap = new HashMap<IRNode, Set<IRNode>>();
        buryingLoads.put(lv, fieldMap);
      }
      addToMappedSet(fieldMap, fieldDecl, srcOp);
    }
  }
  
  public void recordBuryingFieldRead(final IRNode fieldDecl,
      final Set<Object> affectedVars, final IRNode srcOp) {
    if (!suppressDrops) {
      recordBuryingLoad(fieldDecl, affectedVars, srcOp);
      for (final Object v : affectedVars) {
        if (v instanceof Integer) {
          addToMappedSet(stackBuriedAt, (Integer) v, srcOp);

        }
      }
    }
  }
  
  public void recordBuryingMethodEffects(final Set<IRNode> loadedFields,
      final Set<Object> affectedVars, final IRNode srcOp,
      final RegionEffectsPromiseDrop fxDrop) {
    if (!suppressDrops) {
      for (final IRNode fieldDecl : loadedFields) {
        recordBuryingLoad(fieldDecl, affectedVars, srcOp);
      }
      for (final Object v : affectedVars) {
        if (v instanceof Integer) {
          addToMappedSet(stackIndirectlyBuriedAt, (Integer) v, 
              new Triple<Set<IRNode>, IRNode, RegionEffectsPromiseDrop>(
                  loadedFields, srcOp, fxDrop));
        }
      }
    }
  }

  
    
  // ==================================================================
  // == Bad Values
  // ==================================================================

  public void recordBadSet(final Object local, final IRNode op) {
    if (!suppressDrops) {
      addToMappedSet(badSets, local, op);
    }
  }



  // ==================================================================
  // == Manage Result Drops
  // ==================================================================

  private ResultDropBuilder createResultDrop(
      final boolean abruptDrops, final boolean addToControlFlow,
      final PromiseDrop<? extends IAASTRootNode> promiseDrop,
      final IRNode node, final boolean isConsistent, 
      final int msg, final Object... args) {
    final Object[] newArgs = new Object[args.length+1];
    System.arraycopy(args, 0, newArgs, 0, args.length);
    newArgs[args.length] =
      abruptDrops ? Messages.ABRUPT_EXIT : Messages.NORMAL_EXIT;
    
    final ResultDropBuilder result =
      ResultDropBuilder.create(analysis, Messages.toString(msg));
    drops.add(result);
    analysis.setResultDependUponDrop(result, node);
    result.addCheckedPromise(promiseDrop);
    if (promiseDrop != controlFlowDrop) {
      if (addToControlFlow) {
        result.addCheckedPromise(controlFlowDrop);
        hasControlFlowResults = true;
      }
    } else {
      hasControlFlowResults = true;
    }
    result.setConsistent(isConsistent);
    result.setResultMessage(msg, newArgs);
    return result;
  }

  private ResultDropBuilder createResultDrop(
      final boolean abruptDrops,
      final PromiseDrop<? extends IAASTRootNode> promiseDrop,
      final IRNode node, final boolean isConsistent, 
      final int msg, final Object... args) {
    return createResultDrop(
        abruptDrops, true, promiseDrop, node, isConsistent, msg, args);
  }

  public void makeResultDrops() {
    // Link reads of buried references to burying field loads
    for (final BuriedRead read : buriedReads) {
      final boolean varIsReturn = (read.var instanceof IRNode)
          && ReturnValueDeclaration.prototype.includes((IRNode) read.var);          
      final Map<IRNode, Set<IRNode>> loads = buryingLoads.get(read.var);
      if (loads != null) {
        for (final Map.Entry<IRNode, Set<IRNode>> e : loads.entrySet()) {
          final ResultDropBuilder r = createResultDrop(read.isAbrupt,
              UniquenessUtils.getFieldUniqueOrBorrowed(e.getKey()), read.srcOp,              
              false, varIsReturn ? Messages.RETURN_OF_BURIED : Messages.READ_OF_BURIED);
          for (final IRNode buriedAt : e.getValue()) {
            r.addSupportingInformation(buriedAt, Messages.BURIED_BY, 
                DebugUnparser.toString(buriedAt));
          }
        }
      }
      
      // Could be undefined because we were assigned an undefined value
      final Set<IRNode> z = badSets.get(read.var);
      if (z != null) {
        final ResultDropBuilder r = createResultDrop(read.isAbrupt,
            controlFlowDrop, read.srcOp, false, Messages.READ_OF_BURIED);
        for (final IRNode setOp : z) {
          r.addSupportingInformation(setOp, Messages.ASSIGNED_UNDEFINED_BY,
              VariableUseExpression.prototype.includes(read.srcOp) ?
                  VariableUseExpression.getId(read.srcOp) : "*UNKNOWN*",
              DebugUnparser.toString(setOp));
        }
      }
    }

    /* TODO: If we haven't already added results to the control flow drop, then 
     * we add a single "invariants respected" positive result.
     */
    if (!hasControlFlowResults) {
      // TODO
    }
  }
}
