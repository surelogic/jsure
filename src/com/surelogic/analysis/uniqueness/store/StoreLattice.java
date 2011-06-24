package com.surelogic.analysis.uniqueness.store;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.PessimisticMayAlias;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.BorrowedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.FilterIterator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableSet;
import edu.cmu.cs.fluid.util.IteratorUtil;
import edu.cmu.cs.fluid.util.SimpleRemovelessIterator;
import edu.uwm.cs.fluid.util.FlatLattice2;
import edu.uwm.cs.fluid.util.FlatLattice2.Element;
import edu.uwm.cs.fluid.util.TripleLattice;
import edu.uwm.cs.fluid.util.UnionLattice;

public final class StoreLattice
extends TripleLattice<Element<Integer>,
    ImmutableSet<ImmutableHashOrderSet<Object>>,
    ImmutableSet<FieldTriple>,
    Store> {
  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis.unique");
    
  private static final ImmutableHashOrderSet<Object> EMPTY =
    ImmutableHashOrderSet.<Object>emptySet();

  private static final ImmutableHashOrderSet<Object> PSEUDOS =
    EMPTY.
      addElement(State.UNDEFINED).
      addElement(State.BORROWED).
      addElement(State.SHARED);

  private final IRNode[] locals;

  private final IMayAlias mayAlias;
  private final Set<Effect> effects;
  

  // ==================================================================
  // === Constructor 
  // ==================================================================
  
  public StoreLattice(final IRNode[] locals, IMayAlias ma, Set<Effect> fx) {
    super(new FlatLattice2<Integer>(),
        new UnionLattice<ImmutableHashOrderSet<Object>>(),
        new UnionLattice<FieldTriple>());
    this.locals = locals;
    mayAlias = ma;
    effects = fx;
  }
	  
  public StoreLattice(final IRNode[] locals) {
	  this(locals,PessimisticMayAlias.INSTANCE,ImmutableHashOrderSet.<Effect>emptySet());
  }
	  
  public int getNumLocals() {
    return locals.length;
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

  
  
  // ==================================================================
  // === Basic operations 
  // ==================================================================

  public Store push(final Store s) {
    if (!s.isValid()) return s;
    final int n = s.getStackSize();
    return setStackSize(s, n+1);
  }
  
  public Store pop(final Store s) {
    if (!s.isValid()) return s;
    final Integer topOfStack = getStackTop(s);
    final int n = topOfStack.intValue();
    if (n == 0) {
      return errorStore("stack underflow");
    }
    return setStackSize(
        apply(s, new Remove(EMPTY.addElement(topOfStack))),
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
   * Get the declared (by annotation) status of the given node.
   * This method is used for Value Flow Analysis
   * @param node parameter, receiver or field
   * @return annotation converted into state
   */
  public State declStatus(final IRNode node) {
	  if (UniquenessRules.isUnique(node)) return State.UNIQUE;
	  if (UniquenessRules.isBorrowed(node)) return State.BORROWED;
	  // if (UniquenessRules.isReadOnly(node)) return State.READONLY;
	  // if (LockRules.isImmutableType(node)) return State.IMMUTABLE;
	  return State.SHARED;
  }

  /**
   * return whether a local must be null or a primitive value. Actually the name
   * is a misnomer. It probably should be isNoObject() or something to that
   * effect.
   */
  public boolean isNull(final Store s, final Object local) {
    return localStatus(s, local) == State.NULL;
  }

  /** Return whether a local or stack location is unique. */
  public boolean isUnique(final Store s, final Object local) {
    return State.lattice.lessEq(localStatus(s, local),State.UNIQUE);
  }

  /** Return whether a local or stack location is defined and not borrowed. */
  public boolean isStoreable(final Store s, final Object local) {
    return State.lattice.lessEq(localStatus(s, local),State.SHARED);
  }

  /** Return whether local or stack location is defined. */
  public boolean isDefined(final Store s, final Object local) {
    return State.lattice.lessEq(localStatus(s, local),State.BORROWED);
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

  public Store opStart() {
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
      addElement( EMPTY.addElement(State.SHARED) );
    temp = setObjects(temp, objects);
    
    /* Now add each parameter or local in turn.  Currently undefined locals are
     * held back until the end, when they are made undefined (or actually, removed altogether)
     */
    ImmutableHashOrderSet<Object> undefinedLocals = EMPTY;
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
      IRNode parent = JJNode.tree.getParent(local);
      if (ReceiverDeclaration.prototype.includes(op) ||
          QualifiedReceiverDeclaration.prototype.includes(op) ||
          ParameterDeclaration.prototype.includes(op) && 
          (parent == null || !CatchClause.prototype.includes(JJNode.tree.getOperator(parent)))) {
        if (isReceiverFromUniqueReturningConstructor
            || UniquenessRules.isBorrowed(local)) {
        	// TODO we don't need to pull in shared if we have a constructor
          temp = join(opExistingBetter(temp, State.BORROWED, mayAlias, local),
        		      opExistingBetter(temp, State.SHARED, mayAlias, local));
//          temp = opExisting(temp, State.BORROWED);
        } else if (UniquenessRules.isUnique(local)) {
          temp = opNew(temp);
        } else {
        	temp = opExistingBetter(temp, State.SHARED, mayAlias, local);
//          temp = opExisting(temp, State.SHARED);
        }
        temp = pop(apply(temp, new Add(getStackTop(temp), EMPTY.addElement(local))));
      } else {
        undefinedLocals = undefinedLocals.addElement(local);
      }
    }
    // NB: There's no need to make them undefined since they are out of scope
    // We can assume variables are not used before they are in scope/defined.
    // temp = apply(temp, new Add(State.UNDEFINED, undefinedLocals));
    return temp;
  }

  /**
   * Leave scope of method. Remove all local bindings.
   */
  public Store opStop(final Store s) {
    return opClear(s,(Object[])locals);
  }

  /**
   * Fetch the value of a local onto stack.
   **/
  public Store opGet(final Store s, final Object local) {
    if (!s.isValid()) return s;
    if (isDefined(s, local)) {
      Store temp = push(s);
      return apply(temp, new Add(local, EMPTY.addElement(getStackTop(temp))));
    } else {
      final String name = (local instanceof IRNode) ? DebugUnparser
          .toString((IRNode) local) : local.toString();
      return errorStore("read undefined local: " + name);
    }
  }

  /**
   * Remove bindings of all variables listed.
   * @param s store before
   * @param vars variables to assign to null
   * @return store after
   */
  public Store opClear(final Store s, final Object... vars) {
	  if (!s.isValid()) return s;
	  return apply(s, new Remove(new ImmutableHashOrderSet<Object>(vars)));
  }

  /**
   * Special case of {@link #opGet} for the receiver.
   */
  public Store opThis(final Store s) {
    if (!s.isValid()) return s;
    if (locals == null) {
      return errorStore("no 'this' (or anything else) in scope");
    }
    for (final IRNode l : locals) {
      if (ReceiverDeclaration.prototype.includes(l)) {
        return opGet(s, l);
      }
    }
    return errorStore("no 'this' in scope");
  }
  
  /**
   * Duplicate a stack value from further down stack
   * 
   * @param fromTop
   *          0 for duplicate top, 1 for under top etc.
   */
  public Store opDup(final Store s, final int fromTop) {
    if (!s.isValid()) return s;
    final Integer i = Integer.valueOf(getStackTop(s).intValue() - fromTop);
    return opGet(s, i);
  }
  
  /** Store the top of the stack into a local. */
  public Store opSet(final Store s, final Object local) {
    if (!s.isValid()) return s;
    final ImmutableHashOrderSet<Object> lset = EMPTY.addElement(local);
    return pop(
        apply(
            apply(s, new Remove(lset)),
            new Add(getStackTop(s), lset)));
  }

  private static final IRNode fromField = null; // new EnumeratedIRNode<FieldKind>(FieldKind.FROM_FIELD);

  /**
   * Add a from-edge between {@link from} to {@link to}.
   * (We add the edge between every node that "from" aliases to every node
   * that "to" aliases.)
   * @param s store before
   * @param from variable source of edge
   * @param to variable destination of edge
   * @return new store.
   */
  public Store opFrom(Store s, Object from, Object to) {
	  List<ImmutableHashOrderSet<Object>> sources = new ArrayList<ImmutableHashOrderSet<Object>>();
	  List<ImmutableHashOrderSet<Object>> destinations = new ArrayList<ImmutableHashOrderSet<Object>>();
	  for (ImmutableHashOrderSet<Object> obj : s.getObjects()) {
		  if (obj.contains(from)) sources.add(obj);
		  if (obj.contains(to)) destinations.add(obj);
	  }
	  List<FieldTriple> newFields = new ArrayList<FieldTriple>();
	  for (ImmutableHashOrderSet<Object> source : sources) {
		  for (ImmutableHashOrderSet<Object> dest : destinations) {
			  newFields.add(new FieldTriple(source,fromField,dest));
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
			  default: break;
			  case UNIQUE:
				  if (LOG.isLoggable(Level.FINE)) {
					  LOG.fine("Removing unique: " + tripleToString(tr));
				  }
				  it.remove();
				  break;
			  case SHARED: // not sure here, but for monotonicity, should be done
			  case BORROWED:
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

  private Store undefineFromNodes(final Store s, Integer n) {
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
      return apply(apply(s, new Remove(affected)),new Add(State.UNDEFINED, affected));
  }
  
  /**
   * Load a field from memory
   * 
   * @precondition isValid();
   */
  public Store opLoad(Store s, final IRNode fieldDecl) {
	  // System.out.println("opLoad(" + DebugUnparser.toString(fieldDecl) + ": store " + toString(s));
	  if (!s.isValid()) return s;
	  s = undefineFromNodes(s,getStackTop(s));
	  if (!s.isValid()) return s;
	  if (UniquenessUtils.isFieldUnique(fieldDecl) ||
			  UniquenessRules.isBorrowed(fieldDecl) && !UniquenessRules.isReadOnly(fieldDecl)) {
		  final Integer n = getStackTop(s);
		  final ImmutableSet<ImmutableHashOrderSet<Object>> objects = s.getObjects();
		  ImmutableHashOrderSet<Object> affected = EMPTY;
		  final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
		  final Set<ImmutableHashOrderSet<Object>> aliases = new HashSet<ImmutableHashOrderSet<Object>>();
		  for (final FieldTriple t : fieldStore) {
			  final ImmutableHashOrderSet<Object> object = t.first();
			  if (object.contains(n) && objects.contains(object) && t.second().equals(fieldDecl)) {
				  aliases.add(t.third());
				  affected = affected.union(t.third());
			  }
		  }
		  // Alias Burying: If we get rid of alias burying, we can get rid of this check.
		  // Otherwise, we cannot: *shared* (say) becomes undefined.
		  if (nodeStatus(affected) != State.UNIQUE) {
			  return errorStore("loaded compromised field");
		  }

		  // Remove affected nodes:
		  //      s = apply(s,new Remove(affected));
		  // Unfortunately, with combination (a.f = b, b.g = c -> a.* = c), removal of b
		  // causes NEW *-edges to appear which would increase the affected set (by c).  
		  // Rather than following this to the logical conclusion (fixed points),
		  // I leave them in place, but then make stackTop alias existing fields

		  // Alias Burying: make aliases undefined
		  s = apply(s,new Add(State.UNDEFINED, affected));

		  // Allocate new unaliased node on the stack
		  Store temp = opNew(s);
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
		  
		  // now that I am no longer removing the old aliases, I must add the following line:
		  temp = apply(temp,new AddAlias(aliases,newN));
		  return opSet(temp, n);
	  } else if (UniquenessRules.isBorrowed(fieldDecl)) {
		  assert (UniquenessRules.isReadOnly(fieldDecl));
		  // not shared, because it won't let us pass in mutable state in parallel with the iterator
		  return opExistingBetter(opRelease(s), State.BORROWED, mayAlias, fieldDecl);
	  } else {
		  return opExistingBetter(opRelease(s), State.SHARED, mayAlias, fieldDecl);
	  }
  }
  
  public Store opStore(Store s, final IRNode fieldDecl) {
    if (!s.isValid()) return s;
    s = undefineFromNodes(s,getUnderTop(s));
    if (!s.isValid()) return s;
    final Store temp;
    if (UniquenessUtils.isFieldUnique(fieldDecl)) {
      ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
      final Integer undertop = getUnderTop(s);
      for (final FieldTriple t : fieldStore) {
        if (fieldDecl.equals(t.second())) {
          final ImmutableHashOrderSet<Object> object = t.first();
          if (TypeUtil.isStatic(fieldDecl) || object.contains(undertop)) {
            fieldStore = removeElement(fieldStore, t);
          }
        }        
      }
      temp = opUndefine(setFieldStore(s, fieldStore));
    } else if (UniquenessRules.isBorrowed(fieldDecl)) {
    	// only permit if
    	// (1) the field is final
    	// (2) every object aliased to the top includes a borrowed(allowReturn) parameter/receiver
    	// (3) this variable is final
    	// (4) we have effects "writes v:Instance" for that variable (v).
    	if (!isFinalField(fieldDecl)) return errorStore("borrowed field must be final");
    	// perform remaining checks
    	s = opReturn(s, fieldDecl);
    	// if (!UniquenessRules.isReadOnly(fieldDecl)) // even readonly borrowing gets a from
    	{
    			s = opFrom(s,getStackTop(s),getUnderTop(s));
    	}
    	temp = opBorrow(s);
    } else {
      temp = opCompromise(s);
    }
    return opRelease(temp);
  }

  /**
   * Check that the top of the stack is legal to "return" (or store to a borrowed
   * field in a constructor).  Borrowed values must allow borrowing.
   * @param s store before
   * @param destDecl place the value is going (field or return decl)
   * @return same store, or an error store
   */
  public Store opReturn(Store s, final IRNode destDecl) {
	  // to prevent borrowing something twice, we ensure there is nothing from already:
	  s = opLoadReachable(s);
	  if (!s.isValid()) return s;
	  final Integer stackTop = getStackTop(s);
	  for (ImmutableHashOrderSet<Object> obj : s.getObjects()) {
		  if (obj.contains(stackTop) && nodeStatus(obj) == State.BORROWED) {
			  // System.out.println(DebugUnparser.toString(destDecl) + " is readonly? " + UniquenessRules.isReadOnly(destDecl));
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
			  if (!isFinalParam(auth)) 
				  return errorStore("allowReturn must be final");
			  boolean found = false;
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

  private boolean isFinalParam(IRNode auth) {
	  Operator op = JJNode.tree.getOperator(auth);
	  return JavaNode.getModifier(auth, JavaNode.FINAL) ||
	  (op instanceof ReceiverDeclaration) ||
	  (op instanceof QualifiedReceiverDeclaration);
  }

  private boolean isFinalField(IRNode fdecl) {
	  return JavaNode.getModifier(JJNode.tree.getParent(JJNode.tree.getParent(fdecl)),JavaNode.FINAL);
  }

  /**
   * Return the store after reading everything reachable from the top of the
   * stack. In essence, any variable referring to
   * structure reachable from the top of the stack is made undefined (alias
   * burying). Used to implement read (and write) effects.
   */
  public Store opLoadReachable(Store s) {
    if (!s.isValid()) return s;
    final Integer n = getStackTop(s);
    s = undefineFromNodes(s,n);
    if (!s.isValid()) return s;
    ImmutableHashOrderSet<Object> affected = EMPTY;
    final Set<ImmutableHashOrderSet<Object>> found = new HashSet<ImmutableHashOrderSet<Object>>();
    final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
    boolean done;
    do {
      done = true;
      for (final FieldTriple t : fieldStore) {
        final ImmutableHashOrderSet<Object> object = t.first();
        if (object.contains(n) || found.contains(object)) {
          final ImmutableHashOrderSet<Object> newObject = t.third();
          if (found.add(newObject)) done = false;
          affected = affected.union(newObject);
        }
      }
    } while (!done);
    if (nodeStatus(affected) != State.UNIQUE) {
      return errorStore("loaded compromised field");
    }
    return
        apply(
            apply(s, new Remove(affected)),
            new Add(State.UNDEFINED, affected));
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
            	LOG.severe("!! Found edge for {}, should not happen after combination!");
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
  public Store opExisting(final Store s, final State pv) {
    if (!s.isValid()) return s;
    Store temp = push(s);
    final ImmutableHashOrderSet<Object> nset = EMPTY.addElement(getStackTop(temp));
    return join(temp, apply(temp, new Add(pv, nset)));
  }

  public Store opExistingBetter(final Store s, final State pv, final IMayAlias mayAlias, final IRNode decl) {
    if (!s.isValid()) return s;
    Store temp = push(s);
    final ImmutableHashOrderSet<Object> nset = EMPTY.addElement(getStackTop(temp));
    return join(temp, apply(temp, new AddBetter(mayAlias, decl, pv, nset)));
  }

  
  /**
   * discard the value on the top of the stack from the set of objects and from
   * the field store, and then pop the stack.
   */
  public Store opRelease(final Store s) {
    if (!s.isValid()) return s;
    return pop(apply(s, new Remove(EMPTY.addElement(getStackTop(s)))));
  }

  /**
   * Ensure the top of the stack is at least borrowed and then pop the stack.
   */
  public Store opBorrow(final Store s) {
    if (!s.isValid()) return s;
    if (!isDefined(s, getStackTop(s))) { // cannot be undefined
      return errorStore("Undefined value on stack borrowed");
    }
    return opRelease(s);
  }
  
  /**
   * Compromise the value on the top of the stack.
   */
  public Store opCompromiseNoRelease(final Store s) {
    if (!s.isValid()) return s;
    final Integer n = getStackTop(s);
    final State localStatus = localStatus(s, n);
    // the first check here is redundant
    if (!State.lattice.lessEq(localStatus,State.BORROWED)) { // cannot be shared
      return errorStore("Undefined value on stack shared");
    }
    if (!State.lattice.lessEq(localStatus,State.SHARED)) { // cannot be shared
      return errorStore("unshareable value on stack shared");
    }
    return apply(s, new Add(n, EMPTY.addElement(State.SHARED)));
  }
  
  /**
   * Compromise the value on the top of the stack and then pop it off.
   */
  public Store opCompromise(final Store s) {
    return opRelease(opCompromiseNoRelease(s));
  }
  
  /**
   * Make the top of the stack undefined and then pop it. The value is being
   * requested as a unique object and no one is allowed to reference it *ever
   * again*
   */
  public Store opUndefine(final Store s) {
    if (!s.isValid()) return s;
    final Integer n = getStackTop(s);
    final State localStatus = localStatus(s, n);
    // the first two checks are technically redundant
    if (!State.lattice.lessEq(localStatus,State.BORROWED)) { // cannot be undefined
      return errorStore("Undefined value on stack not unique");
    }
    if (!State.lattice.lessEq(localStatus,State.SHARED)) { // cannot be borrowed
      return errorStore("Borowed value on stack not unique");
    }
    if (!State.lattice.lessEq(localStatus,State.UNIQUE)) { // cannot be shared
      return errorStore("Shared value on stack not unique");
    }
    return opRelease(apply(s, new Add(n, EMPTY.addElement(State.UNDEFINED))));
  }
  
  public Store opCheckTopState(final Store s, State required) {
	  if (!s.isValid()) return s;
	  final Integer n = getStackTop(s);
	  final State localStatus = localStatus(s, n);
	  if (!State.lattice.lessEq(localStatus, required)) 
		  return errorStore("Value flow error.  Required: " + required + ", actual: " + localStatus);
	  // TODO: Complete this method
	  return s;
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
  public Store opEqual(final Store s, final boolean areEqual) {
    if (!s.isValid()) return s;
    return opRelease(
        opRelease(
            filter(s, new Equal(getStackTop(s), getUnderTop(s), areEqual))));
  }



  // ==================================================================
  // === Generic operations 
  // ==================================================================

  /** Apply a node-set changing operation to the state */
  protected Store applyOLD(final Store s, final Apply c) {
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
    
    return check(setFieldStore(setObjects(s, newObjects), newFieldStore));
  }
  
  /** Apply a node-set changing operation to the state */
  protected Store apply(final Store s, final Apply c) {
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
		if (newDest.isEmpty()) continue; // triple is dropped
		if (newSource.isEmpty() && !t.first().isEmpty()) {
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
    
    final Store result = check(canonicalize(newTriple(s.first(),newObjects,newFields),s));
    // System.out.println("After apply " + c + ": " + toString(result));
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
  protected Store filter(final Store s, final Filter f) {
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
    
    return check(setFieldStore(setObjects(s, newObjects), newFieldStore));
  }

  /**
   * Check that there are no compromised fields on nodes known only through
   * pseudo-variables.
   */
  protected Store check(final Store s) {
    if (!s.isValid()) return s;
    
    final ImmutableSet<FieldTriple> fieldStore = s.getFieldStore();
    if (fieldStore.isInfinite()) return s;
    for (final FieldTriple t : fieldStore) {
      final ImmutableHashOrderSet<Object> from = t.first();
      if (PSEUDOS.includes(from) && nodeStatus(t.third()) != State.UNIQUE) {
    	  LOG.fine("Lost compromised field error for " + toString(s));
        return errorStore("compromised field has been lost");
      }
    }
    return s;
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
    IRNode field = t.second();
	return nodeToString(t.first()) + "." +
      (field == null ? "*" : VariableDeclarator.getId(field)) + " = " +
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
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static <X> ImmutableHashOrderSet<X> removeElement(
      final ImmutableSet<X> s, final X o) {
    return ((ImmutableHashOrderSet) s).removeElement(o);
  }
}
