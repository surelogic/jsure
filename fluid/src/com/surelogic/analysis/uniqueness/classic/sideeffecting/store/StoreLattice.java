package com.surelogic.analysis.uniqueness.classic.sideeffecting.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.JavaSemanticsVisitor;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.uniqueness.classic.sideeffecting.Messages;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.analysis.uniqueness.classic.sideeffecting.UniquenessAnalysis;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.RegionEffectsPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.BorrowedPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.IUniquePromise;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquePromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquenessControlFlowDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.MethodBody;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.ReturnStatement;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.Triple;
import edu.uwm.cs.fluid.util.FlatLattice2;
import edu.uwm.cs.fluid.util.FlatLattice2.Element;
import edu.uwm.cs.fluid.util.TripleLattice;
import edu.uwm.cs.fluid.util.UnionLattice;

public final class StoreLattice
extends TripleLattice<Element<Integer>,
    ImmutableSet<ImmutableHashOrderSet<Object>>,
    ImmutableSet<FieldTriple>,
    Store,
    FlatLattice2<Integer>,
    UnionLattice<ImmutableHashOrderSet<Object>>,
    UnionLattice<FieldTriple>> {
  private static final ImmutableHashOrderSet<Object> EMPTY =
    ImmutableHashOrderSet.<Object>emptySet();

  private static final ImmutableHashOrderSet<Object> PSEUDOS =
    EMPTY.
      addElement(State.UNDEFINED).
      addElement(State.BORROWED).
      addElement(State.SHARED);

  private final IRNode[] locals;
  
  // for creating drops
  private final IBinder binder;
  private final AbstractWholeIRAnalysis<UniquenessAnalysis,?> analysis;
  
  /**
   * Should we create drops at all.  This is set by the analysis using
   * the lattice.  That is, are we at the side-effecing stage yet.
   */
  private boolean produceSideEffects = false;
    
  /**
   * If we are creating drops, should temporarily suppress creation of the
   * drops.  This is used internally, and set when along the abrupt termination
   * path should not be created because they are going to duplicate results
   * reported along the normal termination path.
   */
  private boolean suppressDrops = false;
  
  /** Are the results we are creating for the abrupt termination path */
  private boolean abruptDrops = false;

  
  
  // ==================================================================
  // === Side-effecting state.  This state is set on the last pass
  // === (when makeDrops is true) and used to build the chains
  // === evidence later.
  // ==================================================================
  
  /**
   * Track which unique fields are possibly compromised and where. This is a map
   * from field declaration IRNodes to a set of IRNodes indicating source
   * locations where the field may have been compromised. Built by
   * {@link #opCompromiseNoRelease(Store, IRNode)}.
   */
  private final Map<IRNode, Set<IRNode>> compromisedAt =
    new HashMap<IRNode, Set<IRNode>>();
  
  /**
   * Track which unique fields are made undefined and where. This is a map
   * from field declaration IRNodes to a set of IRNodes indicating source
   * locations where the field may have been made undefined. Built by
   * {@link #opUndefine(Store, IRNode)}.
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
  private final Map<IRNode, Set<IRNode>> loadedCompromisedFields =
    new HashMap<IRNode, Set<IRNode>>();
  private final Map<IRNode, Set<IRNode>> loadedCompromisedFieldsAbrupt =
    new HashMap<IRNode, Set<IRNode>>();
  
  /**
   * Records where compromised fields are loaded as the result of executing a method.  After analysis is over,
   * we cross reference this set with {@link #compromisedAt} and {@link #undefinedAt} to determine
   * where the loaded value may have been compromised.  Map from 
   * field declaration IRNodes to a set of IRNodes indicating locations where
   * the field is read and found to be compromised.
   */
  private final Map<IRNode, Set<IRNode>> indirectlyLoadedCompromisedFields =
    new HashMap<IRNode, Set<IRNode>>();
  private final Map<IRNode, Set<IRNode>> indirectlyLoadedCompromisedFieldsAbrupt =
    new HashMap<IRNode, Set<IRNode>>();
  
  /**
   * Records where compromised fields are lost.  After analysis is over,
   * we cross reference this set with {@link #compromisedAt} and {@link #undefinedAt} to determine
   * where the lost value may have been compromised.  Map from 
   * field declaration IRNodes to a set of IRNodes indicating locations where
   * the field is lost.
   */
  private final Map<IRNode, Set<IRNode>> lostFields =
    new HashMap<IRNode, Set<IRNode>>();
  private final Map<IRNode, Set<IRNode>> lostFieldsAbrupt =
    new HashMap<IRNode, Set<IRNode>>();
  
  /**
   * Records which local variables are buried by reads of unique fields. Built
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
   * Records where an UNDEFINED value is assigned to a local variable.  Can
   * happen when a variable is assigned a method return value, the 
   * return value is the SHARED object, and the effects of the method call
   * cause the SHARED object to be invalidated.  Map from
   * local variable stack position to set of IRNodes representing invalidating
   * assignments.
   */
  private final Map<Object, Set<IRNode>> badSets = new HashMap<Object, Set<IRNode>>();
  
  /**
   * The places where a unique value is expected to be Y but is actually X. For
   * example, the reference is shared by extected to be unique. Built by
   * {@link #opUndefine(Store, IRNode)} and
   * {@link #opCompromiseNoRelease(Store, IRNode)}.
   */
  private final Set<XNotY> xNotY = new HashSet<XNotY>();

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
   * Track where uniqueness is satisfied. 
   */
  private final Set<GoodResult> goodUnique = new HashSet<GoodResult>();  
  
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
   * Set of borrowed annotations (or UniquePromiseDrop in the case of return
   * value of a constructor) that have results. Checked in
   * {@link #makeResultDrops()}: any promise that doesn't have any results is
   * given a generic "invariants respected" result.
   */
  private final Set<PromiseDrop<? extends IAASTRootNode>> borrowedHasResults =
    new HashSet<PromiseDrop<? extends IAASTRootNode>>();
  
  /**
   * All the result drops we are going to have created.  Saved up here in case
   * we need to cancel them if the analysis times out.
   */
  private final Set<ResultDrop> drops = new HashSet<ResultDrop>();
  
  
  
  // ==================================================================
  // === Constructor 
  // ==================================================================
  
  public StoreLattice(final IRNode flowUnit,
      final AbstractWholeIRAnalysis<UniquenessAnalysis,?> analysis,
      final IBinder binder, final IRNode[] locals) {
    super(new FlatLattice2<Integer>(),
        new UnionLattice<ImmutableHashOrderSet<Object>>(),
        new UnionLattice<FieldTriple>());
    this.locals = locals;
    this.binder = binder;
    this.analysis = analysis;
    this.controlFlowDrop = UniquenessControlFlowDrop.create(flowUnit);
  }
  
  public int getNumLocals() {
    return locals.length;
  }
  
  public void setSideEffects(final boolean value) {
    produceSideEffects = value;
  }
  
  public void setSuppressDrops(final boolean value) {
    suppressDrops = value;
  }
  
  public void setAbruptResults(final boolean value) {
    abruptDrops = value;
  }
  
  
  // ==================================================================
  // === Create a new lattice element 
  // ==================================================================
  
  @Override
  protected Store newTriple(final Element<Integer> size,
    final ImmutableSet<ImmutableHashOrderSet<Object>> objects,
    final ImmutableSet<FieldTriple> edges) {
    return new Store(size, objects, edges);
  }

  
  
  // ==================================================================
  // === Short hand for resetting store members.  Done here, and NOT
  // === in the Store class because we check to see if the resulting
  // === Store is top or bottom.
  // ==================================================================
  
  protected Store setStackSize(final Store store, final Integer ss) {
    return replaceFirst(store, FlatLattice2.asMember(ss));
  }

  protected Store setObjects(final Store store, final ImmutableSet<ImmutableHashOrderSet<Object>> objects) {
    return replaceSecond(store, objects);
  }

  protected Store setFieldStore(final Store store, final ImmutableSet<FieldTriple> fieldStore) {
    return replaceThird(store, fieldStore);
  }
  
  
  
  // ==================================================================
  // === Error handling 
  // ==================================================================
  
  Store errorStore(final String msg) {
    /* Create the new triple directly because we don't want replaceFirst()
     * to substitute the original bottom back in. 
     * Create a store that is invalid, but not top or bottom.
     */
    return newTriple(
        FlatLattice2.<Integer>errorTop(msg), bottom().second(), top().third());
  }
  
  @Override
  public Store join(final Store s1, final Store s2) {
    Store m = super.join(s1, s2);
    if (!m.isValid() && !equals(m, bottom())) {
      // try to preserve cause
      if (!s1.isValid() && !equals(s1, bottom())) {
        return s1;
      }
      if (!s2.isValid() && !equals(s2, bottom())) {
        return s2;
      }
      return errorStore("stacksize mismatch");
    }
    return m;
  }

  
  
  // ==================================================================
  // === Basic operations 
  // ==================================================================

  public Store push(final Store s) {
    if (!s.isValid()) return s;
    final int n = s.getStackSize();
    return setStackSize(s, n+1);
  }
  
  public Store pop(final Store s, final IRNode srcOp) {
    if (!s.isValid()) return s;
    final Integer topOfStack = getStackTop(s);
    final int n = topOfStack.intValue();
    if (n == 0) {
      return errorStore("stack underflow");
    }
    return setStackSize(
        apply(s, srcOp, new Remove(EMPTY.addElement(topOfStack))),
        Integer.valueOf(n-1));
  }
  
  /**
   * Return an enumeration element indicating the status of the local.
   * @see #isUnique
   * @see #isStoreable
   * @see #isDefined
   */
  public State localStatus(final Store s, final Object local) {
    State status = State.NULL;
    final ImmutableSet<ImmutableHashOrderSet<Object>> objects = s.getObjects();
    if (objects.isInfinite()) return State.UNDEFINED;
    for (final ImmutableSet<Object> node : objects) {
      if (node.contains(local)) {
        final State nstatus = nodeStatus(node);
        if (nstatus.compareTo(status) > 0) status = nstatus;
      }
    }
    return status;
  }

  /**
   * Return an enumeration element indicating the status of an object
   * description (a set).
   * 
   * @return The status of the object. Never returns {@link State#NULL}.
   */
  public State nodeStatus(final ImmutableSet<Object> node) {
    if (node.contains(State.UNDEFINED)) {
      return State.UNDEFINED;
    } else if (node.contains(State.SHARED)) {
      return State.SHARED;
    } else if (node.contains(State.BORROWED)) {
      return State.BORROWED;
    }
    return State.UNIQUE;
  }

  /**
   * return whether a local must be null or a primitive value. Actually the name
   * is a misnomer. It probably should be isNoObject() or something to that
   * effect.
   */
  public boolean isNull(final Store s, final Object local) {
    return localStatus(s, local).compareTo(State.NULL) == 0;
  }

  /** Return whether a local or stack location is unique. */
  public boolean isUnique(final Store s, final Object local) {
    return localStatus(s, local).compareTo(State.UNIQUE) <= 0;
  }

  /** Return whether a local or stack location is defined and not borrowed. */
  public boolean isStoreable(final Store s, final Object local) {
    return localStatus(s, local).compareTo(State.SHARED) <= 0;
  }

  /** Return whether local or stack location is defined. */
  public boolean isDefined(final Store s, final Object local) {
    return localStatus(s, local).compareTo(State.BORROWED) <= 0;
  }


  /**
   * Return current top stack location.
   * 
   * @precondition isValid()
   */
  public Integer getStackTop(final Store s) {
    return s.getStackSize();
  }

  /**
   * Return current stack location next to top.
   * 
   * @precondition isValid()
   */
  public Integer getUnderTop(final Store s) {
    return Integer.valueOf(getStackTop(s).intValue() - 1);
  }

  /** Return whether top of stack is unique. */
  public boolean isUnique(final Store s) {
    return isUnique(s, getStackTop(s));
  }

  /** Return whether top of stack may be stored. */
  public boolean isStoreable(final Store s) {
    return isStoreable(s, getStackTop(s));
  }

  /**
   * Return whether top of stack is defined. (It should also be so.)
   */
  public boolean isDefined(final Store s) {
    return isDefined(s, getStackTop(s));
  }

  

  // ==================================================================
  // === Stack Machine Operations 
  // ==================================================================

  public Store opStart(final IMayAlias mayAlias, final IRNode srcOp) {
    Store temp = bottom();
    
    /*
     * Start with nothing on stack, and just four objects {}, {undefined},
     * {borrowed}, {borrowed,shared}
     */
    temp = setStackSize(temp, Integer.valueOf(0));
    ImmutableHashOrderSet<ImmutableHashOrderSet<Object>> objects = 
      ImmutableHashOrderSet.<ImmutableHashOrderSet<Object>>emptySet();
    objects = objects.
      addElement( EMPTY ).
      addElement( EMPTY.addElement(State.UNDEFINED) ).
      addElement( EMPTY.addElement(State.BORROWED) ).
      addElement( EMPTY.addElement(State.BORROWED).addElement(State.SHARED) );
    temp = setObjects(temp, objects);
    
    /* Now add each parameter or local in turn.  Undefined locals are
     * removed altogether.
     */
    for (final IRNode local : locals) {
      final Operator op = JJNode.tree.getOperator(local);
      boolean isReceiverFromUniqueReturningConstructor = false;
      if (ReceiverDeclaration.prototype.includes(op)) {
        /*
         * Check if the receiver is from a constructor, and if so, whether the
         * return node of the constructor is unique
         */
        final IRNode decl = JavaPromise.getPromisedFor(local);
        if (ConstructorDeclaration.prototype.includes(decl)) {
          // It's from a constructor, look for unique on the return node
          final IRNode returnNode = JavaPromise.getReturnNode(decl);
          if (UniquenessRules.isUnique(returnNode)) {
            isReceiverFromUniqueReturningConstructor = true;
          }
        }
      }
      final IRNode parent = JJNode.tree.getParent(local);
      if (ReceiverDeclaration.prototype.includes(op) ||
          QualifiedReceiverDeclaration.prototype.includes(op) ||
          ParameterDeclaration.prototype.includes(op) && 
          (parent == null || !CatchClause.prototype.includes(JJNode.tree.getOperator(parent)))) {
        if (isReceiverFromUniqueReturningConstructor
            || UniquenessRules.isBorrowed(local)) {
          temp = opExistingBetter(temp, srcOp, State.BORROWED, mayAlias, local);
        } else if (UniquenessRules.isUnique(local)) {
          temp = opNew(temp);
        } else {
          temp = opExistingBetter(temp, srcOp, State.SHARED, mayAlias, local);
        }
        temp = pop(apply(temp, srcOp, new Add(getStackTop(temp), EMPTY.addElement(local))), srcOp);
      } 
    }
    
    return temp;
  }

  /**
   * Leave scope of method. Remove all local bindings.
   */
  public Store opStop(final Store s, final IRNode srcOp) {
    if (!s.isValid()) return s;
    return apply(s, srcOp, new Remove(new ImmutableHashOrderSet<Object>(locals)));
  }

  /**
   * Fetch the value of a local onto stack.
   **/
  public Store opGet(final Store s, final IRNode srcOp, final Object local) {
    if (!s.isValid()) return s;
    if (isDefined(s, local)) {
      Store temp = push(s);
      return apply(temp, srcOp, new Add(local, EMPTY.addElement(getStackTop(temp))));
    } else {
      recordBuriedRead(srcOp, local);
      return opNull(s);
    }
  }
  
  /**
   * Duplicate a stack value from further down stack
   * 
   * @param fromTop
   *          0 for duplicate top, 1 for under top etc.
   */
  public Store opDup(final Store s, final IRNode srcOp, final int fromTop) {
    if (!s.isValid()) return s;
    final Integer i = Integer.valueOf(getStackTop(s).intValue() - fromTop);
    return opGet(s, srcOp, i);
  }
  
  /** Store the top of the stack into a local. */
  public Store opSet(final Store s, final IRNode srcOp, final Object local) {
    if (!s.isValid()) return s;
    final ImmutableHashOrderSet<Object> lset = EMPTY.addElement(local);
    final Integer n = getStackTop(s);
    final State localStatus = localStatus(s, n);
    if (localStatus == State.UNDEFINED) {
      recordBadSet(local, srcOp);
    }
    
    return pop(
        apply(
            apply(s, srcOp, new Remove(lset)),
            srcOp, new Add(n, lset)),
        srcOp);
  }

  /**
   * Add a field edge from {@link from} to {@link to}. (We add the edge between
   * every node that "from" aliases to every node that "to" aliases.)
   * 
   * @param s
   *          store before
   * @param from
   *          variable source of edge
   * @param field
   *          field IRnode
   * @param to
   *          variable destination of edge
   * @return new store.
   **/
  public Store opConnect(Store s, Object from, IRNode field, Object to) {
    List<ImmutableHashOrderSet<Object>> sources = new ArrayList<ImmutableHashOrderSet<Object>>();
    List<ImmutableHashOrderSet<Object>> destinations = new ArrayList<ImmutableHashOrderSet<Object>>();
    for (ImmutableHashOrderSet<Object> obj : s.getObjects()) {
      if (obj.contains(from)) sources.add(obj);
      if (obj.contains(to)) destinations.add(obj);
    }
    List<FieldTriple> newFields = new ArrayList<FieldTriple>();
    for (ImmutableHashOrderSet<Object> source : sources) {
      for (ImmutableHashOrderSet<Object> dest : destinations) {
        newFields.add(new FieldTriple(source, field, dest));
      }
    }
    ImmutableHashOrderSet<FieldTriple> newFieldSet =
      new ImmutableHashOrderSet<FieldTriple>(newFields);
    s = setFieldStore(s, s.getFieldStore().union(newFieldSet));
    return s;
  }

  /**
   * Load a field from memory
   * 
   * @precondition isValid();
   */
  public Store opLoad(final Store s, final IRNode srcOp, final IRNode fieldDecl) {
    if (!s.isValid()) return s;
    final IUniquePromise uDrop = UniquenessUtils.getUnique(fieldDecl);
    if (uDrop != null) {
      final Integer n = getStackTop(s);
      final ImmutableSet<ImmutableHashOrderSet<Object>> objects = s.getObjects();
      ImmutableHashOrderSet<Object> affected = EMPTY;
      final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
      if (fieldStore.isInfinite()) return s;
      for (final FieldTriple t : fieldStore) {
        final ImmutableHashOrderSet<Object> object = t.first();
        if (object.contains(n) && objects.contains(object) && t.second().equals(fieldDecl)) {
          affected = affected.union(t.third());
        }
      }
      
      recordBuryingFieldRead(fieldDecl, affected, srcOp);
      
      if (nodeStatus(affected) != State.UNIQUE) {
        recordLoadOfCompromisedField(srcOp, fieldDecl);
      }
      Store temp = opNew(
          apply(
              apply(s, srcOp, new Remove(affected)),
              srcOp,
              new Add(State.UNDEFINED, affected)));
      ImmutableSet<FieldTriple> newFieldStore = temp.getFieldStore();
      final ImmutableHashOrderSet<Object> uniqueNode =
        EMPTY.addElement(getStackTop(temp));
      final ImmutableSet<ImmutableHashOrderSet<Object>> newObjects = temp.getObjects();
      if (newObjects.isInfinite()) return s;
      newFieldStore = addElements(newFieldStore,
          new FilterIterator<ImmutableHashOrderSet<Object>, FieldTriple>(newObjects.iterator()) {
            @Override
            protected Object select(final ImmutableHashOrderSet<Object> object) {
              if (object.contains(n)) {
                return new FieldTriple(object, fieldDecl, uniqueNode);
              }
              return IteratorUtil.noElement;
            }
          });
      temp = setFieldStore(temp, newFieldStore);
      return opSet(temp, srcOp, n);
    } else {
      return opExisting(opRelease(s, srcOp), srcOp, State.SHARED);
    }
  }
  
  private ImmutableHashOrderSet<Object> getUndefinedObject(Store s) {
    for (ImmutableHashOrderSet<Object> obj : s.getObjects()) {
      if (obj.contains(State.UNDEFINED))
        return obj;
    }
    SLLogger.getLogger().log(Level.SEVERE, "No undefined object: " + toString(s));
    return null; // Cause more errors!
  }
  
  public Store opStore(
      final Store s, final IRNode srcOp, final IRNode fieldDecl,
      final BindingContextAnalysis.Query bcaQuery) {
    if (!s.isValid()) return s;
    final Store temp;
    final IUniquePromise uDrop = UniquenessUtils.getUnique(fieldDecl);
    if (uDrop != null) {
      final Integer undertop = getUnderTop(s);
      final Integer stacktop = getStackTop(s);
      // We used to undefined everything that aliased the value stored in the unique field.
      // We now simply undefined other field values that point to it, and keep the
      // aliases as if they just read the field.
      final ImmutableHashOrderSet<Object> undefinedObject = getUndefinedObject(s);
      HashSet<FieldTriple> newFields = new HashSet<FieldTriple>();
      for (FieldTriple t : s.getFieldStore()) {
        if (fieldDecl.equals(t.second()) && t.first().contains(undertop)) continue; // remove
        if (t.third().contains(stacktop)) {
          // old field is compromised
          newFields.add(new FieldTriple(t.first(), t.second(), undefinedObject));
        } else {
          newFields.add(t);
        }
      }
      
      // STOLE THIS FROM opUndefine()
      // Check that the top of stack makes sense
      final State localStatus = localStatus(s, stacktop);
      if (localStatus.compareTo(State.BORROWED) > 0) { // cannot be undefined
        // XXX: Cannot happen.  Results in an error on the load/get of the undefined value
        SLLogger.getLogger().log(Level.SEVERE, "Undefined value on top of stack in opStore");
      } else if (localStatus.compareTo(State.SHARED) > 0) { // cannot be borrowed
        recordBorrowedNotUnique(
            srcOp, uDrop.getDrop(), MessageChooser.ASSIGN, stacktop, s.getObjects());
//        reportError(srcOp, "U2", "Borrowed value encountered when a unique value was expected");
//        return errorStore("Borrowed value on stack not unique");
      } else if (localStatus.compareTo(State.UNIQUE) > 0) { // cannot be shared
        recordSharedNotUnique(srcOp, uDrop.getDrop(), MessageChooser.ASSIGN);      
//        reportError(srcOp, "U3", "Aliased value encountered when a unique value was expected");
//        return errorStore("Shared value on stack not unique");
      } else if (localStatus.compareTo(State.NULL) > 0) { // can be unique
        recordGoodUnique(srcOp, uDrop.getDrop(), Messages.ASSIGN_IS_UNIQUE,
            new InfoAdder() {
              @Override
              public void addSupportingInformation(
                  final AbstractWholeIRAnalysis<UniquenessAnalysis, ?> analysis,
                  final IBinder binder, final ResultDrop resultDrop) {
                final IRNode rhs;
                if (VariableDeclarator.prototype.includes(srcOp)) {
                  rhs = VariableDeclarator.getInit(srcOp);
                } else { // assign expression
                  rhs = AssignExpression.getOp2(JJNode.tree.getParent(srcOp));
                }
                for (final IRNode n : bcaQuery.getResultFor(rhs)) {
                  PromiseDrop<? extends IAASTRootNode> uDrop = null;
                  final Operator op = JJNode.tree.getOperator(n);
                  if (FieldRef.prototype.includes(op)) {
                    uDrop = UniquenessUtils.getUnique(binder.getBinding(n)).getDrop();
                  } else if (ParameterDeclaration.prototype.includes(op)) {
                    uDrop = UniquenessRules.getUnique(n);
                  } else if (NewExpression.prototype.includes(op)) {
                    final IRNode cdecl = binder.getBinding(n);
                    uDrop = UniquenessRules.getBorrowedReceiver(cdecl);
                  } else if (MethodCall.prototype.includes(op)) {
                    uDrop = UniquenessRules.getUnique(
                        JavaPromise.getReturnNode(binder.getBinding(n)));
                  }
                  if (uDrop != null) {
                    resultDrop.addTrusted(uDrop);
                  }
                }
              }
            });
      } else { // can be null
        recordGoodUnique(srcOp, uDrop.getDrop(), Messages.ASSIGN_IS_NULL, null);
      }     
      
      temp = opRelease(opConnect(
          setFieldStore(s, new ImmutableHashOrderSet<FieldTriple>(newFields)),
          undertop, fieldDecl, stacktop), srcOp);
    } else {
      temp = opCompromise(s, srcOp);
    }
    return opRelease(temp, srcOp);
  }
  
  /**
   * Return the store after reading everything reachable from the top of the
   * stack and then popping this value. In essence, any variable referring to
   * structure reachable from the top of the stack is made undefined (alias
   * burying). Used to implement read (and write) effects.
   */
  public Store opLoadReachable(final Store s, final IRNode srcOp,
      final RegionEffectsPromiseDrop fxDrop) {
    if (!s.isValid()) return s;
    final Integer n = getStackTop(s);
    final Set<ImmutableHashOrderSet<Object>> found = new HashSet<ImmutableHashOrderSet<Object>>();
    final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
    if (fieldStore.isInfinite()) {
      SLLogger.getLogger().log(Level.SEVERE, "internal error: loadReachable has an infinite field store");
      return s;
    }
    boolean done;
    final Set<IRNode> loadedFields = new HashSet<IRNode>();
    final Set<Object> affectedM = new HashSet<Object>();
    do {
      done = true;
      for (final FieldTriple t : fieldStore) {
        final ImmutableHashOrderSet<Object> object = t.first();
        if (object.contains(n) || found.contains(object)) {
          final ImmutableHashOrderSet<Object> newObject = t.third();
          final State newStatus = nodeStatus(newObject);
          if (newStatus != State.UNIQUE) {
            recordIndirectLoadOfCompromisedField(srcOp, t.second());
//            reportError(srcOp, "LR", "Method may read a compromised unique field");
//            return errorStore("loaded compromised field");
          }          
          if (found.add(newObject)) done = false;
          
          for (final Object v : newObject) {
            if (!(v instanceof State)) affectedM.add(v);
          }
          
          loadedFields.add(t.second());
        }
      }
    } while (!done);
    final ImmutableHashOrderSet<Object> affected =
      new ImmutableHashOrderSet<Object>(affectedM);

    recordBuryingMethodEffects(loadedFields, affected, srcOp, fxDrop);

    return opRelease(
        apply(
            apply(s, srcOp, new Remove(affected)),
            srcOp,
            new Add(State.UNDEFINED, affected)), srcOp);
  }
  
  /** Push the value "null" onto the top of the stack. */
  public Store opNull(final Store s) {
    if (!s.isValid()) return s;
    return push(s);
  }
    
  /**
   * "Allocate" a new unique value not reachable from anywhere else, but
   * possible having pointers to existing things.
   */
  public Store opNew(final Store s) {
    if (!s.isValid()) return s;
    Store temp = push(s);
    Integer n = temp.getStackSize();
    final ImmutableHashOrderSet<Object> nset = EMPTY.addElement(n);
    temp = setObjects(temp, addElement(temp.getObjects(), nset));
    
    ImmutableSet<FieldTriple> fieldStore = temp.getFieldStore();
    if (fieldStore.isInfinite()) return s;
    fieldStore = addElements(fieldStore,
        new FilterIterator<FieldTriple, FieldTriple>(fieldStore.iterator()) {
          @Override
          protected Object select(final FieldTriple t) {
            if (t.first().isEmpty()) {
              return new FieldTriple(nset, t.second(), t.third());
            }
            return IteratorUtil.noElement;
          }
        });
    return setFieldStore(temp, fieldStore);
  }

  /**
   * Evaluate a pseudo-variable onto the top of the stack. A pseudo-variable can
   * have multiple values.
   */
  public Store opExisting(final Store s, final IRNode srcOp, final State pv) {
    if (!s.isValid()) return s;
    Store temp = push(s);
    final ImmutableHashOrderSet<Object> nset = EMPTY.addElement(getStackTop(temp));
    return join(temp, apply(temp, srcOp, new Add(pv, nset)));
  }

  public Store opExistingBetter(final Store s, final IRNode srcOp, final State pv, final IMayAlias mayAlias, final IRNode decl) {
    if (!s.isValid()) return s;
    Store temp = push(s);
    final ImmutableHashOrderSet<Object> nset = EMPTY.addElement(getStackTop(temp));
    return join(temp, apply(temp, srcOp, new AddBetter(mayAlias, decl, pv, nset)));
  }

  /**
   * discard the value on the top of the stack from the set of objects and from
   * the field store, and then pop the stack.
   */
  public Store opRelease(final Store s, final IRNode srcOp) {
    if (!s.isValid()) return s;
    return pop(apply(s, srcOp, new Remove(EMPTY.addElement(getStackTop(s)))), srcOp);
  }

  /**
   * Ensure the top of the stack is at least borrowed and then pop the stack.
   */
  public Store opBorrow(final Store s, final IRNode srcOp,
      final IRNode calledMethod, final IRNode methodCall,
      final PromiseDrop<? extends IAASTRootNode> bDrop,
      final BindingContextAnalysis.Query bcaQuery) {
    if (!s.isValid()) return s;
    final Integer n = getStackTop(s);
    final State localStatus = localStatus(s, n);

    if (localStatus.compareTo(State.BORROWED) > 0) { // cannot be undefined
      recordUndefinedNotX(srcOp, controlFlowDrop, n);
//      reportError(srcOp, "X100", "(opBorrow) Undefined value where unique is expected: Another actual parameter has made the value undefined here");
//      return errorStore("Undefined value on stack borrowed");
    } else if (localStatus == State.BORROWED) { // is borrowed
      if (shouldRecordResult()) {
        recordBorrowedPassedToBorrowed(
            srcOp, calledMethod, methodCall, bDrop, bcaQuery);
      }
    }
    return opRelease(s, srcOp);
  }

  /**
   * Compromise the value on the top of the stack.
   */
  public Store opCompromiseNoRelease(final Store s, final IRNode srcOp) {
    if (!s.isValid()) return s;
    final Integer n = getStackTop(s);
    final State localStatus = localStatus(s, n);
    
    recordCompromisingOfUnique(srcOp, n, localStatus, s.getFieldStore());

    if (localStatus.compareTo(State.BORROWED) > 0) { // cannot be undefined
      recordUndefinedNotX(srcOp, controlFlowDrop, n);      
//      reportError(srcOp, "X1", "(opCompromiseNoRelease) Use of undefined value");
//      return errorStore("Undefined value on stack shared");
    } else if (localStatus.compareTo(State.SHARED) > 0) { // cannot be borrowed
      recordBorrowedNotShared(srcOp, n, s.getObjects());
//      reportError(srcOp, "X2", "Attempt to alias a borrowed value");
//      return errorStore("Borrowed value on stack shared");
    }
    return apply(s, srcOp, new Add(n, EMPTY.addElement(State.SHARED)));
  }
  
  /**
   * Compromise the value on the top of the stack and then pop it off.
   */
  public Store opCompromise(final Store s, final IRNode srcOp) {
    return opRelease(opCompromiseNoRelease(s, srcOp), srcOp);
  }
  
  /**
   * Make the top of the stack undefined and then pop it. The value is being
   * requested as a unique object and no one is allowed to reference it *ever
   * again*
   */
  public Store opUndefine(
      final Store s, final IRNode srcOp, final UniquePromiseDrop uDrop,
      final MessageChooser msg, final BindingContextAnalysis.Query bcaQuery) {
    if (!s.isValid()) return s;
    final Integer n = getStackTop(s);
    final State localStatus = localStatus(s, n);
    
    recordUndefiningOfUnique(srcOp, n, localStatus, s);
    
    if (localStatus.compareTo(State.BORROWED) > 0) { // cannot be undefined
      recordUndefinedNotX(srcOp, uDrop, n);
//      reportError(srcOp, "U1", "(opUndefine) Undefined value encountered when a unique value was expected");
//      return errorStore("Undefined value on stack not unique");
    } else if (localStatus.compareTo(State.SHARED) > 0) { // cannot be borrowed
      recordBorrowedNotUnique(srcOp, uDrop, msg, n, s.getObjects());
//      reportError(srcOp, "U2", "Borrowed value encountered when a unique value was expected");
//      return errorStore("Borrowed value on stack not unique");
    } else if (localStatus.compareTo(State.UNIQUE) > 0) { // cannot be shared
      recordSharedNotUnique(srcOp, uDrop, msg);      
//      reportError(srcOp, "U3", "Aliased value encountered when a unique value was expected");
//      return errorStore("Shared value on stack not unique");
    } else if (localStatus.compareTo(State.NULL) > 0) { // can be unique
      recordGoodUnique(srcOp, uDrop,
          msg.chooseMsg(Messages.ACTUAL_IS_UNIQUE,
              Messages.RETURN_IS_UNIQUE, Messages.ASSIGN_IS_UNIQUE),
              new InfoAdder() {
                @Override
                public void addSupportingInformation(
                    final AbstractWholeIRAnalysis<UniquenessAnalysis, ?> analysis,
                    final IBinder binder, final ResultDrop resultDrop) {
                  if (msg == MessageChooser.ACTUAL) {
                    for (final IRNode n : bcaQuery.getResultFor(srcOp)) {
                      addUniquePromiseFrom(n, binder, resultDrop);
                    }
                  } else { // return
                    final Set<IRNode> srcs = new HashSet<IRNode>();
                    new JavaSemanticsVisitor(false, true, JJNode.tree.getParent(srcOp)) {
                      @Override
                      public Void visitReturnStatement(final IRNode s) {
                        srcs.addAll(bcaQuery.getResultFor(
                            ReturnStatement.getValue(s)));
                        return null;
                      }
                    }.doAccept(srcOp);
                    
                    for (final IRNode n : srcs) {
                      addUniquePromiseFrom(n, binder, resultDrop);
                    }
                  }
                }

                private void addUniquePromiseFrom(final IRNode n,
                    final IBinder binder, final ResultDrop resultDrop) {
                  PromiseDrop<? extends IAASTRootNode> uDrop = null;
                  final Operator op = JJNode.tree.getOperator(n);
                  if (ReceiverDeclaration.prototype.includes(op)) {
                    uDrop = UniquenessRules.getUnique(n);
                  } else if (FieldRef.prototype.includes(op)) {
                    uDrop = UniquenessUtils.getUnique(binder.getBinding(n)).getDrop();
                  } else if (ParameterDeclaration.prototype.includes(op)) {
                    uDrop = UniquenessRules.getUnique(n);
                  } else if (NewExpression.prototype.includes(op)) {
                    uDrop = UniquenessRules.getBorrowedReceiver(binder.getBinding(n));
                  } else if (MethodCall.prototype.includes(op)) {
                    uDrop = UniquenessRules.getUnique(
                        JavaPromise.getReturnNode(binder.getBinding(n)));
                  }
                  if (uDrop != null) {
                    resultDrop.addTrusted(uDrop);
                  }
                }
              });
    } else { // can be null
      recordGoodUnique(srcOp, uDrop,
          msg.chooseMsg(Messages.ACTUAL_IS_NULL,
              Messages.RETURN_IS_NULL, Messages.ASSIGN_IS_NULL), null);
    }
    return opRelease(apply(s, srcOp, new Add(n, EMPTY.addElement(State.UNDEFINED))), srcOp);
  }
  
  /**
   * Return a new store where we assume the two topmost stack elements are
   * equal/nonequal and then are popped.
   * 
   * @param areEqual
   *          true if the two elements are assumed equal, otherwise they are
   *          assumed unequal.
   */
  public Store opEqual(final Store s, final IRNode srcOp, final boolean areEqual) {
    if (!s.isValid()) return s;
    return opRelease(
        opRelease(
            filter(s, srcOp, new Equal(getStackTop(s), getUnderTop(s), areEqual)), srcOp), srcOp);
  }



  // ==================================================================
  // === Generic operations 
  // ==================================================================

  /** Apply a node-set changing operation to the state */
  protected Store apply(final Store s, final IRNode srcOp, final Apply c) {
    if (!s.isValid()) return s;

    final ImmutableSet<ImmutableHashOrderSet<Object>> objects = s.getObjects();
    // Abort if the set is infinite
    if (objects.isInfinite()) return s;
    final Iterator<ImmutableHashOrderSet<Object>> objsIter = objects.iterator();
    final Iterator<ImmutableHashOrderSet<Object>> filteredObjs = new SimpleRemovelessIterator<ImmutableHashOrderSet<Object>>() {
      @Override
      protected Object computeNext() {
        if (objsIter.hasNext()) {
          return c.apply(objsIter.next());
        }
        return IteratorUtil.noElement;
      }
    };
    final ImmutableSet<ImmutableHashOrderSet<Object>> newObjects =
      ImmutableHashOrderSet.<ImmutableHashOrderSet<Object>>emptySet().addElements(filteredObjs);
    
    final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
    // Abort if the set is infinite
    if (fieldStore.isInfinite()) return s;
    final Iterator<FieldTriple> fsIter = fieldStore.iterator();
    final Iterator<FieldTriple> filteredFields = new SimpleRemovelessIterator<FieldTriple>() {
      @Override
      protected Object computeNext() {
        if (fsIter.hasNext()) {
          final FieldTriple t = fsIter.next();
          return new FieldTriple(c.apply(t.first()), t.second(), c.apply(t.third()));
        }
        return IteratorUtil.noElement;
      }
    };
    final ImmutableSet<FieldTriple> newFieldStore =
      ImmutableHashOrderSet.<FieldTriple>emptySet().addElements(filteredFields);
    
    return check(setFieldStore(setObjects(s, newObjects), newFieldStore), srcOp);
  }
  
  
  
  /** Keep only nodes which fulfill the filter */
  protected Store filter(final Store s, final IRNode srcOp, final Filter f) {
    if (!s.isValid()) return s;

    final ImmutableSet<ImmutableHashOrderSet<Object>> objects = s.getObjects();
    if (objects.isInfinite()) return s;
    final Iterator<ImmutableHashOrderSet<Object>> objsIter = objects.iterator();
    final ImmutableSet<ImmutableHashOrderSet<Object>> newObjects =
      ImmutableHashOrderSet.<ImmutableHashOrderSet<Object>>emptySet().addElements(
          new FilterIterator<ImmutableHashOrderSet<Object>, ImmutableHashOrderSet<Object>>(objsIter) {
            @Override
            protected Object select(final ImmutableHashOrderSet<Object> o) {
              if (f.filter(o)) return o;
              return IteratorUtil.noElement;
            }
          });

    final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
    if (fieldStore.isInfinite()) return s;
    final Iterator<FieldTriple> fsIter = fieldStore.iterator();
    final ImmutableSet<FieldTriple> newFieldStore =
      ImmutableHashOrderSet.<FieldTriple>emptySet().addElements(
          new FilterIterator<FieldTriple, FieldTriple>(fsIter) {
            @Override
            protected Object select(final FieldTriple t) {
              if (f.filter(t.first()) && f.filter(t.third())) {
                return t;
              }
              return IteratorUtil.noElement;
            }
          });
    
    return check(setFieldStore(setObjects(s, newObjects), newFieldStore), srcOp);
  }

  /**
   * Check that there are no compromised fields on nodes known only through
   * pseudo-variables.
   */
  protected Store check(final Store s, final IRNode srcOp) {
    if (!s.isValid()) return s;
    
    final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
    if (fieldStore.isInfinite()) return s;
    ImmutableSet<FieldTriple> newFieldStore = fieldStore;
    for (final FieldTriple t : fieldStore) {
      final ImmutableHashOrderSet<Object> from = t.first();
      if (PSEUDOS.includes(from) && nodeStatus(t.third()).compareTo(State.UNIQUE) > 0) {
        // Remove the field from the store
        newFieldStore = newFieldStore.removeCopy(t);
        // report error
        recordLossOfCompromisedField(srcOp, t.second());
//        reportError(srcOp, "CF", "Reference to an object whose unique field was made undefined as been lost; it is no longer possible to restore the uniqueness invariant of the undefined field");
//        return errorStore("compromised field has been lost");
      }
    }
    return setFieldStore(s, newFieldStore);
  }

  @Override
  public String toString(final Store s) {
    if (s.equals(bottom())) {
      return "bottom";
    }
    final Element<Integer> ss = s.first();
    if (lattice1.equals(ss, lattice1.top())) {
      return lattice1.toString(ss);
    }
    final StringBuilder sb = new StringBuilder();
    sb.append("Stack depth: ");
    sb.append(ss);
    sb.append("\nObjects:\n");
    final ImmutableSet<ImmutableHashOrderSet<Object>> objects = s.getObjects();
    if (objects.isInfinite()) {
      sb.append("  <infinite>\n");
    } else {
      for (final ImmutableHashOrderSet<Object> obj : objects) {
        sb.append("  ");
        sb.append(nodeToString(obj));
        sb.append('\n');
      }
    }
    sb.append("Store:\n");
    final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
    if (fieldStore.isInfinite()) {
      sb.append("  <infinite>\n");
    } else {
      for (final FieldTriple t : fieldStore) {
        sb.append("  ");
        sb.append(tripleToString(t));
        sb.append('\n');
      }
    }
    if (locals != null) {
      sb.append("Summary:\n");
      for (final IRNode local : locals) {
        sb.append("  ");
        sb.append(localToString(local));
        sb.append(": ");
        sb.append(localStatus(s, local));
        sb.append('\n');
      }
    }
    return sb.toString();
  }
  
  public static String tripleToString(final FieldTriple t) {
    return nodeToString(t.first()) + "." +
      VariableDeclarator.getId(t.second()) + " = " +
      nodeToString(t.third());
  }
  
  public static String nodeToString(final ImmutableHashOrderSet<Object> node) {
    final StringBuilder sb = new StringBuilder();
    sb.append('{');
    boolean first = true;
    if (node.isInfinite()) {
      sb.append("...");
    } else {
      for (final Object local : node) {
        if (first) first = false; else sb.append(',');
        sb.append(localToString(local));        
      }
    }
    sb.append('}');
    return sb.toString();
  }
  
  public static String localToString(final Object local) {
    if (local instanceof IRNode) {
      final IRNode n = (IRNode) local;
      final Operator op = JJNode.tree.getOperator(n);
      if (VariableDeclarator.prototype.includes(op)) {
        return VariableDeclarator.getId(n);
      } else if (ParameterDeclaration.prototype.includes(op)) {
        return ParameterDeclaration.getId(n);
      } else if (ReceiverDeclaration.prototype.includes(op)) {
        return "this";
      } else if (QualifiedReceiverDeclaration.prototype.includes(op)) {
//        final String ss = DebugUnparser.toString(JavaPromise.getPromisedFor(n));
        return JavaNames.getQualifiedTypeName(QualifiedReceiverDeclaration.getBase(n)) + ".this";
      } else if (ReturnValueDeclaration.prototype.includes(op)) {
        return "return";
      }
    }
    return local.toString();
  }
  
  
  // ==================================================================
  // === Set operations 
  //
  // The UnionLattice declares its members to have type ImmutableSet<X>.
  // This is a problem because we cannot add/remove elements to/from
  // a set using that interface.  We need to downcast the member to be
  // ImmutableHashOrderSet to access those methods.  We encapsulate these
  // type unsafe operations in three static methods so we don't poison
  // the rest of the code with SuppressWarnings annotations.
  // ==================================================================

  /**
   * Puts the type unsafe casting of a ImmutableSet into a ImmutableHashOrderSet
   * in one place. (Can only invoke <code>addElement()</code> on an
   * ImmutableHashOrderSet.)
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <X> ImmutableHashOrderSet<X> addElement(
      final ImmutableSet<X> s, final X o) {
    return ((ImmutableHashOrderSet) s).addElement(o);
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static <X> ImmutableHashOrderSet<X> addElements(
      final ImmutableSet<X> s, final Iterator<X> i) {
    return ((ImmutableHashOrderSet) s).addElements(i);
  }

  
  
  // ==================================================================
  // === Side-effects
  // ==================================================================
  
  private boolean shouldRecordResult() {
    return produceSideEffects && !suppressDrops;
  }
  
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
  
  // ------------------------------------------------------------------
  // -- Compromised unique fields
  // ------------------------------------------------------------------

  /**
   * Unique value is being compromised.  If the value is referenced by
   * a field, we record that the field is being compromised.
   */
  private void recordCompromisingOfUnique(
      final IRNode srcOp, final Integer topOfStack, final State localStatus,
      final ImmutableSet<FieldTriple> fieldStore) {
    recordLossOfUniqueness(srcOp, topOfStack, localStatus, fieldStore, compromisedAt);
  }

  /**
   * Unique value is being made undefined.  If the value is referenced by
   * a field, we record that the field is being undefined.
   */
  private void recordUndefiningOfUnique(
      final IRNode srcOp, final Integer topOfStack, final State localStatus,
      final Store s) {
    if (shouldRecordResult()) {
      recordLossOfUniqueness(
          srcOp, topOfStack, localStatus, s.getFieldStore(), undefinedAt);
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

  private void recordLossOfUniqueness(final IRNode srcOp, final Integer topOfStack,
      final State localStatus, final ImmutableSet<FieldTriple> fieldStore,
      final Map<IRNode, Set<IRNode>> howLostMap) {
    if (shouldRecordResult()) {
      if (localStatus == State.UNIQUE) {
        for (final FieldTriple ft : fieldStore) {
          if (ft.third().contains(topOfStack)) {
            addToMappedSet(howLostMap, ft.second(), srcOp);
          }
        }
      }
    }
  }
  
  private void recordLoadOfCompromisedField(final IRNode srcOp, final IRNode fieldDecl) {
    if (shouldRecordResult()) {
      // Record look up of compromised field
      addToMappedSet(
          abruptDrops ? loadedCompromisedFieldsAbrupt : loadedCompromisedFields,
          fieldDecl, srcOp);
    }
  }
  
  private void recordIndirectLoadOfCompromisedField(final IRNode srcOp, final IRNode fieldDecl) {
    if (shouldRecordResult()) {
      // Record look up of compromised field
      addToMappedSet(
          abruptDrops ? indirectlyLoadedCompromisedFieldsAbrupt :
            indirectlyLoadedCompromisedFields,
          fieldDecl, srcOp);
    }
  }
  
  private void recordLossOfCompromisedField(final IRNode srcOp, final IRNode fieldDecl) {
    if (shouldRecordResult()) {
      addToMappedSet(abruptDrops ? lostFieldsAbrupt : lostFields, fieldDecl, srcOp);
    }        
  }

  // ------------------------------------------------------------------
  // -- Alias burying
  // ------------------------------------------------------------------
  
  private void recordBuriedRead(final IRNode srcOp, final Object local) {
    if (shouldRecordResult()) {
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
  
  private void recordBuryingFieldRead(final IRNode fieldDecl,
      final Set<Object> affectedVars, final IRNode srcOp) {
    if (shouldRecordResult()) {
      recordBuryingLoad(fieldDecl, affectedVars, srcOp);
      for (final Object v : affectedVars) {
        if (v instanceof Integer) {
          addToMappedSet(stackBuriedAt, (Integer) v, srcOp);

        }
      }
    }
  }
  
  private void recordBuryingMethodEffects(final Set<IRNode> loadedFields,
      final Set<Object> affectedVars, final IRNode srcOp,
      final RegionEffectsPromiseDrop fxDrop) {
    if (shouldRecordResult()) {
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
  
  // ------------------------------------------------------------------
  // -- Good Values
  // ------------------------------------------------------------------
  
  public UniquenessControlFlowDrop getCFDrop() {
    return controlFlowDrop;
  }
  
  private void recordGoodUnique(final IRNode srcOp,
      final PromiseDrop<? extends IAASTRootNode> uDrop, final int msg,
      final InfoAdder infoAdder) {
    if (shouldRecordResult()) {
      goodUnique.add(new GoodResult(uDrop, srcOp, abruptDrops, msg, infoAdder));
    }
  }
  
  // ------------------------------------------------------------------
  // -- Bad Values
  // ------------------------------------------------------------------

  private void recordBadSet(final Object local, final IRNode op) {
    if (shouldRecordResult()) {
      addToMappedSet(badSets, local, op);
    }
  }
  
  // XXX: Method is poorly named
  private void recordBadUnique(final IRNode srcOp,
      final PromiseDrop<? extends IAASTRootNode> uDrop, final int msg,
      final InfoAdder infoAdder) {
    xNotY.add(new XNotY(uDrop, srcOp, abruptDrops, msg, infoAdder));
  }
  
  private void recordBadBorrowed(
      final IRNode srcOp, final Integer topOfStack,  
      final ImmutableSet<ImmutableHashOrderSet<Object>> objects,
      final int msgNormal, final int msgReturn,
      final InfoAdder infoAdder) {
    final Set<Object> referringVars = new HashSet<Object>();
    for (final ImmutableHashOrderSet<Object> obj : objects) {
      if (obj.contains(topOfStack)) {
        referringVars.addAll(obj);
      }
    }
    /* Find all the @Borrowed ParameterDeclarations, including 
     * @Unique("return") in the case of constructors.
     */
    for (final Object v : referringVars) {
      if (v instanceof IRNode) {
        final IRNode nv = (IRNode) v;
        final Operator op = JJNode.tree.getOperator(nv);
        PromiseDrop<? extends IAASTRootNode> promiseDrop = null;
        if (ParameterDeclaration.prototype.includes(op) ||
            ReceiverDeclaration.prototype.includes(op) ||
            QualifiedReceiverDeclaration.prototype.includes(op)) {
          promiseDrop = UniquenessRules.getBorrowed(nv);
          if (promiseDrop == null && ReceiverDeclaration.prototype.includes(op)) {
            final IRNode decl = JavaPromise.getPromisedFor(nv);
            if (ConstructorDeclaration.prototype.includes(decl)) {
              // It's from a constructor, look for unique on the return node
              final IRNode returnNode = JavaPromise.getReturnNode(decl);
              promiseDrop = UniquenessRules.getUnique(returnNode);
            }
          }
          
          if (promiseDrop != null) {
            borrowedHasResults.add(promiseDrop);
            xNotY.add(new XNotY(promiseDrop, srcOp, abruptDrops,
                MethodBody.prototype.includes(srcOp) ? msgReturn : msgNormal,
                infoAdder));
          }
        }
      }
    }
  }

  private void recordBorrowedPassedToBorrowed(final IRNode srcOp,
      final IRNode calledMethod, final IRNode methodCall,
      final PromiseDrop<? extends IAASTRootNode> passedToBorrowedDrop,
      final BindingContextAnalysis.Query bcaQuery) {
    if (srcOp == null) {
      /* Special case: the called method is a a ConstructorCall---
       * this(...) or super(...)---and top of stack is the receiver.
       * The actual parameter in this case is the receiver of the method
       * being analyzed.
       */
      createBorrowedPassedToBorrowedResult(
          UniquenessRules.getBorrowedReceiver(controlFlowDrop.getNode()),
          methodCall, "this", calledMethod, passedToBorrowedDrop);
    } else {
      for (final IRNode x : bcaQuery.getResultFor(srcOp)) {
        if (ReceiverDeclaration.prototype.includes(x)) {
          createBorrowedPassedToBorrowedResult(
              UniquenessRules.getBorrowedReceiver(
                  JavaPromise.getPromisedFor(x)), srcOp, "this",
                  calledMethod, passedToBorrowedDrop);
        } else if (ParameterDeclaration.prototype.includes(x)) {
          createBorrowedPassedToBorrowedResult(
              UniquenessRules.getBorrowed(x), srcOp,
              ParameterDeclaration.getId(x), calledMethod, passedToBorrowedDrop);
        }
      }
    }
  }
  
  private void createBorrowedPassedToBorrowedResult(
      final PromiseDrop<? extends IAASTRootNode> actualBorrowedDrop,
      final IRNode srcOp, final String actualParameterName,
      final IRNode calledMethod,
      final PromiseDrop<? extends IAASTRootNode> passedToBorrowedDrop) {
    if (actualBorrowedDrop != null) {
      borrowedHasResults.add(actualBorrowedDrop);
      final ResultDrop result = 
        createResultDrop(analysis, abruptDrops, actualBorrowedDrop, srcOp, true,
            Messages.BORROWED_PASSED_TO_BORROWED,
            actualParameterName,
            JavaNames.genMethodConstructorName(calledMethod));
      result.addTrusted(passedToBorrowedDrop);
    }
  }

  private void recordSharedNotUnique(
      final IRNode srcOp, final PromiseDrop<? extends IAASTRootNode> uDrop,
      final MessageChooser msg) {
    if (shouldRecordResult()) {
      recordBadUnique(srcOp, uDrop, 
          msg.chooseMsg(
              Messages.SHARED_NOT_UNIQUE_ACTUAL, Messages.SHARED_NOT_UNIQUE_RETURN,
              Messages.SHARED_NOT_UNIQUE_ASSIGN), null);
    }
  }
  
  private void recordBorrowedNotUnique(
      final IRNode srcOp, final PromiseDrop<? extends IAASTRootNode> uDrop,
      final MessageChooser msg, final Integer topOfStack,  
      final ImmutableSet<ImmutableHashOrderSet<Object>> objects) {

    /* Two problems here: (1) A unique reference is expected, but a borrowed
     * one is provided; and (2) A borrowed value is used in a unique context.
     */
    if (shouldRecordResult()) {
      recordBadUnique(srcOp, uDrop,
          msg.chooseMsg(
              Messages.BORROWED_NOT_UNIQUE_ACTUAL, Messages.BORROWED_NOT_UNIQUE_RETURN,
              Messages.BORROWED_NOT_UNIQUE_ASSIGN), null);
      recordBadBorrowed(srcOp, topOfStack, objects,
          Messages.BORROWED_AS_UNIQUE, Messages.BORROWED_AS_UNIQUE_RETURN,
          new InfoAdder() {
            @Override
            public void addSupportingInformation(
                final AbstractWholeIRAnalysis<UniquenessAnalysis,?> analysis,
                final IBinder binder, final ResultDrop resultDrop) {
              resultDrop.addTrusted(uDrop);
            }
          });
    }    
  }

  private void recordUndefinedNotX(
      final IRNode srcOp, final PromiseDrop<? extends IAASTRootNode> uDrop,
      final Integer topOfStack) {
    if (shouldRecordResult()) {
      recordBadUnique(srcOp, uDrop, Messages.ACTUAL_IS_UNDEFINED,
          new InfoAdder() {
            @Override
            public void addSupportingInformation(
                final AbstractWholeIRAnalysis<UniquenessAnalysis, ?> analysis,
                final IBinder binder, final ResultDrop resultDrop) {
              final Set<IRNode> undefinedAt = stackUndefinedAt.get(topOfStack);
              if (undefinedAt != null) {
                for (final IRNode where : undefinedAt) {
                  if (where != srcOp) {
                    resultDrop.addInformationHint(
                        where, Messages.BY_UNIQUE_PARAMETER,
                        DebugUnparser.toString(where));
                  }
                }
              }
              final Set<IRNode> buriedAt = stackBuriedAt.get(topOfStack);
              if (buriedAt != null) {
                for (final IRNode where : buriedAt) {
                  if (where != srcOp) {
                    resultDrop.addInformationHint(
                        where, Messages.BY_UNIQUE_LOAD,
                        FieldRef.getId(where));
                  }
                }
              }
              final Set<Triple<Set<IRNode>, IRNode, RegionEffectsPromiseDrop>> affectedAt =
                stackIndirectlyBuriedAt.get(topOfStack);
              if (affectedAt != null) {
                for (final Triple<Set<IRNode>, IRNode, RegionEffectsPromiseDrop> triple : affectedAt) {
                  if (triple.second() != srcOp) {
                    for (final IRNode field : triple.first()) {
                      if (triple.third() != null) {
                        resultDrop.addTrusted(triple.third());
                      }
                      resultDrop.addInformationHint(
                          triple.second(), Messages.BY_SIDE_EFFECT,
                          VariableDeclarator.getId(field),
                          DebugUnparser.toString(triple.second()));
                    }
                  }
                }
              }
            }
          });
    }
  }
  
  private void recordBorrowedNotShared(
      final IRNode srcOp, final Integer topOfStack,  
      final ImmutableSet<ImmutableHashOrderSet<Object>> objects) {
    if (shouldRecordResult()) {
      recordBadBorrowed(srcOp, topOfStack, objects,
          Messages.BORROWED_AS_SHARED, Messages.BORROWED_AS_SHARED_RETURN,
          null);
    }
  }

  // ------------------------------------------------------------------
  // -- Make result drops
  // ------------------------------------------------------------------
  
  public void cancelResults() {
    for (final ResultDrop drop : drops) {
      drop.invalidate();
    }
  }
  
  private ResultDrop createResultDrop(
      final AbstractWholeIRAnalysis<UniquenessAnalysis,?> analysis,
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
      final AbstractWholeIRAnalysis<UniquenessAnalysis,?> analysis,
      final boolean abruptDrops,
      final PromiseDrop<? extends IAASTRootNode> promiseDrop,
      final IRNode node, final boolean isConsistent, 
      final int msg, final Object... args) {
    return createResultDrop(analysis, abruptDrops, true,
        promiseDrop, node, isConsistent, msg, args);
  }
  
  private void crossReferenceKilledFields(
      final int msg, final boolean isAbrupt,
      final Map<IRNode, Set<IRNode>> compromisedFields) {
    for (final Map.Entry<IRNode, Set<IRNode>> load : compromisedFields.entrySet()) {
      final IRNode fieldDecl = load.getKey();
      final Set<IRNode> compromises = compromisedAt.get(fieldDecl);
      final Set<IRNode> undefines = undefinedAt.get(fieldDecl);
      final PromiseDrop<? extends IAASTRootNode> uniquePromise = UniquenessUtils.getUnique(load.getKey()).getDrop();
      
      for (final IRNode readAt : load.getValue()) {
        final ResultDrop r = createResultDrop(
            analysis, isAbrupt, uniquePromise, readAt, false, msg);
        if (compromises != null) {
          for (final IRNode compromisedAt : compromises) {
            r.addInformationHint(compromisedAt, Messages.COMPROMISED_BY,
                DebugUnparser.toString(compromisedAt));
          }
        }
        if (undefines != null) {
          for (final IRNode undefinedAt : undefines) {
            r.addInformationHint(undefinedAt, Messages.UNDEFINED_BY,
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

    // Link reads of buried references to burying field loads
    for (final BuriedRead read : buriedReads) {
      final Map<IRNode, Set<IRNode>> loads = buryingLoads.get(read.var);
      if (loads != null) {
        for (final Map.Entry<IRNode, Set<IRNode>> e : loads.entrySet()) {
          final ResultDrop r = createResultDrop(analysis, read.isAbrupt,
              UniquenessUtils.getUnique(e.getKey()).getDrop(), read.srcOp,              
              false, Messages.READ_OF_BURIED);
          for (final IRNode buriedAt : e.getValue()) {
            r.addInformationHint(buriedAt, Messages.BURIED_BY, 
                DebugUnparser.toString(buriedAt));
          }
        }
      }
      
      // Could be undefined because we were assigned an undefined value
      final Set<IRNode> z = badSets.get(read.var);
      if (z != null) {
        final ResultDrop r = createResultDrop(analysis, read.isAbrupt,
            controlFlowDrop, read.srcOp, false, Messages.READ_OF_BURIED);
        for (final IRNode setOp : z) {
          r.addInformationHint(setOp, Messages.ASSIGNED_UNDEFINED_BY,
              VariableUseExpression.prototype.includes(read.srcOp) ?
                  VariableUseExpression.getId(read.srcOp) : "*UNKNOWN*",
              DebugUnparser.toString(setOp));
        }
      }
    }
    
    for (final XNotY err: xNotY) {
      err.createDrop(analysis, binder);
    }
    
    for (final GoodResult pos : goodUnique) {
      pos.createDrop(analysis, binder);
    }
    
    
    /* If we haven't already added results to the control flow drop, then 
     * we add a single "invariants respected" positive result.
     */
    final boolean addBorrowedSatisfiedToControlFlow;
    if (!hasControlFlowResults) {
      createResultDrop(analysis, false, controlFlowDrop,
          controlFlowDrop.getNode(), true, Messages.INVARIANTS_RESPECTED);
      /* Don't add any BORROWED_SATISFIED drops in this case, because the
       * message we just added covers this situation already.
       */
      addBorrowedSatisfiedToControlFlow = false;
    } else {
      addBorrowedSatisfiedToControlFlow = true;
    }
    
    final IRNode flowUnit = controlFlowDrop.getNode();
    if (!ClassInitDeclaration.prototype.includes(flowUnit)) {
      /* Test each actual parameter annotated with @Borrowed.  Test
       * the receiver if it is annotated with @Borrowed.  If the flowUnit is a 
       * constructor, test the return value for uniqueness, and then test that.
       * 
       * Any promise for which borrowedHasResult.get(x) == null gets the 
       * BORROWED_SATISFIED message.
       */
      final IRNode formals = SomeFunctionDeclaration.getParams(flowUnit);
      for (final IRNode p : Parameters.getFormalIterator(formals)) {
        final BorrowedPromiseDrop pd = UniquenessRules.getBorrowed(p);
        if (pd != null && !borrowedHasResults.contains(pd)) {
          createResultDrop(analysis, false, addBorrowedSatisfiedToControlFlow, pd, p, true, Messages.BORROWED_SATISFIED);
        }
      }
      
      final IRNode rcvr = JavaPromise.getReceiverNodeOrNull(flowUnit);
      if (rcvr != null) {
        final BorrowedPromiseDrop pd = UniquenessRules.getBorrowed(rcvr);
        if (pd != null && !borrowedHasResults.contains(pd)) {
          createResultDrop(analysis, false, addBorrowedSatisfiedToControlFlow, pd, rcvr, true, Messages.BORROWED_SATISFIED);
        }
      }      
      
      final IRNode ret = JavaPromise.getReturnNodeOrNull(flowUnit);
      if (ret != null) {
        final UniquePromiseDrop pd = UniquenessRules.getUnique(ret);
        if (ConstructorDeclaration.prototype.includes(flowUnit)) {
          // CONSTRUCTOR: Unique(return) == Borrowed(this)
          if (pd != null && !borrowedHasResults.contains(pd)) {
            createResultDrop(analysis, false, addBorrowedSatisfiedToControlFlow, pd, ret, true, Messages.BORROWED_SATISFIED);
          }
        }
      }
    }
  }




  private final static class BuriedRead {
    public final Object var;
    public final IRNode srcOp;
    public final boolean isAbrupt;
    
    public BuriedRead(final Object var, final IRNode n, final boolean a) {
      this.var = var;
      this.srcOp = n;
      this.isAbrupt = a;
    }
  }
  
  private abstract class AbstractResult {
    private final PromiseDrop<? extends IAASTRootNode> promiseDrop;
    private final IRNode srcOp;
    private final boolean isAbrupt;
    private final int msg;
    private final InfoAdder adder;
    
    // Every AbstractResult object will be put into a HashSet, so we know the hashCode
    // will be needed at least once.
    private final int hashCode;
    
    public AbstractResult(final PromiseDrop<? extends IAASTRootNode> pd,
        final IRNode srcOp, final boolean isAbrupt,
        final int msg, final InfoAdder adder) {
      this.promiseDrop = pd;
      this.srcOp = srcOp;
      this.isAbrupt = isAbrupt;
      this.msg = msg;
      this.adder = adder;
      
      int hc = 17;
      hc = 31 * hc + (pd == null ? 0 : pd.hashCode());
      hc = 31 * hc + (srcOp == null ? 0 : srcOp.hashCode());
      hc = 31 * hc + (isAbrupt ? 1 : 0);
      hc = 31 * hc + msg;
      this.hashCode = hc;
    }
    
    @Override
    public final boolean equals(final Object other) {
      if (other instanceof AbstractResult) {
        /* The behavior of the InfoAdder is determined by msg, so it is sufficent
         * to check for equality of the msg..
         */
        final AbstractResult o2 = (AbstractResult) other;
        return promiseDrop.equals(o2.promiseDrop)
            && srcOp.equals(o2.srcOp)
            && isAbrupt == o2.isAbrupt
            && msg == o2.msg;
      }
      return false;
    }
    
    @Override
    public final int hashCode() {
      return hashCode;
    }
    
    public final ResultDrop createDrop(
        final AbstractWholeIRAnalysis<UniquenessAnalysis,?> analysis,
        final IBinder binder) {
      final ResultDrop result = createResultDrop(
          analysis, isAbrupt, promiseDrop, srcOp, isGood(), msg);
      if (adder != null) {
        adder.addSupportingInformation(analysis, binder, result);
      }
      return result;          
    }
    
    protected abstract boolean isGood();
  }
  
  private final class XNotY extends AbstractResult {
    public XNotY(final PromiseDrop<? extends IAASTRootNode> pd,
        final IRNode srcOp, final boolean isAbrupt,
        final int msg, final InfoAdder adder) {
      super(pd, srcOp, isAbrupt, msg, adder);
    }

    @Override
    protected boolean isGood() {
      return false;
    }
  }
  
  private final class GoodResult extends AbstractResult {
    public GoodResult(final PromiseDrop<? extends IAASTRootNode> pd,
        final IRNode srcOp, final boolean isAbrupt,
        final int msg, final InfoAdder adder) {
      super(pd, srcOp, isAbrupt, msg, adder);
    }

    @Override
    protected boolean isGood() {
      return true;
    }
  }


  private static interface InfoAdder {
    public void addSupportingInformation(
        AbstractWholeIRAnalysis<UniquenessAnalysis,?> analysis,
        IBinder binder, ResultDrop resultDrop);
  }
  
  public static enum MessageChooser {
    ACTUAL {
      @Override
      public int chooseMsg(final int actual, final int ret, final int assign) {
        return actual;
      }      
    },
    
    RETURN {
      @Override
      public int chooseMsg(final int actual, final int ret, final int assign) {
        return ret;
      }
    },
    
    ASSIGN {
      @Override
      public int chooseMsg(final int actual, final int ret, final int assign) {
        return assign;
      }
    };
    
    public abstract int chooseMsg(int actual, int ret, int assign);
  }
}