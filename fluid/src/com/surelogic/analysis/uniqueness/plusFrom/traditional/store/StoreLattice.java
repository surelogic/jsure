package com.surelogic.analysis.uniqueness.plusFrom.traditional.store;

import java.util.ArrayList;
import java.util.Collections;
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
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.CatchClause;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.NamedType;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeRef;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReturnValueDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.BorrowedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.IUniquePromise;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
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
    Store> {
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
  

  // ==================================================================
  // === Constructor 
  // ==================================================================
  
  public StoreLattice(final IRNode[] locals, IBinder b, IMayAlias ma, List<Effect> fx) {
    super(new FlatLattice2<Integer>(),
        new UnionLattice<ImmutableHashOrderSet<Object>>(),
        new UnionLattice<FieldTriple>());
    this.locals = locals;
    binder = b;
    mayAlias = ma;
    effects = fx;
  }
	  
  public StoreLattice(final IRNode[] locals) {
	  this(locals,null,PessimisticMayAlias.INSTANCE,Collections.<Effect>emptyList());
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
  
  private Store pop(Store s) {
    if (!s.isValid()) return s;
    final Integer topOfStack = getStackTop(s);
    final int n = topOfStack.intValue();
    if (n == 0) {
      return errorStore("stack underflow");
    }
    s = apply(s, new Remove(EMPTY.addElement(topOfStack)));
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
//  public State declStatus(final IRNode node) {
//	  if (UniquenessRules.isUnique(node)) {
//		  UniquePromiseDrop drop = UniquenessRules.getUnique(node);
//		  if (drop.allowRead()) return State.UNIQUEWRITE;
//		  return State.UNIQUE;
//	  }
//	  if (UniquenessUtils.isFieldUnique(node)) {
//	    return State.UNIQUE;
//	  }
//	  // TODO: BorrowedReadOnly
//	  if (UniquenessUtils.isFieldBorrowed(node)) {
//	    return State.BORROWED;
//	  }
//	  if (UniquenessRules.isReadOnly(node)) {
//	    return State.READONLY;
//	  }
//	  if (LockRules.isImmutableRef(node)) {
//	    return State.IMMUTABLE;
//	  }
//	  if (isValueNode(node)) {
//	    return State.IMMUTABLE;
//	  }
//	  return State.SHARED;
//  }

  public State declStatus(final IRNode node) {
    final IUniquePromise uDrop = UniquenessUtils.getUnique(node);
    if (uDrop != null) {
      if (uDrop.allowRead()) return State.UNIQUEWRITE;
      return State.UNIQUE;
    }
//    if (UniquenessRules.isUnique(node)) {
//      UniquePromiseDrop drop = UniquenessRules.getUnique(node);
//      if (drop.allowRead()) return State.UNIQUEWRITE;
//      return State.UNIQUE;
//    }
//    if (UniquenessUtils.isFieldUnique(node)) {
//      return State.UNIQUE;
//    }
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
	  /*
	  if (retDecl == null) {
		  System.out.println("No return on "+JavaNames.genQualifiedMethodConstructorName(decl));
	  }
	  */

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

  public Store opStart() {
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
    	  temp = opGenerate(temp,receiverStatus(decl,local),local);
    	  temp = opSet(temp,local);
      } else if (QualifiedReceiverDeclaration.prototype.includes(op) ||
    		  ParameterDeclaration.prototype.includes(op)) {
    	  IRNode parent = JJNode.tree.getParent(local);
    	  if (parent == null || !CatchClause.prototype.includes(JJNode.tree.getOperator(parent))) {
    		  temp = opGenerate(temp,declStatus(local),local);
    		  temp = opSet(temp,local);
    	  }
      }
    }
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
    if (localStatus(s, local) != State.UNDEFINED) {
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

  /** Store the top of the stack into a local. */
  public Store opSetAliasAware(final Store s, final IRNode local) {
    if (!s.isValid()) return s;
    final ImmutableHashOrderSet<Object> lset = EMPTY.addElement(local);
    return pop(
        apply(
            apply(s, new Remove(lset)),
            new Add(getStackTop(s), lset, local, mayAlias)));
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
	  if (!s.isValid()) return s;
	  s = undefineFromNodes(s,getStackTop(s));
	  if (!s.isValid()) return s;
	  // we don't allow reading of 'from' fields except when this object is 
	  // independent, because otherwise, the aliasing rules are too tricky to
	  // figure out.
	  if (UniquenessUtils.isFieldBorrowed(fieldDecl)) {
		  final Integer n = getStackTop(s);
		  for (FieldTriple ft : s.getFieldStore()) {
			  if (ft.third().contains(n)) {
				  return errorStore("can't read borrowed field of object except in methods of class");
			  }
		  }
	  }
    final IUniquePromise uPromise = UniquenessUtils.getUnique(fieldDecl);
	  if (uPromise != null ||
	      (UniquenessUtils.isFieldBorrowed(fieldDecl) && !UniquenessRules.isReadOnly(fieldDecl))) {
		  final Integer n = getStackTop(s);

		  if (localStatus(s,n) == State.IMMUTABLE) {
			  // special case: we generate an immutable non-value reference:
			  return opGenerate(opRelease(s),State.IMMUTABLE,fieldDecl);
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
  		  temp = opNew(s);  
		  } else {
		    temp = opGenerate(s,declStatus(fieldDecl),fieldDecl);
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
  		  temp = apply(temp,new AddAlias(aliases,newN));
		  }
		  return opSet(temp, n);
	  } else {
		  return opGenerate(opRelease(s),declStatus(fieldDecl),fieldDecl);
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
  
  public Store opStore(Store s, final IRNode fieldDecl) {
    if (!s.isValid()) return s;
    s = undefineFromNodes(s,getUnderTop(s));
    if (!s.isValid()) return s;
    // avoid checking assignment of final fields in "Immutable" constructors:
    if (!TypeUtil.isFinal(fieldDecl)) s = opCheckMutable(s,getUnderTop(s));
    if (!s.isValid()) return s;
    final Store temp;
    if (UniquenessUtils.isUnique(fieldDecl)) {
    	// added for better/faster error reporting
    	s = opCheckTopState(s,declStatus(fieldDecl));
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
    							   undertop,fieldDecl,stacktop));
    } else if (UniquenessUtils.isFieldBorrowed(fieldDecl)) {
    	// only permit if
    	// (1) the field is final
    	// (2) every object aliased to the top includes a borrowed(allowReturn) parameter/receiver
    	// (3) this variable is final
    	// (4) we have effects "writes v:Instance" for that variable (v).
      
      // Used to check that the borrowed field is final, but the sanity checker already does that.

    	// perform remaining checks
    	s = opReturn(s, fieldDecl);
      if (!s.isValid()) return s;
    	// if (!UniquenessRules.isReadOnly(fieldDecl)) // even readonly borrowing gets a from
    	{
    			s = opConnect(s, getStackTop(s), fromField, getUnderTop(s));
    	}
    	// Consume the item being assigned to the field as BORROWED
    	temp = opConsume(s, State.BORROWED);
    } else if (isValueNode(fieldDecl)) {
    	temp = opRelease(s); // Java Type system does all we need
    } else {
    	temp = opConsume(s,declStatus(fieldDecl));
    }
    /* Make sure that the object being dereferenced by the field assignment is
     * not undefined.  Can happen if the a @Borrowed parameter is assigned to
     * two or more fields of the object.
     */
    return opConsume(temp, State.BORROWED);
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
  public Store opLoadReachable(Store s) {
    if (!s.isValid()) return s;
    final Integer n = getStackTop(s);
    s = undefineFromNodes(s,n);
    if (!s.isValid()) return s;
    Set<Object> affectedM = new HashSet<Object>();
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
        		  return errorStore("loaded compromised unique(allowRead) field");
        	  }
          } else {
        	  if (newStatus != State.UNIQUE) { 
        		  return errorStore("loaded compromised field");
        	  }
          }
          if (found.add(newObject)) done = false;
          for (Object v : newObject) {
        	  if (!(v instanceof State)) affectedM.add(v);
          }
        }
      }
    } while (!done);
    ImmutableHashOrderSet<Object> affected = new ImmutableHashOrderSet<Object>(affectedM);
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
   * Push an immutable value on the stack.  Useful for Strings, enumerated types etc.
   * @param s store before push
   * @return store after push
   */
  public Store opValue(final Store s) {
	  if (!s.isValid()) return s;
	  return apply(push(s),new Add(VALUE, EMPTY.addElement(1+getStackTop(s))));
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
  public Store opExisting(final Store s, final State pv, final IRNode decl) {
    if (!s.isValid()) return s;
    Store temp = push(s);
    final ImmutableHashOrderSet<Object> nset = EMPTY.addElement(getStackTop(temp));
    return join(temp, apply(temp, new Add(pv, nset, decl, mayAlias)));
  }

  /**
   * discard the value on the top of the stack from the set of objects and from
   * the field store, and then pop the stack.
   */
  public Store opRelease(final Store s) {
    if (!s.isValid()) return s;
    return pop(s);
  }
  
  /**
   * Compromise the value on the top of the stack.
   */
  public Store opCompromiseNoRelease(final Store s) {
	  return opYieldTopState(opCheckTopState(s,State.SHARED),State.SHARED);
  }
  
  /**
   * Compromise the value on the top of the stack and then pop it off.
   */
  public Store opCompromise(final Store s) {
    return opConsume(s,State.SHARED);
  }

  public Store opGenerate(final Store s, State required, IRNode exprORdecl) {
	  if (!s.isValid()) return s;
	  if (isValueNode(exprORdecl)) return opValue(s);
	  switch (required) {
	  case UNDEFINED:
		  throw new FluidError("should not generate undefined things");
		  
	  // TODO: BorrowedReadOnly
		  
	  case BORROWED: 
		  return join(join(opExisting(s,State.BORROWED,exprORdecl),
				           opExisting(s,State.READONLY,exprORdecl)), // remove for new BORROWED
				      join(opExisting(s,State.SHARED,exprORdecl),
				    	   opExisting(s,State.UNIQUEWRITE,exprORdecl)));
	  case READONLY: 
		  return join( opExisting(s,State.READONLY,exprORdecl),
				  join(opExisting(s,State.SHARED,exprORdecl),
					   opExisting(s,State.UNIQUEWRITE,exprORdecl)));
		  
	  case IMMUTABLE:
	    // TODO: Use the alias-aware constructor of ADD?
		  return apply(push(s), new Add(NONVALUE, EMPTY.addElement(getStackTop(s)+1)));
		  
	  case SHARED:
		  return opExisting(s,State.SHARED,exprORdecl);
		  
	  case UNIQUEWRITE:
		  Store temp = opNew(s);
		  ImmutableHashOrderSet<Object> newAliases = EMPTY.addElement(State.UNIQUEWRITE).addElement(getStackTop(temp));
		  return join(temp, apply(temp, new Replace(mayAlias,exprORdecl,State.READONLY, newAliases)));
		  
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
  public Store opYieldTopState(final Store s, State state) {
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
		  Store temp = apply(s,new Add(NONVALUE,aliases));
		  // then replace any occurrence of n with the NONVALUE set.
		  // (This will involve replacing the NONVALUE object with itself...)
		  return apply(temp, new ReplaceEntire(n,getPseudoObject(temp,NONVALUE)));
	  case READONLY: 
		  return apply(s, new Downgrade(n, State.UNIQUEWRITE));
	  case SHARED:
		  return apply(s, new Downgrade(n, State.SHARED));
	  case UNIQUEWRITE:
		  return apply(s, new Downgrade(n, State.READONLY));
	  case UNIQUE:
		  return apply(s, new Downgrade(n, State.UNDEFINED));
	  case NULL: 
		  throw new FluidError("canot yield a non-null value as null!");
	  }
	  return s;
  }
  
  /**
   * Remove the top element of the stack and yielding it up using the state given.
   * @param s store before
   * @param state required state of stack top which will be yielded.
   * @return store after
   */
  public Store opConsume(final Store s, State state) {
	  return opRelease(opYieldTopState(opCheckTopState(s,state),state));
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
    
    final Store result = check(canonicalize(newTriple(s.first(),newObjects,newFields),s));
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
	  final List<FieldTriple> newFieldList = new ArrayList<FieldTriple>(); 
	  for (final FieldTriple t : fieldStore) {
		  final ImmutableHashOrderSet<Object> from = t.first();
		  final State status = nodeStatus(t.third());
		  if (PSEUDOS.includes(from)) {
			  if (UniquenessUtils.isUniqueWrite(t.second())) {
				  if (status != State.UNIQUEWRITE) {
//					  System.out.println("Lost compromised unique(allowRead) field for " + toString(s));
					  return errorStore("compromised unique(allowRead) field has been lost");
				  }
			  } else if (status != State.UNIQUE){
//				  System.out.println("Lost compromised field error for " + toString(s));
				  return errorStore("compromised field has been lost");
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
}
