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
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.FieldTriple;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.State;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.Store;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store.StoreLattice;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.RegionEffectsPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.IUniquePromise;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquenessControlFlowDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableSet;
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
   * Record illegal reads of borrowed fields.  Map from 
   * Borrowed or BorrowedInRegion annotation to set of locations where the 
   * field is read.
   */
  private final Map<PromiseDrop<? extends IAASTRootNode>, Set<BorrowedRead>> readBorrowedFields =
    new HashMap<PromiseDrop<? extends IAASTRootNode>, Set<BorrowedRead>>();
  
  /**
   * Track which unique fields are possibly compromised and where. This is a map
   * from field declaration IRNodes to a set of IRNodes indicating source
   * locations where the field may have been compromised. Built by
   * {@link StoreLattice#opCompromiseNoRelease(Store, IRNode) and
   * {@link StoreLattice#opConsume} when the state is SHARED.
   */
  private final Map<IRNode, Set<CompromisingSite>> compromisedAt =
    new HashMap<IRNode, Set<CompromisingSite>>();
  
  /**
   * Track which unique fields are made undefined and where. This is a map
   * from field declaration IRNodes to a set of IRNodes indicating source
   * locations where the field may have been made undefined. Built by
   * {@link StoreLattice#opConsume(Store, IRNode, State)} when the state is
   * UNIQUE
   */
  private final Map<IRNode, Set<IRNode>> undefinedAt =
    new HashMap<IRNode, Set<IRNode>>();
  
  /**
   * Records where compromised fields are loaded.  After analysis is over,
   * we cross reference this set with {@link #compromisedAt} and {@link #undefinedAt} to determine
   * where the loaded value may have been compromised.  Map from 
   * field declaration IRNodes to a set of IRNodes indicating locations where
   * the field is read and found to be compromised.
   */
  private final Map<IRNode, Set<CompromisedField>> loadedCompromisedFields =
    new HashMap<IRNode, Set<CompromisedField>>();
  private final Map<IRNode, Set<CompromisedField>> loadedCompromisedFieldsAbrupt =
    new HashMap<IRNode, Set<CompromisedField>>();
  
  /**
   * Records where compromised fields are loaded as the result of executing a method.  After analysis is over,
   * we cross reference this set with {@link #compromisedAt} and {@link #undefinedAt} to determine
   * where the loaded value may have been compromised.  Map from 
   * field declaration IRNodes to a set of IRNodes indicating locations where
   * the field is read and found to be compromised.
   */
  private final Map<IRNode, Set<CompromisedField>> indirectlyLoadedCompromisedFields =
    new HashMap<IRNode, Set<CompromisedField>>();
  private final Map<IRNode, Set<CompromisedField>> indirectlyLoadedCompromisedFieldsAbrupt =
    new HashMap<IRNode, Set<CompromisedField>>();
  
  /**
   * Records where compromised fields are lost.  After analysis is over,
   * we cross reference this set with {@link #compromisedAt} and {@link #undefinedAt} to determine
   * where the lost value may have been compromised.  Map from 
   * field declaration IRNodes to a set of IRNodes indicating locations where
   * the field is lost.
   */
  private final Map<IRNode, Set<CompromisedField>> lostFields =
    new HashMap<IRNode, Set<CompromisedField>>();
  private final Map<IRNode, Set<CompromisedField>> lostFieldsAbrupt =
    new HashMap<IRNode, Set<CompromisedField>>();
  
  /**
   * Records which local variables are buried by reads of unique/borrowed fields. Built
   * by {@link #opLoad(Store, IRNode, IRNode)} and
   * {@link #opLoadReachable(Store, IRNode)}. Two level map: Local Variable ->
   * Field Declaration -> set of srcOps
   */
  private final Map<Object, Map<IRNode, Set<IRNode>>> buryingLoads =
    new HashMap<Object, Map<IRNode, Set<IRNode>>>();
  
  /**
   * Records where variables with buried references are read. Built by
   * {@link #opGet(Store, IRNode, Object)}. After analysis, this is cross
   * referenced with {@link #buriedLocals} to determine where the variable was
   * buried.
   */
  private final Set<BuriedRead> buriedReads = new HashSet<BuriedRead>();  
  
  /**
   * Records which local variables are made undefined when a a from field 
   * is cleared by {@link StoreLattice#undefineFromNodes}.
   * Map of Local Variable -> set of srcOps
   */
  private final Map<Object, Set<UndefinedFrom>> undefinedFroms =
    new HashMap<Object, Set<UndefinedFrom>>();

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
   * Track which stack locations are made undefined and where. This is a map
   * from stack indices to sets of IRNodes indicating source locations where the
   * stack location may have been made undefined by use of a value as a unique
   * parameter. Built by {@link #opUndefine(Store, IRNode)}. Used when handling
   * "undefined" errors to out why the stack position is undefined.
   */
  private final Map<Integer, Set<IRNode>> stackUndefinedAt =
    new HashMap<Integer, Set<IRNode>>();

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
  private final Set<ResultDrop> drops = new HashSet<ResultDrop>();

  
  
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
  // == Compromising unique fields
  // ==================================================================
  
  public void recordReadOfBorrowedField(final IRNode srcOp,
      final PromiseDrop<? extends IAASTRootNode> promiseDrop) {
    addToMappedSet(
        readBorrowedFields, promiseDrop, new BorrowedRead(srcOp, abruptDrops));
  }

  public void recordCompromisingOfUnique(
      final IRNode srcOp, final Integer topOfStack, final State localStatus,
      final ImmutableSet<FieldTriple> fieldStore,
      int msg, final Object... args) {
    recordLossOfUniqueness(
        new CompromisingSite(srcOp, msg, args), topOfStack, localStatus,
        fieldStore, compromisedAt);
  }
  
  public void recordUndefiningOfUnique(
      final IRNode srcOp, final Integer topOfStack, final State localStatus,
      final Store s) {
    if (!suppressDrops) {
      recordLossOfUniqueness(srcOp, topOfStack, localStatus, s.getFieldStore(), undefinedAt);
      // Find all the stack locations about to made undefined
      for (final ImmutableHashOrderSet<Object> object : s.getObjects()) {
        if (object.contains(topOfStack)) {
          for (final Object o : object) {
            if (o instanceof Integer) {
              addToMappedSet(stackUndefinedAt, (Integer) o, srcOp);
            }
          }
        }
      }
    }
  }
  
  private <T> void recordLossOfUniqueness(final T v, final Integer topOfStack,
      final State localStatus, final ImmutableSet<FieldTriple> fieldStore,
      final Map<IRNode, Set<T>> howLostMap) {
    if (!suppressDrops) {
      if (localStatus == State.UNIQUE || localStatus == State.UNIQUEWRITE) {
        for (final FieldTriple ft : fieldStore) {
          if (ft.third().contains(topOfStack)) {
            addToMappedSet(howLostMap, ft.second(), v);
          }
        }
      }
    }
  }
  
  public void recordLoadOfCompromisedField(
      final IRNode srcOp, final State fieldState, final IRNode fieldDecl) {
    if (!suppressDrops) {
      // Record look up of compromised field
      addToMappedSet(
          abruptDrops ? loadedCompromisedFieldsAbrupt : loadedCompromisedFields,
              fieldDecl, new CompromisedField(fieldState, srcOp));
    }        
  }
  
  public void recordIndirectLoadOfCompromisedField(
      final IRNode srcOp, final State fieldState, final IRNode fieldDecl) {
    if (!suppressDrops) {
      // Record look up of compromised field
      addToMappedSet(
          abruptDrops ? indirectlyLoadedCompromisedFieldsAbrupt :
            indirectlyLoadedCompromisedFields,
          fieldDecl, new CompromisedField(fieldState, srcOp));
    }
  }
  
  public void recordLossOfCompromisedField(
      final IRNode srcOp, final State fieldState, final IRNode fieldDecl) {
    if (!suppressDrops) {
      addToMappedSet(abruptDrops ? lostFieldsAbrupt : lostFields, fieldDecl,
          new CompromisedField(fieldState, srcOp));
    }        
  }
  

  
  // ==================================================================
  // == Alias burying
  // ==================================================================
  
  public void recordBuriedRead(final IRNode srcOp, final Object local,
      final BuriedMessage msg) {
    if (!suppressDrops) {
      buriedReads.add(new BuriedRead(msg, local, srcOp, abruptDrops));
    }
  }
  
  public void recordUndefinedFrom(
      final IRNode srcOp, final Set<Object> affectedVars, final int msg) {
    for (final Object lv : affectedVars) {
      addToMappedSet(undefinedFroms, lv, new UndefinedFrom(msg, srcOp));
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
  
  public void recordBuryingFieldRead(final IRNode srcOp,
      final IRNode fieldDecl, final Set<Object> affectedVars) {
    if (!suppressDrops) {
      recordBuryingLoad(fieldDecl, affectedVars, srcOp);
      for (final Object v : affectedVars) {
        if (v instanceof Integer) {
          addToMappedSet(stackBuriedAt, (Integer) v, srcOp);
        }
      }
    }
  }
  
  public void recordBuryingMethodEffects(final IRNode srcOp,
      final Set<IRNode> loadedFields, final Set<Object> affectedVars,
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

  private ResultDrop createResultDrop(
      final boolean abruptDrops, final boolean addToControlFlow,
      final PromiseDrop<? extends IAASTRootNode> promiseDrop,
      final IRNode node, final boolean isConsistent, 
      final int msg, final Object... args) {
    final Object[] newArgs = new Object[args.length+1];
    System.arraycopy(args, 0, newArgs, 0, args.length);
    newArgs[args.length] =
      abruptDrops ? Messages.ABRUPT_EXIT : Messages.NORMAL_EXIT;
    
    final ResultDrop result = new ResultDrop(node);
    drops.add(result);
    result.addChecked(promiseDrop);
    if (promiseDrop != controlFlowDrop) {
      if (addToControlFlow) {
        result.addChecked(controlFlowDrop);
        hasControlFlowResults = true;
      }
    } else {
      hasControlFlowResults = true;
    }
    result.setConsistent(isConsistent);
    result.setMessage(msg, newArgs);
    return result;
  }

  private ResultDrop createResultDrop(
      final boolean abruptDrops,
      final PromiseDrop<? extends IAASTRootNode> promiseDrop,
      final IRNode node, final boolean isConsistent, 
      final int msg, final Object... args) {
    return createResultDrop(
        abruptDrops, true, promiseDrop, node, isConsistent, msg, args);
  }
  
  private void crossReferenceKilledFields(
      final int msg, final boolean isAbrupt,
      final Map<IRNode, Set<CompromisedField>> compromisedFields) {
    for (final Map.Entry<IRNode, Set<CompromisedField>> load : compromisedFields.entrySet()) {
      final IRNode fieldDecl = load.getKey();
      final Set<CompromisingSite> compromises = compromisedAt.get(fieldDecl);
      final Set<IRNode> undefines = undefinedAt.get(fieldDecl);
      
      final IUniquePromise unique = UniquenessUtils.getUnique(fieldDecl);
      final PromiseDrop<? extends IAASTRootNode> fieldPromise =
          (unique != null) ? unique.getDrop() : UniquenessUtils.getFieldBorrowed(fieldDecl);
      // TODO: fix this for real.  Need to handle the 'null' "from" field specailly in the results
      if (fieldPromise == null) continue;
      
      for (final CompromisedField cf : load.getValue()) {
        final ResultDrop r = createResultDrop(
            isAbrupt, fieldPromise, cf.srcOp, false, msg, cf.fieldState.getAnnotation());
        if (compromises != null) {
          for (final CompromisingSite compromisedAt : compromises) {
            r.addSupportingInformation(
                compromisedAt.srcOp, compromisedAt.msg, compromisedAt.varargs);
          }
        }
        if (undefines != null) {
          for (final IRNode undefinedAt : undefines) {
            r.addSupportingInformation(undefinedAt, Messages.UNDEFINED_BY,
                DebugUnparser.toString(undefinedAt));
          }
        }
      }
    }
  }

  public void makeResultDrops() {
    // Link loaded compromised fields with comprising locations
    crossReferenceKilledFields(Messages.COMPROMISED_READ, false, loadedCompromisedFields);
    crossReferenceKilledFields(Messages.COMPROMISED_READ, true, loadedCompromisedFieldsAbrupt);
    crossReferenceKilledFields(Messages.COMPROMISED_INDIRECT_READ, false, indirectlyLoadedCompromisedFields);
    crossReferenceKilledFields(Messages.COMPROMISED_INDIRECT_READ, true, indirectlyLoadedCompromisedFieldsAbrupt);
    
    // Link lost compromised fields with compromising locations
    crossReferenceKilledFields(Messages.LOST_COMPROMISED_FIELD, false, lostFields);
    crossReferenceKilledFields(Messages.LOST_COMPROMISED_FIELD, true, lostFieldsAbrupt);

    /* TODO: Need to make sure we don't create duplicated READ_OF_UNDEFINED_VAR
     * nodes.  Can be created in the first section, and the second section.
     */
    
    // Link reads of buried references to burying field loads
    for (final BuriedRead read : buriedReads) {
      final Map<IRNode, Set<IRNode>> loads = buryingLoads.get(read.var);
      if (loads != null) {
        for (final Map.Entry<IRNode, Set<IRNode>> e : loads.entrySet()) {
          final ResultDrop r = createResultDrop(read.isAbrupt,
              UniquenessUtils.getFieldUniqueOrBorrowed(e.getKey()), read.srcOp,              
              false, read.getMessage(), read.getVarArgs());
          for (final IRNode buriedAt : e.getValue()) {
            r.addSupportingInformation(buriedAt, Messages.BURIED_BY, 
                DebugUnparser.toString(buriedAt));
          }
        }
      }
      
      // Could be undefined because of a cleared FROM field
      final Set<UndefinedFrom> y = undefinedFroms.get(read.var);
      if (y != null) {
        final ResultDrop r = createResultDrop(
            read.isAbrupt, controlFlowDrop, read.srcOp, false,
            Messages.READ_OF_UNDEFINED_VAR);
        for (final UndefinedFrom uf : y) {
          r.addSupportingInformation(uf.srcOp, uf.message);
        }
      }
      
      // Could be undefined because we were assigned an undefined value
      final Set<IRNode> z = badSets.get(read.var);
      if (z != null) {
        final ResultDrop r = createResultDrop(read.isAbrupt,
            controlFlowDrop, read.srcOp, false, Messages.READ_OF_BURIED);
        for (final IRNode setOp : z) {
          r.addSupportingInformation(setOp, Messages.ASSIGNED_UNDEFINED_BY,
              VariableUseExpression.prototype.includes(read.srcOp) ?
                  VariableUseExpression.getId(read.srcOp) : "*UNKNOWN*",
              DebugUnparser.toString(setOp));
        }
      }
    }

    /* Report reads of borrowed fields */
    for (final Map.Entry<PromiseDrop<? extends IAASTRootNode>, Set<BorrowedRead>> e : readBorrowedFields.entrySet()) {
      final PromiseDrop<? extends IAASTRootNode> borrowedPromise = e.getKey();
      for (final BorrowedRead br : e.getValue()) {
        @SuppressWarnings("unused")
        final ResultDrop r = createResultDrop(
            br.isAbrupt, borrowedPromise, br.srcOp, false,
            Messages.CANNOT_READ_BORROWED_FIELD);
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
