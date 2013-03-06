package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.store;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.Messages;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state.BuriedMessage;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state.ISideEffects;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.state.NullSideEffects;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.method.constraints.RegionEffectsPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.BorrowedPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.IUniquePromise;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeRef;
import edu.cmu.cs.fluid.java.operator.VariableDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.FilterIterator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.IteratorUtil;
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
  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis.unique");
    
  private static final ImmutableHashOrderSet<Object> EMPTY =
    ImmutableHashOrderSet.<Object>emptySet();

  static final Object VALUE = "value";
  static final Object NONVALUE = "nonvalue";
  
  private static final ImmutableHashOrderSet<Object> PSEUDOS =
    EMPTY.
      addElement(State.UNDEFINED).
      addElement(State.BORROWED).
      addElement(State.READONLY).
      addElement(State.IMMUTABLE).
      addElement(State.SHARED);

  private final IRNode[] locals;

  private final IBinder binder;
  private final IMayAlias mayAlias;
  private final List<Effect> effects;
  
  
  
  /** The reference to the repository for side-effects. */
  private ISideEffects sideEffects = NullSideEffects.prototype;

  
  
  // ==================================================================
  // === Constructor 
  // ==================================================================
  
  public StoreLattice(
      final IRNode[] locals, IBinder b, IMayAlias ma, List<Effect> fx) {
    super(new FlatLattice2<Integer>(),
        new UnionLattice<ImmutableHashOrderSet<Object>>(),
        new UnionLattice<FieldTriple>());
    this.locals = locals;
    this.binder = b;
    this.mayAlias = ma;
    this.effects = fx;    
  }

  
  
  public int getNumLocals() {
    return locals.length;
  }

  
  
  public void setSideEffects(final ISideEffects se) {
    sideEffects = se;
  }
  
  public void setSuppressDrops(final boolean value) {
    sideEffects.setSuppressDrops(value);
  }
  
  public void setAbruptResults(final boolean value) {
    sideEffects.setAbruptResults(value);
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
  
  public Store errorStore(final String msg) {
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
  
  @Override
  public Store widen(final Store s1, final Store s2) {
    Store m = super.widen(s1, s2);
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

  private Store push(final Store s) {
    if (!s.isValid()) return s;
    final int n = s.getStackSize();
    return setStackSize(s, n+1);
  }
  
  private Store pop(Store s, final IRNode srcOp) {
    if (!s.isValid()) return s;
    final Integer topOfStack = getStackTop(s);
    final int n = topOfStack.intValue();
    if (n == 0) {
      return errorStore("stack underflow");
    }
    s = apply(s, srcOp, new Remove(EMPTY.addElement(topOfStack)));
    if (!s.isValid()) return s;
    return setStackSize(s, Integer.valueOf(n-1));
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
        status = State.lattice.join(status,nstatus);
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
	  State status = State.UNIQUE;
	  for (Object thing : node) {
		  if (thing instanceof State) {
			  status = State.lattice.join(status,(State)thing);
		  }
	  }
	  return status;
  }

  /**
   * Return whether this variable declaration or expression is of an intrinsically immutable type.
   * For example: String, Integer, any enumerated type or some class annotated Immutable
   * (not implementation only).
   * @param node receiver/parameter/field (or any expression)
   * @return true if a declaration or expression of value type
   */
   public boolean isValueNode(final IRNode node) {   
	  if (binder == null) return false;
	  IJavaType type = binder.getJavaType(node);
	  if (type instanceof IJavaSourceRefType) {
		  final IJavaSourceRefType srcRefType = (IJavaSourceRefType) type;
		  final IRNode typeDeclarationNode = srcRefType.getDeclaration();
		  return LockRules.isImmutableType(typeDeclarationNode);
	  }
	  return false;
  }
  
  /**
   * Get the declared (by annotation) status of the given node.
   * This method is used for Value Flow Analysis
   * @param node parameter, receiver or field
   * @return annotation converted into state
   */
  public State declStatus(final IRNode node) {
    final IUniquePromise uDrop = UniquenessUtils.getUnique(node);
    if (uDrop != null) {
      if (uDrop.allowRead()) return State.UNIQUEWRITE;
      return State.UNIQUE;
    }
    // TODO: BorrowedReadOnly
    if (UniquenessUtils.isFieldBorrowed(node)) {
      return State.BORROWED;
    }
    if (UniquenessRules.isReadOnly(node)) {
      return State.READONLY;
    }
    if (LockRules.isImmutableRef(node)) {
      return State.IMMUTABLE;
    }
    if (isValueNode(node)) {
      return State.IMMUTABLE;
    }
    return State.SHARED;
  }

  /**
   * Return the status for a receiver node (given its mdecl/cdecl).
   * Usually, we just look up the status, but for constructors,
   * we need to handle the case that the "return" value has been annotated instead.
   * @param decl method or constructor decl
   * @param recDecl receiver decl node
   * @return annotated state of the receiver
   */
  public State receiverStatus(final IRNode decl, final IRNode recDecl) {
	  final boolean isConstructor = ConstructorDeclaration.prototype.includes(decl);
	  final IRNode retDecl = JavaPromise.getReturnNodeOrNull(decl);

	  State required = declStatus(recDecl);
	  if (isConstructor && required == State.SHARED) {
		  if (UniquenessRules.isUnique(retDecl)) {
			  if (UniquenessRules.getUnique(retDecl).allowRead())
				  required = State.READONLY;
			  else
				  required = State.BORROWED;
		  } else {
			  if (LockRules.isImmutableRef(retDecl)) required = State.IMMUTABLE;
			  else if (UniquenessRules.isReadOnly(retDecl)) required = State.UNIQUEWRITE;
		  }
	  }
	  return required;
  }

  /**
   * Return current top stack location.
   * 
   * @precondition isValid()
   */
  public static Integer getStackTop(final Store s) {
    return s.getStackSize();
  }

  /**
   * Return current stack location next to top.
   * 
   * @precondition isValid()
   */
  public static Integer getUnderTop(final Store s) {
    return Integer.valueOf(getStackTop(s).intValue() - 1);
  }

  /**
   * Find the (first) object that includes the given pseudo-variable (or string).
   * It is an error if there is no object with the given variable
   * @param s store to search in
   * @param pseudo pseudo-varable (or string) to search for
   * @return object from store including this pseudo-variable (never null).
   */
  private ImmutableHashOrderSet<Object> getPseudoObject(Store s, Object pseudo) {
	  for (ImmutableHashOrderSet<Object> obj : s.getObjects()) {
		  if (obj.contains(pseudo)) return obj;
	  }
	  throw new FluidError("no " + pseudo + " object?");
  }

  private ImmutableHashOrderSet<Object> getAliases(Store s, Object var) {
	  Set<Object> aliasSet = new HashSet<Object>();
	  for (ImmutableHashOrderSet<Object> obj : s.getObjects()) {
		  if (obj.contains(var)) {
			  for (Object v : obj) {
				  if (!(v instanceof State)) aliasSet.add(v);
			  }
		  }
	  }
	  return new ImmutableHashOrderSet<Object>(aliasSet);
  }

  

  // ==================================================================
  // === Stack Machine Operations 
  // ==================================================================

  public Store opStart(final IRNode srcOp) {
    Store temp;
    
    /*
     * Start with nothing on stack, and objects {}, 
     * {undefined}, {borrowed}, {readonly}, {shared}, {immutable,VALUE}, {immutable,NONVALUE}
     */
    ImmutableHashOrderSet<ImmutableHashOrderSet<Object>> objects = 
      ImmutableHashOrderSet.<ImmutableHashOrderSet<Object>>emptySet();
    objects = objects.
      addElement( EMPTY ).
      addElement( EMPTY.addElement(State.UNDEFINED) ).
      addElement( EMPTY.addElement(State.BORROWED) ).
      addElement( EMPTY.addElement(State.READONLY) ).
      addElement( EMPTY.addElement(State.SHARED) ).
      addElement( EMPTY.addElement(State.IMMUTABLE).addElement(VALUE) ).
      addElement( EMPTY.addElement(State.IMMUTABLE).addElement(NONVALUE) );
    temp = newTriple(FlatLattice2.asMember(0),objects,ImmutableHashOrderSet.<FieldTriple>emptySet());


    for (final IRNode local : locals) {
      final Operator op = JJNode.tree.getOperator(local);
      
      if (ReceiverDeclaration.prototype.includes(op)) {
          final IRNode decl = JavaPromise.getPromisedFor(local);
    	  temp = opGenerate(temp, srcOp, receiverStatus(decl,local),local);
    	  temp = opSet(temp, srcOp, local);
      } else if (QualifiedReceiverDeclaration.prototype.includes(op) ||
    		  ParameterDeclaration.prototype.includes(op)) {
    	  IRNode parent = JJNode.tree.getParent(local);
    	  if (parent == null || !CatchClause.prototype.includes(JJNode.tree.getOperator(parent))) {
    		  temp = opGenerate(temp, srcOp, declStatus(local), local);
    		  temp = opSet(temp, srcOp, local);
    	  }
      }
    }
    return temp;
  }

  /**
   * Leave scope of method. Remove all local bindings.
   */
  public Store opStop(final Store s, final IRNode srcOp) {
    return opClear(s, srcOp, (Object[])locals);
  }

  /**
   * Fetch the value of a local onto stack.
   **/
  public Store opGet(final Store s, final IRNode srcOp, final Object local) {
    return opGet(s, srcOp, local, BuriedMessage.VAR);
  }
  
  public Store opGet(final Store s, final IRNode srcOp, final Object local,
      final BuriedMessage msg) {
    if (!s.isValid()) return s;
    if (localStatus(s, local) != State.UNDEFINED) {
      Store temp = push(s);
      return apply(temp, srcOp, new Add(local, EMPTY.addElement(getStackTop(temp))));
    } else {
      sideEffects.recordBuriedRead(srcOp, local, msg);
      // keep going: return null value 
      return opNull(s);
    }
  }

  /**
   * Remove bindings of all variables listed.
   * @param s store before
   * @param vars variables to assign to null
   * @return store after
   */
  public Store opClear(final Store s, final IRNode srcOp, final Object... vars) {
	  if (!s.isValid()) return s;
	  return apply(s, srcOp, new Remove(new ImmutableHashOrderSet<Object>(vars)));
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
    if (localStatus(s, n) == State.UNDEFINED) {
      sideEffects.recordBadSet(local, srcOp);
    }
    
    return pop(
        apply(
            apply(s, srcOp, new Remove(lset)), srcOp,
            new Add(n, lset)), srcOp);
  }

  public Store opSetAliasAware(final Store s, final IRNode srcOp, final IRNode local) {
    if (!s.isValid()) return s;
    final ImmutableHashOrderSet<Object> lset = EMPTY.addElement(local);
    final Integer n = getStackTop(s);
    if (localStatus(s, n) == State.UNDEFINED) {
      sideEffects.recordBadSet(local, srcOp);
    }
    
    return pop(
        apply(
            apply(s, srcOp, new Remove(lset)), srcOp,
            new Add(n, lset, local, mayAlias)), srcOp);
  }
  
  public static final IRNode fromField = null; // new EnumeratedIRNode<FieldKind>(FieldKind.FROM_FIELD);

  /**
   * Add a field edge from  {@link from} to {@link to}.
   * (We add the edge between every node that "from" aliases to every node
   * that "to" aliases.)
   * @param s store before
   * @param from variable source of edge
   * @param field field IRnode
   * @param to variable destination of edge
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
			newFields.add(new FieldTriple(source,field,dest));
		  }
	  }
	  ImmutableHashOrderSet<FieldTriple> newFieldSet = new ImmutableHashOrderSet<FieldTriple>(newFields);
	  s = setFieldStore(s,s.getFieldStore().union(newFieldSet));
	  return s;
  }

  /**
   * Remove any "from" connections to "to" that have been declared OK:
   * all objects are unique or have an allowReturn object in them.
   * @param s store before
   * @param to return/this variable that is ready to be done
   * @return store after
   */
  public Store opRemoveFrom(Store s, Object to) {
	  if (!s.isValid()) return s;
	  List<FieldTriple> newFields = new ArrayList<FieldTriple>(s.getFieldStore());
	  for (Iterator<FieldTriple> it = newFields.iterator(); it.hasNext();) {
		  FieldTriple tr = it.next();
		  // if (tr.second() != fromField) continue;
		  if (tr.third().contains(to)) {
			  switch (nodeStatus(tr.first())) {
			  case UNIQUE:
				  if (LOG.isLoggable(Level.FINE)) {
					  LOG.fine("Removing unique: " + tripleToString(tr));
				  }
				  it.remove();
				  break;
			  default:
				  boolean allowed = false;
				  for (Object v : tr.first()) {
					  if (v instanceof IRNode) {
						  BorrowedPromiseDrop borrowed = UniquenessRules.getBorrowed((IRNode)v);
						  if (borrowed != null && borrowed.allowReturn()) allowed = true;
					  }
				  }
				  if (allowed) {
					  if (LOG.isLoggable(Level.FINE)) {
						  LOG.fine("Removing allowed: " + tripleToString(tr));
					  }
					  it.remove();
				  }
			  }
		  }
	  }
	  if (newFields.size() != s.getFieldStore().size()) {
		  return setFieldStore(s,new ImmutableHashOrderSet<FieldTriple>(newFields));
	  }
	  return s;
  }

  private Store undefineFromNodes(
      final Store s, final IRNode srcOp, final Integer n, final int msg) {
      final ImmutableSet<ImmutableHashOrderSet<Object>> objects = s.getObjects();
      ImmutableHashOrderSet<Object> affected = EMPTY;
      final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
      if (fieldStore.isInfinite()) return s;
      for (final FieldTriple t : fieldStore) {
        final ImmutableHashOrderSet<Object> object = t.first();
        if (object.contains(n) && objects.contains(object) && t.second() == fromField) {
          affected = affected.union(t.third());
        }
      }
      if (!affected.isEmpty()) {
        sideEffects.recordUndefinedFrom(srcOp, affected, msg);
        return apply(
            apply(s, srcOp, new Remove(affected)), srcOp,
            new Add(State.UNDEFINED, affected));
      } else {
        return s;
      }
  }
  
  /**
   * Load a field from memory
   * 
   * @precondition isValid();
   */
  public Store opLoad(Store s, final IRNode srcOp, final IRNode fieldDecl) {
	  if (!s.isValid()) return s;
	  s = undefineFromNodes(s, srcOp, getStackTop(s), Messages.MADE_UNDEFINED_BY_FROM_READ);
	  if (!s.isValid()) return s;
	  // we don't allow reading of 'from' fields except when this object is 
	  // independent, because otherwise, the aliasing rules are too tricky to
	  // figure out.
	  final PromiseDrop<? extends IAASTRootNode> borrowedPromise =
	      UniquenessUtils.getFieldBorrowed(fieldDecl);
    if (borrowedPromise != null) {
		  final Integer n = getStackTop(s);
		  for (FieldTriple ft : s.getFieldStore()) {
			  if (ft.third().contains(n)) {
			    sideEffects.recordReadOfBorrowedField(srcOp, borrowedPromise);
			    // Push null on the stack to avoid creating additional strange errors
			    return opNull(opRelease(s, srcOp));
			  }
		  }
	  }
    final IUniquePromise uPromise = UniquenessUtils.getUnique(fieldDecl);
	  if (uPromise != null ||
	      (borrowedPromise != null && !UniquenessRules.isReadOnly(fieldDecl))) {
		  final Integer n = getStackTop(s);

		  if (localStatus(s,n) == State.IMMUTABLE) {
			  // special case: we generate an immutable non-value reference:
			  return opGenerate(opRelease(s, srcOp), srcOp, State.IMMUTABLE, fieldDecl);
		  }
		  
		  Store temp;
      final Set<ImmutableHashOrderSet<Object>> aliases = new HashSet<ImmutableHashOrderSet<Object>>();
		  /* @Borrowed and @Unique fields need to have aliases buried.
		   * Do not bury aliases for @Unique(allowRead=true) fields.
		   */
		  if (uPromise == null || !uPromise.allowRead()) {
  		  final ImmutableSet<ImmutableHashOrderSet<Object>> objects = s.getObjects();
  		  ImmutableHashOrderSet<Object> affected = EMPTY;
  		  final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
  		  for (final FieldTriple t : fieldStore) {
  			  final ImmutableHashOrderSet<Object> object = t.first();
  			  if (object.contains(n) && objects.contains(object) && t.second().equals(fieldDecl)) {
  				  aliases.add(t.third());
  				  affected = affected.union(t.third());
  			  }
  		  }
  		  
  		  sideEffects.recordBuryingFieldRead(srcOp, fieldDecl, affected);
  		  
  		  // Alias Burying: If we get rid of alias burying, we can get rid of this check.
  		  // Otherwise, we cannot: *shared* (say) becomes undefined.
  		  if (nodeStatus(affected) != State.UNIQUE) {
  		    sideEffects.recordLoadOfCompromisedField(srcOp,
  		        uPromise == null ? State.BORROWED :
  		          (uPromise.allowRead() ? State.UNIQUEWRITE : State.UNIQUE),
		          fieldDecl);
  		  }
  
  		  // Remove affected nodes:
  		  //      s = apply(s,new Remove(affected));
  		  // Unfortunately, with combination (a.f = b, b.g = c -> a.* = c), removal of b
  		  // causes NEW *-edges to appear which would increase the affected set (by c).  
  		  // Rather than following this to the logical conclusion (fixed points),
  		  // I leave them in place, but then make stackTop alias existing fields
  
  		  // Alias Burying: make aliases undefined
  		  s = apply(s, srcOp, new Add(State.UNDEFINED, affected));
  
  		  // Allocate new unaliased node on the stack
  		  temp = opNew(s);  
		  } else {
		    temp = opGenerate(s, srcOp, declStatus(fieldDecl),fieldDecl);
		  }
      if (!temp.isValid()) return temp;
		  
		  // first add field edges to this new node.
		  Set<FieldTriple> newFields = new HashSet<FieldTriple>();
		  Integer newN = getStackTop(temp);
		  final ImmutableHashOrderSet<Object> uniqueNode = EMPTY.addElement(newN);
		  for (ImmutableHashOrderSet<Object> obj : temp.getObjects()) {
			  if (obj.contains(n)) {
				  newFields.add(new FieldTriple(obj,fieldDecl,uniqueNode));
			  }
		  }
		  temp = setFieldStore(temp,temp.getFieldStore().union(newFields));
		  
		  // Again, only @Borrowed and @Unique fuss with aliases
		  if (uPromise == null || !uPromise.allowRead()) {
  		  // now that I am no longer removing the old aliases, I must add the following line:
  		  temp = apply(temp, srcOp, new AddAlias(aliases,newN));
		  }
		  return opSet(temp, srcOp, n);
	  } else {
		  return opGenerate(opRelease(s, srcOp), srcOp, declStatus(fieldDecl),fieldDecl);
	  }
  }
  
  /**
   * Check that the local decl give is legal for mutation.
   * @param s store before
   * @param var variable to check
   * @return same store, or error store.
   */
  public Store opCheckMutable(Store s, Object var) {
	  if (!s.isValid()) return s;
	  // check that the object is writable
	  // Needed for "sneaky" writes (flow-sensitive conversion to readonly)
	  
	  State localStatus = localStatus(s,var);
	  if (localStatus == State.BORROWED) return s; // XXX: defer to effects analysis (UNSOUND!)
	  
	  if (!State.lattice.lessEq(localStatus,State.SHARED)) {
     // kludge to permit VALUE objects to be shared:
	    if (isVariableSharable(s, var)) return s;
//		  System.out.println("mutation not legal on this reference: " + var + ": " + localStatus + " in " + toString(s));
		  return errorStore("mutation not legal on this reference");
	  }
	  return s;
  }
  
  public Store opStore(Store s, final IRNode srcOp, final IRNode fieldDecl) {
    if (!s.isValid()) return s;
    s = undefineFromNodes(s, srcOp, getUnderTop(s), Messages.MADE_UNDEFINED_BY_FROM_WRITE);
    if (!s.isValid()) return s;
    // avoid checking assignment of final fields in "Immutable" constructors:
    if (!TypeUtil.isFinal(fieldDecl)) s = opCheckMutable(s,getUnderTop(s));
    if (!s.isValid()) return s;

    final State declStatus = declStatus(fieldDecl);
    
    final Store temp;
    if (UniquenessUtils.isUnique(fieldDecl)) {
    	// added for better/faster error reporting
    	s = opCheckTopState(s,declStatus);
    	if (!s.isValid()) return s;
        final Integer undertop = getUnderTop(s);
        final Integer stacktop = getStackTop(s);
        // We used to undefine everything that aliased the value stored in the unique field.
        // We now simply undefine other field values that point to it, and keep the
        // aliases as if they just read the field.
        // allowRead makes no difference here.
        final ImmutableHashOrderSet<Object> undefinedObject = getPseudoObject(s, State.UNDEFINED);
    	HashSet<FieldTriple> newFields = new HashSet<FieldTriple>();
    	for (FieldTriple t : s.getFieldStore()) {
    		if (fieldDecl.equals(t.second()) && t.first().contains(undertop)) continue; // remove
    		if (t.third().contains(stacktop)) {
    			// old field is compromised
    			newFields.add(new FieldTriple(t.first(),t.second(),undefinedObject));
    		} else {
    			newFields.add(t);
    		}
    	}
    	temp = opRelease(opConnect(setFieldStore(s,new ImmutableHashOrderSet<FieldTriple>(newFields)),
    							   undertop,fieldDecl,stacktop), srcOp);
    } else if (UniquenessUtils.isFieldBorrowed(fieldDecl)) {
    	// only permit if
    	// (1) the field is final
    	// (2) every object aliased to the top includes a borrowed(allowReturn) parameter/receiver
    	// (3) this variable is final
    	// (4) we have effects "writes v:Instance" for that variable (v).
      
      // Used to check that the borrowed field is final, but the sanity checker already does that.

    	// perform remaining checks
    	s = opReturn(s, srcOp, fieldDecl);
      if (!s.isValid()) return s;
    	// if (!UniquenessRules.isReadOnly(fieldDecl)) // even readonly borrowing gets a from
    	{
    	  s = opConnect(s, getStackTop(s), fromField, getUnderTop(s));
    	}
      // Consume the item being assigned to the field as BORROWED
      temp = opConsume(s, srcOp, State.BORROWED);
    } else if (isValueNode(fieldDecl)) {
    	temp = opRelease(s, srcOp); // Java Type system does all we need
    } else if (declStatus == State.SHARED) {
      temp = opConsumeShared(
          s, srcOp, Messages.COMPROMISED_BY_FIELD_ASSIGNMENT,
          VariableDeclaration.getId(fieldDecl));
    } else if (declStatus == State.UNIQUE) {
      temp = opConsumeUnique(s, srcOp);
    } else {
    	temp = opConsume(s, srcOp, declStatus);
    }
    /* Make sure that the object being dereferenced by the field assignment is
     * not undefined.  Can happen if the a @Borrowed parameter is assigned to
     * two or more fields of the object.
     */
    return opConsume(temp, srcOp, State.BORROWED);
  }

  /**
   * Check that the top of the stack is legal to "return" (or store to a borrowed
   * field in a constructor).  Borrowed values must allow borrowing.
   * @param s store before
   * @param destDecl place the value is going (field or return decl)
   * @return same store, or an error store
   */
  public Store opReturn(Store s, final IRNode srcOp, final IRNode destDecl) {
	  // to prevent borrowing something twice, we ensure there is nothing from already:
	  s = opLoadReachable(s, srcOp, null);
	  if (!s.isValid()) return s;
	  final Integer stackTop = getStackTop(s);
	  for (ImmutableHashOrderSet<Object> obj : s.getObjects()) {
		  if (obj.contains(stackTop) && nodeStatus(obj) == State.BORROWED) { //TODO; Change to use contains
			  // we need to find something that allows the return in this object
			  IRNode auth = null;
			  for (Object x : obj) {
				  if (x instanceof IRNode) {
					  IRNode node = (IRNode)x;
					  BorrowedPromiseDrop pd = UniquenessRules.getBorrowed(node);
					  if (pd != null && pd.allowReturn()) {
						  auth = node;
					  }
				  }
			  }
			  if (auth == null) return errorStore("no allowReturn even though returned");
			  // used to check that the @Borrowed(allowReturn=true) parameter is final, but the sanity checker already does that.
			  boolean found = false;
			  // TODO: I think with the new BorrowedReadOnly, we might be able to avoid this looking
			  // around.  Especially if we distinguished USELESS from BORROWEDREADONLY
			  for (Effect f : effects) {
				  if (!UniquenessRules.isReadOnly(destDecl) && !f.isWrite()) continue;
				  Target t = f.getTarget();
				  if (!(t instanceof InstanceTarget)) continue;
				  if (t.getReference() != auth) continue;
				  IRegion region = t.getRegion();
				  if (!region.getModel().isSameRegionAs(RegionModel.getInstanceRegion(auth))) continue;
				  found = true;
			  }
			  if (!found)
				  if (UniquenessRules.isReadOnly(destDecl))
					  return errorStore("need read effect on allowReturn");
				  else
					  return errorStore("need write effect on allowReturn");
		  }
	  }
	  return s;
  }

  /**
   * Return the store after reading everything reachable from the top of the
   * stack. In essence, any variable referring to
   * structure reachable from the top of the stack is made undefined (alias
   * burying). Used to implement read (and write) effects.
   */
  public Store opLoadReachable(Store s, final IRNode srcOp,
      final RegionEffectsPromiseDrop fxDrop) {
    if (!s.isValid()) return s;
    final Integer n = getStackTop(s);
    s = undefineFromNodes(s, srcOp, n, Messages.MADE_UNDEFINED_BY_FROM_METHOD);
    if (!s.isValid()) return s;
    final Set<IRNode> loadedFields = new HashSet<IRNode>();
    final Set<Object> affectedM = new HashSet<Object>();
    final Set<ImmutableHashOrderSet<Object>> found = new HashSet<ImmutableHashOrderSet<Object>>();
    final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
    boolean done;
    do {
      done = true;
      for (final FieldTriple t : fieldStore) {
        final ImmutableHashOrderSet<Object> object = t.first();
        if (object.contains(n) || found.contains(object)) {
          final ImmutableHashOrderSet<Object> newObject = t.third();
          State newStatus = nodeStatus(newObject);
          if (UniquenessUtils.isUniqueWrite(t.second())){
        	  if (!State.lattice.lessEq(newStatus,State.UNIQUEWRITE)) {
              sideEffects.recordIndirectLoadOfCompromisedField(srcOp, State.UNIQUEWRITE, t.second());
        	  }
          } else {
        	  if (newStatus != State.UNIQUE) { 
        	    sideEffects.recordIndirectLoadOfCompromisedField(srcOp, State.UNIQUE, t.second());
        	  }
          }
          if (found.add(newObject)) done = false;
          for (Object v : newObject) {
        	  if (!(v instanceof State)) affectedM.add(v);
          }
          loadedFields.add(t.second());
        }
      }
    } while (!done);
    final ImmutableHashOrderSet<Object> affected =
        new ImmutableHashOrderSet<Object>(affectedM);
    
    sideEffects.recordBuryingMethodEffects(srcOp, loadedFields, affected, fxDrop);
    
    return
        apply(
            apply(s, srcOp, new Remove(affected)), srcOp,
            new Add(State.UNDEFINED, affected));
  }
  
  /** Push the value "null" onto the top of the stack. */
  public Store opNull(final Store s) {
    if (!s.isValid()) return s;
    return push(s);
  }
  
  /**
   * Push an immutable value on the stack.  Useful for Strings, enumerated types etc.
   * @param s store before push
   * @return store after push
   */
  public Store opValue(final Store s, final IRNode srcOp) {
	  if (!s.isValid()) return s;
	  return apply(push(s), srcOp, new Add(VALUE, EMPTY.addElement(1+getStackTop(s))));
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
            	assert UniquenessUtils.isUniqueWrite(t.second()) : "combination didn't remove?";
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
   * @param decl declaration/expression (possibly null) that this generation is for.
   */
  public Store opExisting(final Store s, final IRNode srcOp, final State pv, final IRNode decl) {
    if (!s.isValid()) return s;
    Store temp = push(s);
    final ImmutableHashOrderSet<Object> nset = EMPTY.addElement(getStackTop(temp));
    return join(temp, apply(temp, srcOp, new Add(pv, nset, decl, mayAlias)));
  }

  /**
   * discard the value on the top of the stack from the set of objects and from
   * the field store, and then pop the stack.
   */
  public Store opRelease(final Store s, final IRNode srcOp) {
    if (!s.isValid()) return s;
    return pop(s, srcOp);
  }
  
  /**
   * Compromise the value on the top of the stack.
   */
  public Store opCompromiseNoRelease(final Store s, final IRNode srcOp,
      final int msg, final Object... args) {
    if (!s.isValid()) return s;
    final Integer n = getStackTop(s);
    final State localStatus = localStatus(s, n);
    sideEffects.recordCompromisingOfUnique(
        srcOp,  n, localStatus, s.getFieldStore(), msg, args);
    
	  return opYieldTopState(opCheckTopState(s,State.SHARED), srcOp, State.SHARED);
  }
  
  /**
   * Compromise the value on the top of the stack and then pop it off.
   */
  public Store opCompromise(final Store s, final IRNode srcOp,
      final int msg, final Object... args) {
    return opConsumeShared(s, srcOp, msg, args);
  }

  public Store opGenerate(
      final Store s, final IRNode srcOp, State required, IRNode exprORdecl) {
	  if (!s.isValid()) return s;
	  if (isValueNode(exprORdecl)) return opValue(s, srcOp);
	  switch (required) {
	  case UNDEFINED:
		  throw new FluidError("should not generate undefined things");
		  
	  // TODO: BorrowedReadOnly
		  
	  case BORROWED: 
		  return join(join(opExisting(s, srcOp,State.BORROWED,exprORdecl),
				           opExisting(s, srcOp,State.READONLY,exprORdecl)), // remove for new BORROWED
				      join(opExisting(s, srcOp,State.SHARED,exprORdecl),
				    	   opExisting(s, srcOp,State.UNIQUEWRITE,exprORdecl)));
	  case READONLY: 
		  return join( opExisting(s, srcOp,State.READONLY,exprORdecl),
				  join(opExisting(s, srcOp,State.SHARED,exprORdecl),
					   opExisting(s, srcOp,State.UNIQUEWRITE,exprORdecl)));
		  
	  case IMMUTABLE:
      // TODO: Use the alias-aware constructor of ADD?
		  return apply(push(s), srcOp, new Add(NONVALUE, EMPTY.addElement(getStackTop(s)+1)));
		  
	  case SHARED:
		  return opExisting(s, srcOp,State.SHARED,exprORdecl);
		  
	  case UNIQUEWRITE:
		  Store temp = opNew(s);
		  ImmutableHashOrderSet<Object> newAliases = EMPTY.addElement(State.UNIQUEWRITE).addElement(getStackTop(temp));
		  return join(temp, apply(temp, srcOp, new Replace(mayAlias,exprORdecl,State.READONLY, newAliases)));
		  
	  case UNIQUE:
		  return opNew(s);
		  
	  case NULL: break;
	  }
	  return s;
  }

  /**
   * Return whether the variable 'n' is sharable.  A variable is sharable if
   * every object that it could refer to has a state less than SHARED or is a 
   * VALUE object.
   */
  private boolean isVariableSharable(final Store s, final Object n) {
    boolean sharable = true;
    for (ImmutableHashOrderSet<Object> obj : s.getObjects()) {
      if (obj.contains(n)) {
        if (!obj.contains(VALUE) && 
            !State.lattice.lessEq(nodeStatus(obj), State.SHARED)) {
          sharable = false;
        }
      }
    }
    return sharable;
  }
  
  /**
   * Check that the top element has the required state
   * @param s state before
   * @param required state required
   * @return same state afterwards, or a poisoned state
   */
  public Store opCheckTopState(final Store s, State required) {
	  if (!s.isValid()) return s;
	  final Integer n = getStackTop(s);
	  final State localStatus = localStatus(s, n);
	  if (localStatus == State.IMMUTABLE && required == State.SHARED) {
		  // kludge to permit VALUE objects to be shared:
	    if (isVariableSharable(s, n)) return s;
	  }
	  if (!State.lattice.lessEq(localStatus, required)) {
		  return errorStore("Value flow error.  Required: " + required + ", actual: " + localStatus);
	  }
	  return s;
  }

  /**
   * Yield up access to top element as the given state.
   * @param s state before
   * @param state access being given up
   * @return state after yielding up reference as state
   */
  public Store opYieldTopState(final Store s, final IRNode srcOp, State state) {
	  if (!s.isValid()) return s;
	  final Integer n = getStackTop(s);
	  switch (state) {
	  case UNDEFINED:
	  case BORROWED: 
		  break;
	  case IMMUTABLE:
		  // we assume this is the NON-VALUE case:
		  // the value case is handled by the Java type system.
		  if (localStatus(s,n) == State.IMMUTABLE) return s;
		  ImmutableHashOrderSet<Object> aliases = getAliases(s,n);
		  // add all these aliases to the NONVALUE immutable object
		  Store temp = apply(s, srcOp,new Add(NONVALUE,aliases));
		  // then replace any occurrence of n with the NONVALUE set.
		  // (This will involve replacing the NONVALUE object with itself...)
		  return apply(temp, srcOp, new ReplaceEntire(n,getPseudoObject(temp,NONVALUE)));
	  case READONLY: 
		  return apply(s, srcOp, new Downgrade(n, State.UNIQUEWRITE));
	  case SHARED:
		  return apply(s, srcOp, new Downgrade(n, State.SHARED));
	  case UNIQUEWRITE:
		  return apply(s, srcOp, new Downgrade(n, State.READONLY));
	  case UNIQUE:
		  return apply(s, srcOp, new Downgrade(n, State.UNDEFINED));
	  case NULL: 
		  throw new FluidError("canot yield a non-null value as null!");
	  }
	  return s;
  }
  
  private static interface ConsumeSideEffects {
    public static final ConsumeSideEffects NONE = new ConsumeSideEffects() {
      @Override
      public void produceSideEffects(
          final Store s, final IRNode srcOp, final Integer topOfStack,
          final State localStatus) {
        // do nothing
      }
    };
    
    public void produceSideEffects(
        Store s, IRNode srcOp, Integer topOfStack, State localStatus);
  }
  
  /**
   * Remove the top element of the stack and yielding it up using the state given.
   * @param s store before
   * @param state required state of stack top which will be yielded.
   * @return store after
   */
  public Store opConsume(
      final Store s, final IRNode srcOp, final State state,
      final ConsumeSideEffects cse) {
    if (!s.isValid()) return s;
    final Integer n = getStackTop(s);
    final State localStatus = localStatus(s, n);
    cse.produceSideEffects(s, srcOp, n, localStatus);
    return opRelease(opYieldTopState(opCheckTopState(s,state), srcOp, state), srcOp);
  }
  
  /**
   * Remove the top element of the stack and yielding it up using the state given.
   * @param s store before
   * @param state required state of stack top which will be yielded.  Should 
   * not be SHARED or UNIQUE.  For those use {@link #opConsumeShared} and
   * {@link #opConsumeUnique}, respectively.
   * @return store after
   */
  public Store opConsume(final Store s, final IRNode srcOp, final State state) {
    return opConsume(s, srcOp, state, ConsumeSideEffects.NONE);
  }

  /**
   * Remove the top element of the stack and yielding it up as SHARED
   * @param s store before
   * @return store after
   */
  public Store opConsumeShared(final Store s, final IRNode srcOp,
      final int msg, final Object... args) {
    return opConsume(s, srcOp, State.SHARED,
        new ConsumeSideEffects() {
          @Override
          public void produceSideEffects(
              final Store s, final IRNode srcOp, final Integer topOfStack,
              final State localStatus) {
            sideEffects.recordCompromisingOfUnique(
                srcOp, topOfStack, localStatus, s.getFieldStore(), msg, args);
          }
        });
  }
  
  /**
   * Remove the top element of the stack and yielding it up as UNIQUE.
   * @param s store before
   * @return store after
   */
  public Store opConsumeUnique(final Store s, final IRNode srcOp) {
    return opConsume(s, srcOp, State.UNIQUE,
        new ConsumeSideEffects() {
          @Override
          public void produceSideEffects(
              final Store s, final IRNode srcOp, final Integer topOfStack,
              final State localStatus) {
            sideEffects.recordUndefiningOfUnique(
                srcOp, topOfStack, localStatus, s);
          }
        });
  }
  
  /**
   * Return true if the two variables may be equal to a non-null value.
   * @param s current store
   * @param o1 one variable
   * @param o2 another variable
   * @return whether it is possible they could be equal AND non-null
   */
  public boolean mayAlias(final Store s, final Object o1, final Object o2) {
	  for (ImmutableSet<Object> obj : s.getObjects()) {
		  if (obj.contains(o1) && obj.contains(o2)) return true;
	  }
	  return false;
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

    final List<ImmutableHashOrderSet<Object>> newObjectList = new ArrayList<ImmutableHashOrderSet<Object>>();
    for (ImmutableHashOrderSet<Object> obj : s.getObjects()) {
    	newObjectList.add(c.apply(obj));
    }
    final ImmutableSet<ImmutableHashOrderSet<Object>> newObjects =
      new ImmutableHashOrderSet<ImmutableHashOrderSet<Object>>(newObjectList);
    
    final List<FieldTriple> newFieldList = new ArrayList<FieldTriple>();
    for (FieldTriple t : s.getFieldStore()) {
    	ImmutableHashOrderSet<Object> newSource = c.apply(t.first());
		ImmutableHashOrderSet<Object> newDest = c.apply(t.third());
		if (newDest.isEmpty()) continue; // triple is dropped anyway
		if (newSource.isEmpty() && !t.first().isEmpty() && !UniquenessUtils.isUniqueWrite(t.second())) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("While applying " + c);
				LOG.fine("Found triple about to drop in soup: " + tripleToString(t));
			}
			applyCombine(s,c,newFieldList,t.first(),newDest);
		} else
			newFieldList.add(new FieldTriple(newSource, t.second(), newDest));
    }
    final ImmutableSet<FieldTriple> newFields =
    	new ImmutableHashOrderSet<FieldTriple>(newFieldList);
    
    final Store result = check(canonicalize(newTriple(s.first(),newObjects,newFields),s), srcOp);
    return result;
  }
  
  private void applyCombine(
		  Store s,
		  Apply c, 
		  List<FieldTriple> newFieldList, 
		  ImmutableHashOrderSet<Object> vanishing, 
		  ImmutableHashOrderSet<Object> newDest) {
	  for (FieldTriple p : s.getFieldStore()) {
		  if (p.third().equals(vanishing)) {
			  ImmutableHashOrderSet<Object> newSource = c.apply(p.first());
			  if (newSource.isEmpty()) {
				  // should be very rare
				  LOG.fine("Previous triple also falling into soup: " + tripleToString(p));
				  applyCombine(s,c,newFieldList,p.first(),newDest);
			  } else {
				  FieldTriple fieldTriple = new FieldTriple(newSource,fromField,newDest);
				  if (LOG.isLoggable(Level.FINE)) {
					  LOG.fine("  replacing with " + tripleToString(fieldTriple));
				  }
				  newFieldList.add(fieldTriple);
			  }
		  }
	  }	
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
	  final List<FieldTriple> newFieldList = new ArrayList<FieldTriple>(); 
	  for (final FieldTriple t : fieldStore) {
		  final ImmutableHashOrderSet<Object> from = t.first();
		  final State status = nodeStatus(t.third());
		  if (PSEUDOS.includes(from)) {
			  if (UniquenessUtils.isUniqueWrite(t.second())) {
				  if (status != State.UNIQUEWRITE) {
				    sideEffects.recordLossOfCompromisedField(srcOp, State.UNIQUEWRITE, t.second());
				  }
			  } else if (status != State.UNIQUE){
          sideEffects.recordLossOfCompromisedField(srcOp, State.UNIQUE, t.second());
			  }
			  // otherwise (including if side-effecting), we discard
		  } else {
			  newFieldList.add(t);
		  }
	  }
	  return setFieldStore(s,new ImmutableHashOrderSet<FieldTriple>(newFieldList));
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
    if (!s.invariant()) {
    	throw new AssertionError("Invariant failed.");
    }
    return sb.toString();
  }
  
  public static String tripleToString(final FieldTriple t) {
    IRNode field = t.second();
	return nodeToString(t.first()) + "." +
//      (field == null ? "*" : VariableDeclarator.getId(field)) + " = " +
      (field == null ? "*" : fieldToString(field)) + " = " +
      nodeToString(t.third());
  }
  
  private static String fieldToString(final IRNode fieldDecl) {
    if (VariableDeclarator.prototype.includes(fieldDecl)) {
      return VariableDeclarator.getId(fieldDecl);
    } else { // QualifiedReceiverDecl
      final IRNode base = QualifiedReceiverDeclaration.getBase(fieldDecl);
      if (TypeRef.prototype.includes(base)) {
        return "this[" + TypeRef.getId(base) + "]";
      } else {
        return "this[" + NamedType.getType(base) + "]";
      }
    }
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
  
  
  
//  // ==================================================================
//  // == Manage side effects
//  // ==================================================================
//
//  public UniquenessControlFlowDrop getCFDrop() {
//    return sideEffects.getCFDrop();
//  }
//  
//  public void cancelResults() {
//    sideEffects.cancelResults();
//  }
}
